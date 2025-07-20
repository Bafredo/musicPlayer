package com.muyoma.thapab.ui.pages.visible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.ui.composables.SectionHeader
import com.muyoma.thapab.ui.composables.SongListItem
import com.muyoma.thapab.viewmodel.DataViewModel

@Composable
fun Local(dataViewModel : DataViewModel,navController: NavController){

    val context = LocalContext.current
    val isPlaying = dataViewModel.isPlaying.collectAsState()
    val gradientBackground = Brush.verticalGradient(
        listOf(Color(0xFF0F0F0F), Color.Black)
    )


    LazyColumn (
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground)
    ) {
        item{
            Spacer(
                modifier = Modifier
                    .height(60.dp)
            )

        }
        items(dataViewModel.songs.value){
            SongListItem(it,it.title == dataViewModel.currentSong.collectAsState().value?.title) {song ->
                if (dataViewModel.currentSong.value?.title != it.title) {
                    dataViewModel.playSong(context, it)
                } else if(!isPlaying.value){
                    dataViewModel.playSong(context, it)
                }
                navController.navigate("player/${it.title}")
            }
        }
    }
}