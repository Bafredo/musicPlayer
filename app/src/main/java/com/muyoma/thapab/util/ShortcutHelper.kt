package com.muyoma.thapab.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.muyoma.thapab.MainActivity
import com.muyoma.thapab.R

object ShortcutHelper {

    fun updateDynamicShortcuts(context: Context, playlists: List<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        val dynamicShortcuts = playlists.take(3).map { playlistName ->
            ShortcutInfo.Builder(context, "playlist_$playlistName")
                .setShortLabel(playlistName.take(10))
                .setLongLabel("Play $playlistName")
                .setIcon(Icon.createWithResource(context, R.drawable.music))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(AppIntents.EXTRA_DESTINATION, AppIntents.DESTINATION_PLAYLIST)
                        putExtra(AppIntents.EXTRA_PLAYLIST_NAME, playlistName)
                        putExtra(AppIntents.EXTRA_AUTOPLAY, true)
                    }
                )
                .build()
        }

        shortcutManager.dynamicShortcuts = dynamicShortcuts
    }
}
