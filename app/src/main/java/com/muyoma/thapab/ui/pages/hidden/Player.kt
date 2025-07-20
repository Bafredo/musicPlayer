package com.muyoma.thapab.ui.pages.hidden

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil.compose.rememberAsyncImagePainter
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.ui.common.MusicProgressTracker
import com.muyoma.thapab.viewmodel.DataViewModel

@Composable
fun Player(
    s: Song,
    dataViewModel: DataViewModel
) {

    val context  = LocalContext.current
    val song = dataViewModel.currentSong.collectAsState().value!!
    val isLiked by remember {
        mutableStateOf(
            dataViewModel.likedSongs.value.indexOf(s) >= 0
        )
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
                tint = Color.White
            )
            Text(
                text = "PLAYING FROM LIBRARY",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options",
                tint = Color.White
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
                painter = if(song.coverResId != null)rememberAsyncImagePainter(model = song.coverResId)
                else painterResource(R.drawable.bg),
                contentDescription = "Album Art",
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
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = Color.White
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = song.title.uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .width(180.dp),
                        overflow = TextOverflow.MiddleEllipsis,
                        maxLines = 1
                    )
                    Text(
                        text = song.artist.uppercase(),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(120.dp),
                    )
                }

                Icon(
                    imageVector = if(dataViewModel.currentSongLiked.collectAsState().value) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like Song",
                    tint = Color.White,
                    modifier = Modifier
                        .clickable{
                            if(dataViewModel.isSongLiked(s))
                                dataViewModel.unlikeSong(s)
                            else
                                dataViewModel.likeSong(s)
                        }

                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Slider
            MusicProgressTracker(
          play = { dataViewModel.playSong(context,song)},
                pause = { dataViewModel.pauseSong(context) },
                duration = dataViewModel.getDuration(),
                next = {dataViewModel.playNext(context)},
                prev = {dataViewModel.playPrev(context)}

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
                    tint = Color.White
                )
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add",
                    tint = Color.White
                )
            }
        }
    }
}
