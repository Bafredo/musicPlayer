package com.muyoma.thapab.ui.pages.visible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muyoma.thapab.ui.composables.AlbumCarousel
import com.muyoma.thapab.ui.composables.MostPlayedCarousel
import com.muyoma.thapab.ui.composables.PlayListDialog
import com.muyoma.thapab.ui.composables.PlayListSectionHeader
import com.muyoma.thapab.ui.composables.PlayLister
import com.muyoma.thapab.ui.composables.SectionHeader
import com.muyoma.thapab.ui.composables.SongCarousel
import com.muyoma.thapab.viewmodel.DataViewModel


@Composable
fun Explore(dataViewModel: DataViewModel,navController: NavController) {
    val playLists = dataViewModel.playlists.collectAsState().value
    val mostPlayed = dataViewModel.mostPlayed
    val mostPopular = dataViewModel.mostPopular
    val recommended = dataViewModel.recommended
    val context = LocalContext.current

    val gradientBackground = Brush.verticalGradient(
        listOf(Color(0xFF0F0F0F),Color(0xFF0F0F0F), Color.Black)
    )
    var showCreatePlaylist by remember{
        mutableStateOf(false)
    }

    Box{
        LazyColumn(
            modifier = Modifier
                .background(gradientBackground)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item {
                Spacer(
                    modifier = Modifier
                        .height(20.dp)
                )
            }
            item {
                PlayListSectionHeader("Your Playlists",{showCreatePlaylist = true})
                if(playLists.isNotEmpty()){ PlayLister(playLists) }
            }
            item {
                SectionHeader("Most Played")
                MostPlayedCarousel(
                    mostPlayed,
                    currentSong = dataViewModel.currentSong.collectAsState().value,
                    play = {dataViewModel.playSong(context,it)}
                )
            }

            item {
                SectionHeader("Artists")
                AlbumCarousel(mostPopular)
            }

            item {
                SectionHeader("Must try")
                SongCarousel(recommended)
            }
            item {
                Spacer(
                    modifier = Modifier
                        .height(30.dp)
                )
            }
        }
        if(showCreatePlaylist){
            PlayListDialog(
                "Create Playlist",
                onDismiss = {showCreatePlaylist = false}
            ) { name ->
                dataViewModel.createPlaylist(name)
                showCreatePlaylist = false
            }
        }

    }
}

