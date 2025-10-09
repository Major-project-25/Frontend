package com.example.testapplication

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGeneralInterfaceScreen(
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("General Info", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Show post creation dialog */ }) {
                Icon(painterResource(id = R.drawable.ic_upload), contentDescription = "Upload Post")
            }
        }
    ) { innerPadding ->
        // We will add the list of existing posts here later
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Create a New Post", style = MaterialTheme.typography.titleLarge)
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
                adminViewModel.selectedFileUri?.let {
                    AsyncImage(
                        model = it,
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
                enabled = uiState !is AdminUiState.Uploading // Disable button while uploading
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
}