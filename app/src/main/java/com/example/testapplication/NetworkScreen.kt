package com.example.testapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.AccountCircle

@Composable
fun NetworkScreen(networkViewModel: NetworkViewModel = viewModel()) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // Collect the user ID from the flow
    val userId by sessionManager.getUserIdFlow.collectAsState(initial = null)

    // This will trigger the initial fetch only when the screen is first composed
    // and we have a valid userId.
    LaunchedEffect(userId) {
        // As soon as we have a non-null userId, fetch the matches
        userId?.let {
            networkViewModel.fetchInitialMatches(it)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = networkViewModel.uiState) {
            is NetworkUiState.Loading -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
            is NetworkUiState.Success -> {
                MatchProfileCard(
                    profile = state.profile,
                    onNavigatePrevious = { networkViewModel.navigateToPreviousProfile() },
                    onNavigateNext = { networkViewModel.navigateToNextProfile() },
                    onConnect = {
                        // Pass the current user's ID when connecting
                        userId?.let { networkViewModel.sendConnectionRequest(it) }
                    }
                )
            }
            is NetworkUiState.Empty -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("No new matches right now. Check back later!", textAlign = TextAlign.Center)
                }
            }
            is NetworkUiState.Error -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Something went wrong. Please try again.", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun MatchProfileCard(
    profile: MatchProfile,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    onConnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header ---
        Text(text = "Network", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Let's see who you'll team up with today!",
            fontSize = 16.sp,
            color = Color.Gray,
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
            // Placeholder for Profile Picture
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle, // This line uses the account icon
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Gray // Optional: to make it look like a placeholder
                )
            }
            IconButton(onClick = onNavigateNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", modifier = Modifier.size(48.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- Details ---
        Text(text = profile.usn, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(text = "Bio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = profile.bio, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Interests", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = profile.interests.joinToString(", "), fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.weight(1f)) // Pushes the button to the bottom

        // --- Connect Button ---
        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Connect", fontSize = 18.sp)
        }
    }
}