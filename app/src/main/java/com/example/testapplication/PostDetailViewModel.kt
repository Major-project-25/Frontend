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

// UI State for the Post Detail Screen
sealed interface PostDetailUiState {
    data class Success(val post: PostResponse) : PostDetailUiState
    object Error : PostDetailUiState
    object Loading : PostDetailUiState
}

class PostDetailViewModel : ViewModel() {
    private val userRepository = UserRepository(RetrofitInstance.api)

    var uiState: PostDetailUiState by mutableStateOf(PostDetailUiState.Loading)
        private set

    // The backend's /api/v4/posts/{user_id} endpoint returns a list of ALL posts.
    // Since we don't have an endpoint to fetch a single post by ID,
    // we will re-use the general endpoint but filter the result locally.
    // NOTE: This assumes the general endpoint returns the necessary data.
    fun fetchPostDetails(currentUserId: UUID, postId: UUID) {
        uiState = PostDetailUiState.Loading

        // Use the existing function to fetch all posts (which includes the user_id for reactions)
        userRepository.getAllPosts(currentUserId).enqueue(object : Callback<List<PostResponse>> {
            override fun onResponse(call: Call<List<PostResponse>>, response: Response<List<PostResponse>>) {
                if (response.isSuccessful) {
                    val posts = response.body()

                    // Filter the list to find the specific post requested
                    val post = posts?.find { it.id == postId }

                    if (post != null) {
                        uiState = PostDetailUiState.Success(post)
                    } else {
                        Log.e("PostDetailVM", "Post ID not found: $postId")
                        uiState = PostDetailUiState.Error
                    }
                } else {
                    Log.e("PostDetailVM", "Failed to fetch all posts: ${response.code()}")
                    uiState = PostDetailUiState.Error
                }
            }

            override fun onFailure(call: Call<List<PostResponse>>, t: Throwable) {
                Log.e("PostDetailVM", "Network failure fetching posts", t)
                uiState = PostDetailUiState.Error
            }
        })
    }
}
