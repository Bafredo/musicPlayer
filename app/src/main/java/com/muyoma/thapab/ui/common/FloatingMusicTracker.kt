package com.muyoma.thapab.ui.common

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController

@Composable
fun FloatingMusicTracker(
    song: Song,isLiked : Boolean,
    imagemodifier : Modifier,
    textmodifier : Modifier,
    pause: () -> Unit,
    play: () -> Unit,
    liked :(Song)->Unit,
    clicked: (Song)->Unit,
    frameModifier : Modifier
) {
    val playbackPosition by PlayerController.playbackPosition.collectAsState()
    val duration by PlayerController.playbackDuration.collectAsState()
    val isPlaying by PlayerController._isPlaying.collectAsState()
    var sliderPosition by remember { mutableStateOf(0f) }

    LaunchedEffect(playbackPosition) {
        sliderPosition = playbackPosition
    }

    Row(
        modifier = frameModifier
            .fillMaxWidth()
            .fillMaxHeight(0.1f)
            .padding(10.dp, 5.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(22.dp))
            .clickable{
                clicked(song)
            }
            .padding(5.dp, 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time and slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.weight(8f).fillMaxHeight()
        ) {
            AsyncImage(
                model = song.albumArtUri ?: song.coverResId,
                contentDescription = song.title,
                modifier = imagemodifier
                    .width(55.dp)
                    .height(55.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))

            )

            Column {
                Text(
                    text = song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = textmodifier.width(130.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = sliderPosition.coerceIn(0f, duration),
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = {
                        PlayerController.seekTo(sliderPosition.toInt())
                    },
                    valueRange = 0f..duration,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(10.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Text(
                text = formatTime(sliderPosition.toInt()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        // Playback controls
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(3f)
        ) {
            Icon(
                imageVector =if(isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable{
                        liked(song)
                    }
            )
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        if (PlayerController.mediaPlayer?.isPlaying == true) {
                            pause()
                        } else {
                            play()
                        }
                    }
                    .padding(10.dp),
                tint = MaterialTheme.colorScheme.onPrimary,

            )
        }
    }
}
