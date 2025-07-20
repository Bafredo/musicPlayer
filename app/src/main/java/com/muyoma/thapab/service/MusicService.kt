package com.muyoma.thapab.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Song
import androidx.core.net.toUri
import com.muyoma.thapab.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlayerController {
    var mediaPlayer: MediaPlayer? = null
    var _currentSong: MutableStateFlow<Song?> = MutableStateFlow(null)
    var _isPlaying : MutableStateFlow<Boolean> = MutableStateFlow(false)
    val currentSong : StateFlow<Song?> = _currentSong.asStateFlow()

    private var lastPosition: Int = 0



    fun play(context: Context, song: Song) {
        if (mediaPlayer == null || currentSong.value?.data != song.data) {
            stop() // only stop when a different song is selected
            mediaPlayer = MediaPlayer.create(context, song.data.toUri()).apply {
                start()
            }
            _currentSong.value = song
            _isPlaying.value = true
            lastPosition = 0
        } else {
            // Resume the existing song from where it was paused
            mediaPlayer?.seekTo(lastPosition)
            mediaPlayer?.start()
            _isPlaying.value = true
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                lastPosition = it.currentPosition
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.seekTo(lastPosition)
            it.start()
            _isPlaying.value = true
        }
    }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
        lastPosition = 0
        _currentSong.value = null
        _isPlaying.value = false
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentPosition(): Float? = mediaPlayer?.currentPosition?.toFloat()
}
