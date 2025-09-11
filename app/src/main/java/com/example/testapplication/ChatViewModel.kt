package com.example.testapplication

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

// UI State for the Chat Screen
sealed interface ChatUiState {
    data class Success(val messages: List<Message>) : ChatUiState
    object Error : ChatUiState
    object Loading : ChatUiState
}

class ChatViewModel : ViewModel() {
    private val userRepository = UserRepository(RetrofitInstance.api)

    var uiState: ChatUiState by mutableStateOf(ChatUiState.Loading)
        private set

    // Fetches the initial chat history between two users
    fun fetchHistory(currentUserId: UUID, friendId: UUID) {
        uiState = ChatUiState.Loading
        userRepository.getConversationHistory(currentUserId, friendId).enqueue(object : Callback<List<MessageResponse>> {
            override fun onResponse(call: Call<List<MessageResponse>>, response: Response<List<MessageResponse>>) {
                if (response.isSuccessful) {
                    val messageResponses = response.body() ?: emptyList()
                    // Convert backend models to UI models
                    val uiMessages = messageResponses.map { messageResponse ->
                        messageResponse.toUiMessage(currentUserId)
                    }
                    uiState = ChatUiState.Success(uiMessages)
                } else {
                    uiState = ChatUiState.Error
                }
            }

            override fun onFailure(call: Call<List<MessageResponse>>, t: Throwable) {
                Log.e("ChatViewModel", "Failed to fetch chat history", t)
                uiState = ChatUiState.Error
            }
        })
    }

    // Sends a new message
    fun sendMessage(currentUserId: UUID, friendId: UUID, content: String) {
        val messageToSend = MessageCreate(receiverId = friendId, content = content)

        userRepository.sendMessage(currentUserId, messageToSend).enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    val sentMessage = response.body()
                    if (sentMessage != null && uiState is ChatUiState.Success) {
                        // Add the new message to the current list for an instant update
                        val currentMessages = (uiState as ChatUiState.Success).messages.toMutableList()
                        currentMessages.add(sentMessage.toUiMessage(currentUserId))
                        uiState = ChatUiState.Success(currentMessages)
                    }
                } else {
                    Log.e("ChatViewModel", "Failed to send message: ${response.code()}")
                    // Optionally, you can add a temporary error state for a failed message
                }
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ChatViewModel", "Send message network call failed", t)
            }
        })
    }
}

// Helper function to convert a backend MessageResponse to a UI-friendly Message
private fun MessageResponse.toUiMessage(currentUserId: UUID): Message {
    val author = if (this.senderId == currentUserId) MessageAuthor.ME else MessageAuthor.THEM
    // We can add proper date/time formatting later
    return Message(
        text = this.content,
        author = author,
        timestamp = this.timestamp.toLocalTime().toString().substring(0, 5) // Example: "14:27"
    )
}