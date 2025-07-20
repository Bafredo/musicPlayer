package com.muyoma.thapab.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.muyoma.thapab.R
import com.muyoma.thapab.data.DBHandler
import com.muyoma.thapab.models.Playlist
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.service.PlayerService
import com.muyoma.thapab.ui.composables.PlayLister
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.collections.emptyList

class DataViewModel(application: Application) : AndroidViewModel(application) {
    var _songs = MutableStateFlow<List<Song>>(emptyList())
    var songs : StateFlow<List<Song>> = _songs.asStateFlow()
    val _isPlaying = PlayerController._isPlaying
    val isPlaying: StateFlow<Boolean> = _isPlaying
    val _currentSong = PlayerController._currentSong
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    val _showPlayer = PlayerController._isPlaying
    val showPlayer: StateFlow<Boolean> = _showPlayer.asStateFlow()
    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs = _likedSongs.asStateFlow()
    val context = application.applicationContext
    val dbHandler = DBHandler(context)


    fun refreshLikedSongs() {
        _likedSongs.value = getLikedSongs()
    }
    init {
        refreshLikedSongs()
    }



//    fun initDatabase(context: Context) {
//        dbHandler = DBHandler(context)
//    }

    fun likeSong(song: Song) {
        dbHandler.likeSong(song.id.toString())
    }

    fun unlikeSong(song: Song) {
        dbHandler.unlikeSong(song.id.toString())
    }

    fun isSongLiked(song: Song): Boolean {
        return dbHandler.isSongLiked(song.id.toString())
    }

    fun getLikedSongs(): List<Song> {
        val likedIds = dbHandler.getLikedSongs().toSet()
        return songs.value.filter { likedIds.contains(it.id.toString()) }
    }
    fun createPlaylist(name: String) {
        dbHandler.createPlaylist(name)
    }

    fun deletePlaylist(name: String) {
        dbHandler.deletePlaylist(name)
    }

    fun getAllPlaylists(): List<String> {
        return dbHandler.getAllPlaylists()
    }

    fun addSongToPlaylist(playlistName: String, song: Song) {
        dbHandler.addSongToPlaylist(playlistName, song.id.toString())
    }

    fun removeSongFromPlaylist(playlistName: String, song: Song) {
        dbHandler.removeSongFromPlaylist(playlistName, song.id.toString())
    }

    fun getSongsFromPlaylist(playlistName: String): List<Song> {
        val ids = dbHandler.getSongsFromPlaylist(playlistName).toSet()
        return songs.value.filter { ids.contains(it.id.toString()) }
    }






    fun getAllSongs(context: Context){
        val songList = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val song = Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol),
                    artist = cursor.getString(artistCol),
                    data = cursor.getString(dataCol),
                )
                songList += song
            }
        }
        _songs.value = songList
        _currentSong.value = songList.firstOrNull()

        recommended = songList.take(5)
        mostPopular = songList.takeLast(5)
        mostPlayed = songList.shuffled().take(5)

    }



    fun playSong(context: Context, song: Song) {
        PlayerController.play(context, song)

        // Only update if new song
        val serviceIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra(PlayerService.EXTRA_SONG, song) // ✅ Corrected this line
            putParcelableArrayListExtra(PlayerService.EXTRA_SONG_LIST, ArrayList(songs.value)) // ✅ Pass full list
        }

        ContextCompat.startForegroundService(context, serviceIntent)

        _isPlaying.value = true
        _showPlayer.value = true
    }



    fun pauseSong(context: Context) {
        PlayerController.pause()
        _isPlaying.value = false

        val pauseIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PAUSE
        }
        context.startService(pauseIntent)
    }

    fun playNext(context: Context){
        var index = songs.value.indexOf(currentSong.value)
        if (index == (songs.value.size-1)){
            _currentSong.value = songs.value.first()
        } else {
            _currentSong.value = songs.value[++index]
        }
        currentSong.value?.let { it->
            playSong(context,it)
        }

    }
    fun playPrev(context: Context){
        var index = songs.value.indexOf(currentSong.value)
        if (index == 0){
            _currentSong.value = songs.value.last()
        } else {
            _currentSong.value = songs.value[--index]
        }
        currentSong.value?.let { it->
            playSong(context,it)
        }

    }
    fun unpauseSong() {
        PlayerController.resume()
        _isPlaying.value = true
        _showPlayer.value = true
    }

    fun stopSong(){
        PlayerController.stop()
    }
    fun getDuration() : Float{
       return PlayerController.mediaPlayer?.duration?.toFloat() ?: 3.00f
    }




    var recommended by mutableStateOf(emptyList<Song>())
        private set

    var mostPopular by mutableStateOf(emptyList<Song>())
        private set

    var mostPlayed by mutableStateOf(emptyList<Song>())
        private set


    var samplePlaylists by mutableStateOf(
             listOf(
                Playlist(
                    id = 1L,
                    title = "Chill Vibes",
                    songs = 15,
                    duration = 52.3,
                    thumbnail = R.drawable.bg4
                ),
                Playlist(
                    id = 2L,
                    title = "Workout Hits",
                    songs = 20,
                    duration = 70.0,
                    thumbnail = R.drawable.bg2
                ),
                Playlist(
                    id = 3L,
                    title = "Retro Classics",
                    songs = 12,
                    duration = 48.2,
                    thumbnail = R.drawable.bg
                ),
                Playlist(
                    id = 4L,
                    title = "Afro Beats",
                    songs = 18,
                    duration = 61.5,
                    thumbnail = R.drawable.bg3
                ),
                Playlist(
                    id = 5L,
                    title = "Late Night",
                    songs = 10,
                    duration = 42.0,
                    thumbnail = R.drawable.bg4
                )
            )

    )
        private set

    fun getSongByTitle(title: String): Song? {
        return songs.value.find { it.title == title }
    }
}
