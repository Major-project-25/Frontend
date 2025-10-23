package com.example.testapplication

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    editViewModel: EditProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId by sessionManager.getUserIdFlow.collectAsState(initial = null)

    // When the screen is first shown, load the user's current profile data
    LaunchedEffect(userId) {
        userId?.let {
            editViewModel.loadInitialProfile(it)
        }
    }

    // Listen for the UpdateSuccess state to show a message and navigate back
    val uiState = editViewModel.uiState
    LaunchedEffect(uiState) {
        if (uiState is EditProfileUiState.UpdateSuccess) {
            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
            editViewModel.onNavigationDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (uiState is EditProfileUiState.Success) {
                        userId?.let { editViewModel.updateProfile(it, (uiState as EditProfileUiState.Success).profile) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Save Changes")
            }
        }
    ) { innerPadding ->
        when (val state = editViewModel.uiState) {
            is EditProfileUiState.Loading -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
            is EditProfileUiState.Success -> {
                val profile = state.profile
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    // Profile Picture (not editable for now)
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = null, // We don't have a profile picture URL yet
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_launcher_foreground)
                        )
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Picture",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Name and USN fields (locked)
                    OutlinedTextField(
                        value = profile.name,
                        onValueChange = {},
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false // This locks the field
                    )
                    OutlinedTextField(
                        value = profile.universityRegNo,
                        onValueChange = {},
                        label = { Text("USN") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        enabled = false // This locks the field
                    )

                    // Bio field (editable)
                    OutlinedTextField(
                        value = editViewModel.bio,
                        onValueChange = { editViewModel.bio = it },
                        label = { Text("Bio") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .height(120.dp),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // Interests (editable) - Logic reused from ProfileSetupScreen
                    Text("Rate Your Interests", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        editViewModel.interests.forEachIndexed { index, interest ->

                            // REQUIRED CHANGE: Implement correct filtering logic
                            // Step 1: Identify interests currently used in OTHER slots (index != i)
                            val interestsInUse = editViewModel.interests
                                .filterIndexed { i, _ -> i != index }
                                .map { it.name }

                            // Step 2: Filter the master list (predefinedInterests)
                            val filteredAvailableInterests = editViewModel.predefinedInterests.filter { interestName ->
                                // Allow 'None', OR allow any interest that is NOT in the 'interestsInUse' list.
                                interestName == "None" || !interestsInUse.contains(interestName)
                            }

                            Box(modifier = Modifier.width(280.dp)) {
                                InterestDropdownAndSlider(
                                    // Use the full filtered list
                                    availableInterests = filteredAvailableInterests,
                                    selectedInterest = interest,
                                    onInterestChange = { newInterest ->
                                        val newInterests = editViewModel.interests.toMutableList()
                                        newInterests[index] = newInterest
                                        editViewModel.interests = newInterests
                                    }
                                )
                            }
                        }
                    }
                }
            }
            is EditProfileUiState.Error -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Failed to load data. Please try again.")
                }
            }
            is EditProfileUiState.UpdateSuccess -> {
                // This state is handled by the LaunchedEffect to navigate back
            }
        }
    }
}