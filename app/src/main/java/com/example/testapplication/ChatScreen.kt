package com.example.testapplication

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
// REMOVED: Audiotrack and Description icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
// REMOVED: ImageVector import
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
// REMOVED: TextOverflow import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID
import coil.compose.AsyncImage
import java.net.URLEncoder
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.ExperimentalFoundationApi

private val BubblesBlue = Color(0xFFE5F3FD)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val messages = uiState.messages

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }

    var unreadCount by remember { mutableIntStateOf(0) }

    // --- REVERTED LAUNCHER ---
    val filePickerLauncher = rememberLauncherForActivityResult(
        // Back to image and video only
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val currentUserId = userId
            if (currentUserId == null) {
                Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()
                return@let
            }

            val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"

            @Suppress("UNUSED_VARIABLE")
            val file = context.contentResolver.getFile(context, it)

            // Determine the messageType based on the MIME type
            val messageType = when {
                mimeType.startsWith("image/") -> "image"
                mimeType.startsWith("video/") -> "video"
                // REMOVED: Audio and File cases
                else -> "file" // Default, though shouldn't happen with this launcher
            }

            chatViewModel.sendMediaMessage(currentUserId, friendId, it, mimeType, messageType)
            unreadCount = 0
        }
    }
    // --- END REVERTED LAUNCHER ---

    LaunchedEffect(userId, friendId) {
        userId?.let {
            chatViewModel.loadChat(it, friendId)
        }
    }

    LaunchedEffect(uiState.moderationWarning) {
        uiState.moderationWarning?.let { warning ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = warning,
                    duration = SnackbarDuration.Long,
                    actionLabel = "DISMISS"
                )
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
            unreadCount = 0
        }
    }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatTopBar(
                navController = navController,
                friendName = friendName,
                onVideoCallClick = {
                    if (userId != null) {
                        RetrofitInstance.api.getVideoCallLink(userId!!, friendId).enqueue(object : Callback<MeetLinkResponse> {
                            override fun onResponse(call: Call<MeetLinkResponse>, response: Response<MeetLinkResponse>) {
                                response.body()?.meetLink?.let { link ->
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
            ChatInputBar(
                onSendMessage = { messageContent ->
                    userId?.let {
                        chatViewModel.sendMessage(friendId, messageContent, it)
                    }
                },
                onPlusClick = {
                    // Back to image/video only
                    filePickerLauncher.launch("image/*,video/*")
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null) {
                Text("Something went wrong. Please try again.", textAlign = TextAlign.Center)
            } else if (messages.isEmpty()) {
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
                    items(messages.reversed()) { message ->
                        MessageBubbleWithMenu(
                            message = message,
                            onMediaClick = { mediaUrl ->
                                // Only image/video are possible now
                                val encodedUrl = URLEncoder.encode(mediaUrl, "UTF-8")
                                navController.navigate("media_viewer/$encodedUrl")
                            },
                            onDelete = { messageId: Long ->
                                userId?.let {
                                    chatViewModel.deleteMessage(messageId, it)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            if (unreadCount > 0) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                            unreadCount = 0
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleWithMenu(message: Message, onMediaClick: (String) -> Unit, onDelete: (Long) -> Unit) {
    val isMyMessage = message.author == MessageAuthor.ME
    val horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start

    var showMenuIcon by remember { mutableStateOf(false) }
    var showTimestampMenu by remember { mutableStateOf(false) }

    val canDelete = isMyMessage

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isMyMessage) {
            AnimatedVisibility(
                visible = showMenuIcon,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { showTimestampMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                    DropdownMenu(
                        expanded = showTimestampMenu,
                        onDismissRequest = { showTimestampMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sent: ${message.timestamp}") },
                            onClick = { showTimestampMenu = false }
                        )
                    }
                }
            }
        }

        MessageBubbleContent(
            message = message,
            onMediaClick = onMediaClick,
            onBubbleClick = { showMenuIcon = !showMenuIcon },
            onBubbleLongPress = {
                showMenuIcon = true
                showTimestampMenu = true
            }
        )

        if (isMyMessage) {
            AnimatedVisibility(
                visible = showMenuIcon,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { showTimestampMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                    DropdownMenu(
                        expanded = showTimestampMenu,
                        onDismissRequest = { showTimestampMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sent: ${message.timestamp}") },
                            onClick = { showTimestampMenu = false }
                        )
                        HorizontalDivider()
                        if (canDelete) {
                            DropdownMenuItem(
                                text = { Text("Delete Message", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    onDelete(message.id)
                                    showTimestampMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- REVERTED COMPOSABLE ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleContent(
    message: Message,
    onMediaClick: (String) -> Unit,
    onBubbleClick: () -> Unit,
    onBubbleLongPress: () -> Unit
) {
    val isMyMessage = message.author == MessageAuthor.ME
    val bubbleColor = if (isMyMessage) Color(0xFFD0F0C0) else Color(0xFFF0F0F0)
    val shape = if (isMyMessage) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp)
    }

    val isClickable = message.messageType != "text"

    Box(
        modifier = Modifier
            .clip(shape)
            .background(bubbleColor)
            .combinedClickable(
                onClick = {
                    if (isClickable && message.mediaUrl != null) {
                        onMediaClick(message.mediaUrl)
                    } else {
                        onBubbleClick()
                    }
                },
                onLongClick = onBubbleLongPress
            )
            .widthIn(max = 280.dp) // Max width constraint
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

            // Display media if present (image or video)
            if (message.mediaUrl != null && (message.messageType == "image" || message.messageType == "video")) {
                AsyncImage(
                    model = message.mediaUrl,
                    contentDescription = "Attachment",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Text Content (Only if messageType is 'text')
            if (message.messageType == "text" && !message.text.isNullOrBlank()) {
                Text(
                    text = message.text, // Safe non-null assertion removed
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.wrapContentWidth(Alignment.Start)
                )
            }
        }
    }
}
// --- END REVERTED COMPOSABLE ---

// REMOVED: FileAttachmentBubble composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenMediaViewer(navController: NavController, mediaUrl: String) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Viewer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
            ZoomableImage(mediaUrl)
        }
    }
}

@Composable
fun ZoomableImage(mediaUrl: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val maxScale = 5f
    val minScale = 1f

    val fullUrl = mediaUrl

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)

                    val boundsWidth = size.width * (newScale - 1) / 2
                    val boundsHeight = size.height * (newScale - 1) / 2

                    val newOffset = if (newScale > 1f) {
                        Offset(
                            x = (offset.x + pan.x * newScale).coerceIn(-boundsWidth, boundsWidth),
                            y = (offset.y + pan.y * newScale).coerceIn(-boundsHeight, boundsHeight)
                        )
                    } else {
                        Offset.Zero
                    }

                    scale = newScale
                    offset = newOffset
                }
            }
    ) {
        AsyncImage(
            model = fullUrl,
            contentDescription = "Chat Media",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    navController: NavController,
    friendName: String,
    onVideoCallClick: () -> Unit
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
            IconButton(onClick = onVideoCallClick) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Call")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BubblesBlue,
            titleContentColor = Color.Black,
            navigationIconContentColor = Color.Black,
            actionIconContentColor = Color.Black
        )
    )
}

@Composable
fun ChatInputBar(onSendMessage: (String) -> Unit, onPlusClick: () -> Unit) {
    var text by remember { mutableStateOf("") }

    Surface(
        shadowElevation = 8.dp,
        color = BubblesBlue
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
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