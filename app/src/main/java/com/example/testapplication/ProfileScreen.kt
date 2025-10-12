package com.example.testapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
// FIX: Using AutoMirrored version is best practice
import androidx.compose.material.icons.automirrored.filled.ExitToApp // <-- FIX 1: Use AutoMirrored
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope // <-- FIX 2: Missing import

data class UserInterest(val name: String, val rating: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId by sessionManager.getUserIdFlow.collectAsState(initial = null)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val coroutineScope = rememberCoroutineScope() // <-- FIX 3: Coroutine scope is now imported

    LaunchedEffect(navBackStackEntry, userId) {
        userId?.let {
            profileViewModel.fetchUserProfile(it)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        // Logout logic
                        coroutineScope.launch {
                            sessionManager.setLoggedIn(false)
                            sessionManager.setAdminStatus(false)
                            // Navigate to splash screen, clearing the back stack
                            navController.navigate("splash") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    }) {
                        // FIX: Use the imported AutoMirrored icon to resolve deprecation
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out"
                        )
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
            when (val state = profileViewModel.uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is ProfileUiState.Success -> {
                    val profile = state.profile
                    val interests = listOfNotNull(
                        profile.interest1?.let { UserInterest(it, profile.interest1Weight ?: 0) },
                        profile.interest2?.let { UserInterest(it, profile.interest2Weight ?: 0) },
                        profile.interest3?.let { UserInterest(it, profile.interest3Weight ?: 0) }
                    )

                    UserProfileContent(
                        name = profile.name,
                        usn = profile.universityRegNo,
                        bio = profile.biography ?: "No bio provided.",
                        interests = interests,
                        navController = navController
                    )
                }
                is ProfileUiState.Error -> {
                    Text("Failed to load profile. Please try again.", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun UserProfileContent(
    name: String,
    usn: String,
    bio: String,
    interests: List<UserInterest>,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = usn, fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(text = "Bio", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(text = bio, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(text = "Interests", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            interests.forEach { interest ->
                InterestRow(interest = interest)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navController.navigate("edit_profile") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Edit Profile", fontSize = 18.sp)
        }
    }
}

@Composable
fun InterestRow(interest: UserInterest) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = interest.name, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            // FIX: Use the modern Material3 LinearProgressIndicator which takes a progress float
            LinearProgressIndicator(
                progress = interest.rating / 10f,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = interest.rating.toString(), fontWeight = FontWeight.Bold)
    }
}