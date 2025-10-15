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

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val moderationWarning: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(RetrofitInstance.api)
    // Assumes KtorWebSocketService includes .warnings and .messages flows
    private val ktorWebSocketService = KtorWebSocketService(application.applicationContext)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val appContext = application.applicationContext

    // Store the chat metadata for resyncing after delete
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

        // --- Setup WebSocket Listener for incoming messages (including echo) ---
        ktorWebSocketService.messages
            .onEach { messageResponse ->
                // This block adds messages received from the server (including the sender's echo)
                val uiMessage = messageResponse.toUiMessage(currentUserId)
                _uiState.update { currentState ->
                    currentState.copy(messages = currentState.messages + uiMessage, moderationWarning = null)
                }
            }
            .launchIn(viewModelScope)

        // --- Setup WebSocket Listener for moderation warnings ---
        ktorWebSocketService.warnings
            .onEach { warning ->
                _uiState.update { currentState ->
                    currentState.copy(moderationWarning = warning.message)
                }
            }
            .launchIn(viewModelScope)

        // --- NEW LISTENER: For real-time deletion events from server ---
        ktorWebSocketService.deletionEvents
            .onEach { messageIdToDelete ->
                _uiState.update { currentState ->
                    // Filter out the message by the ID received from the server
                    currentState.copy(messages = currentState.messages.filter { it.id != messageIdToDelete })
                }
            }
            .launchIn(viewModelScope)
        // -----------------------------------------------------------------


        viewModelScope.launch {
            ktorWebSocketService.connect(currentUserId)
        }

        // Fetch the initial message history via HTTP
        fetchHistory(currentUserId, friendId)
    }

    // Extracted history fetching to its own function
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

    // --- FIXED FUNCTION: sendMediaMessage (No optimistic update) ---
    fun sendMediaMessage(currentUserId: UUID, receiverId: UUID, fileUri: Uri, mimeType: String, messageType: String) {
        _uiState.update { it.copy(moderationWarning = null) }
        // Relying on WebSocket echo to update UI.

        // 1. Upload file and then send WebSocket message
        val file: File = appContext.contentResolver.getFile(appContext, fileUri)
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

        userRepository.uploadMediaFile(filePart).enqueue(object : Callback<MediaUploadResponse> {
            override fun onResponse(call: Call<MediaUploadResponse>, response: Response<MediaUploadResponse>) {
                val mediaUrl = response.body()?.mediaUrl
                if (response.isSuccessful && mediaUrl != null) {
                    viewModelScope.launch {
                        val messageToSend = MessageCreate(
                            receiverId = receiverId,
                            content = null,
                            mediaUrl = mediaUrl,
                            messageType = messageType
                        )
                        ktorWebSocketService.sendMessage(messageToSend)
                    }
                } else {
                    Log.e("ChatViewModel", "Media upload failed: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<MediaUploadResponse>, t: Throwable) {
                Log.e("ChatViewModel", "Media upload network error: ${t.message}")
            }
        })
    }

    // --- FIXED FUNCTION: sendMessage (No optimistic update) ---
    fun sendMessage(receiverId: UUID, content: String, currentUserId: UUID) {
        _uiState.update { it.copy(moderationWarning = null) }
        // Relying on WebSocket echo to update UI.

        // 1. Send the message via WebSocket
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

    // NEW FUNCTION: Handles message deletion
    fun deleteMessage(messageId: Long, currentUserId: UUID) {
        // 1. Optimistically remove the message from the UI on the sender's side
        _uiState.update { currentState ->
            currentState.copy(messages = currentState.messages.filter { it.id != messageId })
        }

        // 2. Send deletion request to the server
        userRepository.deleteChatMessage(messageId, currentUserId).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (!response.isSuccessful) {
                    Log.e("ChatViewModel", "Failed to delete message: ${response.code()}")
                    // If deletion fails, re-fetch history to resynchronize the UI
                    currentChatUserId?.let { userId ->
                        currentChatFriendId?.let { friendId ->
                            fetchHistory(userId, friendId)
                        }
                    }
                }
                // NOTE: The recipient's deletion is handled by the WebSocket listener (deletionEvents)
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e("ChatViewModel", "Network error on delete message", t)
            }
        })
    }


    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            ktorWebSocketService.disconnect()
        }
    }
}

private fun MessageResponse.toUiMessage(currentUserId: UUID): Message {
    val author = if (this.senderId == currentUserId) MessageAuthor.ME else MessageAuthor.THEM
    val contentText = this.content
    val mediaFullPath = if (this.mediaUrl != null) {
        // NOTE: Assumes RetrofitInstance.BASE_URL is accessible and ends with '/'
        RetrofitInstance.BASE_URL.dropLast(1) + this.mediaUrl
    } else {
        null
    }
    return Message(
        id = this.id, // Ensure the server ID is used here
        text = contentText,
        author = author,
        authorId = this.senderId,
        timestamp = this.timestamp.substring(11, 16),
        mediaUrl = mediaFullPath,
        messageType = this.messageType
    )
}