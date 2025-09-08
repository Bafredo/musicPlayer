package com.muyoma.thapab.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.app.DownloadManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri // Import Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.muyoma.thapab.R
import com.muyoma.thapab.data.DBHandler
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
import android.util.Log // Import for Android's Log

// Ktor Client imports
import com.muyoma.thapab.network.ApiService
import com.muyoma.thapab.network.ApiServiceImpl
import com.muyoma.thapab.network.models.YoutubeVideo // Import your Ktor models
import java.net.URLEncoder
import androidx.core.net.toUri

class DataViewModel(application: Application) : AndroidViewModel(application) {

    // Use the singleton instance of DBHandler
    private val dbHandler: DBHandler = DBHandler.getInstance(application.applicationContext)

    // Ktor API Service instance
    private val apiService: ApiService = ApiServiceImpl() // Initialize your API service

    var _songs = MutableStateFlow<List<Song>>(emptyList())
    var songs: StateFlow<List<Song>> = _songs.asStateFlow()

    val _currentSong = PlayerController._currentSong
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    val _isPlaying = MutableStateFlow<Boolean>(PlayerController.isPlaying())
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val _currentSongLiked = MutableStateFlow<Boolean>(false)
    val currentSongLiked = _currentSongLiked.asStateFlow()
    val _showPlayer = MutableStateFlow<Boolean>(false)
    val showPlayer: StateFlow<Boolean> = _showPlayer.asStateFlow()
    var _playLists = MutableStateFlow<List<Playlist>>(emptyList())
    var playlists = _playLists.asStateFlow()
    val _openPlayListOptions = MutableStateFlow<Boolean>(false)
    val openPlayListOptions : StateFlow<Boolean> = _openPlayListOptions.asStateFlow()

    var _showPlayListSheet = MutableStateFlow<Boolean>(false)
    val showPlayListSheet = _showPlayListSheet.asStateFlow()
    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs = _likedSongs.asStateFlow()

    var _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong: StateFlow<Song?> = _selectedSong.asStateFlow()

    var _selectedPlaylist = MutableStateFlow<String?>(null)
    val selectedPlaylist: StateFlow<String?> = _selectedPlaylist.asStateFlow()

    val _repeatSong = MutableStateFlow<Boolean>(false)
    val repeatSong = _repeatSong.asStateFlow()

    var _currentList = MutableStateFlow<List<Song>>(songs.value)
    val currentList: StateFlow<List<Song>> = _currentList.asStateFlow()

    // --- Ktor related StateFlows ---
    private val _youtubeSearchResults = MutableStateFlow<List<YoutubeVideo>>(emptyList())
    val youtubeSearchResults = _youtubeSearchResults.asStateFlow()

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus = _downloadStatus.asStateFlow()

    private val _apiErrorMessage = MutableStateFlow<String?>(null)
    val apiErrorMessage = _apiErrorMessage.asStateFlow()
    // ---------------------------------


    fun togglePlaylistSheet(show: Boolean) {
        _showPlayListSheet.value = show
    }

    fun refreshLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedLikedSongs = getLikedSongs()
            withContext(Dispatchers.Main) {
                _likedSongs.value = updatedLikedSongs
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getAllSongs(application.applicationContext)
            refreshLikedSongs()
            loadPlaylistsFromDB(songs.value)
        }

        viewModelScope.launch {
            PlayerController._isPlaying.collect { isPlaying ->
                _isPlaying.value = isPlaying
            }
        }

