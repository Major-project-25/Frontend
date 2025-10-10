package com.example.testapplication

import android.content.Context
import android.net.Uri
import android.util.Log // <-- FIX: Missing Log import
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf // <-- FIX: Missing mutableStateOf import
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.UUID // <-- FIX: Missing UUID import

sealed interface AdminUiState {
    object Idle : AdminUiState
    object Uploading : AdminUiState
    object Success : AdminUiState
    data class Error(val message: String) : AdminUiState
}

class AdminViewModel : ViewModel() {
    var postContent by mutableStateOf("")
    var selectedFileUri by mutableStateOf<Uri?>(null)
    var uiState: AdminUiState by mutableStateOf(AdminUiState.Idle)
        private set

    fun createPost(adminId: UUID, context: Context) {
        uiState = AdminUiState.Uploading

        val contentPart = postContent.toRequestBody("text/plain".toMediaTypeOrNull())

        var filePart: MultipartBody.Part? = null
        selectedFileUri?.let { uri ->
            // Use the globally available file helper function from FileUtility.kt
            val file = context.contentResolver.getFile(context, uri)
            val requestFile = file.asRequestBody(context.contentResolver.getType(uri)?.toMediaTypeOrNull())
            filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
        }

        RetrofitInstance.api.createPost(adminId, contentPart, filePart).enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                if (response.isSuccessful) {
                    uiState = AdminUiState.Success
                    postContent = ""
                    selectedFileUri = null
                } else {
                    uiState = AdminUiState.Error("Upload failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                Log.e("AdminViewModel", "Post creation failed", t)
                uiState = AdminUiState.Error("Upload failed: ${t.message}")
            }
        })
    }

    fun resetState() {
        uiState = AdminUiState.Idle
    }
}
