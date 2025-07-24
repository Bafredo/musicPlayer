package com.muyoma.thapab.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder // Import TaskStackBuilder
import androidx.media.session.MediaButtonReceiver
import com.muyoma.thapab.MainActivity
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Song
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
    private var currentIndex: Int = -1
    private var progressJob: Job? = null

    private var hasStartedForeground = false
    private var playbackStarted = false
    private var albumArtBitmap: Bitmap? = null // Cached bitmap for notification


    private fun startProgressUpdates() {
        progressJob?.cancel()
        // Use Dispatchers.Main for UI-related updates like notification,
        // or a dedicated thread for heavy processing before updating UI on Main.
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (PlayerController.isPlaying()) {
                updateNotificationProgress()
                delay(200) // Update every 200ms
            }
        }
    }

    private fun updateNotificationProgress() {
        val mediaPlayer = PlayerController.mediaPlayer
        if (mediaPlayer != null && currentSong != null) {
            val updatedNotification = buildNotification(mediaPlayer.currentPosition, mediaPlayer.duration)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, updatedNotification)

            // Update MediaSession's PlaybackState for external controllers
            val playbackState = PlaybackStateCompat.Builder(mediaSession.controller.playbackState)
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    mediaPlayer.currentPosition.toLong(), 1f
                )
                .build()
            mediaSession.setPlaybackState(playbackState)
        }
    }


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
        }

        val newSong = intent?.getParcelableExtra<Song>(EXTRA_SONG)
        val shouldPlayNewSong = newSong != null && newSong.id != currentSong?.id

        if (newSong != null) {
            currentSong = newSong
            currentIndex = songList.indexOfFirst { it.id == newSong.id }
            updateMediaSessionMetadata(newSong)
        } else if (currentSong == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_PLAY -> {
                if (shouldPlayNewSong || !isPlaying) {
                    currentSong?.let {
                        PlayerController.play(applicationContext, it)
                        isPlaying = true
                        playbackStarted = true
                        updateNotification()
                        startProgressUpdates()
                    }
                }
            }
            ACTION_PAUSE -> {
                mediaSession.controller.transportControls.pause()
            }
            ACTION_NEXT -> {
                mediaSession.controller.transportControls.skipToNext()
            }
            ACTION_PREV -> {
                mediaSession.controller.transportControls.skipToPrevious()
            }
            null -> {
                if (!playbackStarted && currentSong != null) {
                    PlayerController.play(applicationContext, currentSong!!)
                    isPlaying = true
                    playbackStarted = true
                    // Start foreground immediately if a song is ready and not already playing
                    startForeground(NOTIFICATION_ID, createNotification(currentSong!!))
                    hasStartedForeground = true // Set this flag
                    startProgressUpdates()
                }
            }
        }

        // Refined logic for startForeground and stopForeground
        if (isPlaying && !hasStartedForeground) {
            currentSong?.let {
                startForeground(NOTIFICATION_ID, createNotification(it))
                hasStartedForeground = true
            }
        } else if (!isPlaying && hasStartedForeground) {
            // Keep notification visible when paused, but not as foreground if playback truly stopped
            stopForeground(false) // false means the notification is not removed
            hasStartedForeground = false
        }


        return START_STICKY
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            if (!playbackStarted && currentSong != null) {
                PlayerController.play(applicationContext, currentSong!!)
                isPlaying = true
                playbackStarted = true
                updateNotification()
                startProgressUpdates()
            } else if (playbackStarted && !isPlaying) { // If paused, just resume
                PlayerController.resume()
                isPlaying = true
                updateNotification()
                startProgressUpdates()
            }
            broadcastAction(BROADCAST_ACTION_PLAY)
        }

        override fun onPause() {
            PlayerController.pause()
            isPlaying = false
            playbackStarted = false
            updateNotification()
            progressJob?.cancel() // Stop progress updates when paused
            broadcastAction(BROADCAST_ACTION_PAUSE)
        }

        override fun onSkipToNext() {
            if (songList.isNotEmpty() && currentIndex < songList.lastIndex) {
                currentIndex++
                currentSong = songList[currentIndex]
                PlayerController.play(applicationContext, currentSong!!)
                isPlaying = true
                updateNotification()
                startProgressUpdates()
                broadcastAction(BROADCAST_ACTION_NEXT, currentSong)
            }
        }

        override fun onSkipToPrevious() {
            if (songList.isNotEmpty() && currentIndex > 0) {
                currentIndex--
                currentSong = songList[currentIndex]
                PlayerController.play(applicationContext, currentSong!!)
                isPlaying = true
                updateNotification()
                startProgressUpdates()
                broadcastAction(BROADCAST_ACTION_PREV, currentSong)
            }
        }

        override fun onSeekTo(pos: Long) {
            PlayerController.seekTo(pos.toInt())
            updateNotificationProgress()
            broadcastAction(BROADCAST_ACTION_SEEK) // No need to send song with seek, as the UI should already know the song
        }
    }

    private fun updateNotification() {
        currentSong?.let {
            val notification = createNotification(it)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(song: Song): Notification {
        val mediaPlayer = PlayerController.mediaPlayer
        val currentPosition = mediaPlayer?.currentPosition ?: 0
        val duration = mediaPlayer?.duration ?: 0
        return buildNotification(currentPosition, duration)
    }

    private fun buildNotification(currentPosition: Int, duration: Int): Notification {
        val channelId = "music_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Music Playback", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val playIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_PLAY }
        val pauseIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_PAUSE }
        val nextIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_NEXT }
        val prevIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_PREV }

        // --- START: MODIFICATIONS FOR OPENING PLAYER PAGE ---
        val playerPageIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN // Standard action for launching an activity
            addCategory(Intent.CATEGORY_LAUNCHER) // Make it launchable
            // IMPORTANT: Pass the song identifier to the MainActivity
            // This allows the MainActivity to navigate to the correct player screen
            // and load the song. Using title for simplicity, but ID is better.
            putExtra("song_title_to_play", currentSong?.title)
        }

        // Use TaskStackBuilder to ensure correct back stack behavior
        val pendingContentIntent: PendingIntent = TaskStackBuilder.create(this).run {
            // Add the main activity to the back stack.
            // This is crucial if your Player Composable is not a top-level route
            // and you want the user to be able to go "back" to other parts of your app.
            addNextIntentWithParentStack(playerPageIntent)
            getPendingIntent(
                0, // Request code
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        // --- END: MODIFICATIONS FOR OPENING PLAYER PAGE ---


        val largeIconBitmap = albumArtBitmap ?: BitmapFactory.decodeResource(resources, R.drawable.bg4)

        val progress = currentPosition

        val builder = NotificationCompat.Builder(this, channelId)
            .setLargeIcon(largeIconBitmap)
            .setSmallIcon(R.drawable.music)
            .setContentTitle(currentSong?.title ?: "Unknown Title")
            .setContentText(currentSong?.artist ?: "Unknown Artist")
            .setContentIntent(pendingContentIntent) // Set the modified PendingIntent here
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(duration, progress, false)
            .addAction(
                R.drawable.back, "Previous",
                PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE)
            )
            .addAction(
                if (isPlaying) R.drawable.pause else R.drawable.play,
                if (isPlaying) "Pause" else "Play",
                PendingIntent.getService(
                    this, 1,
                    if (isPlaying) pauseIntent else playIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                R.drawable.forward, "Next",
                PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE)
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(1)
            )
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
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING
                else PlaybackStateCompat.STATE_PAUSED,
                currentPosition.toLong(), 1f
            )
            .build()

        mediaSession.setPlaybackState(playbackState)

        return builder.build()
    }

    private fun updateMediaSessionMetadata(song: Song) {
        val albumArt = albumArtBitmap ?: getSquareThumbnail(BitmapFactory.decodeResource(resources, R.drawable.bg4))

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, PlayerController.mediaPlayer?.duration?.toLong() ?: 0L)
            .build()

        mediaSession.setMetadata(metadata)
    }

    override fun onDestroy() {
        mediaSession.release()
        progressJob?.cancel()
        albumArtBitmap?.recycle()
        albumArtBitmap = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun broadcastAction(action: String, song: Song? = null) {
        val intent = Intent(action)
        song?.let { intent.putExtra("song", it) }
        sendBroadcast(intent)
    }

    fun getSquareThumbnail(bitmap: Bitmap, size: Int = 128): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, size, size, true)
    }


    companion object {
        const val ACTION_PLAY = "com.muyoma.thapab.ACTION_PLAY"
        const val ACTION_PAUSE = "com.muyoma.thapab.ACTION_PAUSE"
        const val ACTION_NEXT = "com.muyoma.thapab.ACTION_NEXT"
        const val ACTION_PREV = "com.muyoma.thapab.ACTION_PREV"
        const val EXTRA_SONG = "com.muyoma.thapab.EXTRA_SONG"
        const val EXTRA_SONG_LIST = "com.muyoma.thapab.EXTRA_SONG_LIST"
        const val BROADCAST_ACTION_PLAY = "com.muyoma.thapab.PLAY"
        const val BROADCAST_ACTION_PAUSE = "com.muyoma.thapab.PAUSE"
        const val BROADCAST_ACTION_NEXT = "com.muyoma.thapab.NEXT"
        const val BROADCAST_ACTION_PREV = "com.muyoma.thapab.PREV"
        const val BROADCAST_ACTION_SEEK = "com.muyoma.thapab.SEEK"
        const val NOTIFICATION_ID = 1001
    }
}