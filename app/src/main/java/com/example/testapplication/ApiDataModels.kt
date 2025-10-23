package com.example.testapplication

import com.google.gson.annotations.SerializedName
import java.util.UUID
import java.time.OffsetDateTime

data class BooleanResponse(
    @SerializedName("is_valid")
    val isValid: Boolean
)

data class ValidationResponse(
    @SerializedName("is_valid")
    val isValid: Boolean,
    @SerializedName("user_id")
    val userId: UUID?,
    @SerializedName("is_admin")
    val isAdmin: Boolean? = false // Add this new field
)

data class StatusResponse(
    @SerializedName("message")
    val message: String
)

data class UserCreate(
    val email: String,
    val password: String
)

data class ConnectionRequest(
    @SerializedName("addressee_id")
    val addresseeId: UUID
)

data class AccountSetup(
    val name: String,
    @SerializedName("university_reg_no")
    val universityRegNo: String,
    val biography: String?,
    val interest1: String?,
    @SerializedName("interest1_weight")
    val interest1Weight: Int?,
    val interest2: String?,
    @SerializedName("interest2_weight")
    val interest2Weight: Int?,
    val interest3: String?,
    @SerializedName("interest3_weight")
    val interest3Weight: Int?
)

data class MatchResponse(
    @SerializedName("matches")
    val matches: List<UUID>
)

data class UserProfile(
    @SerializedName("university_reg_no")
    val universityRegNo: String?,

    @SerializedName("biography")
    val biography: String?,

    @SerializedName("interest1")
    val interest1: String?,

    @SerializedName("interest2")
    val interest2: String?,

    @SerializedName("interest3")
    val interest3: String?
)

data class PendingRequestDetail(
    @SerializedName("requester_id")
    val requesterId: UUID,

    @SerializedName("university_reg_no")
    val universityRegNo: String?,

    @SerializedName("biography")
    val biography: String?,

    @SerializedName("interest1")
    val interest1: String?,

    @SerializedName("interest2")
    val interest2: String?,

    @SerializedName("interest3")
    val interest3: String?
)

data class ConnectionUpdate(
    @SerializedName("requester_id")
    val requesterId: UUID,

    @SerializedName("new_status")
    val newStatus: String // "accepted" or "declined"
)

data class FriendsResponse(
    @SerializedName("friends")
    val friends: List<FriendDetail>
)

data class GetFullProfile(
    @SerializedName("name")
    val name: String,

    @SerializedName("university_reg_no")
    val universityRegNo: String,

    @SerializedName("biography")
    val biography: String?,

    @SerializedName("interest1")
    val interest1: String?,

    @SerializedName("interest1_weight")
    val interest1Weight: Int?,

    @SerializedName("interest2")
    val interest2: String?,

    @SerializedName("interest2_weight")
    val interest2Weight: Int?,

    @SerializedName("interest3")
    val interest3: String?,

    @SerializedName("interest3_weight")
    val interest3Weight: Int?
)

// For sending a new message - UPDATED
data class MessageCreate(
    @SerializedName("receiver_id")
    val receiverId: UUID,
    @SerializedName("content")
    val content: String? = null, // MADE NULLABLE
    @SerializedName("media_url")
    val mediaUrl: String? = null,
    @SerializedName("message_type")
    val messageType: String = "text"
)

// For receiving a message's details - UPDATED
data class MessageResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("sender_id")
    val senderId: UUID,
    @SerializedName("receiver_id")
    val receiverId: UUID,
    @SerializedName("content")
    val content: String?, // MADE NULLABLE
    @SerializedName("media_url")
    val mediaUrl: String?,
    @SerializedName("message_type")
    val messageType: String,
    @SerializedName("timestamp")
    val timestamp: String // Using String for simplicity
)

data class FriendDetail(
    @SerializedName("user_id")
    val userId: UUID,
    @SerializedName("university_reg_no")
    val universityRegNo: String?,
    @SerializedName("name")
    val name: String?
)

data class ReactionCreate(
    @SerializedName("reaction_type")
    val reactionType: String // Can be "like", "dislike", or "none"
)

data class PostResponse(
    val id: UUID,
    val content: String?,
    val media_url: String?,
    val content_type: String,
    val created_at: String, // Using String for simplicity
    val author_id: UUID,
    // --- NEW FIELDS FROM BACKEND ---
    val likes: Int, // Total number of likes
    val dislikes: Int, // Total number of dislikes
    @SerializedName("user_reaction")
    val userReaction: String? // "like", "dislike", or null
)
data class MeetLinkResponse(
    @SerializedName("meet_link")
    val meetLink: String
)

// NEW CLASS for media upload response
data class MediaUploadResponse(
    @SerializedName("media_url")
    val mediaUrl: String
)


