package com.example.testapplication

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

// UI State for the Requests Screen
sealed interface RequestsUiState {
    data class Success(val request: PendingRequestDetail) : RequestsUiState
    object Empty : RequestsUiState
    object Error : RequestsUiState
    object Loading : RequestsUiState
}

class RequestsViewModel : ViewModel() {
    private val userRepository = UserRepository(RetrofitInstance.api)

    var uiState: RequestsUiState by mutableStateOf(RequestsUiState.Loading)
        private set

    private var requestsList = mutableListOf<PendingRequestDetail>()
    private var currentIndex = 0

    // Fetches the initial list of pending requests
    fun fetchPendingRequests(userId: UUID) {
        uiState = RequestsUiState.Loading
        userRepository.getPendingRequests(userId).enqueue(object : Callback<List<PendingRequestDetail>> {
            override fun onResponse(call: Call<List<PendingRequestDetail>>, response: Response<List<PendingRequestDetail>>) {
                if (response.isSuccessful) {
                    requestsList = response.body()?.toMutableList() ?: mutableListOf()
                    if (requestsList.isNotEmpty()) {
                        currentIndex = 0
                        displayRequestAtIndex(currentIndex)
                    } else {
                        uiState = RequestsUiState.Empty
                    }
                } else {
                    uiState = RequestsUiState.Error
                }
            }

            override fun onFailure(call: Call<List<PendingRequestDetail>>, t: Throwable) {
                Log.e("RequestsViewModel", "Failed to fetch requests", t)
                uiState = RequestsUiState.Error
            }
        })
    }

    // Responds to the current request with "accepted" or "declined"
    fun respondToCurrentRequest(currentUserId: UUID, wasAccepted: Boolean) {
        if (requestsList.isEmpty()) return

        val currentRequest = requestsList[currentIndex]
        val status = if (wasAccepted) "accepted" else "declined"
        val responseBody = ConnectionUpdate(requesterId = currentRequest.requesterId, newStatus = status)

        userRepository.respondToRequest(currentUserId, responseBody).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    // Action was successful, remove the request from our local list
                    requestsList.removeAt(currentIndex)

                    if (requestsList.isEmpty()) {
                        uiState = RequestsUiState.Empty
                    } else {
                        // Make sure the index is still valid
                        if (currentIndex >= requestsList.size) {
                            currentIndex = requestsList.size - 1
                        }
                        displayRequestAtIndex(currentIndex)
                    }
                } else {
                    Log.e("RequestsViewModel", "Failed to respond to request: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e("RequestsViewModel", "Respond request network call failed", t)
            }
        })
    }

    fun navigateToNextRequest() {
        if (currentIndex < requestsList.size - 1) {
            currentIndex++
            displayRequestAtIndex(currentIndex)
        }
    }

    fun navigateToPreviousRequest() {
        if (currentIndex > 0) {
            currentIndex--
            displayRequestAtIndex(currentIndex)
        }
    }

    // Helper to update the UI with the currently selected request
    private fun displayRequestAtIndex(index: Int) {
        if (requestsList.isNotEmpty() && index < requestsList.size) {
            uiState = RequestsUiState.Success(requestsList[index])
        }
    }
}