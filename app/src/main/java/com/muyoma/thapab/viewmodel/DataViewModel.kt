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
import androidx.lifecycle.viewModelScope
import com.muyoma.thapab.R
import com.muyoma.thapab.data.DBHandler // Ensure this import is correct
import com.muyoma.thapab.models.Playlist
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.service.PlayerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.emptyList

class DataViewModel(application: Application) : AndroidViewModel(application) {

    // Use the singleton instance of DBHandler
    private val dbHandler: DBHandler = DBHandler.getInstance(application.applicationContext)

    var _songs = MutableStateFlow<List<Song>>(emptyList())
    var songs : StateFlow<List<Song>> = _songs.asStateFlow()

    val _currentSong = PlayerController._currentSong
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    // Initialize _isPlaying based on PlayerController's actual state
    val _isPlaying = MutableStateFlow<Boolean>(PlayerController.isPlaying())
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
    // val context = application.applicationContext // No longer directly needed here for DBHandler

    var _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong : StateFlow<Song?> = _selectedSong.asStateFlow()

    val _repeatSong = MutableStateFlow<Song?>(null)
    val repeatSong = _repeatSong.asStateFlow()


    fun togglePlaylistSheet(show: Boolean) {
        _showPlayListSheet.value = show
    }

    // This function will now be called from a background thread
    fun refreshLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedLikedSongs = getLikedSongs() // This now runs on IO
            // Update MutableStateFlow on the Main thread to ensure UI observes changes correctly
            withContext(Dispatchers.Main) {
                _likedSongs.value = updatedLikedSongs
            }
        }
    }

    init {
        // Load initial data in a background coroutine when the ViewModel is created
        viewModelScope.launch(Dispatchers.IO) {
            getAllSongs(application.applicationContext)
            // After songs are loaded, refresh liked songs and playlists
            refreshLikedSongs() // This will now internally use Dispatchers.IO
            loadPlaylistsFromDB(songs.value) // This will now internally use Dispatchers.IO
        }

        // Observe PlayerController's playing state to update _isPlaying in ViewModel
        // This ensures the UI state in the ViewModel always matches the actual player
        viewModelScope.launch {
            PlayerController._isPlaying.collect { isPlaying ->
                _isPlaying.value = isPlaying
            }
        }

        // Observe PlayerController's current song to update liked status
        viewModelScope.launch {
            PlayerController._currentSong.collect { song ->
                song?.let {
                    // Update liked status for the new current song
                    viewModelScope.launch(Dispatchers.IO) {
                        val liked = isSongLiked(it)
                        withContext(Dispatchers.Main) {
                            _currentSongLiked.value = liked
                        }
                    }
                } ?: run {
                    // If current song becomes null (e.g., player stopped), set liked to false
                    _currentSongLiked.value = false
                }
            }
        }
    }

    fun likeSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.likeSong(song.data) // Use song.data (URI) for liked songs based on DB schema
            // No need to set _currentSongLiked.value here, it will be updated by the PlayerController._currentSong observer
            refreshLikedSongs() // Refresh liked songs list in background
        }
    }

    fun unlikeSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.unlikeSong(song.data) // Use song.data (URI) for liked songs based on DB schema
            // No need to set _currentSongLiked.value here, it will be updated by the PlayerController._currentSong observer
            refreshLikedSongs() // Refresh liked songs list in background
        }
    }

    // This method will be called from within coroutines, so no need for explicit dispatch
    fun isSongLiked(song: Song?): Boolean {
        // Use song.data (URI) for liked songs based on DB schema
        return song?.let { dbHandler.isSongLiked(it.data) } ?: false
    }

    // This method will be called from within coroutines, so no need for explicit dispatch
    fun getLikedSongs(): List<Song> {
        val likedUris = dbHandler.getLikedSongs().toSet()
        // Filter the main song list based on liked URIs
        return songs.value.filter { likedUris.contains(it.data) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.createPlaylist(name)
            loadPlaylistsFromDB(songs.value) // Reload playlists after creation
        }
    }

    fun deletePlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.deletePlaylist(name)
            loadPlaylistsFromDB(songs.value) // Reload playlists after deletion
        }
    }

    // This method can remain synchronous if it's only called from within a coroutine
    fun getAllPlaylists(): List<String> {
        return dbHandler.getAllPlaylists()
    }

    fun addSongToPlaylist(playlistName: String, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.addSongToPlaylist(playlistName, song.data) // Use song.data (URI)
            // No need to refresh liked songs here, it's unrelated
            loadPlaylistsFromDB(allSongs = songs.value) // Reload playlists after modification
        }
    }

    fun removeSongFromPlaylist(playlistName: String, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.removeSongFromPlaylist(playlistName, song.data) // Use song.data (URI)
            loadPlaylistsFromDB(songs.value) // Reload playlists after modification
        }
    }

    // This method will be called from within coroutines, so no need for explicit dispatch
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

    // This function now runs entirely on Dispatchers.IO due to its call site in init {}
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
                    id = cursor.getLong(idCol).toString(), // Ensure ID is String if used as such in DB
                    title = cursor.getString(titleCol),
                    artist = cursor.getString(artistCol),
                    data = cursor.getString(dataCol)
                )
                songList += song
            }
        }
        // Update MutableStateFlows on the Main thread after data is prepared
        viewModelScope.launch(Dispatchers.Main) {
            _songs.value = songList
            // Set initial current song only if it's not already set (e.g., from a deep link)
            if (_currentSong.value == null && songList.isNotEmpty()) {
                _currentSong.value = songList.first()
            }
            // Liked status will be updated by the observer for _currentSong
            // _currentSongLiked.value = isSongLiked(currentSong.value)

            recommended = songList.take(5)
            mostPopular = songList.takeLast(5)
            mostPlayed = songList.shuffled().take(5)
            // refreshLikedSongs() and loadPlaylistsFromDB are already launched from init{}
        }
    }


    fun playSong(context: Context, song: Song) {
        // Check if the same song is already playing
        if (PlayerController.isPlaying() && PlayerController.playingSong.value?.id == song.id) {
            println("DataViewModel: Song already playing, no need to restart service.")
            return // Do nothing if the same song is already playing
        }

        // If a different song is playing, or nothing is playing, proceed.
        println("DataViewModel: Initiating playback for new song: ${song.title}")

        // Call PlayerController to start playback
        PlayerController.play(context, song)

        // Send intent to PlayerService
        val serviceIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra(PlayerService.EXTRA_SONG, song)
            putParcelableArrayListExtra(PlayerService.EXTRA_SONG_LIST, ArrayList(songs.value))
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // Update UI-related states immediately on the Main thread
        _showPlayer.value = true // Show the player UI
        _currentSong.value = song // Update current song in ViewModel
        // _isPlaying.value = true // This will be updated by the PlayerController._isPlaying observer
        // Liked status will be updated by the PlayerController._currentSong observer
    }

    fun pauseSong(context: Context) {
        if (!PlayerController.isPlaying()) {
            println("DataViewModel: Song already paused, no action needed.")
            return
        }
        PlayerController.pause()
        // _isPlaying.value = false // This will be updated by the PlayerController._isPlaying observer

        val pauseIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PAUSE
        }
        context.startService(pauseIntent) // Use startService for pause, it doesn't need to be foreground-started again
    }

    fun unpauseSong(context: Context) {
        if (PlayerController.isPlaying()) {
            println("DataViewModel: Song already playing, no need to unpause.")
            return
        }
        PlayerController.resume()
        // _isPlaying.value = true // This will be updated by the PlayerController._isPlaying observer
        _showPlayer.value = true // Ensure player UI is visible if unpaused

        // Send a play action to the service to update its notification if necessary
        val playIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY // Use play action to update notification
            putExtra(PlayerService.EXTRA_SONG, _currentSong.value) // Pass current song for notification update
        }
        ContextCompat.startForegroundService(context, playIntent)
    }

    fun playNext(context: Context){
        var index = songs.value.indexOf(currentSong.value)
        if (index == (songs.value.size-1)){
            _currentSong.value = songs.value.first()
            // Liked status will be updated by the PlayerController._currentSong observer
        } else {
            _currentSong.value = songs.value[++index]
            // Liked status will be updated by the PlayerController._currentSong observer
        }
        currentSong.value?.let { it->
            playSong(context,it)
        }
    }

    fun playPrev(context: Context){
        var index = songs.value.indexOf(currentSong.value)
        if (index == 0){
            _currentSong.value = songs.value.last()
            // Liked status will be updated by the PlayerController._currentSong observer
        } else {
            _currentSong.value = songs.value[--index]
            // Liked status will be updated by the PlayerController._currentSong observer
        }
        currentSong.value?.let { it->
            playSong(context,it)
        }
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

    // This function will now be called from a background thread
    fun loadPlaylistsFromDB(allSongs: List<Song>) {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedPlaylists = dbHandler.getAllPlaylists().mapIndexed { index, name ->
                // Ensure cleanUpInvalidSongsInPlaylist is also running on a background thread
                cleanUpInvalidSongsInPlaylist(name, allSongs)
                // getSongsFromPlaylist is also a DB call, so it's good it's in a Dispatchers.IO scope
                val songsInPlaylist = dbHandler.getSongsFromPlaylist(name, allSongs)
                println("$name : ${songsInPlaylist.map { it.title }}") // Log song titles for better debugging
                Playlist(
                    id = index.toLong(),
                    title = name,
                    songs = songsInPlaylist.size,
                    thumbnail = pickThumbnailFor(name, index),
                )
            }
            // Update MutableStateFlow on the Main thread
            withContext(Dispatchers.Main) {
                _playLists.value = loadedPlaylists
            }
        }
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

    // This function also contains DB operations and should be called from a background thread
    fun cleanUpInvalidSongsInPlaylist(playlistName: String, allSongs: List<Song>) {
        val validUris = allSongs.map { it.data }.toSet() // Use song.data (URI)
        val playlistSongUris = dbHandler.getSongsFromPlaylist(playlistName, allSongs = allSongs)
            .map { it.data } // Use song.data (URI)

        playlistSongUris.forEach { uri ->
            if (!validUris.contains(uri)) {
                dbHandler.removeSongFromPlaylist(playlistName, uri) // Use song.data (URI)
                println("Removed stale song URI=$uri from playlist=$playlistName")
            }
        }
    }
}