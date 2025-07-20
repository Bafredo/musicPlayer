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
import com.muyoma.thapab.R
import com.muyoma.thapab.data.DBHandler
import com.muyoma.thapab.models.Playlist
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.service.PlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.collections.emptyList

class DataViewModel(application: Application) : AndroidViewModel(application) {
    var _songs = MutableStateFlow<List<Song>>(emptyList())
    var songs : StateFlow<List<Song>> = _songs.asStateFlow()

    val _currentSong = PlayerController._currentSong
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    val _isPlaying = MutableStateFlow<Boolean>(currentSong.value != null)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val _currentSongLiked = MutableStateFlow<Boolean>(false)
    val currentSongLiked = _currentSongLiked.asStateFlow()
    val _showPlayer = MutableStateFlow<Boolean>(false)
    val showPlayer: StateFlow<Boolean> = _showPlayer.asStateFlow()
    var _playLists = MutableStateFlow<List<Playlist>>(emptyList())
    var playlists = _playLists.asStateFlow()

    var _showPlayListSheet = MutableStateFlow<Boolean>(false)
    val showPlayListSheet = _showPlayListSheet.asStateFlow()
    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs = _likedSongs.asStateFlow()
    val context = application.applicationContext
    val dbHandler = DBHandler(context)
     var _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong : StateFlow<Song?> = _selectedSong.asStateFlow()

    val _repeatSong = MutableStateFlow<Song?>(null)
    val repeatSong = _repeatSong.asStateFlow()


    fun togglePlaylistSheet(show: Boolean) {
        _showPlayListSheet.value = show
    }


    fun refreshLikedSongs() {
        _likedSongs.value = getLikedSongs()
    }
    init {

    }



//    fun initDatabase(context: Context) {
//        dbHandler = DBHandler(context)
//    }

    fun likeSong(song: Song) {
        dbHandler.likeSong(song.id.toString())
        _currentSongLiked.value = true
    }

    fun unlikeSong(song: Song) {
        dbHandler.unlikeSong(song.id.toString())
        _currentSongLiked.value = false
    }

    fun isSongLiked(song: Song?): Boolean {
        return dbHandler.isSongLiked(song?.id.toString())
    }

    fun getLikedSongs(): List<Song> {
        val likedIds = dbHandler.getLikedSongs().toSet()
        return songs.value.filter { likedIds.contains(it.id.toString()) }
    }
    fun createPlaylist(name: String) {
        dbHandler.createPlaylist(name)
        loadPlaylistsFromDB(songs.value)
    }

    fun deletePlaylist(name: String) {
        dbHandler.deletePlaylist(name)
    }

    fun getAllPlaylists(): List<String> {
        return dbHandler.getAllPlaylists()
    }

    fun addSongToPlaylist(playlistName: String, song: Song) {
        dbHandler.addSongToPlaylist(playlistName, song.data)
        refreshLikedSongs()
        loadPlaylistsFromDB(
            allSongs = songs.value
        )
    }

    fun removeSongFromPlaylist(playlistName: String, song: Song) {
        dbHandler.removeSongFromPlaylist(playlistName, song.id.toString())
    }

    fun getSongsFromPlaylist(playlistName: String): List<Song> {
        return dbHandler.getSongsFromPlaylist(
            playlistName,
            allSongs = songs.value
        )
    }

    fun repeatSong(s: Song){
        _repeatSong.value = s
    }
    fun undoRepeat(){
        _repeatSong.value = null
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
        _currentSongLiked.value = isSongLiked(currentSong.value)

        recommended = songList.take(5)
        mostPopular = songList.takeLast(5)
        mostPlayed = songList.shuffled().take(5)
        refreshLikedSongs()
        loadPlaylistsFromDB(
            allSongs = songs.value
        )

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
            _currentSongLiked.value = isSongLiked(currentSong.value)

        } else {
            _currentSong.value = songs.value[++index]
            _currentSongLiked.value = isSongLiked(currentSong.value)

        }
        currentSong.value?.let { it->
            playSong(context,it)
        }

    }
    fun playPrev(context: Context){
        var index = songs.value.indexOf(currentSong.value)
        if (index == 0){
            _currentSong.value = songs.value.last()
            _currentSongLiked.value = isSongLiked(currentSong.value)

        } else {
            _currentSong.value = songs.value[--index]
            _currentSongLiked.value = isSongLiked(currentSong.value)

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


    fun loadPlaylistsFromDB(allSongs: List<Song>) {
        val playlists = dbHandler.getAllPlaylists().mapIndexed { index, name ->
            cleanUpInvalidSongsInPlaylist(name, allSongs)
            val songs = dbHandler.getSongsFromPlaylist(name, allSongs)
            println("$name : ${songs.map { it.id }}")
            Playlist(
                id = index.toLong(),
                title = name,
                songs = songs.size,
                thumbnail = pickThumbnailFor(name, index),
            )
        }
        _playLists.value = playlists
    }


    private fun pickThumbnailFor(name: String, index: Int): Int {
        val thumbnails = listOf(
            R.drawable.bg, R.drawable.bg2, R.drawable.bg3, R.drawable.bg4
        )
        return thumbnails[index % thumbnails.size]
    }



    fun getSongByTitle(title: String): Song? {
        return songs.value.find { it.title == title }
    }
    fun cleanUpInvalidSongsInPlaylist(playlistName: String, allSongs: List<Song>) {
        val validIds = allSongs.map { it.id.toString() }.toSet()
        val playlistSongIds = dbHandler.getSongsFromPlaylist(playlistName, allSongs = allSongs)
            .map { it.id.toString() }

        playlistSongIds.forEach { id ->
            if (!validIds.contains(id)) {
                dbHandler.removeSongFromPlaylist(playlistName, id)
                println("Removed stale song ID=$id from playlist=$playlistName")
            }
        }
    }

}
