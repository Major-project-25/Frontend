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
    val error: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(RetrofitInstance.api)
    private val ktorWebSocketService = KtorWebSocketService(application.applicationContext)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val appContext = application.applicationContext

    fun loadChat(currentUserId: UUID, friendId: UUID) {
        // --- Setup WebSocket Listener ---
        ktorWebSocketService.messages
            .onEach { messageResponse ->
                val uiMessage = messageResponse.toUiMessage(currentUserId)
                _uiState.update { currentState ->
                    currentState.copy(messages = currentState.messages + uiMessage)
                }
            }
            .launchIn(viewModelScope)


        // Launch a coroutine to handle the suspendable connect function
        viewModelScope.launch {
            ktorWebSocketService.connect(currentUserId)
        }

        // Fetch the message history via HTTP
        userRepository.getConversationHistory(currentUserId, friendId).enqueue(object : Callback<List<MessageResponse>> {
            override fun onResponse(call: Call<List<MessageResponse>>, response: Response<List<MessageResponse>>) {
                if (response.isSuccessful) {
                    val messageResponses = response.body() ?: emptyList()
                    val uiMessages = messageResponses.map { it.toUiMessage(currentUserId) }
                    _uiState.update { it.copy(messages = uiMessages, isLoading = false) }
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

    // --- NEW FUNCTION: Handles the entire file upload and send process ---
    fun sendMediaMessage(currentUserId: UUID, receiverId: UUID, fileUri: Uri, mimeType: String, messageType: String) {
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

                    // Optimistically update the UI locally
                    // FIX: Access BASE_URL directly from RetrofitInstance
                    val fullMediaUrl = RetrofitInstance.BASE_URL.dropLast(1) + mediaUrl
                    val optimisticUiMessage = Message(
                        text = null,
                        author = MessageAuthor.ME,
                        authorId = currentUserId,
                        timestamp = LocalTime.now().toString().substring(0, 5),
                        mediaUrl = fullMediaUrl,
                        messageType = messageType
                    )
                    _uiState.update { currentState ->
                        currentState.copy(messages = currentState.messages + optimisticUiMessage)
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
    // --- END NEW FUNCTION ---

    fun sendMessage(receiverId: UUID, content: String, currentUserId: UUID) {
        // Send the message via WebSocket
        viewModelScope.launch {
            val messageToSend = MessageCreate(
                receiverId = receiverId,
                content = content,
                mediaUrl = null,
                messageType = "text"
            )
            ktorWebSocketService.sendMessage(messageToSend)
        }

        // Optimistically update the UI
        val optimisticUiMessage = Message(
            text = content,
            author = MessageAuthor.ME,
            authorId = currentUserId,
            timestamp = LocalTime.now().toString().substring(0, 5),
            mediaUrl = null,
            messageType = "text"
        )
        _uiState.update { currentState ->
            currentState.copy(messages = currentState.messages + optimisticUiMessage)
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
        // FIX: Access BASE_URL directly from RetrofitInstance
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
