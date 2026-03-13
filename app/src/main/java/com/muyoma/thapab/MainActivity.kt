package com.muyoma.thapab

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.ArcMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerService
import com.muyoma.thapab.ui.common.*
import com.muyoma.thapab.ui.composables.PlayListDialog
import com.muyoma.thapab.ui.composables.PlaylistBottomSheet
import com.muyoma.thapab.ui.pages.auth.Auth
import com.muyoma.thapab.ui.pages.hidden.PlayListExplorer
import com.muyoma.thapab.ui.pages.hidden.Player
import com.muyoma.thapab.ui.pages.hidden.SongListExplorer
import com.muyoma.thapab.ui.pages.visible.*
import com.muyoma.thapab.ui.theme.AppTheme
import com.muyoma.thapab.ui.theme.ThemeMode
import com.muyoma.thapab.util.AppIntents
import com.muyoma.thapab.viewmodel.AuthViewModel
import com.muyoma.thapab.viewmodel.DataViewModel
import com.muyoma.thapab.viewmodel.ThemeViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainActivity : ComponentActivity() {

    private val PLAYER_ARG_SONG_ID = "songId"
    private lateinit var dataViewModel: DataViewModel
    private lateinit var playerStateReceiver: BroadcastReceiver

    // NEW: SharedFlow to expose onNewIntent calls to Composables
    // extraBufferCapacity = 1 ensures that if an intent comes in before
    // the collector is ready, it's not immediately dropped.
    private val _newIntentFlow = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val newIntentFlow = _newIntentFlow.asSharedFlow()
    private lateinit var downloadReceiver: BroadcastReceiver


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (::dataViewModel.isInitialized) {
                        dataViewModel.handleDownloadCompleted(context, downloadId)
                        Toast.makeText(context, "Download complete", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        registerReceiverCompat(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))


        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themeMode by themeViewModel.themeMode.collectAsState()

            AppTheme(themeMode = themeMode) {
                val authViewModel: AuthViewModel = viewModel()
                dataViewModel = viewModel()

                val uiState by authViewModel.uiState.collectAsState()
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                var showBars by remember { mutableStateOf(false) }
                var showCreatePlaylist by remember { mutableStateOf(false) }

                val liked by dataViewModel.currentSongLiked.collectAsState()
                val showPlayer by dataViewModel.showPlayer.collectAsState()
                val librarySongs by dataViewModel.songs.collectAsState()

                val systemUiController = rememberSystemUiController()
                val systemBarColor = MaterialTheme.colorScheme.surface
                val useDarkIcons = themeMode == ThemeMode.LIGHT
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = systemBarColor,
                        darkIcons = useDarkIcons
                    )
                }

                LaunchedEffect(currentRoute) {
                    showBars = when {
                        currentRoute == "auth" -> false
                        currentRoute?.startsWith("player") == true -> false
                        else -> true
                    }
                }

                LaunchedEffect(uiState.isSuccess) {
                    if (uiState.isSuccess) {
                        navController.navigate("explore") {
                            popUpTo("auth") { inclusive = true }
                        }
                        authViewModel.clearMessages()
                    }
                }

                val context = LocalContext.current
                lateinit var aniScope : AnimatedVisibilityScope
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        dataViewModel.getAllSongs(context)
                    } else {
                        // Handle permission denied: show a message, disable features, etc.
                    }
                }

                LaunchedEffect(Unit) {
                    if (dataViewModel.songs.value.isEmpty()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    } else {
                        dataViewModel.ensureSongsLoaded()
                    }
                }

                // --- START: Handle Intent from Notification ---
                val activity = LocalContext.current as MainActivity
                var pendingIntent by remember { mutableStateOf(activity.intent) }

                fun navigateTo(route: String) {
                    navController.navigate(route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                fun processAppIntent(intent: Intent) {
                    val destination = intent.getStringExtra(AppIntents.EXTRA_DESTINATION)
                    val autoplay = intent.getBooleanExtra(AppIntents.EXTRA_AUTOPLAY, false)
                    when {
                        intent.action == "com.muyoma.thapab.OPEN_LIKED" -> {
                            dataViewModel.playLikedSongs(context)
                            navigateTo("liked")
                        }

                        intent.action == "com.muyoma.thapab.OPEN_LOCAL" -> navigateTo("local")

                        intent.action == Intent.ACTION_VIEW && intent.data != null -> {
                            val uri: Uri = intent.data ?: return
                            dataViewModel.openAudioUri(
                                context,
                                uri,
                                autoPlay = if (intent.hasExtra(AppIntents.EXTRA_AUTOPLAY)) autoplay else true
                            )
                            navigateTo("player")
                        }

                        destination == AppIntents.DESTINATION_LIKED -> {
                            if (autoplay) {
                                dataViewModel.playLikedSongs(context)
                            }
                            navigateTo("liked")
                        }

                        destination == AppIntents.DESTINATION_PLAYLIST -> {
                            val playlistName = intent.getStringExtra(AppIntents.EXTRA_PLAYLIST_NAME) ?: return
                            if (autoplay) {
                                dataViewModel.playPlaylist(context, playlistName)
                            }
                            dataViewModel._selectedPlaylist.value = playlistName
                            navigateTo("playlist/$playlistName")
                        }

                        destination == AppIntents.DESTINATION_LOCAL -> navigateTo("local")
                        intent.getStringExtra(AppIntents.EXTRA_SONG_TITLE_TO_PLAY) != null -> navigateTo("player")
                    }
                }

                LaunchedEffect(Unit) {
                    activity.newIntentFlow.collect { intent ->
                        pendingIntent = intent
                    }
                }

                LaunchedEffect(pendingIntent, librarySongs.size) {
                    val intent = pendingIntent ?: return@LaunchedEffect
                    val needsLibrary = intent.getStringExtra(AppIntents.EXTRA_DESTINATION) == AppIntents.DESTINATION_PLAYLIST ||
                        intent.getStringExtra(AppIntents.EXTRA_DESTINATION) == AppIntents.DESTINATION_LIKED ||
                        intent.getStringExtra(AppIntents.EXTRA_SONG_TITLE_TO_PLAY) != null
                    if (needsLibrary && librarySongs.isEmpty()) return@LaunchedEffect

                    processAppIntent(intent)
                    pendingIntent = null
                    intent.removeExtra(AppIntents.EXTRA_SONG_TITLE_TO_PLAY)
                    intent.removeExtra(AppIntents.EXTRA_DESTINATION)
                    intent.removeExtra(AppIntents.EXTRA_PLAYLIST_NAME)
                }

                val textBoundsTransform = BoundsTransform { initialBounds, targetBounds ->
                    keyframes {
                        durationMillis = 550
                        initialBounds at 0 using ArcMode.ArcBelow using FastOutSlowInEasing
                        targetBounds at 550
                    }
                }


                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        SharedTransitionLayout {
                            Box(modifier = Modifier.fillMaxSize()) {
                                NavHost(
                                    navController = navController,
                                    startDestination = "explore",
                                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                                ) {
//                                composable("auth") { Auth(authViewModel) }
                                    composable("explore") { Explore(dataViewModel, navController) }
                                    composable("liked") { Liked(dataViewModel, navController) }
                                    composable("local") { Local(dataViewModel, navController) }
                                    composable("search") { Search(dataViewModel) }
                                    composable("player") {
                                        val song by dataViewModel.currentSong.collectAsState()
                                        song?.let {
                                            Player(
                                                s = it,
                                                dataViewModel = dataViewModel,
                                                navController = navController,
                                                imageModifier = Modifier,
                                                textModifier = Modifier,
                                                frameModifier = Modifier
                                            )
                                        }
                                    }
                                    composable(
                                        "playlist/{$PLAYER_ARG_SONG_ID}",
                                        arguments = listOf(navArgument(PLAYER_ARG_SONG_ID) {
                                            type = NavType.StringType
                                        })
                                    ) { entry ->
                                        val listName =
                                            entry.arguments?.getString(PLAYER_ARG_SONG_ID)
                                        val list =
                                            listName?.let { dataViewModel.getSongsFromPlaylist(it) }
                                        list?.let {
                                            PlayListExplorer(dataViewModel, navController, it)
                                        }
                                    }
                                    composable(
                                        "songlist/{$PLAYER_ARG_SONG_ID}",
                                        arguments = listOf(navArgument(PLAYER_ARG_SONG_ID) {
                                            type = NavType.StringType
                                        })
                                    ) { entry ->
                                        val artistName =
                                            entry.arguments?.getString(PLAYER_ARG_SONG_ID)
                                        val list =
                                            artistName?.let { dataViewModel.getSongsByArtist(it) }
                                        list?.let {
                                            SongListExplorer(dataViewModel, navController, it)
                                        }
                                    }
                                    composable(
                                        "player/{$PLAYER_ARG_SONG_ID}",
                                        arguments = listOf(navArgument(PLAYER_ARG_SONG_ID) {
                                            type = NavType.StringType
                                        })
                                    ) { entry ->
                                        val songTitle =
                                            entry.arguments?.getString(PLAYER_ARG_SONG_ID)
                                        val song =
                                            songTitle?.let { dataViewModel.getSongByTitle(it) }
                                        song?.let {
                                            Player(
                                                s = it,
                                                dataViewModel = dataViewModel,
                                                navController = navController,
                                                imageModifier = Modifier.sharedElement(
                                                    sharedContentState = rememberSharedContentState(key = "song_${it.id}"),
                                                    animatedVisibilityScope = this // 👈 fix here
                                                ),
                                                textModifier = Modifier.sharedElement(
                                                    sharedContentState = rememberSharedContentState(key = "text_${it.id}"),
                                                    animatedVisibilityScope = this ,
                                                    boundsTransform = textBoundsTransform

                                                ),
                                                frameModifier = Modifier.sharedBounds(
                                                    sharedContentState = rememberSharedContentState(key = "player_${it.id}"),
                                                    animatedVisibilityScope = this,
                                                    boundsTransform = textBoundsTransform

                                                )
                                            )
                                        }
                                    }
                                }


                                if (showBars) {
                                    if (currentRoute == "explore" || currentRoute == "local") {
                                        TopBar()
                                    }
                                }

                                if (showBars) {
                                    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                                        AnimatedVisibility(
                                            visible = showPlayer,
                                            enter = fadeIn(tween(220)) + slideInVertically(tween(320)) { it / 2 } + scaleIn(tween(280), initialScale = 0.94f),
                                            exit = fadeOut(tween(180)) + slideOutVertically(tween(240)) { it / 2 } + scaleOut(tween(200), targetScale = 0.94f)
                                        ) {
                                            val currentPlayingSong by dataViewModel.currentSong.collectAsState()
                                            currentPlayingSong?.let { song ->
                                                FloatingMusicTracker(
                                                    song = song,
                                                    isLiked = liked,
                                                    pause = { dataViewModel.pauseSong(context) },
                                                    play = { dataViewModel.unpauseSong(context) },
                                                    liked = {
                                                        if (dataViewModel.isSongLiked(it))
                                                            dataViewModel.unlikeSong(it)
                                                        else
                                                            dataViewModel.likeSong(it)
                                                    },
                                                    clicked = {
                                                        navController.navigate("player/${it.title}") {
                                                            launchSingleTop = true
                                                        }
                                                    },
                                                    imagemodifier = Modifier.sharedElement(
                                                        sharedContentState = rememberSharedContentState("song_${song.id}"),
                                                        animatedVisibilityScope = this,

                                                    ),
                                                    textmodifier = Modifier.sharedElement(
                                                        sharedContentState = rememberSharedContentState("text_${song.id}"),
                                                        animatedVisibilityScope = this,
                                                        boundsTransform = textBoundsTransform

                                                    ),
                                                    frameModifier = Modifier.sharedBounds(
                                                        sharedContentState = rememberSharedContentState(key = "player_${song.id}"),
                                                        animatedVisibilityScope = this,
                                                        boundsTransform = textBoundsTransform
                                                    )
                                                )
                                            }
                                        }

                                        BottomNavigationBar(
                                            modifier = Modifier
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.surface,
                                                            MaterialTheme.colorScheme.surface,
                                                            Color.Transparent
                                                        ),
                                                        startY = Float.POSITIVE_INFINITY,
                                                        endY = 0f
                                                    )
                                                )
                                                .padding(horizontal = 18.dp),
                                            navController = navController,
                                            themeMode = themeMode,
                                            onThemeToggle = themeViewModel::cycleThemeMode
                                        )
                                    }
                                }


                                if (dataViewModel.showPlayListSheet.collectAsState().value) {
                                    ModalBottomSheet(
                                        onDismissRequest = { dataViewModel.togglePlaylistSheet(false) },
                                        containerColor = Color(0XBF000000)
                                    ) {
                                        PlaylistBottomSheet(
                                            playlists = dataViewModel.getAllPlaylists(),
                                            onAddPlaylist = {
                                                dataViewModel.togglePlaylistSheet(false)
                                                showCreatePlaylist = true
                                            },
                                            onDismiss = { dataViewModel.togglePlaylistSheet(false) },
                                            onSelect = {
                                                val song = dataViewModel.selectedSong.value
                                                if (song != null) {
                                                    dataViewModel.addSongToPlaylist(it, song)
                                                    dataViewModel._showPlayListSheet.value = false
                                                }
                                            },
                                        )
                                    }

                                }
                                if (showCreatePlaylist) {
                                    PlayListDialog(
                                        "Create Playlist",
                                        onDismiss = { showCreatePlaylist = false }
                                    ) { name ->
                                        if (dataViewModel.selectedSong.value != null) {
                                            dataViewModel.createPlaylist(name)
                                            dataViewModel.addSongToPlaylist(
                                                name,
                                                dataViewModel.selectedSong.value!!
                                            )
                                        }
                                        showCreatePlaylist = false
                                        dataViewModel._showPlayListSheet.value = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        playerStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    PlayerService.BROADCAST_ACTION_PLAY -> {
                        dataViewModel.unpauseSong(applicationContext)
                    }
                    PlayerService.BROADCAST_ACTION_PAUSE -> {
                        // Handled by direct observation of PlayerController._isPlaying if DataViewModel is set up for it
                    }
                    PlayerService.BROADCAST_ACTION_NEXT,
                    PlayerService.BROADCAST_ACTION_PREV -> {
                        val song = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra("song", Song::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra<Song>("song")
                        }
                        song?.let {
                            dataViewModel._currentSong.value = it
                            // _isPlaying updated by PlayerController directly
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(PlayerService.BROADCAST_ACTION_PLAY)
            addAction(PlayerService.BROADCAST_ACTION_PAUSE)
            addAction(PlayerService.BROADCAST_ACTION_NEXT)
            addAction(PlayerService.BROADCAST_ACTION_PREV)
        }

        registerReceiverCompat(playerStateReceiver, filter)
    }

    // This is the actual Android Activity lifecycle method to override
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Set the activity's current intent to the new one
        setIntent(intent)
        // Emit the new intent to the SharedFlow for Composables to observe
        intent?.let { _newIntentFlow.tryEmit(it) } // Use tryEmit for non-suspending context
    }


    override fun onDestroy() {
        unregisterReceiver(downloadReceiver)
        unregisterReceiver(playerStateReceiver)
        super.onDestroy()
    }

    private fun registerReceiverCompat(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }
}
