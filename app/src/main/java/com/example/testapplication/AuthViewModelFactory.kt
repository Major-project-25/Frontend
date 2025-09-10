package com.example.testapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// This factory class knows how to create an AuthViewModel.
class AuthViewModelFactory(private val sessionManager: SessionManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel is of type AuthViewModel
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            // If it is, create and return an instance of AuthViewModel, passing the sessionManager.
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(sessionManager) as T
        }
        // If it's a different ViewModel, throw an error.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}