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
import java.time.LocalTime
import java.util.UUID

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val moderationWarning: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(RetrofitInstance.api)
    private val ktorWebSocketService = KtorWebSocketService(application.applicationContext)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val appContext = application.applicationContext

    // Store the chat metadata needed for reloading
    private var currentChatFriendId: UUID? = null
    private var currentChatUserId: UUID? = null

    fun loadChat(currentUserId: UUID, friendId: UUID) {
        // Store IDs for the reload mechanism
        currentChatUserId = currentUserId
        currentChatFriendId = friendId

        // --- Setup WebSocket Listener (Keep for future improvement and moderation warnings) ---
        ktorWebSocketService.messages
            .onEach { messageResponse ->
                // The messages are still being received here, but we now rely on loadChat
                // for the actual UI update. This part remains useful for *cross-device* updates.

                // If the message is for the current chat, manually trigger a reload
                if (messageResponse.senderId == currentChatUserId || messageResponse.senderId == currentChatFriendId) {
                    forceChatReload()
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


        viewModelScope.launch {
            ktorWebSocketService.connect(currentUserId)
        }

        // Fetch the message history via HTTP
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

    // NEW FUNCTION: Triggers a manual fetch from the API using stored IDs
    private fun forceChatReload() {
        val userId = currentChatUserId
        val friendId = currentChatFriendId

        if (userId != null && friendId != null) {
            // Set loading state briefly (optional)
            _uiState.update { it.copy(isLoading = true) }

            userRepository.getConversationHistory(userId, friendId).enqueue(object : Callback<List<MessageResponse>> {
                override fun onResponse(call: Call<List<MessageResponse>>, response: Response<List<MessageResponse>>) {
                    if (response.isSuccessful) {
                        val messageResponses = response.body() ?: emptyList()
                        val uiMessages = messageResponses.map { it.toUiMessage(userId) }
                        // Replace the entire message list, forcing a Compose recomposition
                        _uiState.update { it.copy(messages = uiMessages, isLoading = false) }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                override fun onFailure(call: Call<List<MessageResponse>>, t: Throwable) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            })
        }
    }


    fun sendMediaMessage(currentUserId: UUID, receiverId: UUID, fileUri: Uri, mimeType: String, messageType: String) {
        _uiState.update { it.copy(moderationWarning = null) }

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
                        // Trigger the HTTP reload after sending the WebSocket message
                        forceChatReload() // <-- NEW: Force reload for sender
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

    fun sendMessage(receiverId: UUID, content: String, currentUserId: UUID) {
        _uiState.update { it.copy(moderationWarning = null) }

        // Send the message via WebSocket
        viewModelScope.launch {
            val messageToSend = MessageCreate(
                receiverId = receiverId,
                content = content,
                mediaUrl = null,
                messageType = "text"
            )
            ktorWebSocketService.sendMessage(messageToSend)
            // Trigger the HTTP reload immediately after sending the WebSocket message
            forceChatReload() // <-- NEW: Force reload for sender
        }
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
        RetrofitInstance.BASE_URL.dropLast(1) + this.mediaUrl
    } else {
        null
    }

    return Message(
        text = contentText,
        author = author,
        authorId = this.senderId,
        timestamp = this.timestamp.substring(11, 16),
        mediaUrl = mediaFullPath,
        messageType = this.messageType
    )
}