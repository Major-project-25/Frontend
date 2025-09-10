package com.example.testapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    navController: NavController,
    requestsViewModel: RequestsViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId by sessionManager.getUserIdFlow.collectAsState(initial = null)

    // When the screen is first shown and we have a user ID, fetch the requests
    LaunchedEffect(userId) {
        userId?.let {
            requestsViewModel.fetchPendingRequests(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val state = requestsViewModel.uiState) {
                is RequestsUiState.Loading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator()
                    }
                }
                is RequestsUiState.Success -> {
                    RequestProfileCard(
                        request = state.request,
                        onNavigatePrevious = { requestsViewModel.navigateToPreviousRequest() },
                        onNavigateNext = { requestsViewModel.navigateToNextRequest() },
                        onAccept = {
                            userId?.let { requestsViewModel.respondToCurrentRequest(it, wasAccepted = true) }
                        },
                        onDecline = {
                            userId?.let { requestsViewModel.respondToCurrentRequest(it, wasAccepted = false) }
                        }
                    )
                }
                is RequestsUiState.Empty -> {
                    // This handles the "No Requests" screen
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Requests", color = Color.Gray)
                    }
                }
                is RequestsUiState.Error -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Something went wrong. Please try again.", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun RequestProfileCard(
    request: PendingRequestDetail,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header ---
        Text(text = "Requests", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "These individuals appear to have shared interests.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // --- Profile Picture and Navigation ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onNavigatePrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous", modifier = Modifier.size(48.dp))
            }
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(150.dp),
                tint = Color(0xFF3F51B5) // Example color
            )
            IconButton(onClick = onNavigateNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", modifier = Modifier.size(48.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- Details ---
        Text(text = request.universityRegNo ?: "N/A", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(24.dp))

        val interests = listOfNotNull(request.interest1, request.interest2, request.interest3)
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(text = "Bio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = request.biography ?: "No bio.", fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Interests", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = interests.joinToString(", "), fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.weight(1f)) // Pushes the buttons to the bottom

        // --- Action Buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("Accept", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onDecline,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Refuse", fontSize = 18.sp)
            }
        }
    }
}