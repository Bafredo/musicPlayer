package com.muyoma.thapab.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Song

object MusicLibraryRepository {

    fun loadAllSongs(context: Context): List<Song> {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID
        )

        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                songs += Song(
                    id = id.toString(),
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol) ?: "Unknown",
                    data = uri.toString(),
                    albumArtUri = buildAlbumArtUri(cursor.getLong(albumIdCol))
                )
            }
        }

        return songs
    }

    fun createSongFromUri(context: Context, uri: Uri): Song {
        var displayName = "Shared audio"
        var artist = "External"

        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }
        }

        return Song(
            id = "external_${uri}",
            title = displayName.substringBeforeLast('.'),
            artist = artist,
            coverResId = R.drawable.bg,
            data = uri.toString(),
            albumArtUri = null
        )
    }

    fun likedSongs(context: Context, allSongs: List<Song> = loadAllSongs(context)): List<Song> {
        val db = DBHandler.getInstance(context)
        val likedUris = db.getLikedSongs().toSet()
        return allSongs.filter { it.data in likedUris }
    }

    fun playlistSongs(context: Context, playlistName: String, allSongs: List<Song> = loadAllSongs(context)): List<Song> {
        val db = DBHandler.getInstance(context)
        return db.getSongsFromPlaylist(playlistName, allSongs)
    }

    fun playlists(context: Context): List<String> {
        return DBHandler.getInstance(context).getAllPlaylists()
    }

    private fun buildAlbumArtUri(albumId: Long): Uri? {
        return if (albumId > 0) {
            ContentUris.withAppendedId(ALBUM_ART_CONTENT_URI, albumId)
        } else {
            null
        }
    }

    private val ALBUM_ART_CONTENT_URI: Uri = Uri.parse("content://media/external/audio/albumart")
}
