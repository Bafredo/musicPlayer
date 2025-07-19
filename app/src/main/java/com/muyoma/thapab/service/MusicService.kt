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

object PlayerController {
    var mediaPlayer: MediaPlayer? = null

    fun play(context: Context, song: Song) {
        stop()
        mediaPlayer = MediaPlayer.create(context, song.data.toUri()).apply {
            start()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }
    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentPosition() : Float? = mediaPlayer?.currentPosition?.toFloat()
}
//class MusicService : Service() {
//
//
//    private lateinit var mediaPlayer : MediaPlayer
//    private lateinit var mediaSession: MediaSession
//    private lateinit var audioManager: AudioManager
//    private lateinit var audioFocusRequest: AudioFocusRequest
//
//
//    override fun onCreate() {
//        super.onCreate()
//        mediaSession =
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun playMedia(){
//        val result = audioManager.requestAudioFocus(audioFocusRequest)
//        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
//            mediaPlayer.start()
//        }
//    }
//    private fun createNotification() : Notification{
//        val intent = Intent(this, MainActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(this,0,intent, PendingIntent.FLAG_IMMUTABLE)
//
//        return NotificationCompat.Builder(this,"MUSIC_CHANNEL")
//            .setContentTitle("Now Playing")
//            .setContentText("Title")
//            .setSmallIcon(R.drawable.bg4)
//            .setContentIntent(pendingIntent)
//            .setStyle(Notification.MediaStyle().setMediaSession(mediaSession.sessionToken) as NotificationCompat.Style?)
//            .addAction(R.drawable.bg,"Prev",null)
//            .addAction(R.drawable.bg,"Pause",null)
//            .addAction(R.drawable.bg,"Next",null)
//            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//            .build()
//    }
//    override fun onBind(intent: Intent?) : IBinder? = null
//}

