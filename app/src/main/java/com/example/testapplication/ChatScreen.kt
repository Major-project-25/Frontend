package com.example.testapplication

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

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

    LaunchedEffect(userId, friendId) {
        userId?.let {
            chatViewModel.loadChat(it, friendId)
        }
    }

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
            ChatInputBar(onSendMessage = { messageContent ->
                userId?.let {
                    chatViewModel.sendMessage(friendId, messageContent, it)
                }
            })
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
                LaunchedEffect(uiState.messages.size) {
                    if (uiState.messages.isNotEmpty()) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            }
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = message.text, color = Color.Black, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = message.timestamp, color = Color.Gray, fontSize = 10.sp)
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

@Composable
fun ChatInputBar(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO: Plus action */ }) {
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