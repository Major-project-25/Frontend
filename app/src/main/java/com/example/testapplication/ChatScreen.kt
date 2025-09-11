package com.example.testapplication

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.util.UUID

// Note: You will need to get the friend's UUID to fetch the chat history.
// For now, we will pass the USN, but a real app would need the ID.
// This is a placeholder until you have a way to get a user's ID from their USN.
val friendIdPlaceholder = UUID.randomUUID() // Replace this later

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    friendUsn: String,
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId by sessionManager.getUserIdFlow.collectAsState(initial = null)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Fetch the chat history when the screen is first launched with a valid user ID.
    LaunchedEffect(userId) {
        userId?.let {
            // In a real app, you would get the friend's UUID from their USN.
            // For now, we use a placeholder.
            chatViewModel.fetchHistory(it, friendIdPlaceholder)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                navController = navController,
                friendUsn = friendUsn
            )
        },
        bottomBar = {
            ChatInputBar(onSendMessage = { messageContent ->
                userId?.let {
                    chatViewModel.sendMessage(it, friendIdPlaceholder, messageContent)
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
            when (val state = chatViewModel.uiState) {
                is ChatUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is ChatUiState.Success -> {
                    if (state.messages.isEmpty()) {
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
                            items(state.messages.reversed()) { message ->
                                Column {
                                    MessageBubble(message = message)
                                    message.date?.let {
                                        DateDivider(date = it)
                                    }
                                }
                            }
                        }
                        // Scroll to the bottom when new messages arrive
                        LaunchedEffect(state.messages.size) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    }
                }
                is ChatUiState.Error -> {
                    Text("Something went wrong. Please try again.", textAlign = TextAlign.Center)
                }
            }
        }
    }
}


@Composable
fun DateDivider(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF0F0F0))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isMyMessage = message.author == MessageAuthor.ME
    val bubbleColor = if (isMyMessage) Color(0xFFD0F0C0) else Color(0xFFF0F0F0) // Light green / light gray
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
fun ChatTopBar(navController: NavController, friendUsn: String) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(friendUsn, fontWeight = FontWeight.SemiBold)
            }
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: Video call action */ }) {
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
                    text = "" // Clear the input field after sending
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
            }
        }
    }
}