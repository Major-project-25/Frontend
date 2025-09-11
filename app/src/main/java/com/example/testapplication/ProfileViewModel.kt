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

// UI State for the Profile Screen - updated to use the new data model
sealed interface ProfileUiState {
    data class Success(val profile: GetFullProfile) : ProfileUiState
    object Error : ProfileUiState
    object Loading : ProfileUiState
}

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository(RetrofitInstance.api)

    var uiState: ProfileUiState by mutableStateOf(ProfileUiState.Loading)
        private set

    fun fetchUserProfile(userId: UUID) {
        uiState = ProfileUiState.Loading
        // Call the new repository function
        userRepository.getUserFullProfile(userId).enqueue(object : Callback<GetFullProfile> {
            // The typo was in the line below
            override fun onResponse(call: Call<GetFullProfile>, response: Response<GetFullProfile>) {
                if (response.isSuccessful && response.body() != null) {
                    uiState = ProfileUiState.Success(response.body()!!)
                } else {
                    uiState = ProfileUiState.Error
                }
            }

            override fun onFailure(call: Call<GetFullProfile>, t: Throwable) {
                Log.e("ProfileViewModel", "Failed to fetch user profile", t)
                uiState = ProfileUiState.Error
            }
        })
    }
}