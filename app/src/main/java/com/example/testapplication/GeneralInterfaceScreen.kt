package com.example.testapplication

import androidx.compose.foundation.Image
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
import com.google.gson.Gson
import java.net.URLEncoder
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralInterfaceScreen(
    navController: NavController,
    generalViewModel: GeneralInterfaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    // 1. Fetch the current user's ID
    val userId by sessionManager.getUserIdFlow.collectAsState(initial = null)

    // 2. Trigger the post fetch whenever the userId becomes available or changes
    LaunchedEffect(userId) {
        userId?.let {
            generalViewModel.fetchPosts(it)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Updates & Events", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
                                onClick = {
                                    // Navigate using only the Post ID (UUID)
                                    navController.navigate("post_detail/${post.id}")
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
}

@Composable
fun PostCard(post: PostResponse, onClick: () -> Unit) {
    // --- Timestamp Formatting Logic ---
    val odt = OffsetDateTime.parse(post.created_at)
    val istOdt = odt.atZoneSameInstant(ZoneId.of("Asia/Kolkata"))
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    val formattedTimestamp = istOdt.format(formatter)
    // --- End of Formatting Logic ---

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Make the card clickable to view details
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            if (post.media_url != null) {
                AsyncImage(
                    model = "http://172.17.2.88:8000${post.media_url}",
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
                    horizontalArrangement = Arrangement.End
                ) {
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