package com.muyoma.thapab.ui.common

import android.annotation.SuppressLint
import android.media.MediaPlayer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MusicProgressTracker(
    play: (Song)->Unit,
    pause : ()->Unit,
    duration : Float,
    next : ()->Unit,
    prev : ()->Unit

) {
    var currentPosition by remember { mutableStateOf(0f) }
    val mediaPlayer = PlayerController.mediaPlayer
    mediaPlayer?.setOnCompletionListener {
        next()
    }
    var isPlaying by remember { mutableStateOf(mediaPlayer?.isPlaying) }
    val scope = rememberCoroutineScope()


    // Update the slider every 200ms
    LaunchedEffect(Unit) {
        while (true) {
            if (mediaPlayer != null) {
                scope.launch{
                    if (PlayerController.isPlaying()) {
                        currentPosition = if(PlayerController.getCurrentPosition() != null) PlayerController.getCurrentPosition()!! else 0f
                        isPlaying = true
                    } else {
                        isPlaying = false
                    }
                }
            }
            delay(100L)
        }
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
                text = formatTime(currentPosition.toInt()),
                fontSize = 12.sp,
                color = Color.White
            )
            Slider(
                value = currentPosition,
                onValueChange = { currentPosition = it },
                onValueChangeFinished = {
                    mediaPlayer?.seekTo(currentPosition.toInt())
                },
                valueRange = 0f..duration,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(10.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.LightGray,
                    inactiveTrackColor = Color.DarkGray,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
            Text(
                text = formatTime(duration.toInt()),
                color = Color.LightGray,
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
                tint = Color.White,
                modifier = Modifier
                    .clickable {
                        if (mediaPlayer != null) {
                            prev()
                        }

                    }
            )

            Icon(
                imageVector = if (isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying == true) "Pause" else "Play",
                modifier = Modifier
                    .padding(10.dp)
                    .background(Color(0x9E07F6F6), CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying) {
                                pause()
                            } else {
                                mediaPlayer.start()
                            }
                        }
                        if (mediaPlayer != null) {
                            isPlaying = mediaPlayer.isPlaying
                        }
                    }
                    .padding(10.dp),
                tint = Color.White,

            )

            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier
                    .clickable {
                        if (mediaPlayer != null) {
                            next()
                        }

                    }
            )
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatTime(milliseconds: Int): String {
    val minutes = (milliseconds / 1000) / 60
    val seconds = (milliseconds / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

