package com.muyoma.thapab.ui.common

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController

@Composable
fun MusicProgressTracker(
    play: (Song)->Unit,
    pause : ()->Unit,
    duration : Float,
    next : ()->Unit,
    prev : ()->Unit

) {
    val playbackPosition by PlayerController.playbackPosition.collectAsState()
    val playerDuration by PlayerController.playbackDuration.collectAsState()
    val isPlaying by PlayerController._isPlaying.collectAsState()
    var sliderPosition by remember { mutableStateOf(0f) }

    LaunchedEffect(playbackPosition) {
        sliderPosition = playbackPosition
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Time and Slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = formatTime(sliderPosition.toInt()),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = {
                    PlayerController.seekTo(sliderPosition.toInt())
                },
                valueRange = 0f..playerDuration.coerceAtLeast(duration),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(10.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
            Text(
                text = formatTime(playerDuration.coerceAtLeast(duration).toInt()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        // Controls: Skip Prev | Play/Pause | Skip Next
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(20.dp, 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable { prev() }
            )

            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier
                    .padding(10.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        if (isPlaying) {
                            pause()
                        } else {
                            PlayerController.currentSong.value?.let(play)
                        }
                    }
                    .padding(10.dp),
                tint = MaterialTheme.colorScheme.onPrimary,

            )

            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { next() }
            )
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatTime(milliseconds: Int): String {
    val minutes = (milliseconds / 1000) / 60
    val seconds = (milliseconds / 1000) % 60
    val hours = minutes/60

    return if(hours == 0)
        String.format("%02d:%02d", minutes, seconds)
    else
        String.format("%02d:%02d:%02d", hours, minutes, seconds)

}
