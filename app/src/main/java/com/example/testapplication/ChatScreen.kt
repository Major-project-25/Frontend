package com.example.testapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.AccountCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, friendUsn: String) {
    // Updated dummy data to match the new Message data class
    val sampleMessages = listOf(
        Message(text = "I think top two are:", author = MessageAuthor.ME, timestamp = "11:50"),
        Message(text = "Do you like it?", author = MessageAuthor.THEM, timestamp = "11:45"),
        Message(text = "What is the most popular meal in Japan?", author = MessageAuthor.THEM, timestamp = "11:45"),
        Message(text = "It's morning in Tokyo 😎", author = MessageAuthor.ME, timestamp = "11:43"),
        Message(text = "Do you know what time it is?", author = MessageAuthor.THEM, timestamp = "11:40"),
        Message(text = "Japan looks amazing!", author = MessageAuthor.ME, timestamp = "10:10", date = "Fri, Jul 26")
    )

    Scaffold(
        topBar = {
            ChatTopBar(
                navController = navController,
                friendUsn = friendUsn
            )
        },
        bottomBar = {
            ChatInputBar()
        }
    ) { innerPadding ->
        // Use a Box to handle the empty state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (sampleMessages.isEmpty()) {
                Text(
                    text = "No messages here yet.\nBe the first to say hi!",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    reverseLayout = true
                ) {
                    items(sampleMessages) { message ->
                        Column {
                            MessageBubble(message = message)
                            message.date?.let {
                                DateDivider(date = it)
                            }
                        }
                    }
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
    val alignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
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
fun ChatInputBar() {
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
            IconButton(onClick = { /* Handle send message */ }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
            }
        }
    }
}