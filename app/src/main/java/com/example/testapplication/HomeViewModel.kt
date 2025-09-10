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

// UI State for the Home Screen
sealed interface HomeUiState {
    data class Success(val friendUsns: List<String>) : HomeUiState
    object Empty : HomeUiState
    object Error : HomeUiState
    object Loading : HomeUiState
}

class HomeViewModel : ViewModel() {
    private val userRepository = UserRepository(RetrofitInstance.api)

    var uiState: HomeUiState by mutableStateOf(HomeUiState.Loading)
        private set

    fun fetchFriends(userId: UUID) {
        uiState = HomeUiState.Loading
        userRepository.getFriends(userId).enqueue(object : Callback<FriendsResponse> {
            override fun onResponse(call: Call<FriendsResponse>, response: Response<FriendsResponse>) {
                if (response.isSuccessful) {
                    val usns = response.body()?.usns
                    if (usns.isNullOrEmpty()) {
                        uiState = HomeUiState.Empty
                    } else {
                        uiState = HomeUiState.Success(usns)
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
}