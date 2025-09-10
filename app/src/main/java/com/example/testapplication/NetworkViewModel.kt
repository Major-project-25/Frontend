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

// A detailed user profile data class for the UI
data class MatchProfile(
    val id: UUID,
    val usn: String,
    val bio: String,
    val interests: List<String>
)

// Represents the different states the UI can be in
sealed interface NetworkUiState {
    data class Success(val profile: MatchProfile) : NetworkUiState
    object Empty : NetworkUiState // For when there are no matches left
    object Error : NetworkUiState
    object Loading : NetworkUiState
}

class NetworkViewModel : ViewModel() {
    private val userRepository = UserRepository(RetrofitInstance.api)

    var uiState: NetworkUiState by mutableStateOf(NetworkUiState.Loading)
        private set

    // --- State Properties ---
    private var matchIds = mutableListOf<UUID>()
    private var currentIndex = 0

    // --- Public Functions (Called from the UI) ---

    fun fetchInitialMatches(userId: UUID) {
        uiState = NetworkUiState.Loading
        userRepository.getMatches(userId).enqueue(object : Callback<MatchResponse> {
            override fun onResponse(call: Call<MatchResponse>, response: Response<MatchResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    matchIds = response.body()!!.matches.toMutableList()
                    if (matchIds.isNotEmpty()) {
                        currentIndex = 0
                        fetchProfileAtIndex(currentIndex)
                    } else {
                        uiState = NetworkUiState.Empty
                    }
                } else {
                    uiState = NetworkUiState.Error
                }
            }

            override fun onFailure(call: Call<MatchResponse>, t: Throwable) {
                Log.e("NetworkViewModel", "Failed to fetch matches", t)
                uiState = NetworkUiState.Error
            }
        })
    }

    fun navigateToNextProfile() {
        if (currentIndex < matchIds.size - 1) {
            currentIndex++
            fetchProfileAtIndex(currentIndex)
        }
    }

    fun navigateToPreviousProfile() {
        if (currentIndex > 0) {
            currentIndex--
            fetchProfileAtIndex(currentIndex)
        }
    }

    fun sendConnectionRequest(currentUserId: UUID) {
        if (matchIds.isEmpty()) return

        val addresseeId = matchIds[currentIndex]
        val requestBody = ConnectionRequest(addresseeId = addresseeId)

        userRepository.sendConnectionRequest(currentUserId, requestBody).enqueue(object : Callback<StatusResponse> {
            override fun onResponse(call: Call<StatusResponse>, response: Response<StatusResponse>) {
                if (response.isSuccessful) {
                    // Success! Remove the user from the list and show the next one.
                    matchIds.removeAt(currentIndex)

                    if (matchIds.isEmpty()) {
                        uiState = NetworkUiState.Empty
                    } else {
                        // Ensure index is valid after removal
                        if (currentIndex >= matchIds.size) {
                            currentIndex = matchIds.size - 1
                        }
                        fetchProfileAtIndex(currentIndex)
                    }
                } else {
                    // Handle API error (e.g., show a Toast)
                    Log.e("NetworkViewModel", "Failed to connect: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                // Handle network failure
                Log.e("NetworkViewModel", "Connection request failed", t)
            }
        })
    }

    // --- Private Helper Function ---

    private fun fetchProfileAtIndex(index: Int) {
        uiState = NetworkUiState.Loading
        val userId = matchIds[index]
        userRepository.getUserProfile(userId).enqueue(object : Callback<UserProfile> {
            override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                val profile = response.body()
                if (response.isSuccessful && profile != null) {
                    val interests = listOfNotNull(profile.interest1, profile.interest2, profile.interest3)
                    uiState = NetworkUiState.Success(
                        MatchProfile(
                            id = userId,
                            usn = profile.universityRegNo ?: "N/A",
                            bio = profile.biography ?: "No bio.",
                            interests = interests
                        )
                    )
                } else {
                    uiState = NetworkUiState.Error
                }
            }

            override fun onFailure(call: Call<UserProfile>, t: Throwable) {
                Log.e("NetworkViewModel", "Failed to fetch profile", t)
                uiState = NetworkUiState.Error
            }
        })
    }
}