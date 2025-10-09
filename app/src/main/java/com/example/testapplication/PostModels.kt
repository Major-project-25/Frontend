package com.example.testapplication

import java.util.UUID

data class Post(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val author: String,
    val timestamp: String,
    val description: String,
    val imageId: Int? = null // Change imageUrl to imageId and type to Int?
)