package com.muyoma.thapab.ui.pages.visible

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.ui.composables.SongListItem
import com.muyoma.thapab.ui.composables.SongOptionsDialog
import com.muyoma.thapab.viewmodel.DataViewModel

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun Liked(dataViewModel: DataViewModel, navController: NavController) {
    val songs = dataViewModel.getLikedSongs()
    val nowPlaying = dataViewModel.currentSong.collectAsState()
    val isPlaying = dataViewModel.isPlaying.collectAsState()
    val context = LocalContext.current

    // Dialog state
    val openDialog = remember { mutableStateOf(false) }
    var selectedSong = remember { mutableStateOf<Song?>(null) }

    fun toggleMedia() {
        if (isPlaying.value) {
            dataViewModel.pauseSong(context)
        } else {
            nowPlaying.value?.let { dataViewModel.playSong(context, it) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item {
                    Spacer(modifier = Modifier.height(58.dp))
                }
                items(songs) { song ->
                    SongListItem(
                        song,
                        nowPlaying.value?.title == song.title,
                        onMore = {
                            selectedSong.value = song
                            openDialog.value = true
                        },
                        onItemClick = {
                            if (nowPlaying.value?.title != song.title) {
                                dataViewModel.playSong(context, song, songs)
                            } else if (!isPlaying.value) {
                                dataViewModel.playSong(context, song, songs)
                            }
                            navController.navigate("player/${song.title}")
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }

        // Top gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .align(Alignment.TopCenter)
                .offset(y = (0.3f * LocalConfiguration.current.screenHeightDp).dp - 24.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            Color.Transparent
                        ),
                        startY = Float.POSITIVE_INFINITY,
                        endY = 0f
                    )
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Icon(
                    imageVector = if (isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(55.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { toggleMedia() }
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Top black strip for padding/design
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.09f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface,
                            Color.Transparent
                        )
                    )
                )
                .padding(17.dp, 4.dp)
        ) {}

        // Dialog
        if (openDialog.value) {
            SongOptionsDialog(
                openDialog = openDialog,
                selectedSong = selectedSong.value!!,
                dataViewModel = dataViewModel,
                context = context,
                inPlayList = false
            )
        }
    }
}

