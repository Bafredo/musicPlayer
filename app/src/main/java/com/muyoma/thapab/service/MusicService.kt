package com.muyoma.thapab.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.muyoma.thapab.models.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

object PlayerController {
    var mediaPlayer: MediaPlayer? = null
    var _currentSong: MutableStateFlow<Song?> = MutableStateFlow(null)
    var _isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var _isPreparing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _playbackPosition = MutableStateFlow(0f)
    private val _playbackDuration = MutableStateFlow(1f)
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    private val _shuffleEnabled = MutableStateFlow(false)
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    private val _currentIndex = MutableStateFlow(-1)

    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()
    val playbackPosition: StateFlow<Float> = _playbackPosition.asStateFlow()
    val playbackDuration: StateFlow<Float> = _playbackDuration.asStateFlow()
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    var playingSong: MutableStateFlow<Song?> = MutableStateFlow(null)

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var progressJob: Job? = null
    private var lastPosition: Int = 0

    fun setQueue(songs: List<Song>, currentSongId: String? = null) {
        _queue.value = songs
        _currentIndex.value = when {
            songs.isEmpty() -> -1
            currentSongId == null -> _currentIndex.value.coerceIn(0, songs.lastIndex)
            else -> songs.indexOfFirst { it.id == currentSongId }.takeIf { it >= 0 } ?: 0
        }
    }

    fun cycleRepeatMode(): RepeatMode {
        val nextMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = nextMode
        return nextMode
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }

    fun toggleShuffle(): Boolean {
        val enabled = !_shuffleEnabled.value
        _shuffleEnabled.value = enabled
        return enabled
    }

    fun setShuffleEnabled(enabled: Boolean) {
        _shuffleEnabled.value = enabled
    }

    fun syncCurrentSong(song: Song, songs: List<Song> = _queue.value) {
        if (songs.isNotEmpty()) {
            setQueue(songs, song.id)
        } else if (_queue.value.isEmpty()) {
            _queue.value = listOf(song)
            _currentIndex.value = 0
        }
        _currentSong.value = song
        playingSong.value = song
    }

    fun play(context: Context, song: Song) {
        val currentlyPlayingId = playingSong.value?.id
        syncCurrentSong(song)
        if (currentlyPlayingId == song.id && mediaPlayer != null) {
            if (!mediaPlayer!!.isPlaying && !_isPreparing.value) {
                mediaPlayer?.seekTo(lastPosition)
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressUpdates()
            }
            return
        }

        stop(keepQueue = true)

        val uri = Uri.parse(song.data)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            when (uri.scheme) {
                "content", "android.resource", "file" -> setDataSource(context, uri)
                else -> setDataSource(song.data)
            }
            setOnPreparedListener {
                _isPreparing.value = false
                it.start()
                _isPlaying.value = true
                _playbackDuration.value = it.duration.toFloat().coerceAtLeast(1f)
                _playbackPosition.value = 0f
                lastPosition = 0
                startProgressUpdates()
            }
            setOnCompletionListener {
                _isPlaying.value = false
                progressJob?.cancel()
                _playbackPosition.value = _playbackDuration.value
                (context as? PlayerService)?.handleTrackCompletion()
            }
            setOnErrorListener { _, what, extra ->
                Log.e("PlayerController", "MediaPlayer error what=$what extra=$extra for ${song.title}")
                _isPreparing.value = false
                _isPlaying.value = false
                progressJob?.cancel()
                true
            }
            prepareAsync()
        }

        playingSong.value = song
        _currentSong.value = song
        _isPreparing.value = true
        _isPlaying.value = false
        lastPosition = 0
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                lastPosition = it.currentPosition
                it.pause()
                _isPlaying.value = false
                _playbackPosition.value = lastPosition.toFloat()
                progressJob?.cancel()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.seekTo(lastPosition)
            it.start()
            _isPlaying.value = true
            startProgressUpdates()
        }
    }

    fun stop(keepQueue: Boolean = false) {
        _isPreparing.value = false
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        lastPosition = 0
        _playbackPosition.value = 0f
        _playbackDuration.value = 1f
        _currentSong.value = null
        _isPlaying.value = false
        playingSong.value = null
        if (!keepQueue) {
            _queue.value = emptyList()
            _currentIndex.value = -1
        }
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentPosition(): Float? = mediaPlayer?.currentPosition?.toFloat()

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let {
            val validPosition = positionMs.coerceIn(0, it.duration)
            it.seekTo(validPosition)
            lastPosition = validPosition
            _playbackPosition.value = validPosition.toFloat()
        }
    }

    fun playNext(context: Context): Song? {
        val nextSong = resolveNextSong() ?: return null
        play(context, nextSong)
        return nextSong
    }

    fun playPrevious(context: Context): Song? {
        val previousSong = resolvePreviousSong() ?: return null
        play(context, previousSong)
        return previousSong
    }

    fun hasUpcomingSong(): Boolean {
        val songs = _queue.value
        return when {
            songs.isEmpty() -> false
            _repeatMode.value != RepeatMode.OFF -> true
            _shuffleEnabled.value && songs.size > 1 -> true
            _currentIndex.value in 0 until songs.lastIndex -> true
            else -> false
        }
    }

    private fun resolveNextSong(): Song? {
        val songs = _queue.value
        if (songs.isEmpty()) return null

        val current = _currentSong.value
        val currentIdx = songs.indexOfFirst { it.id == current?.id }
        if (currentIdx >= 0) {
            _currentIndex.value = currentIdx
        }

        if (_repeatMode.value == RepeatMode.ONE) {
            return current ?: songs.firstOrNull()
        }

        if (_shuffleEnabled.value && songs.size > 1) {
            val candidates = songs.filterNot { it.id == current?.id }
            return candidates.randomOrNull(Random(System.currentTimeMillis())) ?: current ?: songs.first()
        }

        return when {
            currentIdx in 0 until songs.lastIndex -> songs[currentIdx + 1]
            _repeatMode.value == RepeatMode.ALL -> songs.first()
            else -> null
        }
    }

    private fun resolvePreviousSong(): Song? {
        val songs = _queue.value
        if (songs.isEmpty()) return null

        val current = _currentSong.value
        val currentIdx = songs.indexOfFirst { it.id == current?.id }
        if (currentIdx >= 0) {
            _currentIndex.value = currentIdx
        }

        if (_repeatMode.value == RepeatMode.ONE) {
            return current ?: songs.firstOrNull()
        }

        if (_shuffleEnabled.value && songs.size > 1) {
            val candidates = songs.filterNot { it.id == current?.id }
            return candidates.randomOrNull(Random(System.currentTimeMillis())) ?: current ?: songs.first()
        }

        return when {
            currentIdx > 0 -> songs[currentIdx - 1]
            _repeatMode.value == RepeatMode.ALL -> songs.last()
            current != null -> current
            else -> songs.first()
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = controllerScope.launch {
            while (true) {
                val player = mediaPlayer ?: break
                _playbackPosition.value = player.currentPosition.toFloat()
                _playbackDuration.value = player.duration.toFloat().coerceAtLeast(1f)
                delay(500L)
            }
        }
    }
}
