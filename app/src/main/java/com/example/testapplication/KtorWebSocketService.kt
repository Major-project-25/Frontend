package com.example.testapplication

import android.content.Context
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.*

class KtorWebSocketService(private val context: Context) {
    private val client = HttpClient(CIO) {
        install(WebSockets)
    }
    private val gson = Gson()
    private var session: DefaultClientWebSocketSession? = null

    private val _messages = MutableSharedFlow<MessageResponse>()
    val messages = _messages.asSharedFlow()

    suspend fun connect(userId: UUID) {
        try {
            client.webSocket(
                method = HttpMethod.Get,
                host = "172.17.0.78", // IMPORTANT: Use your server IP
                port = 8000,
                path = "/api/v3/messages/ws/$userId"
            ) {
                session = this
                println("Ktor WebSocket Connected!")

                // Listen for incoming messages
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()

                        // Check the type of message before fully parsing
                        val jsonObject = gson.fromJson(text, Map::class.java)
                        when (jsonObject["type"]) {
                            "video_call_invitation" -> {
                                // This is a video call notification
                                val callerName = jsonObject["caller_name"] as String
                                val meetLink = jsonObject["meet_link"] as String
                                NotificationService.showVideoCallNotification(context, callerName, meetLink)
                            }
                            else -> {
                                // This is a regular chat message
                                val message = gson.fromJson(text, MessageResponse::class.java)
                                _messages.tryEmit(message)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Ktor WebSocket Error: ${e.localizedMessage}")
        } finally {
            println("Ktor WebSocket Disconnected.")
            session = null
        }
    }

    suspend fun sendMessage(message: MessageCreate) {
        val jsonMessage = gson.toJson(message)
        session?.send(Frame.Text(jsonMessage))
    }

    suspend fun disconnect() {
        session?.close()
        client.close()
    }
}