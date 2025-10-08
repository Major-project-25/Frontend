package com.example.testapplication

import androidx.compose.foundation.Image
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralInterfaceScreen(
    generalViewModel: GeneralInterfaceViewModel = viewModel()
) {
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
                        reverseLayout = true // This flips the list upside down
                    ) {
                        items(state.posts) { post -> // The list from backend is already newest first
                            PostCard(post = post)
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
fun PostCard(post: PostResponse) {
    // --- Timestamp Formatting Logic ---
    val odt = OffsetDateTime.parse(post.created_at)
    val istOdt = odt.atZoneSameInstant(ZoneId.of("Asia/Kolkata"))
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    val formattedTimestamp = istOdt.format(formatter)
    // --- End of Formatting Logic ---

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            if (post.media_url != null) {
                AsyncImage(
                    model = "http://10.100.211.211:8000${post.media_url}", // IMPORTANT: Use your server IP
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
                    // Use the newly formatted timestamp
                    text = "Posted by Admin on $formattedTimestamp",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (post.content != null) {
                    Text(
                        text = post.content,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
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