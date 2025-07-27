package com.muyoma.thapab.ui.composables

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.viewmodel.DataViewModel

@Composable
fun PlayListOptionsDialog(
    dataViewModel: DataViewModel,
    context: Context,
    selectedPlayList : String,
) {
    val songs = dataViewModel.getSongsFromPlaylist(selectedPlayList)
   AlertDialog(
        onDismissRequest = { dataViewModel._openPlayListOptions.value = false },
        title = {
            Text(
                text = "PlayList Options",
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
                    if(songs.isNotEmpty()){ dataViewModel.playSong(context, songs[0], songs) }
                    dataViewModel._openPlayListOptions.value = false
                }
                OptionButton("Stop") {
                    dataViewModel.pauseSong(context)
                    dataViewModel._openPlayListOptions.value = false
                }
                OptionButton("Delete") {
                    dataViewModel.deletePlaylist(selectedPlayList)
                    dataViewModel._openPlayListOptions.value = false
                }

            }
        },
        confirmButton = {},
        containerColor = Color(0xC91D1D1F),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray
    )
}
