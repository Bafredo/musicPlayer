package com.muyoma.thapab.ui.pages.visible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muyoma.thapab.ui.composables.AlbumCarousel
import com.muyoma.thapab.ui.composables.MostPlayedCarousel
import com.muyoma.thapab.ui.composables.PlayLister
import com.muyoma.thapab.ui.composables.SectionHeader
import com.muyoma.thapab.ui.composables.SongCarousel
import com.muyoma.thapab.viewmodel.DataViewModel


@Composable
fun Explore(dataViewModel: DataViewModel,navController: NavController) {
    val samplePlaylists = dataViewModel.samplePlaylists
    val mostPlayed = dataViewModel.mostPlayed
    val mostPopular = dataViewModel.mostPopular
    val recommended = dataViewModel.recommended

    val gradientBackground = Brush.verticalGradient(
        listOf(Color(0xFF0F0F0F),Color(0xFF0F0F0F), Color.Black)
    )
    var showCreatePlaylist by remember{
        mutableStateOf(true)
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
                SectionHeader("Your Playlists")
                PlayLister(samplePlaylists)
            }
            item {
                SectionHeader("Most Played")
                MostPlayedCarousel(mostPlayed)
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
                onDismiss = {showCreatePlaylist = !showCreatePlaylist}
            ) { name -> }
        }

    }
}
@Composable
fun PlayListDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.Black,
        tonalElevation = 8.dp,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(playlistName) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.defaultMinSize(minWidth = 100.dp)
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.defaultMinSize(minWidth = 100.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
