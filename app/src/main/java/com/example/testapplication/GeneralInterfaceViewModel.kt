package com.example.testapplication

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID // <-- ADDED THIS IMPORT STATEMENT

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

    // We removed the init block, relying on the screen to call this function

    fun fetchPosts(userId: UUID) {
        uiState = GeneralUiState.Loading
        userRepository.getAllPosts(userId).enqueue(object : Callback<List<PostResponse>> {
            override fun onResponse(call: Call<List<PostResponse>>, response: Response<List<PostResponse>>) {
                if (response.isSuccessful) {
                    val posts = response.body()
                    if (posts.isNullOrEmpty()) {
                        uiState = GeneralUiState.Empty
                    } else {
                        uiState = GeneralUiState.Success(posts)
                    }
                } else {
                    uiState = GeneralUiState.Error
                }
            }

            override fun onFailure(call: Call<List<PostResponse>>, t: Throwable) {
                Log.e("GenInterfaceViewModel", "Failed to fetch posts", t)
                uiState = GeneralUiState.Error
            }
        })
    }
}
