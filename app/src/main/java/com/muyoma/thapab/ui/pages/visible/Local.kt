package com.muyoma.thapab.ui.pages.visible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.ui.composables.OptionButton
import com.muyoma.thapab.ui.composables.SectionHeader
import com.muyoma.thapab.ui.composables.SongListItem
import com.muyoma.thapab.ui.composables.SongOptionsDialog
import com.muyoma.thapab.viewmodel.DataViewModel

@Composable
fun Local(dataViewModel: DataViewModel, navController: NavController) {
    val context = LocalContext.current
    val isPlaying = dataViewModel.isPlaying.collectAsState()
    val currentSong = dataViewModel.currentSong.collectAsState()
    val gradientBackground = Brush.verticalGradient(listOf(Color(0xFF0F0F0F), Color.Black))

    // State to control dialog visibility and current selected song
    val openDialog = remember { mutableStateOf(false) }
    val selectedSong = remember { mutableStateOf<Song?>(null) }

    if (openDialog.value && selectedSong.value != null) {
        SongOptionsDialog(
            openDialog = openDialog,
            selectedSong = selectedSong.value!!,
            dataViewModel = dataViewModel,
            context = context
        )

    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground)
    ) {
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }

        items(dataViewModel.songs.value) {
            SongListItem(
                song = it,
                playing = it.title == currentSong.value?.title,
                onMore = {
                    selectedSong.value = it
                    openDialog.value = true
                }
            ) { clickedSong ->
                if (currentSong.value?.title != it.title || !isPlaying.value) {
                    dataViewModel.playSong(context, it)
                }
                navController.navigate("player/${it.title}")
            }
        }
    }
}



