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

    // Store the chat metadata
    private var currentChatFriendId: UUID? = null
    private var currentChatUserId: UUID? = null

    fun loadChat(currentUserId: UUID, friendId: UUID) {
        currentChatUserId = currentUserId
        currentChatFriendId = friendId

        // --- Setup WebSocket Listener for incoming messages ---
        ktorWebSocketService.messages
            .onEach { messageResponse ->
                // --- 1. ADD THIS LOG ---
                // This will prove that the ViewModel is receiving the event from the service.
                println(">>> ViewModel received new message via Flow: ${messageResponse.content}")

                val uiMessage = messageResponse.toUiMessage(currentUserId)

                // This is the correct real-time update logic.
                // It directly adds the new message to the list.
                _uiState.update { currentState ->
                    // --- 2. ADD THIS LOG ---
                    // This will prove that the UI state is being updated.
                    println(">>> Updating UI State with new message.")
                    currentState.copy(messages = currentState.messages + uiMessage, moderationWarning = null)
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

    // Your sendMedia and sendMessage functions (unchanged from your version)
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