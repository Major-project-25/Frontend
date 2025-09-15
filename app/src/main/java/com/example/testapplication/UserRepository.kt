package com.example.testapplication

import retrofit2.Call
import java.util.UUID

class UserRepository(private val apiService: ApiService) {
    fun signupUser(userData: UserCreate): Call<ValidationResponse> {
        return apiService.signupUser(userData)
    }

    fun loginUser(loginData: UserCreate): Call<ValidationResponse> {
        return apiService.loginUser(loginData)
    }

    fun setupUserProfile(userId: UUID, profileData: AccountSetup): Call<BooleanResponse> {
        return apiService.setupUserProfile(userId, profileData)
    }

    fun getMatches(userId: UUID): Call<MatchResponse> {
        return apiService.getMatches(userId)
    }

    // ADD THIS FUNCTION
    fun getUserFullProfile(userId: UUID): Call<GetFullProfile> {
        return apiService.getUserFullProfile(userId)
    }

    fun getUserProfile(userId: UUID): Call<UserProfile> {
        return apiService.getUserProfile(userId)
    }

    // ADD THIS FUNCTION
    fun sendConnectionRequest(requesterId: UUID, requestBody: ConnectionRequest): Call<StatusResponse> {
        return apiService.sendConnectionRequest(requesterId, requestBody)
    }

    // In UserRepository.kt

// ... (keep all existing functions)

    // ADD THIS FUNCTION
    fun getPendingRequests(userId: UUID): Call<List<PendingRequestDetail>> {
        return apiService.getPendingRequests(userId)
    }

    // ADD THIS FUNCTION
    fun respondToRequest(userId: UUID, response: ConnectionUpdate): Call<Unit> {
        return apiService.respondToRequest(userId, response)
    }

    fun getFriends(userId: UUID): Call<FriendsResponse> {
        return apiService.getFriends(userId)
    }

    fun sendMessage(senderId: UUID, message: MessageCreate): Call<MessageResponse> {
        return apiService.sendMessage(senderId, message)
    }

    fun getConversationHistory(userId: UUID, otherUserId: UUID): Call<List<MessageResponse>> {
        return apiService.getConversationHistory(userId, otherUserId)
    }




}