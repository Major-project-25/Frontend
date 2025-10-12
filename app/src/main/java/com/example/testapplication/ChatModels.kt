package com.example.testapplication

import java.util.UUID

// Represents who sent a message
enum class MessageAuthor {
    ME, THEM
}

// Represents a single chat message
data class Message(
    val id: UUID = UUID.randomUUID(),
    val text: String?, // Must be nullable to support media-only messages
    val author: MessageAuthor,
    val authorId: UUID,
    val timestamp: String, // e.g., "10:10"
    val date: String? = null, // e.g., "Fri, Jul 26"
    val mediaUrl: String? = null, // URL for the uploaded file
    val messageType: String = "text" // Type of content (text, image, video)
)