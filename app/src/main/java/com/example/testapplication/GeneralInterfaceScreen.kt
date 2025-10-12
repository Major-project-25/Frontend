package com.example.testapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import java.util.UUID
import androidx.compose.material.icons.filled.Delete

// The @OptIn annotation is no longer needed here as Scaffold and TopAppBar were removed
@Composable
fun GeneralInterfaceScreen(
    navController: NavController,
    generalViewModel: GeneralInterfaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // 1. Fetch the current user's ID and Admin status
    val userId by sessionManager.getUserIdFlow.collectAsState(initial = null)
    val isAdmin by sessionManager.isAdminFlow.collectAsState(initial = false)

    // 2. Trigger the post fetch whenever the userId becomes available or changes
    LaunchedEffect(userId) {
        userId?.let {
            generalViewModel.fetchPosts(it)
        }
    }

    // REMOVED: Scaffold and TopBar to avoid double titles when hosted by AdminHostScreen

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = generalViewModel.uiState) {
            is GeneralUiState.Loading -> {
                CircularProgressIndicator()
            }
            is GeneralUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    reverseLayout = true
                ) {
                    items(state.posts) { post ->
                        PostCard(
                            post = post,
                            // Pass the admin status and current user ID for deletion logic
                            isAdmin = isAdmin,
                            currentUserId = userId,
                            onClick = {
                                // Navigate using only the Post ID (UUID)
                                navController.navigate("post_detail/${post.id}")
                            },
                            onDeletePost = { postId ->
                                // Trigger deletion using the current user ID as the Admin ID
                                userId?.let { adminId ->
                                    generalViewModel.deletePost(adminId, postId)
                                }
                            }
                        )
                    }
                }
            }
            is GeneralUiState.Empty -> {
                Text("No posts yet. Check back later!", textAlign = TextAlign.Center)
            }
            is GeneralUiState.Error -> {
                Text("Something went wrong. Please try again.", textAlign = TextAlign.Center)
            }
        }
    }
}

// MODIFIED: Added isAdmin, currentUserId, and onDeletePost parameters
@Composable
fun PostCard(
    post: PostResponse,
    isAdmin: Boolean,
    currentUserId: UUID?,
    onClick: () -> Unit,
    onDeletePost: (UUID) -> Unit
) {
    // --- Timestamp Formatting Logic ---
    val odt = OffsetDateTime.parse(post.created_at)
    val istOdt = odt.atZoneSameInstant(ZoneId.of("Asia/Kolkata"))
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    val formattedTimestamp = istOdt.format(formatter)
    // --- End of Formatting Logic ---

    // Logic to determine if the delete button should be visible
    // It must be an Admin AND the post author must match the current user
    val showDeleteButton = isAdmin && currentUserId == post.author_id

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Make the card clickable to view details
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            if (post.media_url != null) {
                // Use RetrofitInstance.BASE_URL for media
                AsyncImage(
                    model = RetrofitInstance.BASE_URL.dropLast(1) + post.media_url,
                    contentDescription = post.content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_launcher_background)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Posted by Admin on $formattedTimestamp",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (post.content != null) {
                    Text(
                        text = post.content,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, // Use space between to push delete left
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete button, only visible to the Admin who created the post
                    if (showDeleteButton) {
                        IconButton(onClick = { onDeletePost(post.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Post")
                        }
                    } else {
                        // Spacer maintains alignment if the delete button is hidden (for student view)
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    // Like/Dislike buttons (aligned to the right)
                    Row {
                        IconButton(onClick = { /* TODO: Handle like action */ }) {
                            Icon(Icons.Outlined.ThumbUp, contentDescription = "Like")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { /* TODO: Handle dislike action */ }) {
                            Icon(Icons.Outlined.ThumbDown, contentDescription = "Dislike")
                        }
                    }
                }
            }
        }
    }
}