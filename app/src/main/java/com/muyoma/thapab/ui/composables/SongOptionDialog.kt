package com.muyoma.thapab.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.viewmodel.DataViewModel

@Composable
fun SongOptionsDialog(
    openDialog: MutableState<Boolean>,
    selectedSong: Song,
    dataViewModel: DataViewModel,
    context: android.content.Context,
    selectedPlayList : String? = null,
    inPlayList : Boolean = true
) {
   AlertDialog(
        onDismissRequest = { openDialog.value = false },
        title = {
            Text(
                text = "Song Options",
                color = Color.White,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OptionButton("Play") {
                    selectedSong.let {
                        dataViewModel.playSong(context, it)
                        openDialog.value = false
                    }
                }
                OptionButton("Pause") {
                    dataViewModel.pauseSong(context)
                    openDialog.value = false
                }
                OptionButton("Add to Playlist") {
                    // TODO: Implement playlist logic
                    dataViewModel._showPlayListSheet.value = true
                    dataViewModel._selectedSong.value = selectedSong
                    openDialog.value = false
                }
                if(inPlayList){
                    OptionButton("Remove from Playlist") {
                        // TODO: Implement playlist logic
                        dataViewModel._selectedSong.value = selectedSong
                        dataViewModel.removeSongFromPlaylist()
                    }
                }
                OptionButton("Add to Favourites") {
                    selectedSong.let {
                        dataViewModel.likeSong(it)
                        openDialog.value = false
                    }
                }
                OptionButton(" Remove from Favourites") {
                    selectedSong.let {
                        dataViewModel.unlikeSong(it)
                        openDialog.value = false
                    }
                }
//                OptionButton("⋯ More") {
//                    // TODO: Add more options
//                    openDialog.value = false
//                }
            }
        },
        confirmButton = {},
        containerColor = Color(0xC91D1D1F),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray
    )
}
