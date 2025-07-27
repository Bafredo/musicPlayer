package com.muyoma.thapab.ui.pages.visible

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyoma.thapab.ui.composables.SearchResult
import com.muyoma.thapab.viewmodel.DataViewModel
import com.muyoma.thapab.network.models.YoutubeVideo // Import the YouTubeVideo data class

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Search(dataViewModel: DataViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Observe local songs filter
    val localSearchResults by dataViewModel.songs.collectAsState().value.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
    }.let { remember(searchQuery, it) { mutableStateOf(it) } } // Only update if query or songs change

    // Observe Youtube results
    val youtubeSearchResults by dataViewModel.youtubeSearchResults.collectAsState()
    val apiErrorMessage by dataViewModel.apiErrorMessage.collectAsState()
    val downloadStatus by dataViewModel.downloadStatus.collectAsState()


    val gradientBackground = Brush.verticalGradient(
        listOf(Color(0xFF0F0F0F), Color.Black)
    )
    fun search(){
        if(searchQuery.isNotEmpty()){
            dataViewModel.searchSongOnYouTube(searchQuery)
        } else{
            dataViewModel.clearYoutubeSearchResults()

        }
    }

    Column(
        modifier = Modifier
            .background(gradientBackground)
    ) {
        // Top Row for padding/spacing (as in your original code)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.03f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                    )
                )
                .padding(17.dp, 4.dp)
        ) {
            // Content for this row if any
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Title / Heading
            Text(
                text = "Search Music",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newValue ->
                    searchQuery = newValue
                    // Trigger Youtube only if the query is not empty

                },
                placeholder = {
                    Text("Search songs, artists...", color = Color.Gray)
                },
                trailingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search", modifier = Modifier
                        .clip(
                            CircleShape
                        )
                        .clickable { search() })
                },
                shape = RoundedCornerShape(25.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(25.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filters (optional for future)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                listOf("All", "Local Files", "Online (YouTube)").forEach { filter ->
                    FilterChip(
                        selected = false, // You can make this dynamic if implementing filter logic
                        onClick = { /* handle filter */ },
                        label = { Text(filter) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display API Errors
            apiErrorMessage?.let {
                Text(text = "API Error: $it", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Display Download Status
            downloadStatus?.let {
                Text(text = "Download Status: $it", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Results Section
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // --- Local Song Results ---
                if (localSearchResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "Local Songs",
                            style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(localSearchResults) { song ->
                        SearchResult(song) {
                            dataViewModel.playSong(context, song)
                        }
                    }
                }

                // --- Youtube Results ---
                if (youtubeSearchResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "Online Results",
                            style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(youtubeSearchResults) { youtubeVideo ->
                        SearchResultItem(youtubeVideo, longPress = {id,title->
                            Toast.makeText(context,"Downloading ${title}.mp3",
                                Toast.LENGTH_SHORT).show()
                            dataViewModel.downloadMp3ToPhone(context,id,title)
                        }) {
                            // When this button is clicked, trigger the download on the backend
                            // and then potentially stream the song
                            dataViewModel.downloadYoutubeSong(youtubeVideo)
                        }
                    }
                }

                // --- No Results / Placeholder ---
                if (localSearchResults.isEmpty() && youtubeSearchResults.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Text(
                            "No local or online results found for \"$searchQuery\".",
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else if (searchQuery.isBlank()) {
                    item {
                        Text(
                            "Start typing to search for music.",
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(132.dp)) // Padding at bottom for player UI
                }
            }
        }
    }
}

// Separate Composable for Youtube results for better modularity
@Composable
fun SearchResultItem(youtubeVideo: YoutubeVideo,longPress : (String,String)->Unit, onDownloadAndStreamClick: (YoutubeVideo) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(8.dp))

            .background(Color(0xFF202020), RoundedCornerShape(8.dp)), // Darker background for card
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent) // Make card background transparent to show row background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF202020)) // Explicit background for the row content
                .padding(12.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            longPress(youtubeVideo.videoId, youtubeVideo.title)
                        }
                    )
                },

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = youtubeVideo.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Source:(Video ID: ${youtubeVideo.videoId})",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier,

                onClick = { onDownloadAndStreamClick(youtubeVideo) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
            ) {
                Text("play")
            }
        }
    }
}