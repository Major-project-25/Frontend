package com.example.testapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.clickable

@Composable
fun FriendsList(friends: List<String>, navController: NavController) { // 1. Add NavController here
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(friends) { usn ->
            // 2. Pass a clickable modifier to FriendRow
            FriendRow(
                usn = usn,
                modifier = Modifier.clickable {
                    navController.navigate("chat/$usn")
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

// 3. Update FriendRow to accept and use the modifier
@Composable
fun FriendRow(usn: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier // Apply the modifier here
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
        Text(
            text = usn,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId by sessionManager.getUserIdFlow.collectAsState(initial = null)

    // When the screen is first shown and we have a user ID, fetch the friends list
    LaunchedEffect(userId) {
        userId?.let {
            homeViewModel.fetchFriends(it)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(navController = navController)

        when (val state = homeViewModel.uiState) {
            is HomeUiState.Loading -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
            is HomeUiState.Success -> {
                // CORRECTED: Pass the navController here
                FriendsList(friends = state.friendUsns, navController = navController)
            }
            is HomeUiState.Empty -> {
                EmptyChatView(navController = navController)
            }
            is HomeUiState.Error -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Something went wrong. Please try again.", textAlign = TextAlign.Center)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(navController: NavController) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "Chats",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White
            )
        },
        actions = {
            IconButton(
                onClick = { navController.navigate("requests") },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Requests",
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFF0077FF)
        )
    )
}

@Composable
fun EmptyChatView(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Empty chat", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("network") },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
        ) {
            Text(
                "Network",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                fontSize = 16.sp
            )
        }
    }
}