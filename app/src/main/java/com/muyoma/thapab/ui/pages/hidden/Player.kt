package com.muyoma.thapab.ui.pages.hidden

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.ui.common.MusicProgressTracker
import com.muyoma.thapab.viewmodel.DataViewModel

@Composable
fun Player(
    s: Song,
    dataViewModel: DataViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val currentPlayingSong by dataViewModel.currentSong.collectAsState()
    val isLiked by dataViewModel.currentSongLiked.collectAsState()
    LaunchedEffect(s) {
        dataViewModel.playSong(context, s)
    }

    val displaySong = currentPlayingSong ?: s
    val onRepeat by dataViewModel.repeatSong.collectAsState()
    if(onRepeat){
        PlayerController.mediaPlayer?.setOnCompletionListener(MediaPlayer.OnCompletionListener(){
            PlayerController.seekTo(0)
        })
    }

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color.Black, Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close Player",
                tint = Color.White,
                modifier = Modifier.clickable {
                    navController.popBackStack()
                }
            )
            Text(
                text = "PLAYING FROM LIBRARY",
                fontSize = 12.sp,
                color = Color.Gray
            )
//            Icon(
//                imageVector = Icons.Default.MoreVert,
//                contentDescription = "More Options",
//                tint = Color.White,
//                modifier = Modifier.clickable { /* TODO: Implement more options dialog */ }
//            )
            Spacer(
                modifier = Modifier
                    .width(10.dp)
            )
        }

        // Album Artwork
        Card(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(10.dp)
        ) {
            Image(
                painter = if(displaySong.coverResId != R.drawable.bg) rememberAsyncImagePainter(model = displaySong.coverResId)
                else painterResource(R.drawable.bg),
                contentDescription = "Album Art for ${displaySong.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Track Info & Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF111111),
                            Color.Black
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (!onRepeat) Icons.Default.Repeat else Icons.Default.ArrowForward,
                    contentDescription = "Repeat",
                    tint = Color.White,
                    modifier = Modifier.clickable {
                        // Implement repeat toggle logic here in DataViewModel
                         dataViewModel.toggleRepeat() // Example
                    }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displaySong.title.uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .width(180.dp), // Consider using fillMaxWidth with padding instead of fixed width
                        overflow = TextOverflow.MiddleEllipsis,
                        maxLines = 1
                    )
                    Text(
                        text = displaySong.artist.uppercase(),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(120.dp), // Consider using fillMaxWidth with padding
                    )
                }

                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like Song",
                    tint = Color.White,
                    modifier = Modifier.clickable {
                        // Use displaySong to like/unlike
                        if (isLiked)
                            dataViewModel.unlikeSong(displaySong)
                        else
                            dataViewModel.likeSong(displaySong)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Music Progress Tracker and Playback Controls
            MusicProgressTracker(
                play = { dataViewModel.unpauseSong(context) }, // Use unpause for resuming
                pause = { dataViewModel.pauseSong(context) },
                duration = dataViewModel.getDuration(),
                next = { dataViewModel.playNext(context) },
                prev = { dataViewModel.playPrev(context) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Row Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Headset,
                    contentDescription = "Headset",
                    tint = Color.White,
                    modifier = Modifier.clickable {

                    }
                )
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add to Playlist",
                    tint = Color.White,
                    modifier = Modifier.clickable {
                        dataViewModel.togglePlaylistSheet(true) // Pass the song to add
                    }
                )
            }
        }
    }
}


