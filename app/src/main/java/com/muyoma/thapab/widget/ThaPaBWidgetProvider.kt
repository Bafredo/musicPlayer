package com.muyoma.thapab.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.muyoma.thapab.MainActivity
import com.muyoma.thapab.R
import com.muyoma.thapab.data.MusicLibraryRepository
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.service.PlayerService
import com.muyoma.thapab.util.AppIntents

class ThaPaBWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_CYCLE_SOURCE -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    cycleSource(context, widgetId)
                }
                refreshAll(context)
            }

            ACTION_TOGGLE_PLAYBACK -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                togglePlayback(context, widgetId)
                refreshAll(context)
            }

            PlayerService.BROADCAST_ACTION_STATE_CHANGED,
            ACTION_REFRESH_WIDGETS,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> refreshAll(context)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_thapab)
        val selectedSource = readSource(context, widgetId)
        val currentSong = PlayerController.currentSong.value
        val isPlaying = PlayerController.isPlaying()

        views.setTextViewText(R.id.widgetTitle, currentSong?.title ?: context.getString(R.string.widget_idle_title))
        views.setTextViewText(R.id.widgetSubtitle, currentSong?.artist ?: context.getString(R.string.widget_idle_subtitle))
        views.setTextViewText(R.id.widgetSource, selectedSource.displayName(context))
        views.setImageViewResource(R.id.widgetPlayPause, if (isPlaying) R.drawable.pause else R.drawable.play)

        views.setOnClickPendingIntent(
            R.id.widgetSource,
            broadcastIntent(context, ACTION_CYCLE_SOURCE, widgetId)
        )
        views.setOnClickPendingIntent(
            R.id.widgetInfo,
            activityIntent(context, widgetId, selectedSource, autoplay = false)
        )
        views.setOnClickPendingIntent(
            R.id.widgetPrev,
            serviceIntent(context, PlayerService.ACTION_PREV)
        )
        views.setOnClickPendingIntent(
            R.id.widgetPlayPause,
            broadcastIntent(context, ACTION_TOGGLE_PLAYBACK, widgetId)
        )
        views.setOnClickPendingIntent(
            R.id.widgetNext,
            serviceIntent(context, PlayerService.ACTION_NEXT)
        )

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun togglePlayback(context: Context, widgetId: Int) {
        if (PlayerController.currentSong.value == null || PlayerController.queue.value.isEmpty()) {
            val source = readSource(context, widgetId)
            val songs = songsForSource(context, source)
            val firstSong = songs.firstOrNull() ?: return
            val intent = Intent(context, PlayerService::class.java).apply {
                action = PlayerService.ACTION_PLAY
                putExtra(PlayerService.EXTRA_SONG, firstSong)
                putParcelableArrayListExtra(PlayerService.EXTRA_SONG_LIST, ArrayList(songs))
            }
            ContextCompat.startForegroundService(context, intent)
            return
        }

        val action = if (PlayerController.isPlaying()) PlayerService.ACTION_PAUSE else PlayerService.ACTION_PLAY
        context.startService(Intent(context, PlayerService::class.java).apply { this.action = action })
    }

    private fun songsForSource(context: Context, source: WidgetSource): List<Song> {
        return when (source.type) {
            WidgetSourceType.LIKED -> MusicLibraryRepository.likedSongs(context)
            WidgetSourceType.PLAYLIST -> MusicLibraryRepository.playlistSongs(context, source.value)
        }
    }

    private fun cycleSource(context: Context, widgetId: Int) {
        val options = availableSources(context)
        if (options.isEmpty()) return
        val current = readSource(context, widgetId)
        val currentIndex = options.indexOfFirst { it == current }.takeIf { it >= 0 } ?: 0
        val next = options[(currentIndex + 1) % options.size]
        writeSource(context, widgetId, next)
    }

    private fun availableSources(context: Context): List<WidgetSource> {
        val playlists = MusicLibraryRepository.playlists(context).map { WidgetSource(WidgetSourceType.PLAYLIST, it) }
        return listOf(WidgetSource(WidgetSourceType.LIKED, WidgetSourceType.LIKED.name)) + playlists
    }

    private fun refreshAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ThaPaBWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        onUpdate(context, appWidgetManager, ids)
    }

    private fun broadcastIntent(context: Context, action: String, widgetId: Int): PendingIntent {
        val intent = Intent(context, ThaPaBWidgetProvider::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            widgetId + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun activityIntent(context: Context, widgetId: Int, source: WidgetSource, autoplay: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            when (source.type) {
                WidgetSourceType.LIKED -> putExtra(AppIntents.EXTRA_DESTINATION, AppIntents.DESTINATION_LIKED)
                WidgetSourceType.PLAYLIST -> {
                    putExtra(AppIntents.EXTRA_DESTINATION, AppIntents.DESTINATION_PLAYLIST)
                    putExtra(AppIntents.EXTRA_PLAYLIST_NAME, source.value)
                }
            }
            putExtra(AppIntents.EXTRA_AUTOPLAY, autoplay)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getActivity(
            context,
            widgetId + 2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun serviceIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, PlayerService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun writeSource(context: Context, widgetId: Int, source: WidgetSource) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(keyFor(widgetId), source.encoded())
            .apply()
    }

    private fun readSource(context: Context, widgetId: Int): WidgetSource {
        val encoded = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(keyFor(widgetId), null)
        return WidgetSource.decode(encoded) ?: availableSources(context).firstOrNull()
        ?: WidgetSource(WidgetSourceType.LIKED, WidgetSourceType.LIKED.name)
    }

    private fun keyFor(widgetId: Int): String = "widget_source_$widgetId"

    companion object {
        const val ACTION_CYCLE_SOURCE = "com.muyoma.thapab.widget.CYCLE_SOURCE"
        const val ACTION_TOGGLE_PLAYBACK = "com.muyoma.thapab.widget.TOGGLE_PLAYBACK"
        const val ACTION_REFRESH_WIDGETS = "com.muyoma.thapab.widget.REFRESH"
        private const val PREFS_NAME = "widget_prefs"
    }
}

private enum class WidgetSourceType {
    LIKED,
    PLAYLIST
}

private data class WidgetSource(
    val type: WidgetSourceType,
    val value: String
) {
    fun encoded(): String = "${type.name}|$value"

    fun displayName(context: Context): String {
        return when (type) {
            WidgetSourceType.LIKED -> context.getString(R.string.widget_source_liked)
            WidgetSourceType.PLAYLIST -> value
        }
    }

    companion object {
        fun decode(encoded: String?): WidgetSource? {
            if (encoded.isNullOrBlank() || !encoded.contains("|")) return null
            val typeName = encoded.substringBefore("|")
            val value = encoded.substringAfter("|")
            val type = WidgetSourceType.entries.firstOrNull { it.name == typeName } ?: return null
            return WidgetSource(type, value)
        }
    }
}
