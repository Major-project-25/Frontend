package com.example.testapplication

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE // <-- NEW IMPORT
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

interface ApiService {
    // --- Version 1 Endpoints ---

    @POST("api/v1/users/signup")
    fun signupUser(@Body userData: UserCreate): Call<ValidationResponse>

    @POST("api/v1/auth/login")
    fun loginUser(@Body loginData: UserCreate): Call<ValidationResponse>

    @PUT("api/v1/users/{user_id}/setup")
    fun setupUserProfile(
        @Path("user_id") userId: UUID,
        @Body profileData: AccountSetup
    ): Call<BooleanResponse>

    @GET("api/v1/users/{user_id}/profile")
    fun getUserProfile(@Path("user_id") userId: UUID): Call<UserProfile>

    // --- Version 2 Endpoints ---

    @POST("api/v2/algo/match/{user_id}")
    fun getMatches(@Path("user_id") userId: UUID): Call<MatchResponse>

    @POST("api/v2/connections/request")
    fun sendConnectionRequest(
        @Query("user_id") requesterId: UUID,
        @Body addressee: ConnectionRequest
    ): Call<StatusResponse>

    @GET("api/v2/connections/{user_id}/requests/pending")
    fun getPendingRequests(@Path("user_id") userId: UUID): Call<List<PendingRequestDetail>>

    @PUT("api/v2/connections/{user_id}/requests/respond")
    fun respondToRequest(
        @Path("user_id") userId: UUID,
        @Body response: ConnectionUpdate
    ): Call<Unit> // Use Call<Unit> because there is no response body

    @GET("api/v2/connections/{user_id}/friends")
    fun getFriends(@Path("user_id") userId: UUID): Call<FriendsResponse>

    // --- Version 3 Endpoints (Messaging) ---
    @POST("api/v3/messages/{sender_id}/send")
    fun sendMessage(
        @Path("sender_id") senderId: UUID,
        @Body message: MessageCreate // Uses UPDATED MessageCreate
    ): Call<MessageResponse>

    @GET("api/v3/messages/{user_id}/conversation/{other_user_id}")
    fun getConversationHistory(
        @Path("user_id") userId: UUID,
        @Path("other_user_id") otherUserId: UUID
    ): Call<List<MessageResponse>>

    @GET("api/v3/messages/{user_id}/meet/{other_user_id}")
    fun getVideoCallLink(
        @Path("user_id") userId: UUID,
        @Path("other_user_id") otherUserId: UUID
    ): Call<MeetLinkResponse>

    // NEW ENDPOINT for media upload
    @Multipart
    @POST("api/v3/messages/upload-media")
    fun uploadMediaFile(
        @Part file: MultipartBody.Part
    ): Call<MediaUploadResponse>

    @GET("api/v1/users/{user_id}/Fullprofile")
    fun getUserFullProfile(@Path("user_id") userId: UUID): Call<GetFullProfile>

    // --- Version 4 Endpoints (Admin/Posts) ---
    @Multipart
    @POST("api/v4/admin/{admin_id}/posts")
    fun createPost(
        @Path("admin_id") adminId: UUID,
        @Part("content") content: RequestBody?,
        @Part file: MultipartBody.Part?
    ): Call<PostResponse>

    // NEW ADMIN ENDPOINT: DELETE a specific post
    @DELETE("api/v4/admin/{admin_id}/posts/{post_id}")
    fun deletePost(
        @Path("admin_id") adminId: UUID,
        @Path("post_id") postId: UUID
    ): Call<Unit> // Assuming the backend returns an empty body or just 204 No Content

    @GET("api/v4/posts/{user_id}")
    fun getAllPosts(@Path("user_id") userId: UUID): Call<List<PostResponse>>


    @POST("api/v4/posts/{post_id}/react/{user_id}")
    fun reactToPost(
        @Path("post_id") postId: UUID,
        @Path("user_id") userId: UUID,
        @Body reaction: ReactionCreate
    ): Call<PostResponse>
}
