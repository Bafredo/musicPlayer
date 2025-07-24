package com.muyoma.thapab.service

import android.content.Context
import android.media.MediaPlayer
import androidx.core.net.toUri
import com.muyoma.thapab.models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlayerController {
    var mediaPlayer: MediaPlayer? = null
    var _currentSong: MutableStateFlow<Song?> = MutableStateFlow(null)
    var _isPlaying : MutableStateFlow<Boolean> = MutableStateFlow(false)
    val currentSong : StateFlow<Song?> = _currentSong.asStateFlow()
    var playingSong : MutableStateFlow<Song?> = MutableStateFlow(null)

    private var lastPosition: Int = 0

    fun play(context: Context, song: Song) {
        println("PlayerController.play: Requesting to play song: ${song.title}, Current playingSong: ${playingSong.value?.title}, MediaPlayer null: ${mediaPlayer == null}")

        if (playingSong.value?.id == song.id && mediaPlayer != null) {
            if (!mediaPlayer!!.isPlaying) { // Check if it's actually paused
                mediaPlayer?.seekTo(lastPosition)
                mediaPlayer?.start()
                _isPlaying.value = true
                println("PlayerController.play: Resuming same song: ${song.title}")
            } else {
                println("PlayerController.play: Song already playing: ${song.title}, no action needed.")
            }
        } else {
            println("PlayerController.play: Stopping current and playing new song: ${song.title}")
            stop() // Release existing MediaPlayer before creating a new one

            mediaPlayer = MediaPlayer.create(context, song.data.toUri()).apply {
                setOnCompletionListener {
                    // Handle song completion here (e.g., broadcast for DataViewModel to play next)
                    // You might want to send a specific broadcast here, or use a callback mechanism
                    // to trigger playNext() in your DataViewModel.
                    println("PlayerController: Song completed: ${song.title}")
                    _isPlaying.value = false // Set playing state to false on completion
                    // A simple way to trigger next song:
                    // This is a basic example. For a more robust solution, consider
                    // a callback interface from PlayerController to PlayerService.
                    (context as? PlayerService)?.mediaSession?.controller?.transportControls?.skipToNext()
                }
                start()
            }
            playingSong.value = song
            _currentSong.value = song
            _isPlaying.value = true
            lastPosition = 0
            println("PlayerController.play: Started new song: ${song.title}")
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
        playingSong.value = null // Clear playingSong as well
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentPosition(): Float? = mediaPlayer?.currentPosition?.toFloat()

    // --- NEW: Method to perform seeking ---
    fun seekTo(positionMs: Int) {
        mediaPlayer?.let {
            val validPosition = if (positionMs < 0) 0 else if (positionMs > it.duration) it.duration else positionMs
            it.seekTo(validPosition)
            lastPosition = validPosition // Update last position for resume
            println("PlayerController: Seeked to $validPosition ms")
        }
    }
}