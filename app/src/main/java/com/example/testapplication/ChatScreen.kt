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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    val filePickerLauncher = rememberLauncherForActivityResult(
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

            val messageType = when {
                mimeType.startsWith("image/") -> "image"
                mimeType.startsWith("video/") -> "video"
                mimeType.startsWith("audio/") -> "audio"
                else -> "file"
            }

            chatViewModel.sendMediaMessage(currentUserId, friendId, it, mimeType, messageType)
            unreadCount = 0
        }
    }

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
                // Scroll to the newest message (index 0 because reverseLayout = true)
                listState.animateScrollToItem(0)
            }
            // Reset unread count when a new message arrives (or history loads)
            unreadCount = 0
        }
    }


    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                // If the user scrolls up to the newest message, clear the count
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
                    filePickerLauncher.launch("*/*")
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
                    reverseLayout = true // Newest messages at the bottom
                ) {
                    items(messages.reversed(), key = { message -> message.id }) { message ->
                        MessageBubbleWithMenu(
                            message = message,
                            onMediaClick = { msg ->
                                if (msg.mediaUrl == null) return@MessageBubbleWithMenu

                                when (msg.messageType) {
                                    "image" -> {
                                        val encodedUrl = URLEncoder.encode(msg.mediaUrl, "UTF-8")
                                        navController.navigate("media_viewer/$encodedUrl")
                                    }
                                    "video", "file", "audio" -> {
                                        try {
                                            uriHandler.openUri(msg.mediaUrl)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open file.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
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

            // Floating action button for unread messages (optional)
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
                            // Adjust padding based on the bottom bar's height
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
fun MessageBubbleWithMenu(message: Message, onMediaClick: (Message) -> Unit, onDelete: (Long) -> Unit) {
    val isMyMessage = message.author == MessageAuthor.ME
    val horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start

    var showMenuIcon by remember { mutableStateOf(false) }
    var showTimestampMenu by remember { mutableStateOf(false) }

    // Only allow deletion if it's my message and it's not currently sending
    val canDelete = isMyMessage && message.status != MessageStatus.SENDING

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically // Align items vertically
    ) {
        // Show options menu icon for the other user's messages
        if (!isMyMessage) {
            AnimatedVisibility(
                visible = showMenuIcon,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp) // Provide ample clickable area
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
                            onClick = { showTimestampMenu = false } // Just dismiss
                        )
                        // Add other options for received messages here if needed
                    }
                }
            }
        }

        // The actual message content bubble
        MessageBubbleContent(
            message = message,
            onBubbleClick = {
                // Only handle clicks if the message is fully sent
                if (message.status == MessageStatus.SENT) {
                    if (message.mediaUrl != null) {
                        onMediaClick(message) // Trigger media action
                    } else {
                        showMenuIcon = !showMenuIcon // Toggle menu visibility for text messages
                    }
                }
                // Do nothing if SENDING or FAILED
            },
            onBubbleLongPress = {
                // Always allow long press to show options, regardless of status
                showMenuIcon = true
                showTimestampMenu = true
            }
        )

        // Show status indicator and options menu icon for my messages
        if (isMyMessage) {
            // Status Indicator (Spinner or Error Icon)
            when (message.status) {
                MessageStatus.SENDING -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(horizontal = 4.dp),
                        strokeWidth = 2.dp
                    )
                }
                MessageStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Failed to send",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                MessageStatus.SENT -> {
                    // Don't show anything for sent messages here
                }
            }

            // Options Menu Icon (Timestamp, Delete)
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
                        HorizontalDivider() // Separator
                        if (canDelete) { // Only show delete if allowed
                            DropdownMenuItem(
                                text = { Text("Delete Message", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    onDelete(message.id)
                                    showTimestampMenu = false // Dismiss menu after action
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleContent(
    message: Message,
    onBubbleClick: () -> Unit,
    onBubbleLongPress: () -> Unit
) {
    val isMyMessage = message.author == MessageAuthor.ME
    val bubbleColor = if (isMyMessage) Color(0xFFD0F0C0) else Color(0xFFF0F0F0) // Light green for me, light gray for them
    // Define bubble shapes based on sender
    val shape = if (isMyMessage) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp) // Tail on bottom right
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp) // Tail on bottom left
    }

    // Make bubble semi-transparent if it's currently sending
    val bubbleAlpha = if (message.status == MessageStatus.SENDING) 0.5f else 1.0f

    Box(
        modifier = Modifier
            .clip(shape) // Apply the rounded corner shape
            .background(bubbleColor) // Set the background color
            .alpha(bubbleAlpha) // Apply transparency if sending
            .combinedClickable( // Handle both click and long press
                onClick = onBubbleClick,
                onLongClick = onBubbleLongPress
            )
            .widthIn(max = 280.dp) // Constrain the maximum width of the bubble
    ) {
        // Use a `when` block to display content based on message type
        when (message.messageType) {
            "text" -> {
                Text(
                    text = message.text ?: "", // Display the text content
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .wrapContentWidth(Alignment.Start) // Align text to the start
                )
            }
            "image" -> {
                // Display image using AsyncImage, inside a Column for potential future captions
                Column(modifier = Modifier.padding(4.dp)) { // Minimal padding around the image
                    AsyncImage(
                        model = message.mediaUrl, // Load image from URL
                        contentDescription = "Attachment",
                        modifier = Modifier
                            .size(200.dp) // Fixed size for the image preview
                            .clip(RoundedCornerShape(12.dp)), // Slightly rounded corners for the image itself
                        contentScale = ContentScale.Crop, // Crop image to fit the bounds
                    )
                    // If you want captions for images, add Text here conditional on message.text != null
                }
            }
            "video" -> {
                // Display video using the FileAttachmentBubble with a video icon
                FileAttachmentBubble(message = message, icon = Icons.Default.Videocam)
            }
            "file" -> {
                // Display file using the FileAttachmentBubble with the default document icon
                FileAttachmentBubble(message = message)
            }
            "audio" -> {
                // Display audio using the FileAttachmentBubble (could use a specific audio icon)
                // TODO: Replace Description icon with a dedicated audio icon if available
                FileAttachmentBubble(message = message, icon = Icons.Default.Description)
            }
            else -> {
                // Fallback for any unknown message types
                Text(
                    text = "Unsupported message type",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun FileAttachmentBubble(
    message: Message,
    icon: ImageVector = Icons.Default.Description // Default icon is a document
) {
    // Row to display icon and filename side-by-side
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp) // Standard padding within the bubble
            .wrapContentWidth(), // Bubble width adjusts to content
        verticalAlignment = Alignment.CenterVertically // Center icon and text vertically
    ) {
        Icon(
            imageVector = icon, // Use the provided icon (document, video, etc.)
            contentDescription = "File Attachment",
            modifier = Modifier.size(40.dp), // Fixed size for the icon
            tint = MaterialTheme.colorScheme.primary // Use primary color for the icon tint
        )
        Spacer(modifier = Modifier.width(8.dp)) // Space between icon and text
        Text(
            text = message.text ?: "file", // Display filename (stored in message.text) or "file" as fallback
            color = Color.Black,
            fontSize = 16.sp,
            maxLines = 2, // Allow up to two lines for long filenames
            overflow = TextOverflow.Ellipsis // Add ellipsis (...) if filename is too long
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenMediaViewer(navController: NavController, mediaUrl: String) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Viewer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Navigate back on click
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), // Apply padding from Scaffold
            contentAlignment = Alignment.Center // Center the content (the image)
        ) {
            ZoomableImage(mediaUrl) // Display the zoomable image component
        }
    }
}

@Composable
fun ZoomableImage(mediaUrl: String) {
    // State variables for scale and offset, remembered across recompositions
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val maxScale = 5f // Maximum zoom level
    val minScale = 1f // Minimum zoom level (original size)

    // Construct the full URL for the image (assuming it's relative)
    // NOTE: This might need adjustment if your URLs are already absolute
    val fullUrl = mediaUrl // If mediaUrl is already absolute, just use it

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Add pointer input detector for zoom and pan gestures
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Calculate new scale, clamping between min and max
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)

                    // Calculate maximum allowed translation based on new scale
                    val boundsWidth = size.width * (newScale - 1) / 2
                    val boundsHeight = size.height * (newScale - 1) / 2

                    // Calculate new offset, applying pan and clamping within bounds
                    val newOffset = if (newScale > 1f) {
                        Offset(
                            x = (offset.x + pan.x * newScale).coerceIn(-boundsWidth, boundsWidth),
                            y = (offset.y + pan.y * newScale).coerceIn(-boundsHeight, boundsHeight)
                        )
                    } else {
                        // Reset offset if scaled back to original size
                        Offset.Zero
                    }

                    // Update the state variables
                    scale = newScale
                    offset = newOffset
                }
            }
    ) {
        // Display the image using AsyncImage
        AsyncImage(
            model = fullUrl, // URL of the image
            contentDescription = "Chat Media",
            contentScale = ContentScale.Fit, // Fit the image within the bounds initially
            modifier = Modifier
                .fillMaxSize()
                // Apply graphics layer transformations for scale and translation
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
            // Row for profile icon and friend's name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(friendName, fontWeight = FontWeight.SemiBold) // Display friend's name
            }
        },
        navigationIcon = {
            // Back button
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // Video call button
            IconButton(onClick = onVideoCallClick) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Call")
            }
        },
        // Customize TopAppBar colors
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BubblesBlue, // Use the custom blue color
            titleContentColor = Color.Black,
            navigationIconContentColor = Color.Black,
            actionIconContentColor = Color.Black
        )
    )
}

@Composable
fun ChatInputBar(onSendMessage: (String) -> Unit, onPlusClick: () -> Unit) {
    // State for the text field content
    var text by remember { mutableStateOf("") }

    // Surface for elevation and background color
    Surface(
        shadowElevation = 8.dp, // Add a shadow
        color = BubblesBlue // Use the custom blue color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp), // Padding around the input bar
            verticalAlignment = Alignment.CenterVertically // Center items vertically
        ) {
            // Attachment button (+)
            IconButton(onClick = onPlusClick) {
                Icon(Icons.Default.Add, contentDescription = "Attach")
            }
            // Text input field
            TextField(
                value = text,
                onValueChange = { text = it }, // Update state on text change
                modifier = Modifier.weight(1f), // Take up remaining space
                placeholder = { Text("Message...") }, // Hint text
                shape = RoundedCornerShape(24.dp), // Rounded corners
                // Customize TextField colors for a clean look
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent, // No underline
                    unfocusedIndicatorColor = Color.Transparent // No underline
                )
            )
            // Send button
            IconButton(onClick = {
                if (text.isNotBlank()) { // Only send if text is not empty
                    onSendMessage(text) // Trigger send action
                    text = "" // Clear the text field
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
            }
        }
    }
}