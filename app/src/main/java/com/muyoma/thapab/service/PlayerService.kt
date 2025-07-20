package com.muyoma.thapab.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.muyoma.thapab.MainActivity
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Song
import kotlinx.coroutines.*

class PlayerService : Service() {

    private lateinit var mediaSession: MediaSessionCompat
    private var currentSong: Song? = null
    private var isPlaying = false
    private var songList: List<Song> = emptyList()
    private var currentIndex: Int = -1
    private var progressJob: Job? = null

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (PlayerController.isPlaying()) {
                updateNotificationProgress()
                delay(200)
            }
        }
    }

    private fun updateNotificationProgress() {
        val mediaPlayer = PlayerController.mediaPlayer
        if (mediaPlayer != null) {
            val updatedNotification = buildNotification(mediaPlayer.currentPosition, mediaPlayer.duration)
            startForeground(NOTIFICATION_ID, updatedNotification)
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        val incomingList = intent?.getParcelableArrayListExtra<Song>(EXTRA_SONG_LIST)
        if (!incomingList.isNullOrEmpty()) {
            songList = incomingList
        }
        val song = intent?.getParcelableExtra<Song>(EXTRA_SONG) ?: currentSong
        if (song != null) {
            currentSong = song
            currentIndex = songList.indexOfFirst { it.id == song.id }
        }

        if (song == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_PLAY -> {
                PlayerController.play(applicationContext, song)
                isPlaying = true
                updateNotification()
                startProgressUpdates()
            }

            ACTION_PAUSE -> {
                PlayerController.pause()
                isPlaying = false
                updateNotification()
                progressJob?.cancel()
            }

            ACTION_NEXT -> {
                if (songList.isNotEmpty() && currentIndex < songList.lastIndex) {
                    currentIndex++
                    currentSong = songList[currentIndex]
                    PlayerController.play(applicationContext, currentSong!!)
                    isPlaying = true
                    updateNotification()
                    startProgressUpdates()
                }
            }

            ACTION_PREV -> {
                if (songList.isNotEmpty() && currentIndex > 0) {
                    currentIndex--
                    currentSong = songList[currentIndex]
                    PlayerController.play(applicationContext, currentSong!!)
                    isPlaying = true
                    updateNotification()
                    startProgressUpdates()
                }
            }

            null -> {
                PlayerController.play(applicationContext, song)
                isPlaying = true
                startForeground(NOTIFICATION_ID, createNotification(song))
                startProgressUpdates()
            }
        }

        return START_STICKY
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            currentSong?.let {
                PlayerController.play(applicationContext, it)
                isPlaying = true
                updateNotification()
                startProgressUpdates()
            }
            broadcastAction(BROADCAST_ACTION_PLAY)
        }

        override fun onPause() {
            PlayerController.pause()
            isPlaying = false
            updateNotification()
            progressJob?.cancel()
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

        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingContentIntent = PendingIntent.getActivity(
            this, 100, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.bg4)
            .setContentTitle(currentSong?.title ?: "Song")
            .setContentText(currentSong?.artist ?: "Artist")
            .setContentIntent(pendingContentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setProgress(duration, currentPosition, false)
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
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
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

    override fun onDestroy() {
        mediaSession.release()
        progressJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun broadcastAction(action: String, song: Song? = null) {
        val intent = Intent(action)
        song?.let { intent.putExtra("song", it) }
        sendBroadcast(intent)
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
        const val NOTIFICATION_ID = 1001
    }
}
