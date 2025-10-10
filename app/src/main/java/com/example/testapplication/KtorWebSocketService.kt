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

    // NEW: A StateFlow to hold and expose the current connection status to the app.
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus = _connectionStatus.asStateFlow()

    suspend fun connect(userId: UUID) {
        // Prevent trying to connect if a connection is already active or in progress.
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

                // This loop listens for incoming messages for the duration of the connection.
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
                            else -> {
                                val message = gson.fromJson(text, MessageResponse::class.java)
                                _messages.tryEmit(message)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // If any error occurs (e.g., network issue, server down), update the status.
            println("Ktor WebSocket Error: ${e.localizedMessage}")
            _connectionStatus.value = ConnectionStatus.FAILED
        } finally {
            // This block runs when the connection is closed for any reason.
            println("Ktor WebSocket Disconnected.")
            session = null
            // Only set to DISCONNECTED if it wasn't a FAILED state.
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