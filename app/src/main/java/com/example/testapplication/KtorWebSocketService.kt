package com.example.testapplication

import android.content.Context
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

// NEW: Shared Flow for immediate warnings/errors that don't belong in the chat history
data class ChatWarning(val message: String)

// NEW: Enum to clearly represent the WebSocket's connection state.
enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}

class KtorWebSocketService(private val context: Context) {
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            // Sending a ping periodically helps keep the connection alive through routers and firewalls.
            pingInterval = 20_000
        }
    }
    private val gson = Gson()
    private var session: DefaultClientWebSocketSession? = null

    private val _messages = MutableSharedFlow<MessageResponse>()
    val messages = _messages.asSharedFlow()

    // NEW FLOW: To emit moderation warnings back to the sender
    private val _warnings = MutableSharedFlow<ChatWarning>()
    val warnings = _warnings.asSharedFlow()

    // A StateFlow to hold and expose the current connection status to the app.
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus = _connectionStatus.asStateFlow()

    suspend fun connect(userId: UUID) {
        if (_connectionStatus.value == ConnectionStatus.CONNECTED || _connectionStatus.value == ConnectionStatus.CONNECTING) {
            return
        }
        _connectionStatus.value = ConnectionStatus.CONNECTING
        try {
            client.webSocket(
                method = HttpMethod.Get,
                host = "172.17.2.88", // Your confirmed server IP
                port = 8000,
                path = "/api/v3/messages/ws/$userId"
            ) {
                session = this
                _connectionStatus.value = ConnectionStatus.CONNECTED
                println("Ktor WebSocket Connected!")

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()

                        // Logic to handle different types of incoming messages.
                        val jsonObject = gson.fromJson(text, Map::class.java)
                        when (jsonObject.get("type")) {
                            "video_call_invitation" -> {
                                val callerName = jsonObject["caller_name"] as String
                                val meetLink = jsonObject["meet_link"] as String
                                NotificationService.showVideoCallNotification(context, callerName, meetLink)
                            }
                            // NEW CASE: Handle moderation warnings
                            "moderation_warning" -> {
                                val warningMessage = jsonObject["message"] as String
                                _warnings.tryEmit(ChatWarning(warningMessage))
                            }
                            else -> {
                                val message = gson.fromJson(text, MessageResponse::class.java)
                                _messages.tryEmit(message)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Ktor WebSocket Error: ${e.localizedMessage}")
            _connectionStatus.value = ConnectionStatus.FAILED
        } finally {
            println("Ktor WebSocket Disconnected.")
            session = null
            if (_connectionStatus.value != ConnectionStatus.FAILED) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        }
    }

    suspend fun sendMessage(message: MessageCreate) {
        val jsonMessage = gson.toJson(message)
        session?.send(Frame.Text(jsonMessage))
    }

    suspend fun disconnect() {
        session?.close()
    }
}