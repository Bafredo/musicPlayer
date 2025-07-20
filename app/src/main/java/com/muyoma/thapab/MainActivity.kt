package com.muyoma.thapab

import android.Manifest
import android.content.*
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.service.PlayerService
import com.muyoma.thapab.ui.common.*
import com.muyoma.thapab.ui.composables.PlayListDialog
import com.muyoma.thapab.ui.composables.PlaylistBottomSheet
import com.muyoma.thapab.ui.pages.auth.Auth
import com.muyoma.thapab.ui.pages.hidden.PlayListExplorer
import com.muyoma.thapab.ui.pages.hidden.Player
import com.muyoma.thapab.ui.pages.visible.*
import com.muyoma.thapab.ui.theme.AppTheme
import com.muyoma.thapab.viewmodel.AuthViewModel
import com.muyoma.thapab.viewmodel.DataViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    private val PLAYER_ARG_SONG_ID = "songId"
    private lateinit var dataViewModel: DataViewModel
    private lateinit var playerStateReceiver: BroadcastReceiver

    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                val authViewModel: AuthViewModel = viewModel()
                dataViewModel = viewModel()

                val uiState by authViewModel.uiState.collectAsState()
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                var showBars by remember { mutableStateOf(false) }
                var showCreatePlaylist by remember { mutableStateOf(false) }

                val _isPlaying = MutableStateFlow(false)
                val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

                val liked by dataViewModel.currentSongLiked.collectAsState()

                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Black,
                        darkIcons = false
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
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        dataViewModel.getAllSongs(context)
                        dataViewModel._currentSong.value = dataViewModel.songs.value.firstOrNull()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }


                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = "explore",
                                modifier = Modifier.background(MaterialTheme.colorScheme.background)
                            ) {
                                composable("auth") { Auth(authViewModel) }
                                composable("explore") { Explore(dataViewModel, navController) }
                                composable("liked") { Liked(dataViewModel, navController) }
                                composable("local") { Local(dataViewModel, navController) }
                                composable("search") { Search(dataViewModel) }
                                composable("playlist/{$PLAYER_ARG_SONG_ID}",
                                    arguments = listOf(navArgument(PLAYER_ARG_SONG_ID) { type = NavType.StringType })
                                ) { entry ->
                                    val listName = entry.arguments?.getString(PLAYER_ARG_SONG_ID)
                                    val list = listName?.let { dataViewModel.getSongsFromPlaylist(it) }
                                    list?.let {
                                        PlayListExplorer(dataViewModel, navController,it)
                                    }
                                }
                                composable(
                                    "player/{$PLAYER_ARG_SONG_ID}",
                                    arguments = listOf(navArgument(PLAYER_ARG_SONG_ID) { type = NavType.StringType })
                                ) { entry ->
                                    val songId = entry.arguments?.getString(PLAYER_ARG_SONG_ID)
                                    val song = songId?.let { dataViewModel.getSongByTitle(it) }
                                    song?.let {
                                        Player(s = it, dataViewModel = dataViewModel)
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
                                    if (dataViewModel.showPlayer.collectAsState().value) {

                                        val song = if(PlayerController.mediaPlayer == null){
                                            dataViewModel.currentSong.collectAsState().value!!
                                        }else{
                                            PlayerController.currentSong.collectAsState().value!!
                                        }

                                            FloatingMusicTracker(
                                                song = song,
                                                isLiked = liked,
                                                pause = { dataViewModel.pauseSong(context) },
                                                play = { dataViewModel.unpauseSong() },
                                                liked = {
                                                    if (dataViewModel.isSongLiked(it))
                                                        dataViewModel.unlikeSong(it)
                                                    else
                                                        dataViewModel.likeSong(it)
                                                }
                                            )

                                    }

                                    BottomNavigationBar(
                                        modifier = Modifier
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Black,
                                                        Color.Black,
                                                        Color.Transparent
                                                    ),
                                                    startY = Float.POSITIVE_INFINITY,
                                                    endY = 0f
                                                )
                                            )
                                            .padding(horizontal = 18.dp),
                                        navController = navController
                                    )
                                }
                            }
                        }
                        if (dataViewModel.showPlayListSheet.collectAsState().value) {
                            ModalBottomSheet(
                                onDismissRequest = { dataViewModel.togglePlaylistSheet(false) },
                                containerColor = Color(0XBF000000)
                            ) {
                                PlaylistBottomSheet(
                                    playlists = dataViewModel.playlists.collectAsState().value,
                                    onAddPlaylist = {
                                        dataViewModel.togglePlaylistSheet(false)
                                        showCreatePlaylist = true
                                    },
                                    onDismiss = { dataViewModel.togglePlaylistSheet(false) },
                                    onSelect = {
                                        val song = dataViewModel.selectedSong.value
                                        if(song != null) {
                                            dataViewModel.addSongToPlaylist(it, song)
                                            dataViewModel._showPlayListSheet.value = false
                                        }
                                    },
                                )
                            }

                        }
                        if(showCreatePlaylist){
                            PlayListDialog(
                                "Create Playlist",
                                onDismiss = {showCreatePlaylist = false}
                            ) { name ->
                                if(dataViewModel.selectedSong.value != null){
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

        // Broadcast receiver registered outside of setContent
        playerStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    PlayerService.BROADCAST_ACTION_PLAY -> {
                        dataViewModel.unpauseSong()
                    }
                    PlayerService.BROADCAST_ACTION_PAUSE -> {
                        dataViewModel._isPlaying.value = false
                    }
                    PlayerService.BROADCAST_ACTION_NEXT,
                    PlayerService.BROADCAST_ACTION_PREV -> {
                        val song = intent.getParcelableExtra<Song>("song")
                        song?.let {
                            dataViewModel._currentSong.value = it
                            dataViewModel._isPlaying.value = true
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

        registerReceiver(playerStateReceiver, filter)
    }

    override fun onDestroy() {
        unregisterReceiver(playerStateReceiver)
        super.onDestroy()
    }
}
