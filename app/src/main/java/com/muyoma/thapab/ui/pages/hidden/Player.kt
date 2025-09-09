package com.muyoma.thapab.ui.pages.hidden

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.ui.common.MusicProgressTracker
import com.muyoma.thapab.viewmodel.DataViewModel

@Composable
fun Player(
    s: Song,
    dataViewModel: DataViewModel,
    navController: NavController,
    imageModifier: Modifier,
    textModifier: Modifier,
    frameModifier : Modifier
) {
    val context = LocalContext.current

    val currentSong by dataViewModel.currentSong.collectAsState()
    val isLiked by dataViewModel.currentSongLiked.collectAsState()
    val onRepeat by dataViewModel.repeatSong.collectAsState()

    // ✅ Use currentSong as source of truth
    val displaySong = currentSong ?: s

    // ✅ Play song only when song id changes
    LaunchedEffect(displaySong.id) {
        dataViewModel.playSong(context, displaySong)
    }

    // ✅ Precompute expensive values
    val titleText = remember(displaySong.id) { displaySong.title.uppercase() }
    val artistText = remember(displaySong.id) { displaySong.artist.uppercase() }
    val artworkModel = remember(displaySong.id) { displaySong.albumArtUri ?: displaySong.coverResId }

    val gradientBackground = remember {
        Brush.verticalGradient(
            listOf(Color.Black, Color.Transparent)
        )
    }
    val panelGradient = remember {
        Brush.verticalGradient(
            listOf(Color(0xFF111111), Color.Black)
        )
    }

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = frameModifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 🔹 Header Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close Player",
                tint = Color.White,
                modifier = Modifier.clickable { navController.popBackStack() }
            )
            Text(
                text = "PLAYING FROM LIBRARY",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(Modifier.width(10.dp))
        }

        // 🔹 Album Artwork
        Card(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(10.dp)
        ) {
            AsyncImage(
                model = artworkModel,
                contentDescription = "Album Art for ${displaySong.title}",
                contentScale = ContentScale.Crop,
                modifier = imageModifier
                    .fillMaxSize()

            )
        }

        // 🔹 Track Info & Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(panelGradient)
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
                    modifier = Modifier.clickable { dataViewModel.toggleRepeat() }
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = titleText,
                        color = Color.White,
                        fontSize = 18.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = textModifier.fillMaxWidth(0.9f)
                    )
                    Text(
                        text = artistText,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                }

                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like Song",
                    tint = Color.White,
                    modifier = Modifier.clickable {
                        if (isLiked) dataViewModel.unlikeSong(displaySong)
                        else dataViewModel.likeSong(displaySong)
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            // 🔹 Music Progress & Controls
            MusicProgressTracker(
                play = { dataViewModel.unpauseSong(context) },
                pause = { dataViewModel.pauseSong(context) },
                duration = dataViewModel.getDuration(),
                next = { dataViewModel.playNext(context) },
                prev = { dataViewModel.playPrev(context) }
            )

            Spacer(Modifier.height(12.dp))

            // 🔹 Bottom Row Actions
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
                    contentDescription = "Add to Playlist",
                    tint = Color.White,
                    modifier = Modifier.clickable {
                        dataViewModel.togglePlaylistSheet(true)
                    }
                )
            }
        }
    }
}
