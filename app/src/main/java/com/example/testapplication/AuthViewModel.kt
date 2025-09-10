package com.example.testapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// The constructor now requires a SessionManager instance.
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
                    // Launch a coroutine to save the session data
                    viewModelScope.launch {
                        sessionManager.saveUserId(result.userId)
                        // Note: You might want to wait until profile setup is complete
                        // before setting isLoggedIn to true, but for now, we'll save the ID.
                    }
                    _authResult.value = result
                } else {
                    // Handle failure
                    _authResult.value = ValidationResponse(isValid = false, userId = null)
                }
            }

            override fun onFailure(call: Call<ValidationResponse>, t: Throwable) {
                _authResult.value = ValidationResponse(isValid = false, userId = null)
            }
        })
    }

    fun signInUser(loginData: UserCreate) {
        userRepository.loginUser(loginData).enqueue(object : Callback<ValidationResponse> {
            override fun onResponse(call: Call<ValidationResponse>, response: Response<ValidationResponse>) {
                val result = response.body()
                // Check for a successful result with a valid user ID
                if (result != null && result.isValid && result.userId != null) {
                    // Launch a coroutine to save the session data
                    viewModelScope.launch {
                        sessionManager.saveUserId(result.userId)
                        sessionManager.setLoggedIn(true)
                    }
                    _authResult.value = result
                } else {
                    // Handle failure
                    _authResult.value = ValidationResponse(isValid = false, userId = null)
                }
            }

            override fun onFailure(call: Call<ValidationResponse>, t: Throwable) {
                _authResult.value = ValidationResponse(isValid = false, userId = null)
            }
        })
    }

    fun clearResult() {
        _authResult.value = null
    }
}