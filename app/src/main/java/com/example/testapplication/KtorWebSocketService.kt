package com.example.testapplication

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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

data class ChatWarning(val message: String)
enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}

class KtorWebSocketService(private val context: Context) {
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000
        }
    }
    private val gson = Gson()
    private var session: DefaultClientWebSocketSession? = null

    private val _messages = MutableSharedFlow<MessageResponse>(replay = 1)
    val messages = _messages.asSharedFlow()

    private val _warnings = MutableSharedFlow<ChatWarning>(replay = 1)
    val warnings = _warnings.asSharedFlow()

    // --- NEW: Deletion Event Flow ---
    private val _deletionEvents = MutableSharedFlow<Long>(replay = 0)
    val deletionEvents = _deletionEvents.asSharedFlow()
    // -----------------------------------

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
                host = "172.17.3.216", // Your confirmed server IP
                port = 8000,
                path = "/api/v3/messages/ws/$userId"
            ) {
                session = this
                _connectionStatus.value = ConnectionStatus.CONNECTED
                println("Ktor WebSocket Connected!")

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        println("<<<--- RAW MESSAGE RECEIVED FROM SERVER: $text")

                        try {
                            val jsonObject = gson.fromJson(text, Map::class.java)
                            when (jsonObject["type"] as? String) {
                                "video_call_invitation" -> {
                                    val callerName = jsonObject["caller_name"] as String
                                    val meetLink = jsonObject["meet_link"] as String
                                    NotificationService.showVideoCallNotification(context, callerName, meetLink)
                                }
                                "moderation_warning" -> {
                                    val warningMessage = jsonObject["message"] as String
                                    _warnings.emit(ChatWarning(warningMessage))
                                }
                                "message_deleted" -> { // <-- NEW CASE: Handles the deletion notification
                                    // Message IDs often come back as Doubles from generic JSON parsing
                                    val messageId = (jsonObject["message_id"] as? Double)?.toLong()
                                    if (messageId != null) {
                                        _deletionEvents.emit(messageId) // Notify the ViewModel to remove the message
                                    }
                                }
                                else -> {
                                    val message = gson.fromJson(text, MessageResponse::class.java)
                                    _messages.emit(message)
                                }
                            }
                        } catch (e: JsonSyntaxException) {
                            println("!!! GSON PARSING FAILED: ${e.message}")
                        } catch (e: Exception) {
                            println("!!! ERROR PROCESSING FRAME: ${e.message}")
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