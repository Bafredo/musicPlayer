package com.muyoma.thapab.ui.pages.visible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.ui.composables.SongListItem
import com.muyoma.thapab.ui.composables.SongOptionsDialog
import com.muyoma.thapab.viewmodel.DataViewModel

@Composable
fun Local(dataViewModel: DataViewModel, navController: NavController) {
    val context = LocalContext.current
    val isPlaying by dataViewModel.isPlaying.collectAsState()
    val currentSong by dataViewModel.currentSong.collectAsState()
    val songs by dataViewModel.songs.collectAsState()
    val isLibraryLoading by dataViewModel.isLibraryLoading.collectAsState()
    val listState = rememberLazyListState()
    val gradientBackground = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainerHighest
        )
    )

    val openDialog = remember { mutableStateOf(false) }
    val selectedSong = remember { mutableStateOf<Song?>(null) }

    if (openDialog.value && selectedSong.value != null) {
        SongOptionsDialog(
            openDialog = openDialog,
            selectedSong = selectedSong.value!!,
            dataViewModel = dataViewModel,
            context = context,
            inPlayList = false
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground)
    ) {
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }

        if (isLibraryLoading) {
            item {
                Text(
                    text = "Loading your library...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }
        }

        items(
            items = songs,
            key = { song -> song.id },
            contentType = { "song" }
        ) { song ->
            SongListItem(
                song = song,
                playing = song.id == currentSong?.id,
                onMore = {
                    selectedSong.value = song
                    openDialog.value = true
                },
                onItemClick = {
                    if (currentSong?.title != song.title || !isPlaying) {
                        dataViewModel.playSong(context, song)
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

