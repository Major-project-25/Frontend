package com.example.testapplication

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.util.UUID
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.compose.currentBackStackEntryAsState


@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId by sessionManager.getUserIdFlow.collectAsState(initial = null)

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(userId) {
        userId?.let {
            homeViewModel.fetchFriends(it)
        }
    }

    LaunchedEffect(navBackStackEntry, userId) {
        userId?.let {
            homeViewModel.checkForPendingRequests(it)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(
            navController = navController,
            hasPendingRequests = homeViewModel.hasPendingRequests
        )

        when (val state = homeViewModel.uiState) {
            is HomeUiState.Loading -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
            is HomeUiState.Success -> {
                FriendsList(
                    friends = state.friends,
                    unreadCounts = state.unreadCounts,
                    navController = navController
                )
            }
            is HomeUiState.Empty -> {
                EmptyChatView()
            }
            is HomeUiState.Error -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Something went wrong. Please try again.", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun FriendsList(
    friends: List<FriendDetail>,
    unreadCounts: Map<UUID, Int>,
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(friends) { friend ->
            val count = unreadCounts[friend.userId] ?: 0

            FriendRow(
                friend = friend,
                unreadCount = count,
                modifier = Modifier.clickable {
                    val friendName = friend.name ?: "User"
                    val friendId = friend.userId
                    navController.navigate("chat/$friendName/$friendId")
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun FriendRow(
    friend: FriendDetail,
    unreadCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile Icon",
            modifier = Modifier.size(48.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = friend.name ?: "Unknown User",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = friend.universityRegNo ?: "",
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00C853)), // A bright green color
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unreadCount.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    navController: NavController,
    hasPendingRequests: Boolean
) {
    CenterAlignedTopAppBar(
        title = {
            Text("Chats", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
        },
        actions = {
            Box {
                IconButton(onClick = { navController.navigate("requests") }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Requests",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }

                if (hasPendingRequests) {
                    Box(
                        modifier = Modifier
                            // --- THIS IS THE ONLY CHANGE ---
                            .size(14.dp) // <-- Made the dot bigger (was 12.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .border(2.dp, Color(0xFF0077FF), CircleShape)
                            .align(Alignment.TopEnd) // <-- Moved back to TopEnd
                            .offset(x = (-6).dp, y = 6.dp) // <-- Adjusted offset for new size
                        // --- END OF CHANGE ---
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0077FF))
    )
}

@Composable
fun EmptyChatView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Empty chat", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                Toast.makeText(
                    context,
                    "Ready to connect? Your matches are waiting in the network page!\uD83D\uDE0A", // Motivational message
                    Toast.LENGTH_SHORT
                ).show()
                //navController.navigate("network")
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
        ) {
            Text("Guide", modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), fontSize = 16.sp)
        }
    }
}