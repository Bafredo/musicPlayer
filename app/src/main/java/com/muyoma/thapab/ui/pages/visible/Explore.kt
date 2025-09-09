package com.muyoma.thapab.ui.pages.visible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.ui.composables.*
import com.muyoma.thapab.viewmodel.DataViewModel

@Composable
fun Explore(
    dataViewModel: DataViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    // ✅ Collect once at top level
    val playLists by dataViewModel.playlists.collectAsState()
    val songs by dataViewModel.songs.collectAsState()
    val currentSong by dataViewModel.currentSong.collectAsState()
    val openPlayListOptions by dataViewModel.openPlayListOptions.collectAsState()
    val selectedPlaylist by dataViewModel.selectedPlaylist.collectAsState()
    val isPlaying by PlayerController._isPlaying.collectAsState()

    // ✅ Derive values with remember
    val artists = remember(songs) { songs.distinctBy { it.artist } }
    val gradientBackground = remember {
        Brush.verticalGradient(
            listOf(Color(0xFF0F0F0F), Color(0xFF0F0F0F), Color.Black)
        )
    }

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
                add = { dataViewModel._showPlayListSheet.value = true }
            )

            if (playLists.isNotEmpty()) {
                PlayLister(
                    playLists,
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
            SongCarousel(dataViewModel.recommended){it  ->
                dataViewModel.playSong(context,it,dataViewModel.recommended)
            }

            Spacer(Modifier.height(120.dp))
        }

        if (showCreatePlaylist) {
            PlayListDialog(
                title = "Create Playlist",
                onDismiss = { showCreatePlaylist = false }
            ) { name ->
                dataViewModel.createPlaylist(name)
                showCreatePlaylist = false
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
