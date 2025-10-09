package com.example.testapplication

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

// Changed to AndroidViewModel to access the application context
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(RetrofitInstance.api)
    // Pass the context to the WebSocket service
    private val ktorWebSocketService = KtorWebSocketService(application.applicationContext)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Listen for incoming messages from the WebSocket
        ktorWebSocketService.messages
            .onEach { messageResponse ->
                val uiMessage = messageResponse.toUiMessage(_uiState.value.messages.firstOrNull()?.authorId ?: UUID.randomUUID())
                _uiState.update { currentState ->
                    currentState.copy(messages = currentState.messages + uiMessage)
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadChat(currentUserId: UUID, friendId: UUID) {
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

    fun sendMessage(receiverId: UUID, content: String, currentUserId: UUID) {
        // Send the message via WebSocket
        viewModelScope.launch {
            val messageToSend = MessageCreate(receiverId = receiverId, content = content)
            ktorWebSocketService.sendMessage(messageToSend)
        }

        // Optimistically update the UI
        val optimisticUiMessage = Message(
            text = content,
            author = MessageAuthor.ME,
            authorId = currentUserId,
            timestamp = java.time.LocalTime.now().toString().substring(0, 5)
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
    return Message(
        text = this.content,
        author = author,
        authorId = this.senderId,
        timestamp = this.timestamp.substring(11, 16)
    )
}