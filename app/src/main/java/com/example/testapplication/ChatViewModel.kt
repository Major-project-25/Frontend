package com.example.testapplication

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit // For local ID

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val moderationWarning: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(RetrofitInstance.api)

    private val ktorWebSocketService = KtorWebSocketService

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val appContext = application.applicationContext

    private var currentChatFriendId: UUID? = null
    private var currentChatUserId: UUID? = null

    // Helper to generate a reliable local timestamp: HH:mm
    private fun getLocalTimestamp(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Calendar.getInstance().time)
    }

    fun loadChat(currentUserId: UUID, friendId: UUID) {
        currentChatUserId = currentUserId
        currentChatFriendId = friendId

        ktorWebSocketService.setCurrentActiveChat(friendId)
        ktorWebSocketService.clearUnreadCountFor(friendId)

        ktorWebSocketService.messages
            .onEach { messageResponse ->
                val uiMessage = messageResponse.toUiMessage(currentUserId)

                if (messageResponse.senderId == currentUserId && messageResponse.receiverId == friendId) {
                    _uiState.update { currentState ->
                        val newList = currentState.messages.filterNot {
                            it.status == MessageStatus.SENDING && it.text == uiMessage.text
                        }
                        currentState.copy(messages = newList + uiMessage, moderationWarning = null)
                    }
                }
                else if (messageResponse.senderId == friendId && messageResponse.receiverId == currentUserId) {
                    _uiState.update { currentState ->
                        currentState.copy(messages = currentState.messages + uiMessage, moderationWarning = null)
                    }
                }
            }
            .launchIn(viewModelScope)

        ktorWebSocketService.warnings
            .onEach { warning ->
                _uiState.update { currentState ->
                    currentState.copy(moderationWarning = warning.message)
                }
            }
            .launchIn(viewModelScope)

        ktorWebSocketService.deletionEvents
            .onEach { messageIdToDelete ->
                _uiState.update { currentState ->
                    currentState.copy(messages = currentState.messages.filter { it.id != messageIdToDelete })
                }
            }
            .launchIn(viewModelScope)

        fetchHistory(currentUserId, friendId)
    }

    private fun fetchHistory(currentUserId: UUID, friendId: UUID) {
        userRepository.getConversationHistory(currentUserId, friendId).enqueue(object : Callback<List<MessageResponse>> {
            override fun onResponse(call: Call<List<MessageResponse>>, response: Response<List<MessageResponse>>) {
                if (response.isSuccessful) {
                    val messageResponses = response.body() ?: emptyList()
                    val uiMessages = messageResponses.map { it.toUiMessage(currentUserId) }
                    _uiState.update { it.copy(messages = uiMessages, isLoading = false, moderationWarning = null) }
                } else {
                    _uiState.update { it.copy(error = "Failed to load history.", isLoading = false) }
                }
            }
            override fun onFailure(call: Call<List<MessageResponse>>, t: Throwable) {
                Log.e("ChatViewModel", "Failed to fetch chat history", t)
                _uiState.update { it.copy(error = "Failed to load history.", isLoading = false) }
            }
        })
    }

    fun sendMediaMessage(currentUserId: UUID, receiverId: UUID, fileUri: Uri, mimeType: String, messageType: String) {
        _uiState.update { it.copy(moderationWarning = null) }

        val file: File = appContext.contentResolver.getFile(appContext, fileUri)
        val fileName = file.name

        val determinedMessageType = when {
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            mimeType.startsWith("audio/") -> "audio"
            else -> "file"
        }

        val localMessageId = System.currentTimeMillis()

        val tempMessage = Message(
            id = localMessageId,
            text = fileName,
            author = MessageAuthor.ME,
            authorId = currentUserId,
            timestamp = getLocalTimestamp(),
            mediaUrl = null,
            messageType = determinedMessageType,
            status = MessageStatus.SENDING
        )

        _uiState.update { it.copy(messages = it.messages + tempMessage) }

        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

        Log.d("ChatViewModel", "Starting optimistic media upload for: $fileName")

        userRepository.uploadMediaFile(filePart).enqueue(object : Callback<MediaUploadResponse> {
            override fun onResponse(call: Call<MediaUploadResponse>, response: Response<MediaUploadResponse>) {
                val mediaUrl = response.body()?.mediaUrl
                if (response.isSuccessful && mediaUrl != null) {
                    Log.d("ChatViewModel", "Optimistic upload SUCCESS. Sending WebSocket message.")
                    viewModelScope.launch {
                        val messageToSend = MessageCreate(
                            receiverId = receiverId,
                            content = fileName,
                            mediaUrl = mediaUrl,
                            messageType = determinedMessageType
                        )
                        ktorWebSocketService.sendMessage(messageToSend)
                    }
                } else {
                    // --- THIS IS THE FIRST FIX ---
                    Log.e("ChatViewModel", "Optimistic upload FAILED in onResponse: ${response.code()}")
                    _uiState.update { currentState ->
                        // Find the index of the message
                        val messageIndex = currentState.messages.indexOfFirst { it.id == localMessageId }
                        if (messageIndex == -1) {
                            return@update currentState // Message not found, do nothing
                        }
                        // Create an updated message with FAILED status
                        val updatedMessage = currentState.messages[messageIndex].copy(status = MessageStatus.FAILED)

                        // Create a new list and set the updated message at the index
                        val newList = currentState.messages.toMutableList().apply {
                            this[messageIndex] = updatedMessage
                        }
                        currentState.copy(messages = newList) // Return new state with new list
                    }
                    // --- END OF FIX ---
                }
            }

            override fun onFailure(call: Call<MediaUploadResponse>, t: Throwable) {
                // --- THIS IS THE SECOND FIX ---
                Log.e("ChatViewModel", "Optimistic upload FAILED in onFailure: ${t.message}")
                _uiState.update { currentState ->
                    // Find the index of the message
                    val messageIndex = currentState.messages.indexOfFirst { it.id == localMessageId }
                    if (messageIndex == -1) {
                        return@update currentState // Message not found, do nothing
                    }
                    // Create an updated message with FAILED status
                    val updatedMessage = currentState.messages[messageIndex].copy(status = MessageStatus.FAILED)

                    // Create a new list and set the updated message at the index
                    val newList = currentState.messages.toMutableList().apply {
                        this[messageIndex] = updatedMessage
                    }
                    currentState.copy(messages = newList) // Return new state with new list
                }
                // --- END OF FIX ---
            }
        })
    }

    fun sendMessage(receiverId: UUID, content: String, currentUserId: UUID) {
        _uiState.update { it.copy(moderationWarning = null) }

        viewModelScope.launch {
            val messageToSend = MessageCreate(
                receiverId = receiverId,
                content = content,
                mediaUrl = null,
                messageType = "text"
            )
            ktorWebSocketService.sendMessage(messageToSend)
        }
    }

    fun deleteMessage(messageId: Long, currentUserId: UUID) {
        _uiState.update { currentState ->
            currentState.copy(messages = currentState.messages.filter { it.id != messageId })
        }

        userRepository.deleteChatMessage(messageId, currentUserId).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (!response.isSuccessful) {
                    Log.e("ChatViewModel", "Failed to delete message: ${response.code()}")
                    currentChatUserId?.let { userId ->
                        currentChatFriendId?.let { friendId ->
                            fetchHistory(userId, friendId)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e("ChatViewModel", "Network error on delete message", t)
            }
        })
    }


    override fun onCleared() {
        super.onCleared()
        ktorWebSocketService.setCurrentActiveChat(null)
    }
}

private fun MessageResponse.toUiMessage(currentUserId: UUID): Message {
    val author = if (this.senderId == currentUserId) MessageAuthor.ME else MessageAuthor.THEM

    val contentText = this.content

    val mediaFullPath = if (this.mediaUrl != null) {
        RetrofitInstance.BASE_URL.dropLast(1) + this.mediaUrl
    } else {
        null
    }
    return Message(
        id = this.id,
        text = contentText,
        author = author,
        authorId = this.senderId,
        timestamp = this.timestamp.substring(11, 16),
        mediaUrl = mediaFullPath,
        messageType = this.messageType,
        status = MessageStatus.SENT
    )
}