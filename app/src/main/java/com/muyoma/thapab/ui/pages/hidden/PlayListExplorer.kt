package com.muyoma.thapab.ui.pages.hidden

import android.annotation.SuppressLint
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.ui.composables.SongListItem
import com.muyoma.thapab.ui.composables.SongOptionsDialog
import com.muyoma.thapab.viewmodel.DataViewModel

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PlayListExplorer(dataViewModel: DataViewModel, navController: NavController, songs: List<Song>) {
    val displayImage = remember { mutableIntStateOf(0) }
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
                .background(Color.Black)
        ) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f),
                painter = painterResource(
                    if (displayImage.intValue == 0) R.drawable.bg else displayImage.intValue
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )

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
                        }
                    ) { it ->
                        if (nowPlaying.value?.title != song.title) {
                            dataViewModel.playSong(context, song)
                        } else if (!isPlaying.value) {
                            dataViewModel.playSong(context, song)
                        }
                        navController.navigate("player/${song.title}")
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(70.dp))
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
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black,
                            Color.Black.copy(alpha = 0.8f),
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
                    tint = Color.White
                )

                Icon(
                    imageVector = if (isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(55.dp)
                        .clip(CircleShape)
                        .background(Color(0x9E07F6F6))
                        .clickable { toggleMedia() }
                        .padding(8.dp),
                    tint = Color.Black
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
                        colors = listOf(Color.Black, Color.Black, Color.Transparent)
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
                context = context
            )
        }
    }
}


