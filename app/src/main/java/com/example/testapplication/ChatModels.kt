package com.example.testapplication

import java.util.UUID

// Represents who sent a message
enum class MessageAuthor {
    ME, THEM
}

// Represents a single chat message
data class Message(
    // CRITICAL FIX: Change ID type from UUID to Long, and give it a dummy default value
    // (UUID.randomUUID().mostSignificantBits is a common way to get a unique Long)
    val id: Long = 0,
    val text: String?,
    val author: MessageAuthor,
    val authorId: UUID,
    val timestamp: String,
    val date: String? = null,
    val mediaUrl: String? = null,
    val messageType: String = "text"
)