        viewModelScope.launch {
            PlayerController._currentSong.collect { song ->
                song?.let {
                    viewModelScope.launch(Dispatchers.IO) {
                        val liked = isSongLiked(it)
                        withContext(Dispatchers.Main) {
                            _currentSongLiked.value = liked
                        }
                    }
                } ?: run {
                    _currentSongLiked.value = false
                }
            }
        }
    }

    fun likeSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.likeSong(song.data)
            _currentSongLiked.value = true
            refreshLikedSongs()
        }
    }

    fun unlikeSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.unlikeSong(song.data)
            _currentSongLiked.value = false
            refreshLikedSongs()
        }
    }

    fun isSongLiked(song: Song?): Boolean {
        return song?.let { dbHandler.isSongLiked(it.data) } ?: false
    }

    fun getLikedSongs(): List<Song> {
        val likedUris = dbHandler.getLikedSongs().toSet()
        return songs.value.filter { likedUris.contains(it.data) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.createPlaylist(name)
            loadPlaylistsFromDB(songs.value)
        }
    }

    fun deletePlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.deletePlaylist(name)
            loadPlaylistsFromDB(songs.value)
        }
    }

    fun getAllPlaylists(): List<String> {
        return dbHandler.getAllPlaylists()
    }

    fun addSongToPlaylist(playlistName: String, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.addSongToPlaylist(playlistName, song.data)
            loadPlaylistsFromDB(allSongs = songs.value)
        }
    }

    fun removeSongFromPlaylist(playlistName: String = selectedPlaylist.value!!, song: Song = selectedSong.value!!) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.removeSongFromPlaylist(playlistName, song.data)
            loadPlaylistsFromDB(songs.value)
        }
    }

    fun getSongsFromPlaylist(playlistName: String): List<Song> {
        return dbHandler.getSongsFromPlaylist(
            playlistName,
            allSongs = songs.value
        )
    }

    fun repeatSong(s: Song) {
        _repeatSong.value = true
    }

    fun undoRepeat() {
        _repeatSong.value = false
    }

    fun toggleRepeat() {
        _repeatSong.value = !_repeatSong.value
    }

    @SuppressLint("Recycle")
    fun getAllSongs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
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
                MediaStore.Audio.Media.ALBUM_ID
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
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown"
                    val artist = cursor.getString(artistCol) ?: "Unknown"
                    val albumId = cursor.getLong(albumIdCol)

                    // ✅ Build proper content:// URI
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )

                    // ✅ Resolve album art (with fallback)
                    val albumArtUri = resolveAlbumArt(context, contentUri, albumId)

                    val song = Song(
                        id = id.toString(),
                        title = title,
                        artist = artist,
                        data = contentUri.toString(), // store content:// URI instead of file path
                        albumArtUri = albumArtUri
                    )
                    songList += song
                }
            }

            withContext(Dispatchers.Main) {
                _songs.value = songList
                if (_currentSong.value == null && songList.isNotEmpty()) {
                    _currentSong.value = songList.first()
                }
                recommended = songList.take(5)
                mostPopular = songList.takeLast(5)
                mostPlayed = songList.shuffled().take(5)
            }
        }
    }


    fun resolveAlbumArt(context: Context, songUri: Uri, albumId: Long): Uri? {
        // MediaStore lookup
        context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Albums.ALBUM_ART),
            "${MediaStore.Audio.Albums._ID}=?",
            arrayOf(albumId.toString()),
            null
        )?.use { cursor ->
            val artCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART)
            if (cursor.moveToFirst()) {
                cursor.getString(artCol)?.let { path ->
                    return Uri.parse("file://$path")
                }
            }
        }

        // Fallback: Embedded art
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, songUri)
            retriever.embeddedPicture?.let { bytes ->
                val file = java.io.File(context.cacheDir, "${songUri.lastPathSegment}.jpg")
                java.io.FileOutputStream(file).use { out -> out.write(bytes) }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Log.w("DataViewModel", "No embedded album art for $songUri", e)
            null
        } finally {
            retriever.release()
        }
    }



    // MODIFIED: playSong to handle local files or streaming from your backend
    fun playSong(context: Context, song: Song, playlist: List<Song> = songs.value) {
        // If the same song is already playing and it's a local file, do nothing
        if (PlayerController.isPlaying() && PlayerController.playingSong.value?.id == song.id) {
            Log.d("DataViewModel", "Song already playing, no need to restart service.")
            return
        }

        Log.d("DataViewModel", "Initiating playback for new song: ${song.title} from data: ${song.data}")

        // Determine if the song is a local file or a remote URL (from your server)
        val isLocalFile = try {
            val uri = Uri.parse(song.data)
            // Check if it's a file URI or content URI, indicating a local file
            uri.scheme == "file" || uri.scheme == "content"
        } catch (e: Exception) {
            // If parsing fails, assume it's not a standard local file URI, might be a raw path or external URL
            Log.e("DataViewModel", "Error parsing song.data as URI: ${song.data}", e)
            false
        }

        // Call PlayerController to start playback
        // PlayerController.play handles the MediaPlayer lifecycle
        PlayerController.play(context, song)

        // Update UI-related states immediately on the Main thread
        _currentList.value = playlist
        _showPlayer.value = true
        _currentSong.value = song

        // Start the PlayerService (even if local, to manage notification and background playback)
        val serviceIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra(PlayerService.EXTRA_SONG, song)
            putParcelableArrayListExtra(PlayerService.EXTRA_SONG_LIST, ArrayList(playlist))
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    fun pauseSong(context: Context) {
        if (!PlayerController.isPlaying()) {
            Log.d("DataViewModel", "Song already paused, no action needed.")
            return
        }
        PlayerController.pause()
        val pauseIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PAUSE
        }
        context.startService(pauseIntent)
    }

    fun unpauseSong(context: Context) {
        if (PlayerController.isPlaying()) {
            Log.d("DataViewModel", "Song already playing, no need to unpause.")
            return
        }
        PlayerController.resume()
        _showPlayer.value = true

        val playIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra(PlayerService.EXTRA_SONG, _currentSong.value)
        }
        ContextCompat.startForegroundService(context, playIntent)
    }

    fun playNext(context: Context) {
        var index = currentList.value.indexOf(currentSong.value)
        if (index == (currentList.value.size - 1)) {
            _currentSong.value = currentList.value.first()
        } else {
            _currentSong.value = currentList.value[++index]
        }
        currentSong.value?.let { it ->
            playSong(context, it, currentList.value)
        }
    }

    fun playPrev(context: Context) {
        var index = currentList.value.indexOf(currentSong.value)
        if (index == 0) {
            _currentSong.value = currentList.value.last()
        } else {
            _currentSong.value = currentList.value[--index]
        }
        currentSong.value?.let { it ->
            playSong(context, it, currentList.value)
        }
    }


    fun stopSong() {
        PlayerController.stop()
    }

    fun getDuration(): Float {
        return PlayerController.mediaPlayer?.duration?.toFloat() ?: 3.00f
    }

    var recommended by mutableStateOf(emptyList<Song>())
        private set

    var mostPopular by mutableStateOf(emptyList<Song>())
        private set

    var mostPlayed by mutableStateOf(emptyList<Song>())
        private set

    fun loadPlaylistsFromDB(allSongs: List<Song>) {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedPlaylists = dbHandler.getAllPlaylists().mapIndexed { index, name ->
                cleanUpInvalidSongsInPlaylist(name, allSongs)
                val songsInPlaylist = dbHandler.getSongsFromPlaylist(name, allSongs)
                Log.d("DataViewModel", "$name : ${songsInPlaylist.map { it.title }}") // Use Android's Log
                Playlist(
                    id = index.toLong(),
                    title = name,
                    songs = songsInPlaylist.size,
                    thumbnail = pickThumbnailFor(name, index),
                )
            }
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
    fun getSongsByArtist(title : String) : List<Song>{
        return songs.value.filter { it.artist == title }
    }

    fun cleanUpInvalidSongsInPlaylist(playlistName: String, allSongs: List<Song>) {
        val validUris = allSongs.map { it.data }.toSet()
        val playlistSongUris = dbHandler.getSongsFromPlaylist(playlistName, allSongs = allSongs)
            .map { it.data }

        playlistSongUris.forEach { uri ->
            if (!validUris.contains(uri)) {
                dbHandler.removeSongFromPlaylist(playlistName, uri)
                Log.i("DataViewModel", "Removed stale song URI=$uri from playlist=$playlistName") // Use Android's Log
            }
        }
    }

    // --- New Ktor Client Functions ---

    /**
     * Searches for a song on YouTube using the backend API.
     * Updates _youtubeSearchResults and _apiErrorMessage.
     */
    fun searchSongOnYouTube(query: String) {
        _apiErrorMessage.value = null // Clear previous errors
        _youtubeSearchResults.value = emptyList() // Clear previous results
        viewModelScope.launch {
            try {
                val youtubeVideo = apiService.searchYoutubeVideo(query)
                _youtubeSearchResults.value = listOf(youtubeVideo) // Assuming your backend returns max 1 result
                Log.d("DataViewModel", "Youtube successful: ${youtubeVideo.title}")
            } catch (e: Exception) {
                Log.e("DataViewModel", "Error searching YouTube: ${e.message}", e)
                _apiErrorMessage.value = "Search failed: ${e.message}"
            }
        }
    }

    /**
     * Initiates the download of a YouTube video on the backend server.
     * Updates _downloadStatus and _apiErrorMessage.
     * Note: This only starts the download on the server; actual completion needs further handling.
     */
    fun downloadYoutubeSong(youtubeVideo: YoutubeVideo) {
        _apiErrorMessage.value = null // Clear previous errors
        _downloadStatus.value = "Starting download on server..."
        viewModelScope.launch {
            try {
                val response = apiService.downloadAudio(youtubeVideo.videoId, youtubeVideo.title)
                _downloadStatus.value = response.message // e.g., "Download started"
                Log.d("DataViewModel", "Download initiated for ${youtubeVideo.title}: ${response.message}, file: ${response.file}")

                // IMPORTANT: At this point, the file is only being downloaded ON YOUR SERVER.
                // To play it on the app, you have two main options:
                // 1. Stream it from your server using the /stream endpoint.
                // 2. Download the MP3 file from your server to the Android device's local storage.

                // Option 1: Stream the song immediately after download is initiated on server
                // This assumes your PlayerController can handle network URLs.
                // You'd need a Song object representing the streamable file.
                // Example: Create a temporary Song object for streaming
                val streamableSong = Song(
                    id = "stream_${youtubeVideo.videoId}", // Unique ID for streamed song
                    title = youtubeVideo.title,
                    artist = "YouTube",
                    data = apiService.getStreamUrl(response.file), // This is the URL for streaming
                    coverResId = R.drawable.bg // Placeholder
                )

                // You might want to ask the user if they want to stream or download
                // For now, let's assume immediate streaming
                Log.d("DataViewModel", "Attempting to stream from: ${streamableSong.data}")
                playSong(getApplication<Application>().applicationContext, streamableSong, listOf(streamableSong))


                // Option 2 (If you want to download to device):
                // You would typically kick off an Android DownloadManager request here
                // to pull the file from your /stream endpoint to local storage.
                // After local download completes, then add to your local songs.
                // This is more complex and out of scope for just integrating Ktor calls.

            } catch (e: Exception) {
                Log.e("DataViewModel", "Error during download initiation: ${e.message}", e)
                _downloadStatus.value = "Download initiation failed!"
                _apiErrorMessage.value = "Download failed: ${e.message}"
            }
        }
    }
    // ---------------------------------

    fun downloadMp3ToPhone(context: Context, videoId: String, title: String) {
        val sanitizedTitle = title.replace(Regex("[<>:\"/\\\\|?*\\x00-\\x1F]"), "").take(80)
        val fileName = "${sanitizedTitle}_$videoId.mp3"
        val url = "http://muyoma.site/download-file?videoId=$videoId&title=${URLEncoder.encode(title, "UTF-8")}"

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Downloading $title")
            setDescription("Saving to Music folder")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        getAllSongs(context)
    }

    fun clearYoutubeSearchResults() {
        _youtubeSearchResults.value = emptyList()
        _apiErrorMessage.value = null
        _downloadStatus.value = null
    }
}