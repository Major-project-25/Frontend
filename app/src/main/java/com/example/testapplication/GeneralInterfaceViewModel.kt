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

// UI State for the General Interface Screen
sealed interface GeneralUiState {
    data class Success(val posts: List<PostResponse>) : GeneralUiState
    object Empty : GeneralUiState
    object Error : GeneralUiState
    object Loading : GeneralUiState
}

class GeneralInterfaceViewModel : ViewModel() {
    private val userRepository = UserRepository(RetrofitInstance.api)

    var uiState: GeneralUiState by mutableStateOf(GeneralUiState.Loading)
        private set

    fun fetchPosts(userId: UUID) {
        uiState = GeneralUiState.Loading
        userRepository.getAllPosts(userId).enqueue(object : Callback<List<PostResponse>> {
            override fun onResponse(call: Call<List<PostResponse>>, response: Response<List<PostResponse>>) {
                if (response.isSuccessful) {
                    val posts = response.body()
                    if (posts.isNullOrEmpty()) {
                        uiState = GeneralUiState.Empty
                    } else {
                        // Ensure posts are unique before updating the state (good practice)
                        uiState = GeneralUiState.Success(posts.distinctBy { it.id })
                    }
                } else {
                    Log.e("GenInterfaceViewModel", "Response failed: ${response.code()}")
                    uiState = GeneralUiState.Error
                }
            }

            override fun onFailure(call: Call<List<PostResponse>>, t: Throwable) {
                Log.e("GenInterfaceViewModel", "Failed to fetch posts", t)
                uiState = GeneralUiState.Error
            }
        })
    }

    // NEW FUNCTION: Handles post deletion
    fun deletePost(adminId: UUID, postId: UUID) {
        userRepository.deletePost(adminId, postId).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    // Update UI State by removing the deleted post from the list
                    if (uiState is GeneralUiState.Success) {
                        val currentPosts = (uiState as GeneralUiState.Success).posts
                        val updatedPosts = currentPosts.filter { it.id != postId }

                        uiState = if (updatedPosts.isEmpty()) {
                            GeneralUiState.Empty
                        } else {
                            GeneralUiState.Success(updatedPosts)
                        }
                    }
                } else {
                    Log.e("GenInterfaceViewModel", "Deletion failed: ${response.code()}")
                    // Optionally set an error state or show a Toast
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e("GenInterfaceViewModel", "Deletion network error", t)
                // Optionally set an error state or show a Toast
            }
        })
    }
}