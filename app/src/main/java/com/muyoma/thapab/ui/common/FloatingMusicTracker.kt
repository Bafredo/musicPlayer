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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.muyoma.thapab.R

@Composable
fun FloatingMusicTracker(song: Song, pause: () -> Unit, play: () -> Unit) {
    var currentPosition by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(1f) } // Default to 1 to avoid 0..0 crash
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            PlayerController.mediaPlayer?.let { mp ->
                scope.launch {
                    currentPosition = PlayerController.getCurrentPosition() ?: 0f
                    duration = mp.duration.toFloat()
                    isPlaying = mp.isPlaying
                }
            }
            delay(200L)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.1f)
            .padding(10.dp, 5.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, Color.DarkGray, RoundedCornerShape(22.dp))
            .background(Color(0xBF000000), RoundedCornerShape(22.dp))
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
            Image(
                painter = painterResource(song.coverResId),
                contentDescription = song.title,
                modifier = Modifier
                    .width(55.dp)
                    .height(55.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
            )

            Column {
                Text(
                    text = song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(130.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = currentPosition.coerceIn(0f, duration),
                    onValueChange = { currentPosition = it },
                    onValueChangeFinished = {
                        PlayerController.mediaPlayer?.seekTo(currentPosition.toInt())
                    },
                    valueRange = 0f..duration,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(10.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.LightGray,
                        inactiveTrackColor = Color.DarkGray
                    )
                )
            }

            Text(
                text = formatTime(currentPosition.toInt()),
                color = Color.LightGray,
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
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = Color.White
            )
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier
                    .padding(10.dp)
                    .size(30.dp)
                    .background(Color(0x9E07F6F6), CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        if (PlayerController.mediaPlayer?.isPlaying == true) {
                            pause()
                        } else {
                            play()
                        }
                    }
                    .padding(10.dp),
                tint = Color.White
            )
        }
    }
}



