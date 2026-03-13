package com.muyoma.thapab.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.DownloadManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.database.getStringOrNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.muyoma.thapab.R
import com.muyoma.thapab.data.DBHandler
import com.muyoma.thapab.data.MusicLibraryRepository
import com.muyoma.thapab.models.Playlist
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.service.PlayerService
import com.muyoma.thapab.service.RepeatMode
import com.muyoma.thapab.util.ShortcutHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.emptyList
import android.util.Log

import com.muyoma.thapab.network.ApiService
import com.muyoma.thapab.network.ApiServiceImpl
import com.muyoma.thapab.network.models.YoutubeVideo

class DataViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHandler: DBHandler = DBHandler.getInstance(application.applicationContext)
    private val apiService: ApiService = ApiServiceImpl()
    private val pendingDownloads = mutableMapOf<Long, String>()
    private var hasLoadedSongs = false

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

    val repeatMode = PlayerController.repeatMode
    val shuffleEnabled = PlayerController.shuffleEnabled

    var _currentList = MutableStateFlow<List<Song>>(songs.value)
    val currentList: StateFlow<List<Song>> = _currentList.asStateFlow()

    // --- Ktor related StateFlows ---
    private val _youtubeSearchResults = MutableStateFlow<List<YoutubeVideo>>(emptyList())
    val youtubeSearchResults = _youtubeSearchResults.asStateFlow()

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus = _downloadStatus.asStateFlow()

    private val _apiErrorMessage = MutableStateFlow<String?>(null)
    val apiErrorMessage = _apiErrorMessage.asStateFlow()
    private val _isLibraryLoading = MutableStateFlow(false)
    val isLibraryLoading = _isLibraryLoading.asStateFlow()

    private val _isOnlineSearchLoading = MutableStateFlow(false)
    val isOnlineSearchLoading = _isOnlineSearchLoading.asStateFlow()

    private val _activeDownloadTitle = MutableStateFlow<String?>(null)
    val activeDownloadTitle = _activeDownloadTitle.asStateFlow()


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
        ensureSongsLoaded()

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

    fun cycleRepeatMode(): RepeatMode {
        return PlayerController.cycleRepeatMode()
    }

    fun setRepeatMode(mode: RepeatMode) {
        PlayerController.setRepeatMode(mode)
    }

    fun toggleShuffle(): Boolean {
        return PlayerController.toggleShuffle()
    }

    fun ensureSongsLoaded(forceRefresh: Boolean = false) {
        val context = getApplication<Application>().applicationContext
        if (hasAudioPermission(context)) {
            getAllSongs(context, forceRefresh)
        }
    }

    private fun hasAudioPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("Recycle")
    fun getAllSongs(context: Context, forceRefresh: Boolean = false) {
        if (_isLibraryLoading.value) return
        if (hasLoadedSongs && !forceRefresh && songs.value.isNotEmpty()) return
        if (!hasAudioPermission(context)) return

        _isLibraryLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
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

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        )

                        val song = Song(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            data = contentUri.toString(),
                            albumArtUri = buildAlbumArtUri(albumId)
                        )
                        songList += song
                    }
                }

                withContext(Dispatchers.Main) {
                    _songs.value = songList
                    hasLoadedSongs = songList.isNotEmpty()
                    if (_currentSong.value == null && songList.isNotEmpty()) {
                        _currentSong.value = songList.first()
                    }
                    recommended = songList.take(5)
                    mostPopular = songList.takeLast(5)
                    mostPlayed = songList.shuffled().take(5)
                }

                refreshLikedSongs()
                loadPlaylistsFromDB(songList)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLibraryLoading.value = false
                }
            }
        }
    }

    private fun buildAlbumArtUri(albumId: Long): Uri? {
        return if (albumId > 0) {
            ContentUris.withAppendedId(ALBUM_ART_CONTENT_URI, albumId)
        } else {
            null
        }
    }



    fun playSong(context: Context, song: Song, playlist: List<Song> = songs.value) {
        _currentList.value = if (playlist.isNotEmpty()) playlist else listOf(song)
        PlayerController.setQueue(_currentList.value, song.id)
        if (PlayerController.isPlaying() && PlayerController.playingSong.value?.id == song.id) {
            Log.d("DataViewModel", "Song already playing, no need to restart service.")
            return
        }

        Log.d("DataViewModel", "Initiating playback for new song: ${song.title} from data: ${song.data}")

        _showPlayer.value = true
        _currentSong.value = song

        val serviceIntent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra(PlayerService.EXTRA_SONG, song)
            putParcelableArrayListExtra(PlayerService.EXTRA_SONG_LIST, ArrayList(_currentList.value))
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
        if (currentList.value.isEmpty()) return
        val intent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_NEXT
            putParcelableArrayListExtra(PlayerService.EXTRA_SONG_LIST, ArrayList(currentList.value))
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun playPrev(context: Context) {
        if (currentList.value.isEmpty()) return
        val intent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PREV
            putParcelableArrayListExtra(PlayerService.EXTRA_SONG_LIST, ArrayList(currentList.value))
        }
        ContextCompat.startForegroundService(context, intent)
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
            ShortcutHelper.updateDynamicShortcuts(
                getApplication<Application>().applicationContext,
                loadedPlaylists.map { it.title }
            )
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

    fun getSongById(songId: String): Song? {
        return songs.value.find { it.id == songId }
    }

    fun getSongsByArtist(title : String) : List<Song>{
        return songs.value.filter { it.artist == title }
    }

    fun playLikedSongs(context: Context) {
        val liked = getLikedSongs()
        if (liked.isNotEmpty()) {
            playSong(context, liked.first(), liked)
        }
    }

    fun playPlaylist(context: Context, playlistName: String) {
        val playlistSongs = getSongsFromPlaylist(playlistName)
        if (playlistSongs.isNotEmpty()) {
            _selectedPlaylist.value = playlistName
            playSong(context, playlistSongs.first(), playlistSongs)
        }
    }

    fun openAudioUri(context: Context, uri: Uri, autoPlay: Boolean = true) {
        val externalSong = MusicLibraryRepository.createSongFromUri(context, uri)
        _currentList.value = listOf(externalSong)
        _currentSong.value = externalSong
        _showPlayer.value = true
        if (autoPlay) {
            playSong(context, externalSong, listOf(externalSong))
        }
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
        _apiErrorMessage.value = null
        _youtubeSearchResults.value = emptyList()
        _isOnlineSearchLoading.value = true
        viewModelScope.launch {
            try {
                val youtubeVideo = apiService.searchYoutubeVideo(query)
                _youtubeSearchResults.value = listOf(youtubeVideo)
                Log.d("DataViewModel", "Youtube successful: ${youtubeVideo.title}")
            } catch (e: Exception) {
                Log.e("DataViewModel", "Error searching YouTube: ${e.message}", e)
                _apiErrorMessage.value = "Search failed: ${e.message}"
            } finally {
                _isOnlineSearchLoading.value = false
            }
        }
    }

    /**
     * Initiates the download of a YouTube video on the backend server.
     * Updates _downloadStatus and _apiErrorMessage.
     * Note: This only starts the download on the server; actual completion needs further handling.
     */
    fun downloadYoutubeSong(youtubeVideo: YoutubeVideo) {
        _apiErrorMessage.value = null
        _downloadStatus.value = "Starting download on server..."
        viewModelScope.launch {
            try {
                val response = apiService.downloadAudio(youtubeVideo.videoId, youtubeVideo.title)
                _downloadStatus.value = response.message
                Log.d("DataViewModel", "Download initiated for ${youtubeVideo.title}: ${response.message}, file: ${response.file}")
                val streamableSong = Song(
                    id = "stream_${youtubeVideo.videoId}",
                    title = youtubeVideo.title,
                    artist = "YouTube",
                    data = apiService.getStreamUrl(response.file),
                    coverResId = R.drawable.bg
                )

                Log.d("DataViewModel", "Attempting to stream from: ${streamableSong.data}")
                playSong(getApplication<Application>().applicationContext, streamableSong, listOf(streamableSong))
            } catch (e: Exception) {
                Log.e("DataViewModel", "Error during download initiation: ${e.message}", e)
                _downloadStatus.value = "Download initiation failed!"
                _apiErrorMessage.value = "Download failed: ${e.message}"
            }
        }
    }

    fun downloadYoutubeSongToDevice(context: Context, youtubeVideo: YoutubeVideo) {
        _apiErrorMessage.value = null
        _activeDownloadTitle.value = youtubeVideo.title
        _downloadStatus.value = "Preparing ${youtubeVideo.title} for download..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.downloadAudio(youtubeVideo.videoId, youtubeVideo.title)
                val fileName = sanitizeFileName(youtubeVideo.title, youtubeVideo.videoId)
                val request = DownloadManager.Request(Uri.parse(apiService.getStreamUrl(response.file))).apply {
                    setTitle(youtubeVideo.title)
                    setDescription("Saving to Music")
                    setMimeType("audio/mpeg")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, fileName)
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }

                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = downloadManager.enqueue(request)
                pendingDownloads[downloadId] = youtubeVideo.title

                withContext(Dispatchers.Main) {
                    _downloadStatus.value = "Downloading ${youtubeVideo.title}..."
                }
            } catch (e: Exception) {
                Log.e("DataViewModel", "Error queueing device download: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _activeDownloadTitle.value = null
                    _downloadStatus.value = null
                    _apiErrorMessage.value = "Unable to download ${youtubeVideo.title}: ${e.message}"
                }
            }
        }
    }

    fun downloadMp3ToPhone(context: Context, videoId: String, title: String) {
        downloadYoutubeSongToDevice(context, YoutubeVideo(title = title, videoId = videoId, url = ""))
    }

    fun handleDownloadCompleted(context: Context, downloadId: Long) {
        val requestedTitle = pendingDownloads.remove(downloadId) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)

            var localUriString: String? = null
            var successful = false
            downloadManager.query(query)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    successful = statusIndex >= 0 &&
                        cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL
                    localUriString = if (localUriIndex >= 0) {
                        cursor.getStringOrNull(localUriIndex)
                    } else {
                        null
                    }
                }
            }

            val scannedPath = localUriString?.let { Uri.parse(it).path }
            if (successful && scannedPath != null) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(scannedPath),
                    arrayOf("audio/mpeg"),
                    null
                )
                getAllSongs(context, forceRefresh = true)
                withContext(Dispatchers.Main) {
                    _downloadStatus.value = "$requestedTitle saved to Music"
                    _activeDownloadTitle.value = null
                }
            } else {
                withContext(Dispatchers.Main) {
                    _downloadStatus.value = null
                    _activeDownloadTitle.value = null
                    _apiErrorMessage.value = "Download failed for $requestedTitle"
                }
            }
        }
    }

    fun clearYoutubeSearchResults() {
        _youtubeSearchResults.value = emptyList()
        _apiErrorMessage.value = null
        if (_activeDownloadTitle.value == null) {
            _downloadStatus.value = null
        }
    }

    private fun sanitizeFileName(title: String, videoId: String): String {
        val sanitizedTitle = title.replace(Regex("[<>:\"/\\\\|?*\\x00-\\x1F]"), "").take(80)
        return "${sanitizedTitle}_$videoId.mp3"
    }

    private companion object {
        val ALBUM_ART_CONTENT_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
