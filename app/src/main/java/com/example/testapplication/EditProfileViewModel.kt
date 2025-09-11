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

// UI State for the Edit Profile Screen
sealed interface EditProfileUiState {
    data class Success(val profile: GetFullProfile) : EditProfileUiState
    object Error : EditProfileUiState
    object Loading : EditProfileUiState
    object UpdateSuccess : EditProfileUiState // New state for when the update is successful
}

class EditProfileViewModel : ViewModel() {
    private val userRepository = UserRepository(RetrofitInstance.api)

    var uiState: EditProfileUiState by mutableStateOf(EditProfileUiState.Loading)
        private set

    // State for the editable fields
    var bio by mutableStateOf("")
    var interests by mutableStateOf(
        listOf(
            Interest("None", 0),
            Interest("None", 0),
            Interest("None", 0)
        )
    )
    fun onNavigationDone() {
        // Reset the state to loading for the next time the screen is opened
        uiState = EditProfileUiState.Loading
    }

    // Fetches the user's current data to pre-fill the form
    fun loadInitialProfile(userId: UUID) {
        uiState = EditProfileUiState.Loading
        userRepository.getUserFullProfile(userId).enqueue(object : Callback<GetFullProfile> {
            override fun onResponse(call: Call<GetFullProfile>, response: Response<GetFullProfile>) {
                val profile = response.body()
                if (response.isSuccessful && profile != null) {
                    // Pre-fill the editable fields
                    bio = profile.biography ?: ""
                    interests = listOfNotNull(
                        profile.interest1?.let { Interest(it, profile.interest1Weight ?: 0) },
                        profile.interest2?.let { Interest(it, profile.interest2Weight ?: 0) },
                        profile.interest3?.let { Interest(it, profile.interest3Weight ?: 0) }
                    ).take(3).let { it + List(3 - it.size) { Interest("None", 0) } } // Ensure list has 3 items

                    uiState = EditProfileUiState.Success(profile)
                } else {
                    uiState = EditProfileUiState.Error
                }
            }
            override fun onFailure(call: Call<GetFullProfile>, t: Throwable) {
                uiState = EditProfileUiState.Error
            }
        })
    }

    // Sends the updated data to the server
    fun updateProfile(userId: UUID, currentProfile: GetFullProfile) { // 1. Accept the current profile
        val profileUpdateData = AccountSetup(
            name = currentProfile.name, // 2. Add the current name
            universityRegNo = currentProfile.universityRegNo, // 3. Add the current USN
            biography = bio.takeIf { it.isNotBlank() },
            interest1 = interests.getOrNull(0)?.name?.takeIf { it != "None" },
            interest1Weight = interests.getOrNull(0)?.rating,
            interest2 = interests.getOrNull(1)?.name?.takeIf { it != "None" },
            interest2Weight = interests.getOrNull(1)?.rating,
            interest3 = interests.getOrNull(2)?.name?.takeIf { it != "None" },
            interest3Weight = interests.getOrNull(2)?.rating
        )

        userRepository.setupUserProfile(userId, profileUpdateData).enqueue(object : Callback<BooleanResponse>  {
            override fun onResponse(call: Call<BooleanResponse>, response: Response<BooleanResponse>) {
                if (response.isSuccessful && response.body()?.isValid == true) {
                    uiState = EditProfileUiState.UpdateSuccess
                } else {
                    uiState = EditProfileUiState.Error
                }
            }
            override fun onFailure(call: Call<BooleanResponse>, t: Throwable) {
                Log.e("EditProfileViewModel", "Failed to update profile", t)
                uiState = EditProfileUiState.Error
            }
        })
    }
}