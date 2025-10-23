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

    // NEW: Private list to hold the current posts. This is essential for
    // updating the UI list in real-time (via WebSocket) without refetching.
    private var currentPosts: List<PostResponse> = emptyList()

    fun fetchPosts(userId: UUID) {
        uiState = GeneralUiState.Loading
        userRepository.getAllPosts(userId).enqueue(object : Callback<List<PostResponse>> {
            override fun onResponse(call: Call<List<PostResponse>>, response: Response<List<PostResponse>>) {
                if (response.isSuccessful) {
                    val posts = response.body()
                    if (posts.isNullOrEmpty()) {
                        currentPosts = emptyList()
                        uiState = GeneralUiState.Empty
                    } else {
                        // The posts are sorted here so the newest post is the first item (index 0).
                        val sortedPosts = posts.distinctBy { it.id }.sortedByDescending { it.created_at }
                        currentPosts = sortedPosts // Store the sorted list
                        uiState = GeneralUiState.Success(sortedPosts)
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

    // ... (deletePost logic remains unchanged)
    fun deletePost(adminId: UUID, postId: UUID) {
        if (uiState is GeneralUiState.Success) {
            val updatedPosts = currentPosts.filter { it.id != postId }

            currentPosts = updatedPosts // Update private list
            uiState = if (updatedPosts.isEmpty()) {
                GeneralUiState.Empty
            } else {
                GeneralUiState.Success(updatedPosts)
            }
        }

        userRepository.deletePost(adminId, postId).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (!response.isSuccessful) {
                    Log.e("GenInterfaceViewModel", "Deletion failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e("GenInterfaceViewModel", "Deletion network error", t)
            }
        })
    }

    // UPDATED: Handle user reaction and update currentPosts list
    fun handleReaction(userId: UUID, postId: UUID, reactionType: String) {
        userRepository.reactToPost(postId, userId, reactionType).enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                val updatedPost = response.body()
                if (response.isSuccessful && updatedPost != null) {
                    if (uiState is GeneralUiState.Success) {
                        // Map the list and replace the old post object with the new one
                        val newPosts = currentPosts.map { post ->
                            if (post.id == postId) updatedPost else post
                        }
                        currentPosts = newPosts // Update the private list
                        uiState = GeneralUiState.Success(newPosts)
                    }
                } else {
                    Log.e("GenInterfaceViewModel", "Reaction failed: ${response.code()}")
                    // Note: In a real app, you would rollback the UI state here if the reaction failed.
                }
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                Log.e("GenInterfaceViewModel", "Reaction network error", t)
            }
        })
    }
}