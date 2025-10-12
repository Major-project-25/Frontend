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
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import java.util.UUID
import androidx.compose.material.icons.filled.Delete

@Composable
fun GeneralInterfaceScreen(
    navController: NavController,
    generalViewModel: GeneralInterfaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val userId by sessionManager.getUserIdFlow.collectAsState(initial = null)
    val isAdmin by sessionManager.isAdminFlow.collectAsState(initial = false)

    LaunchedEffect(userId) {
        userId?.let {
            generalViewModel.fetchPosts(it)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
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
                            isAdmin = isAdmin,
                            currentUserId = userId,
                            onClick = {
                                navController.navigate("post_detail/${post.id}")
                            },
                            onDeletePost = { postId ->
                                userId?.let { adminId ->
                                    generalViewModel.deletePost(adminId, postId)
                                }
                            },
                            onReact = { postId, reactionType ->
                                userId?.let { id ->
                                    generalViewModel.handleReaction(id, postId, reactionType)
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

// MODIFIED: PostCard now uses the 'isAdmin' flag to control the visibility of reaction counts.
@Composable
fun PostCard(
    post: PostResponse,
    isAdmin: Boolean,
    currentUserId: UUID?,
    onClick: () -> Unit,
    onDeletePost: (UUID) -> Unit,
    onReact: (postId: UUID, reactionType: String) -> Unit
) {
    val isLiked = post.userReaction == "like"
    val isDisliked = post.userReaction == "dislike"

    val likeColor = if (isLiked) MaterialTheme.colorScheme.primary else Color.Gray
    val dislikeColor = if (isDisliked) MaterialTheme.colorScheme.error else Color.Gray

    // --- Timestamp Formatting Logic ---
    val odt = OffsetDateTime.parse(post.created_at)
    val istOdt = odt.atZoneSameInstant(ZoneId.of("Asia/Kolkata"))
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    val formattedTimestamp = istOdt.format(formatter)
    // --- End of Formatting Logic ---

    val showDeleteButton = isAdmin && currentUserId == post.author_id

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            if (post.media_url != null) {
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete button area
                    if (showDeleteButton) {
                        IconButton(onClick = { onDeletePost(post.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Post")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    // Like/Dislike buttons (aligned to the right)
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        // NEW LOGIC: Only display count if the user is an Admin
                        if (isAdmin) {
                            // Display Likes count
                            Text(text = post.likes.toString(), color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // Like Button
                        IconButton(
                            onClick = {
                                val newReaction = if (isLiked) "none" else "like"
                                onReact(post.id, newReaction)
                            }
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = likeColor
                            )
                        }

                        // NEW LOGIC: Only display count if the user is an Admin
                        if (isAdmin) {
                            Spacer(modifier = Modifier.width(8.dp))
                            // Display Dislikes count
                            Text(text = post.dislikes.toString(), color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            // Add a small spacer if counts are hidden to prevent icons from touching
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        // Dislike Button
                        IconButton(
                            onClick = {
                                val newReaction = if (isDisliked) "none" else "dislike"
                                onReact(post.id, newReaction)
                            }
                        ) {
                            Icon(
                                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                tint = dislikeColor
                            )
                        }
                    }
                }
            }
        }
    }
}