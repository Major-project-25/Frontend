package com.example.testapplication

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

sealed interface HomeUiState {
    data class Success(
        val friends: List<FriendDetail>,
        val unreadCounts: Map<UUID, Int>
    ) : HomeUiState
    object Empty : HomeUiState
    object Error : HomeUiState
    object Loading : HomeUiState
}

class HomeViewModel : ViewModel() {
    private val userRepository = UserRepository(RetrofitInstance.api)
    private val ktorWebSocketService = KtorWebSocketService

    var uiState: HomeUiState by mutableStateOf(HomeUiState.Loading)
        private set

    // --- NEW ---
    /**
     * This state tracks whether there are any pending connection requests.
     * It's separate from UiState to avoid making the whole screen "load".
     */
    var hasPendingRequests by mutableStateOf(false)
        private set
    // --- END NEW ---

    private var isObservingCounts = false

    fun fetchFriends(userId: UUID) {
        uiState = HomeUiState.Loading
        userRepository.getFriends(userId).enqueue(object : Callback<FriendsResponse> {
            override fun onResponse(call: Call<FriendsResponse>, response: Response<FriendsResponse>) {
                if (response.isSuccessful) {
                    val friendsList = response.body()?.friends
                    if (friendsList.isNullOrEmpty()) {
                        uiState = HomeUiState.Empty
                    } else {
                        val initialUnreadCounts = friendsList
                            .filter { (it.unread_count ?: 0) > 0 }
                            .associate { it.userId to (it.unread_count ?: 0) }

                        uiState = HomeUiState.Success(friendsList, initialUnreadCounts)

                        observeUnreadCounts(initialUnreadCounts)
                    }
                } else {
                    uiState = HomeUiState.Error
                }
            }

            override fun onFailure(call: Call<FriendsResponse>, t: Throwable) {
                Log.e("HomeViewModel", "Failed to fetch friends", t)
                uiState = HomeUiState.Error
            }
        })
    }

    // --- NEW FUNCTION ---
    /**
     * Checks the API for any pending connection requests.
     * Updates [hasPendingRequests] state.
     */
    fun checkForPendingRequests(userId: UUID) {
        userRepository.getPendingRequests(userId).enqueue(object : Callback<List<PendingRequestDetail>> {
            override fun onResponse(call: Call<List<PendingRequestDetail>>, response: Response<List<PendingRequestDetail>>) {
                if (response.isSuccessful) {
                    val requests = response.body()
                    // Set to true only if the list is not null AND not empty
                    hasPendingRequests = !requests.isNullOrEmpty()
                } else {
                    // If the call fails, assume no requests
                    hasPendingRequests = false
                }
            }

            override fun onFailure(call: Call<List<PendingRequestDetail>>, t: Throwable) {
                // On network failure, assume no requests
                hasPendingRequests = false
                Log.e("HomeViewModel", "Failed to check for requests", t)
            }
        })
    }
    // --- END NEW FUNCTION ---

    private fun observeUnreadCounts(initialCounts: Map<UUID, Int>) {
        if (isObservingCounts) return
        isObservingCounts = true

        KtorWebSocketService.mergeInitialCounts(initialCounts)

        viewModelScope.launch {
            ktorWebSocketService.unreadCounts.collectLatest { countsFromWebSocket ->
                val currentState = uiState
                if (currentState is HomeUiState.Success) {
                    uiState = currentState.copy(
                        unreadCounts = countsFromWebSocket
                    )
                }
            }
        }
    }
}