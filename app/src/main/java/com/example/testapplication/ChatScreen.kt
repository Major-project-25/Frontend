package com.example.testapplication

import android.net.Uri // ADDED
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult // ADDED
import androidx.activity.result.contract.ActivityResultContracts // ADDED
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale // ADDED
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.snapshotFlow // ADDED
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID
import coil.compose.AsyncImage // ADDED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    friendName: String,
    friendId: UUID,
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId by sessionManager.getUserIdFlow.collectAsState(initial = null)
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    // --- NEW STATE: Unread Message Count ---
    var unreadCount by remember { mutableIntStateOf(0) }
    val lastKnownMessageCount = remember { mutableIntStateOf(0) }
    // --- END NEW STATE ---

    // --- NEW: File Picker Launcher ---
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val currentUserId = userId
            if (currentUserId == null) {
                Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()
                return@let
            }

            // Determine file details and initiate upload
            val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
            // NOTE: getFile is assumed to be available from FileUtility.kt
            val file = context.contentResolver.getFile(context, it)

            val messageType = when {
                mimeType.startsWith("image/") -> "image"
                mimeType.startsWith("video/") -> "video"
                else -> "file"
            }

            chatViewModel.sendMediaMessage(currentUserId, friendId, it, mimeType, messageType)
            unreadCount = 0 // Reset count since sender is viewing screen
        }
    }
    // --- END NEW LAUNCHER ---

    LaunchedEffect(userId, friendId) {
        userId?.let {
            chatViewModel.loadChat(it, friendId)
        }
    }

    // --- LOGIC 1: Initial Load Scroll & New Message Detection (FOR INDICATOR) ---
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.size > lastKnownMessageCount.intValue) {
            // A new message has arrived (WebSocket or optimistic send)
            val isAtBottom = listState.firstVisibleItemIndex == 0

            if (lastKnownMessageCount.intValue == 0) {
                // Case A: Initial message load (history fetch). Always scroll to bottom.
                coroutineScope.launch { listState.scrollToItem(0) }
                unreadCount = 0
            } else if (isAtBottom) {
                // Case B: New message arrived, and user is already at the bottom.
                coroutineScope.launch { listState.animateScrollToItem(0) }
                unreadCount = 0
            } else {
                // Case C: New message arrived, and user is scrolled up.
                unreadCount++
            }
        }
        lastKnownMessageCount.intValue = uiState.messages.size
    }

    // --- LOGIC 2: Reset unread count when the user scrolls to the bottom ---
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                if (firstVisibleIndex == 0) {
                    if (unreadCount > 0) {
                        unreadCount = 0
                    }
                }
            }
    }
    // --- END NEW LOGIC ---

    Scaffold(
        topBar = {
            ChatTopBar(
                navController = navController,
                friendName = friendName,
                onVideoCallClick = {
                    // When the video icon is clicked, call the API
                    if (userId != null) {
                        RetrofitInstance.api.getVideoCallLink(userId!!, friendId).enqueue(object : Callback<MeetLinkResponse> {
                            override fun onResponse(call: Call<MeetLinkResponse>, response: Response<MeetLinkResponse>) {
                                response.body()?.meetLink?.let { link ->
                                    // Open the received link in the browser
                                    uriHandler.openUri(link)
                                }
                            }
                            override fun onFailure(call: Call<MeetLinkResponse>, t: Throwable) {
                                Toast.makeText(context, "Could not generate video call link.", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            )
        },
        bottomBar = {
            // UPDATED: Pass the onPlusClick lambda
            ChatInputBar(
                onSendMessage = { messageContent ->
                    userId?.let {
                        chatViewModel.sendMessage(friendId, messageContent, it)
                        unreadCount = 0
                    }
                },
                onPlusClick = {
                    // Trigger the file picker
                    filePickerLauncher.launch("image/*,video/*")
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null) {
                Text("Something went wrong. Please try again.", textAlign = TextAlign.Center)
            } else if (uiState.messages.isEmpty()) {
                Text(
                    text = "No messages here yet.\nBe the first to say hi!",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    reverseLayout = true
                ) {
                    items(uiState.messages.reversed()) { message ->
                        MessageBubble(message = message)
                    }
                }
            }

            // --- UI: Floating Button/Badge for Manual Scroll ---
            if (unreadCount > 0) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            // Manual scroll to the newest message (index 0)
                            listState.animateScrollToItem(0)
                            unreadCount = 0
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        // Position the button above the input bar and inside the padding
                        .padding(
                            end = 16.dp,
                            bottom = innerPadding.calculateBottomPadding() + 8.dp
                        )
                        .size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = unreadCount.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }
            // --- END NEW UI ---
        }
    }
}


@Composable
fun MessageBubble(message: Message) {
    val isMyMessage = message.author == MessageAuthor.ME
    val bubbleColor = if (isMyMessage) Color(0xFFD0F0C0) else Color(0xFFF0F0F0)
    val horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
    val shape = if (isMyMessage) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column { // Use a Column to stack media and text
                // NEW: Display media if present
                if (message.mediaUrl != null && message.messageType != "text") {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "${message.messageType} attachment",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Only show text content if it exists
                if (!message.text.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = message.text, color = Color.Black, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = message.timestamp, color = Color.Gray, fontSize = 10.sp)
                    }
                } else if (message.mediaUrl != null && message.messageType != "text") {
                    // For media-only messages, just show the timestamp
                    Text(text = message.timestamp, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    navController: NavController,
    friendName: String,
    onVideoCallClick: () -> Unit // Add this callback
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(friendName, fontWeight = FontWeight.SemiBold)
            }
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // Update the IconButton to use the callback
            IconButton(onClick = onVideoCallClick) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Call")
            }
        }
    )
}

// UPDATED: Added onPlusClick parameter
@Composable
fun ChatInputBar(onSendMessage: (String) -> Unit, onPlusClick: () -> Unit) {
    var text by remember { mutableStateOf("") }

    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mapped '+' icon to the new action
            IconButton(onClick = onPlusClick) {
                Icon(Icons.Default.Add, contentDescription = "Attach")
            }
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...") },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    onSendMessage(text)
                    text = ""
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
            }
        }
    }
}