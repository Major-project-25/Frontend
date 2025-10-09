package com.example.testapplication

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import java.io.FileOutputStream
import java.util.UUID

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
            // Pass the context to the helper function
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

// Helper function updated to accept a Context
private fun android.content.ContentResolver.getFile(context: Context, uri: Uri): File {
    val file = File(context.cacheDir, this.getFileName(uri))
    this.openInputStream(uri).use { inputStream ->
        FileOutputStream(file).use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
    }
    return file
}

private fun android.content.ContentResolver.getFileName(uri: Uri): String {
    var name = ""
    val cursor = this.query(uri, null, null, null, null)
    cursor?.use {
        it.moveToFirst()
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1) {
            name = it.getString(nameIndex)
        }
    }
    return name
}