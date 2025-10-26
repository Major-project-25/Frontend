package com.example.testapplication

import android.content.Context
import android.util.Log // --- NEW IMPORT ---
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
import kotlinx.coroutines.flow.update
import java.util.*

data class ChatWarning(val message: String)
enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}

object KtorWebSocketService {
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000
        }
    }
    private val gson = Gson()
    private var session: DefaultClientWebSocketSession? = null

    // --- ADD A TAG FOR LOGGING ---
    private const val TAG = "KtorWebSocketService"

    private var appContext: Context? = null
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val _messages = MutableSharedFlow<MessageResponse>(replay = 1)
    val messages = _messages.asSharedFlow()

    private val _warnings = MutableSharedFlow<ChatWarning>(replay = 1)
    val warnings = _warnings.asSharedFlow()

    private val _deletionEvents = MutableSharedFlow<Long>(replay = 0)
    val deletionEvents = _deletionEvents.asSharedFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _currentActiveChatId = MutableStateFlow<UUID?>(null)

    private val _unreadCounts = MutableStateFlow<Map<UUID, Int>>(emptyMap())
    val unreadCounts = _unreadCounts.asStateFlow() // This remains a read-only StateFlow


    suspend fun connect(userId: UUID) {
        if (_connectionStatus.value == ConnectionStatus.CONNECTED || _connectionStatus.value == ConnectionStatus.CONNECTING) {
            Log.d(TAG, "Connect called, but already connected or connecting.")
            return
        }
        Log.d(TAG, "Connecting with userId: $userId")
        _connectionStatus.value = ConnectionStatus.CONNECTING
        try {
            client.webSocket(
                method = HttpMethod.Get,
                host = "172.17.0.176",
                port = 8000,
                path = "/api/v3/messages/ws/$userId"
            ) {
                session = this
                _connectionStatus.value = ConnectionStatus.CONNECTED
                Log.d(TAG, "--- WebSocket Connected! ---") // <-- SUCCESS LOG

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        // --- REMOVED THE OLD PRINTLN, REPLACED WITH LOG.D ---
                        Log.d(TAG, "<<<--- RAW MESSAGE RECEIVED: $text")

                        try {
                            val jsonObject = gson.fromJson(text, Map::class.java)
                            when (jsonObject["type"] as? String) {
                                "video_call_invitation" -> {
                                    val callerName = jsonObject["caller_name"] as String
                                    val meetLink = jsonObject["meet_link"] as String

                                    appContext?.let {
                                        NotificationService.showVideoCallNotification(it, callerName, meetLink)
                                    }
                                }
                                "moderation_warning" -> {
                                    val warningMessage = jsonObject["message"] as String
                                    _warnings.emit(ChatWarning(warningMessage))
                                }
                                "message_deleted" -> {
                                    val messageId = (jsonObject["message_id"] as? Double)?.toLong()
                                    if (messageId != null) {
                                        _deletionEvents.emit(messageId)
                                    }
                                }
                                else -> {
                                    val message = gson.fromJson(text, MessageResponse::class.java)
                                    _messages.emit(message)

                                    val isUnread = message.senderId != _currentActiveChatId.value
                                    val isFromOtherUser = message.senderId != userId

                                    if (isUnread && isFromOtherUser) {
                                        _unreadCounts.update { currentMap ->
                                            val newMap = currentMap.toMutableMap()
                                            val currentCount = newMap[message.senderId] ?: 0
                                            newMap[message.senderId] = currentCount + 1
                                            newMap
                                        }
                                    }
                                }
                            }
                        } catch (e: JsonSyntaxException) {
                            Log.e(TAG, "!!! GSON PARSING FAILED: ${e.message}")
                        } catch (e: Exception) {
                            Log.e(TAG, "!!! ERROR PROCESSING FRAME: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ktor WebSocket Error: ${e.localizedMessage}", e) // <-- DETAILED ERROR LOG
            _connectionStatus.value = ConnectionStatus.FAILED
        } finally {
            Log.d(TAG, "--- WebSocket Disconnected (Finally Block) ---") // <-- DISCONNECT LOG
            session = null
            if (_connectionStatus.value != ConnectionStatus.FAILED) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        }
    }

    suspend fun sendMessage(message: MessageCreate) {
        val jsonMessage = gson.toJson(message)

        // --- THIS IS THE KEY LOGIC TO CONFIRM OUR THEORY ---
        if (session != null) {
            Log.d(TAG, "---> SENDING MESSAGE: $jsonMessage")
            session?.send(Frame.Text(jsonMessage))
        } else {
            Log.e(TAG, "!!! FAILED TO SEND: Session is null. WebSocket is disconnected.")
        }
        // --- END KEY LOGIC ---
    }

    suspend fun disconnect() {
        Log.d(TAG, "Disconnect called manually.")
        session?.close()
        _unreadCounts.value = emptyMap()
        _currentActiveChatId.value = null
    }

    fun setCurrentActiveChat(chatId: UUID?) {
        _currentActiveChatId.value = chatId
    }

    fun clearUnreadCountFor(chatId: UUID) {
        _unreadCounts.update { currentMap ->
            val newMap = currentMap.toMutableMap()
            if (newMap.containsKey(chatId)) {
                newMap[chatId] = 0
            }
            newMap
        }
    }

    fun mergeInitialCounts(initialCounts: Map<UUID, Int>) {
        _unreadCounts.update { currentWebSocketCounts ->
            val newMap = initialCounts.toMutableMap()
            newMap.putAll(currentWebSocketCounts)
            newMap
        }
    }
}