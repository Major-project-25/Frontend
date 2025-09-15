package com.example.testapplication

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import java.util.*

class KtorWebSocketService {
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
                host = "172.17.1.140", // IMPORTANT: Use your server IP
                port = 8000,
                path = "/api/v3/messages/ws/$userId"
            ) {
                session = this // Store the session
                println("Ktor WebSocket Connected!")

                // Listen for incoming messages for the duration of the connection
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val message = gson.fromJson(text, MessageResponse::class.java)
                        _messages.tryEmit(message)
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