package com.example.testapplication

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavController,
    postId: UUID, // Now receiving the Post ID directly
    detailViewModel: PostDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val currentUserId by sessionManager.getUserIdFlow.collectAsState(initial = null)

    // Trigger data fetch when both IDs are ready
    LaunchedEffect(currentUserId, postId) {
        currentUserId?.let { userId ->
            detailViewModel.fetchPostDetails(userId, postId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            when (val state = detailViewModel.uiState) {
                is PostDetailUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is PostDetailUiState.Error -> {
                    Text("Failed to load post details.", color = MaterialTheme.colorScheme.error)
                }
                is PostDetailUiState.Success -> {
                    // Display the successful post content
                    PostContent(post = state.post)
                }
            }
        }
    }
}

@Composable
fun PostContent(post: PostResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        PostHeader(post)
        Spacer(modifier = Modifier.height(16.dp))

        // Zoomable Media Area
        if (post.media_url != null) {
            ZoomableMedia(post.media_url)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Full Content
        if (post.content != null) {
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Placeholder for full Interaction buttons (if needed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Note: Use a dedicated ViewModel function here for reacting
            Button(onClick = { /* TODO: Implement Like */ }) { Text("Like") }
            Button(onClick = { /* TODO: Implement Dislike */ }) { Text("Dislike") }
        }
    }
}

@Composable
fun PostHeader(post: PostResponse) {
    // --- Timestamp Formatting Logic ---
    val odt = OffsetDateTime.parse(post.created_at)
    val istOdt = odt.atZoneSameInstant(ZoneId.of("Asia/Kolkata"))
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    val formattedTimestamp = istOdt.format(formatter)
    // --- End of Formatting Logic ---

    Text(
        text = "Posted by Admin on $formattedTimestamp",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    // REMOVED: The line to display the Post ID:
    /*
    Text(
        text = "Post ID: ${post.id}",
        style = MaterialTheme.typography.labelSmall
    )
    */
}

@Composable
fun ZoomableMedia(mediaUrl: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val maxScale = 5f
    val minScale = 1f

    // Construct the full URL (hardcoded IP for media viewing)
    val fullUrl = "http://172.17.2.88:8000$mediaUrl" // IMPORTANT: Match your server IP here

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 250.dp, max = 500.dp)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // 1. Calculate new scale, clamping between min and max
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)

                    // 2. Calculate max translation offset
                    val boundsWidth = size.width * (newScale - 1) / 2
                    val boundsHeight = size.height * (newScale - 1) / 2

                    // 3. Calculate new offset, clamping within bounds
                    val newOffset = if (newScale > 1f) {
                        Offset(
                            x = (offset.x + pan.x * newScale).coerceIn(-boundsWidth, boundsWidth),
                            y = (offset.y + pan.y * newScale).coerceIn(-boundsHeight, boundsHeight)
                        )
                    } else {
                        // Reset offset if scaled back to 1x
                        Offset.Zero
                    }

                    scale = newScale
                    offset = newOffset
                }
            }
    ) {
        AsyncImage(
            model = fullUrl,
            contentDescription = "Post Media",
            contentScale = ContentScale.Fit, // Use Fit for media meant to be zoomed
            modifier = Modifier
                .fillMaxSize()
                // Apply the scale and offset transforms
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
        )
    }
}