package com.example.testapplication

import okhttp3.MultipartBody
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

    fun getUserFullProfile(userId: UUID): Call<GetFullProfile> {
        return apiService.getUserFullProfile(userId)
    }

    fun getUserProfile(userId: UUID): Call<UserProfile> {
        return apiService.getUserProfile(userId)
    }

    fun sendConnectionRequest(requesterId: UUID, requestBody: ConnectionRequest): Call<StatusResponse> {
        return apiService.sendConnectionRequest(requesterId, requestBody)
    }

    fun getPendingRequests(userId: UUID): Call<List<PendingRequestDetail>> {
        return apiService.getPendingRequests(userId)
    }

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

    fun uploadMediaFile(filePart: MultipartBody.Part): Call<MediaUploadResponse> {
        return apiService.uploadMediaFile(filePart)
    }

    fun getAllPosts(userId: UUID): Call<List<PostResponse>> {
        return apiService.getAllPosts(userId)
    }

    // NEW FUNCTION
    fun deletePost(adminId: UUID, postId: UUID): Call<Unit> {
        return apiService.deletePost(adminId, postId)
    }

    fun getVideoCallLink(userId: UUID, otherUserId: UUID): Call<MeetLinkResponse> {
        return apiService.getVideoCallLink(userId, otherUserId)
    }

    fun reactToPost(postId: UUID, userId: UUID, reactionType: String): Call<PostResponse> {
        val reaction = ReactionCreate(reactionType = reactionType)
        return apiService.reactToPost(postId, userId, reaction)
    }

}