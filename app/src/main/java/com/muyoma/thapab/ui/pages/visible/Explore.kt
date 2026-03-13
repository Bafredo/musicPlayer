package com.muyoma.thapab.ui.pages.visible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.ui.composables.AlbumCarousel
import com.muyoma.thapab.ui.composables.MadeForYouCard
import com.muyoma.thapab.ui.composables.MadeForYouHeader
import com.muyoma.thapab.ui.composables.MostPlayedCarousel
import com.muyoma.thapab.ui.composables.PlayListDialog
import com.muyoma.thapab.ui.composables.PlayListOptionsDialog
import com.muyoma.thapab.ui.composables.PlayListSectionHeader
import com.muyoma.thapab.ui.composables.PlayLister
import com.muyoma.thapab.ui.composables.SectionHeader
import com.muyoma.thapab.ui.composables.SongCarousel
import com.muyoma.thapab.viewmodel.DataViewModel

@Composable
fun Explore(
    dataViewModel: DataViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val playLists by dataViewModel.playlists.collectAsState()
    val songs by dataViewModel.songs.collectAsState()
    val currentSong by dataViewModel.currentSong.collectAsState()
    val openPlayListOptions by dataViewModel.openPlayListOptions.collectAsState()
    val selectedPlaylist by dataViewModel.selectedPlaylist.collectAsState()
    val isPlaying by PlayerController._isPlaying.collectAsState()

    val artists = remember(songs) { songs.distinctBy { it.artist } }
    val gradientBackground = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainerHighest
        )
    )

    var showCreatePlaylist by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .background(gradientBackground)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 1.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(40.dp))

            MadeForYouHeader("For you", "Chillaaaaaax")
            MadeForYouCard()

            PlayListSectionHeader(
                title = "Playlists",
                add = { dataViewModel.togglePlaylistSheet(true) }
            )

            if (playLists.isNotEmpty()) {
                PlayLister(
                    playlists = playLists,
                    options = {
                        dataViewModel._selectedPlaylist.value = it
                        dataViewModel._openPlayListOptions.value = true
                    }
                ) { playlistId ->
                    navController.navigate("playlist/$playlistId")
                    dataViewModel._selectedPlaylist.value = playlistId
                }
            }

            SectionHeader("Most Played")
            MostPlayedCarousel(
                songs = dataViewModel.mostPlayed,
                currentSong = currentSong,
                play = { song, list ->
                    when {
                        song == currentSong && isPlaying -> dataViewModel.pauseSong(context)
                        song == currentSong && !isPlaying -> dataViewModel.unpauseSong(context)
                        else -> dataViewModel.playSong(context, song, list)
                    }
                }
            )

            SectionHeader("Artists")
            AlbumCarousel(artists) { artist ->
                navController.navigate("songlist/$artist")
                dataViewModel._selectedPlaylist.value = artist
            }

            SectionHeader("Must try")
            SongCarousel(dataViewModel.recommended) { song ->
                dataViewModel.playSong(context, song, dataViewModel.recommended)
            }

            Spacer(Modifier.height(120.dp))
        }

        if (showCreatePlaylist) {
            PlayListDialog(
                title = "Create Playlist",
                onDismiss = { showCreatePlaylist = false }
            ) { name ->
                if (name.isNotBlank()) {
                    dataViewModel.createPlaylist(name)
                    showCreatePlaylist = false
                }
            }
        }

        if (openPlayListOptions && selectedPlaylist != null) {
            PlayListOptionsDialog(
                dataViewModel = dataViewModel,
                context = context,
                selectedPlayList = selectedPlaylist!!
            )
        }
    }
}
