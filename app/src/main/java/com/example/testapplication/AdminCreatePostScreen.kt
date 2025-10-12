package com.example.testapplication

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Needed for remember, LaunchedEffect, collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import java.util.UUID // Needed for adminId type
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload // Ensure this is imported

// RENAMED COMPOSABLE
@Composable
fun AdminCreatePostScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val adminId by sessionManager.getUserIdFlow.collectAsState(initial = null)

    val uiState = adminViewModel.uiState

    // Launcher for picking an image or video from the phone's storage
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        adminViewModel.selectedFileUri = uri
    }

    // Show a message and reset state after a successful upload
    LaunchedEffect(uiState) {
        if (uiState is AdminUiState.Success) {
            Toast.makeText(context, "Post uploaded successfully!", Toast.LENGTH_LONG).show()
            adminViewModel.resetState()
        } else if (uiState is AdminUiState.Error) {
            Toast.makeText(context, uiState.message, Toast.LENGTH_LONG).show()
            adminViewModel.resetState()
        }
    }

    // REMOVED Scaffold/TopBar/Title content for hosting within AdminHostScreen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Text field for post content
        OutlinedTextField(
            value = adminViewModel.postContent,
            onValueChange = { adminViewModel.postContent = it },
            label = { Text("Post content...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // File selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { filePickerLauncher.launch("image/*, video/*") }) {
                Text("Select Image/Video")
            }
            // Show a preview of the selected image
            adminViewModel.selectedFileUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected media",
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Upload button
        Button(
            onClick = {
                adminId?.let {
                    adminViewModel.createPost(it, context)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            // Only enable if we have a valid admin ID and are not already uploading
            enabled = uiState !is AdminUiState.Uploading && adminId != null
        ) {
            if (uiState is AdminUiState.Uploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Upload Post")
            }
        }
    }
}