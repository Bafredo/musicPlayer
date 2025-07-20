package com.muyoma.thapab.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.muyoma.thapab.models.Song

class DBHandler(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "MusicDB"
        private const val DATABASE_VERSION = 2

        private const val TABLE_LIKED_SONGS = "LikedSongs"
        private const val TABLE_PLAYLISTS = "Playlists"
        private const val TABLE_PLAYLIST_SONGS = "PlaylistSongs"

        private const val KEY_ID = "id"
        private const val KEY_SONG_URI = "songUri"
        private const val KEY_PLAYLIST_NAME = "name"
        private const val KEY_PLAYLIST_ID = "playlistId"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createLikedSongs = """
            CREATE TABLE $TABLE_LIKED_SONGS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_SONG_URI TEXT UNIQUE
            );
        """.trimIndent()

        val createPlaylists = """
            CREATE TABLE $TABLE_PLAYLISTS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_PLAYLIST_NAME TEXT UNIQUE
            );
        """.trimIndent()

        val createPlaylistSongs = """
            CREATE TABLE $TABLE_PLAYLIST_SONGS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_PLAYLIST_ID INTEGER,
                $KEY_SONG_URI TEXT,
                FOREIGN KEY($KEY_PLAYLIST_ID) REFERENCES $TABLE_PLAYLISTS($KEY_ID)
            );
        """.trimIndent()

        db.execSQL(createLikedSongs)
        db.execSQL(createPlaylists)
        db.execSQL(createPlaylistSongs)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LIKED_SONGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLIST_SONGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")
        onCreate(db)
    }

    // --- Liked Songs ---
    fun likeSong(songUri: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_SONG_URI, songUri)
        }
        db.insertWithOnConflict(TABLE_LIKED_SONGS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        db.close()
    }

    fun unlikeSong(songUri: String) {
        val db = writableDatabase
        db.delete(TABLE_LIKED_SONGS, "$KEY_SONG_URI=?", arrayOf(songUri))
        db.close()
    }

    fun getLikedSongs(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $KEY_SONG_URI FROM $TABLE_LIKED_SONGS", null)
        val songs = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            do {
                songs.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return songs
    }

    fun isSongLiked(songUri: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_LIKED_SONGS WHERE $KEY_SONG_URI=?",
            arrayOf(songUri)
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
    }

    // --- Playlists ---
    fun createPlaylist(name: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_PLAYLIST_NAME, name)
        }
        val id = db.insertWithOnConflict(TABLE_PLAYLISTS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        db.close()
        return id
    }

    fun deletePlaylist(name: String) {
        val db = writableDatabase
        val idCursor = db.rawQuery("SELECT $KEY_ID FROM $TABLE_PLAYLISTS WHERE $KEY_PLAYLIST_NAME=?", arrayOf(name))
        if (idCursor.moveToFirst()) {
            val playlistId = idCursor.getInt(0)
            db.delete(TABLE_PLAYLIST_SONGS, "$KEY_PLAYLIST_ID=?", arrayOf(playlistId.toString()))
            db.delete(TABLE_PLAYLISTS, "$KEY_ID=?", arrayOf(playlistId.toString()))
        }
        idCursor.close()
        db.close()
    }

    fun renamePlaylist(oldName: String, newName: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_PLAYLIST_NAME, newName)
        }
        val rows = db.update(TABLE_PLAYLISTS, values, "$KEY_PLAYLIST_NAME=?", arrayOf(oldName))
        db.close()
        return rows > 0
    }

    fun getAllPlaylists(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $KEY_PLAYLIST_NAME FROM $TABLE_PLAYLISTS", null)
        val names = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            do {
                names.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return names
    }

    // --- Playlist Songs ---
    fun addSongToPlaylist(playlistName: String, songUri: String) {
        val db = writableDatabase
        val cursor = db.rawQuery(
            "SELECT $KEY_ID FROM $TABLE_PLAYLISTS WHERE $KEY_PLAYLIST_NAME=?",
            arrayOf(playlistName)
        )

        if (cursor.moveToFirst()) {
            val playlistId = cursor.getInt(0)

            val checkCursor = db.rawQuery(
                "SELECT 1 FROM $TABLE_PLAYLIST_SONGS WHERE $KEY_PLAYLIST_ID=? AND $KEY_SONG_URI=?",
                arrayOf(playlistId.toString(), songUri)
            )

            if (!checkCursor.moveToFirst()) {
                val values = ContentValues().apply {
                    put(KEY_PLAYLIST_ID, playlistId)
                    put(KEY_SONG_URI, songUri)
                }
                db.insert(TABLE_PLAYLIST_SONGS, null, values)
            }

            checkCursor.close()
        }

        cursor.close()
        db.close()
    }

    fun removeSongFromPlaylist(playlistName: String, songUri: String) {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT $KEY_ID FROM $TABLE_PLAYLISTS WHERE $KEY_PLAYLIST_NAME=?", arrayOf(playlistName))
        if (cursor.moveToFirst()) {
            val playlistId = cursor.getInt(0)
            db.delete(
                TABLE_PLAYLIST_SONGS,
                "$KEY_PLAYLIST_ID=? AND $KEY_SONG_URI=?",
                arrayOf(playlistId.toString(), songUri)
            )
        }
        cursor.close()
        db.close()
    }

    fun getSongsFromPlaylist(playlistName: String, allSongs: List<Song>): List<Song> {
        val db = readableDatabase
        val matchedSongs = mutableListOf<Song>()

        val cursor = db.rawQuery(
            "SELECT $KEY_ID FROM $TABLE_PLAYLISTS WHERE $KEY_PLAYLIST_NAME=?",
            arrayOf(playlistName.trim())
        )

        if (cursor.moveToFirst()) {
            val playlistId = cursor.getInt(0)

            val songCursor = db.rawQuery(
                "SELECT $KEY_SONG_URI FROM $TABLE_PLAYLIST_SONGS WHERE $KEY_PLAYLIST_ID=?",
                arrayOf(playlistId.toString())
            )

            if (songCursor.moveToFirst()) {
                do {
                    val uriString = songCursor.getString(0)
                    val matched = allSongs.find { it.data.toString() == uriString }
                    if (matched != null) {
                        matchedSongs.add(matched)
                    } else {
                        println("⚠ URI $uriString not found in allSongs")
                    }
                } while (songCursor.moveToNext())
            }

            songCursor.close()
        }

        cursor.close()
        db.close()
        return matchedSongs
    }
}
