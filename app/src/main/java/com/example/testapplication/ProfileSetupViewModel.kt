package com.example.testapplication

import android.net.Uri
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

class ProfileSetupViewModel : ViewModel() {
    private val userRepository = UserRepository(RetrofitInstance.api)

    var imageUri by mutableStateOf<Uri?>(null)
    var name by mutableStateOf("")
    var usn by mutableStateOf("")
    var bio by mutableStateOf("")

    val predefinedInterests = listOf(
        "None", "AIML", "Data Science", "Natural Language Processing",
        "Prompt Engineering", "Agent Building", "Web Development", "App Development",
        "Flutter", "Python Coding", "Fintech", "Trading", "Venture Capital (VC)",
        "Hackathon", "Collaboration", "Vibe Coding", "UI/UX Design", "Poetry"
    )

    var interests by mutableStateOf(
        listOf(
            Interest("None", 0),
            Interest("None", 0),
            Interest("None", 0)
        )
    )

    fun onNameChange(newName: String) { name = newName }
    fun onUsnChange(newUsn: String) { usn = newUsn }
    fun onBioChange(newBio: String) { if (newBio.length <= 150) bio = newBio }
    fun onImageUriChange(newUri: Uri?) { imageUri = newUri }
    fun onInterestsChange(newInterests: List<Interest>) { interests = newInterests }

    fun validateProfile(): String? {
        if (name.isBlank() || usn.isBlank() || bio.isBlank()) {
            return "Please fill in all fields."
        }
        if (interests.all { it.name == "None" }) {
            return "Please select at least one interest."
        }
        return null
    }

    fun updateUserProfile(userId: UUID, sessionManager: SessionManager) {
        val profileData = AccountSetup(
            name = name,
            universityRegNo = usn,
            biography = bio.takeIf { it.isNotBlank() },
            interest1 = interests.getOrNull(0)?.name?.takeIf { it != "None" },
            interest1Weight = interests.getOrNull(0)?.rating,
            interest2 = interests.getOrNull(1)?.name?.takeIf { it != "None" },
            interest2Weight = interests.getOrNull(1)?.rating,
            interest3 = interests.getOrNull(2)?.name?.takeIf { it != "None" },
            interest3Weight = interests.getOrNull(2)?.rating
        )

        userRepository.setupUserProfile(userId, profileData).enqueue(object : Callback<BooleanResponse> {
            override fun onResponse(call: Call<BooleanResponse>, response: Response<BooleanResponse>) {
                if (response.isSuccessful && response.body()?.isValid == true) {
                    viewModelScope.launch {
                        sessionManager.setLoggedIn(true)
                    }
                }
            }
            override fun onFailure(call: Call<BooleanResponse>, t: Throwable) {
                // Handle failure
            }
        })
    }
}