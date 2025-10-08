package com.example.testapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthViewModel(private val sessionManager: SessionManager) : ViewModel() {
    private val userRepository = UserRepository(RetrofitInstance.api)

    private val _authResult = MutableStateFlow<ValidationResponse?>(null)
    val authResult = _authResult.asStateFlow()

    fun signUpUser(userData: UserCreate) {
        userRepository.signupUser(userData).enqueue(object : Callback<ValidationResponse> {
            override fun onResponse(call: Call<ValidationResponse>, response: Response<ValidationResponse>) {
                val result = response.body()
                // Check for a successful result with a valid user ID
                if (result != null && result.isValid && result.userId != null) {
                    viewModelScope.launch {
                        sessionManager.saveUserId(result.userId)
                    }
                }
                _authResult.value = result
            }

            override fun onFailure(call: Call<ValidationResponse>, t: Throwable) {
                _authResult.value = ValidationResponse(isValid = false, userId = null, isAdmin = false)
            }
        })
    }

    fun signInUser(loginData: UserCreate) {
        userRepository.loginUser(loginData).enqueue(object : Callback<ValidationResponse> {
            override fun onResponse(call: Call<ValidationResponse>, response: Response<ValidationResponse>) {
                val result = response.body()
                if (result != null && result.isValid && result.userId != null) {
                    viewModelScope.launch {
                        sessionManager.saveUserId(result.userId)
                        sessionManager.setLoggedIn(true)
                        // Save the admin status here
                        sessionManager.setAdminStatus(result.isAdmin ?: false)
                    }
                }
                // Pass the full result, including the admin flag
                _authResult.value = result
            }
            override fun onFailure(call: Call<ValidationResponse>, t: Throwable) {
                _authResult.value = ValidationResponse(isValid = false, userId = null, isAdmin = false)
            }
        })
    }

    fun clearResult() {
        _authResult.value = null
    }
}