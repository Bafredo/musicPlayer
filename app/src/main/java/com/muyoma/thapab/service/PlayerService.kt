package com.muyoma.thapab.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.media.session.MediaButtonReceiver
import com.muyoma.thapab.MainActivity
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.util.AppIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerService : Service() {

    lateinit var mediaSession: MediaSessionCompat
    private var currentSong: Song? = null
    private var isPlaying = false
    private var songList: List<Song> = emptyList()
    private var progressJob: Job? = null

    private var hasStartedForeground = false
    private var playbackStarted = false
    private var albumArtBitmap: Bitmap? = null
    private var cachedAlbumArtSongId: String? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "PlayerSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(mediaSessionCallback)
            isActive = true
        }
        albumArtBitmap = getSquareThumbnail(BitmapFactory.decodeResource(resources, R.drawable.bg4))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        val incomingList = intent?.getParcelableArrayListExtra<Song>(EXTRA_SONG_LIST)
        if (!incomingList.isNullOrEmpty()) {
            songList = incomingList
            PlayerController.setQueue(songList, currentSong?.id)
        }

        val newSong = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_SONG, Song::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_SONG)
        }

        if (newSong != null) {
            currentSong = newSong
            if (songList.isEmpty()) {
                songList = listOf(newSong)
            }
            PlayerController.setQueue(songList, newSong.id)
            cacheAlbumArt(newSong)
            updateMediaSessionMetadata(newSong)
        } else if (currentSong == null) {
            currentSong = PlayerController.currentSong.value
            if (currentSong == null && PlayerController.queue.value.isNotEmpty()) {
                currentSong = PlayerController.queue.value.first()
            }
        }

        val resolvedSong = currentSong
        if (resolvedSong == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_PLAY -> {
                if (PlayerController.playingSong.value?.id != resolvedSong.id || PlayerController.mediaPlayer == null) {
                    startSongPlayback(resolvedSong)
                } else if (!PlayerController.isPlaying()) {
                    PlayerController.resume()
                    isPlaying = true
                    playbackStarted = true
                    updateNotification()
                    startProgressUpdates()
                    broadcastAction(BROADCAST_ACTION_PLAY, resolvedSong)
                }
            }

            ACTION_PAUSE -> mediaSession.controller.transportControls.pause()
            ACTION_NEXT -> mediaSession.controller.transportControls.skipToNext()
            ACTION_PREV -> mediaSession.controller.transportControls.skipToPrevious()
            ACTION_WIDGET_REFRESH -> updateNotification()
            null -> {
                if (PlayerController.mediaPlayer == null || PlayerController.playingSong.value?.id != resolvedSong.id) {
                    startSongPlayback(resolvedSong)
                } else if (!PlayerController.isPlaying()) {
                    PlayerController.resume()
                    isPlaying = true
                    playbackStarted = true
                    updateNotification()
                    startProgressUpdates()
                }
            }
        }

        ensureForegroundState()
        return START_STICKY
    }

    fun handleTrackCompletion() {
        val nextSong = PlayerController.playNext(this)
        if (nextSong != null) {
            currentSong = nextSong
            isPlaying = true
            playbackStarted = true
            cacheAlbumArt(nextSong)
            updateMediaSessionMetadata(nextSong)
            updateNotification()
            startProgressUpdates()
            broadcastAction(BROADCAST_ACTION_NEXT, nextSong)
            return
        }

        isPlaying = false
        playbackStarted = false
        progressJob?.cancel()
        updateNotification()
        ensureForegroundState()
        broadcastAction(BROADCAST_ACTION_PAUSE, currentSong)
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            val song = currentSong ?: PlayerController.currentSong.value ?: PlayerController.queue.value.firstOrNull()
            if (song == null) return

            currentSong = song
            if (PlayerController.mediaPlayer == null || PlayerController.playingSong.value?.id != song.id) {
                startSongPlayback(song)
            } else if (!isPlaying) {
                PlayerController.resume()
                isPlaying = true
                playbackStarted = true
                updateNotification()
                startProgressUpdates()
            }
            broadcastAction(BROADCAST_ACTION_PLAY, song)
        }

        override fun onPause() {
            PlayerController.pause()
            isPlaying = false
            playbackStarted = PlayerController.mediaPlayer != null
            updateNotification()
            progressJob?.cancel()
            ensureForegroundState()
            broadcastAction(BROADCAST_ACTION_PAUSE, currentSong)
        }

        override fun onSkipToNext() {
            val nextSong = PlayerController.playNext(this@PlayerService) ?: return
            currentSong = nextSong
            isPlaying = true
            playbackStarted = true
            cacheAlbumArt(nextSong)
            updateMediaSessionMetadata(nextSong)
            updateNotification()
            startProgressUpdates()
            broadcastAction(BROADCAST_ACTION_NEXT, nextSong)
        }

        override fun onSkipToPrevious() {
            val previousSong = PlayerController.playPrevious(this@PlayerService) ?: return
            currentSong = previousSong
            isPlaying = true
            playbackStarted = true
            cacheAlbumArt(previousSong)
            updateMediaSessionMetadata(previousSong)
            updateNotification()
            startProgressUpdates()
            broadcastAction(BROADCAST_ACTION_PREV, previousSong)
        }

        override fun onSeekTo(pos: Long) {
            PlayerController.seekTo(pos.toInt())
            updateNotificationProgress()
            broadcastAction(BROADCAST_ACTION_SEEK, currentSong)
        }
    }

    private fun startSongPlayback(song: Song) {
        currentSong = song
        if (songList.isEmpty()) {
            songList = listOf(song)
        }
        PlayerController.setQueue(songList, song.id)
        PlayerController.play(this, song)
        isPlaying = true
        playbackStarted = true
        cacheAlbumArt(song)
        updateMediaSessionMetadata(song)
        updateNotification()
        startProgressUpdates()
        broadcastAction(BROADCAST_ACTION_PLAY, song)
    }

    private fun ensureForegroundState() {
        val notification = createNotification()
        if (isPlaying && !hasStartedForeground) {
            startForeground(NOTIFICATION_ID, notification)
            hasStartedForeground = true
        } else if (!isPlaying && hasStartedForeground) {
            stopForeground(false)
            hasStartedForeground = false
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification)
        } else {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (PlayerController.isPlaying()) {
                updateNotificationProgress()
                delay(1000L)
            }
        }
    }

    private fun updateNotificationProgress() {
        val mediaPlayer = PlayerController.mediaPlayer ?: return
        val song = currentSong ?: return
        val updatedNotification = buildNotification(mediaPlayer.currentPosition, mediaPlayer.duration)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, updatedNotification)

        val playbackState = PlaybackStateCompat.Builder(mediaSession.controller.playbackState)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                mediaPlayer.currentPosition.toLong(),
                1f
            )
            .build()
        mediaSession.setPlaybackState(playbackState)
        broadcastAction(BROADCAST_ACTION_STATE_CHANGED, song)
    }

    private fun updateNotification() {
        currentSong?.let {
            cacheAlbumArt(it)
            updateMediaSessionMetadata(it)
        }
        ensureForegroundState()
        currentSong?.let { broadcastAction(BROADCAST_ACTION_STATE_CHANGED, it) }
    }

    private fun createNotification(): Notification {
        val mediaPlayer = PlayerController.mediaPlayer
        val currentPosition = mediaPlayer?.currentPosition ?: 0
        val duration = mediaPlayer?.duration ?: 0
        return buildNotification(currentPosition, duration)
    }

    fun loadBitmapFromUri(context: Context, uri: Uri?): Bitmap? {
        if (uri == null) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheAlbumArt(song: Song) {
        if (cachedAlbumArtSongId == song.id && albumArtBitmap != null) return

        val nextBitmap = loadBitmapFromUri(applicationContext, song.albumArtUri)
            ?.let { getSquareThumbnail(it) }
            ?: getSquareThumbnail(BitmapFactory.decodeResource(resources, R.drawable.bg4))

        if (cachedAlbumArtSongId != null && cachedAlbumArtSongId != song.id) {
            albumArtBitmap?.recycle()
        }

        albumArtBitmap = nextBitmap
        cachedAlbumArtSongId = song.id
    }

    private fun buildNotification(currentPosition: Int, duration: Int): Notification {
        val channelId = "music_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Music Playback", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val playIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_PLAY }
        val pauseIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_PAUSE }
        val nextIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_NEXT }
        val prevIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_PREV }

        val playerPageIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            putExtra(AppIntents.EXTRA_SONG_TITLE_TO_PLAY, currentSong?.title)
        }

        val pendingContentIntent: PendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(playerPageIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                ?: PendingIntent.getActivity(
                    this@PlayerService,
                    0,
                    playerPageIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
        }

        val largeIconBitmap = albumArtBitmap ?: getSquareThumbnail(BitmapFactory.decodeResource(resources, R.drawable.bg4))
        val builder = NotificationCompat.Builder(this, channelId)
            .setLargeIcon(largeIconBitmap)
            .setSmallIcon(R.drawable.music)
            .setContentTitle(currentSong?.title ?: "Unknown Title")
            .setContentText(currentSong?.artist ?: "Unknown Artist")
            .setContentIntent(pendingContentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(duration.coerceAtLeast(1), currentPosition, false)
            .addAction(
                R.drawable.back,
                "Previous",
                PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            .addAction(
                if (isPlaying) R.drawable.pause else R.drawable.play,
                if (isPlaying) "Pause" else "Play",
                PendingIntent.getService(
                    this,
                    1,
                    if (isPlaying) pauseIntent else playIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .addAction(
                R.drawable.forward,
                "Next",
                PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                currentPosition.toLong(),
                1f
            )
            .build()
        mediaSession.setPlaybackState(playbackState)

        return builder.build()
    }

    private fun updateMediaSessionMetadata(song: Song) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, PlayerController.mediaPlayer?.duration?.toLong() ?: 0L)
            .apply {
                albumArtBitmap?.let { putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) }
            }
            .build()
        mediaSession.setMetadata(metadata)
    }

    override fun onDestroy() {
        mediaSession.release()
        progressJob?.cancel()
        albumArtBitmap?.recycle()
        albumArtBitmap = null
        cachedAlbumArtSongId = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun broadcastAction(action: String, song: Song? = null) {
        val intent = Intent(action)
        song?.let { intent.putExtra("song", it) }
        intent.putExtra(EXTRA_IS_PLAYING, isPlaying)
        sendBroadcast(intent)
    }

    fun getSquareThumbnail(bitmap: Bitmap, size: Int = 128): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newEdge = minOf(width, height)
        val xOffset = (width - newEdge) / 2
        val yOffset = (height - newEdge) / 2
        val square = Bitmap.createBitmap(bitmap, xOffset, yOffset, newEdge, newEdge)
        return Bitmap.createScaledBitmap(square, size, size, true)
    }

    companion object {
        const val ACTION_PLAY = "com.muyoma.thapab.ACTION_PLAY"
        const val ACTION_PAUSE = "com.muyoma.thapab.ACTION_PAUSE"
        const val ACTION_NEXT = "com.muyoma.thapab.ACTION_NEXT"
        const val ACTION_PREV = "com.muyoma.thapab.ACTION_PREV"
        const val ACTION_WIDGET_REFRESH = "com.muyoma.thapab.ACTION_WIDGET_REFRESH"
        const val EXTRA_SONG = "com.muyoma.thapab.EXTRA_SONG"
        const val EXTRA_SONG_LIST = "com.muyoma.thapab.EXTRA_SONG_LIST"
        const val EXTRA_IS_PLAYING = "com.muyoma.thapab.EXTRA_IS_PLAYING"
        const val BROADCAST_ACTION_PLAY = "com.muyoma.thapab.PLAY"
        const val BROADCAST_ACTION_PAUSE = "com.muyoma.thapab.PAUSE"
        const val BROADCAST_ACTION_NEXT = "com.muyoma.thapab.NEXT"
        const val BROADCAST_ACTION_PREV = "com.muyoma.thapab.PREV"
        const val BROADCAST_ACTION_SEEK = "com.muyoma.thapab.SEEK"
        const val BROADCAST_ACTION_STATE_CHANGED = "com.muyoma.thapab.STATE_CHANGED"
        const val NOTIFICATION_ID = 1001
    }
}
