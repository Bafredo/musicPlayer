package com.muyoma.thapab

import android.Manifest
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.ui.common.BottomNavigationBar
import com.muyoma.thapab.ui.common.FloatingMusicTracker
import com.muyoma.thapab.ui.common.TopBar
import com.muyoma.thapab.ui.pages.auth.Auth
import com.muyoma.thapab.ui.pages.hidden.Player
import com.muyoma.thapab.ui.pages.visible.Explore
import com.muyoma.thapab.ui.pages.visible.Liked
import com.muyoma.thapab.ui.pages.visible.Local
import com.muyoma.thapab.ui.pages.visible.Search
import com.muyoma.thapab.ui.theme.AppTheme
import com.muyoma.thapab.viewmodel.AuthViewModel
import com.muyoma.thapab.viewmodel.DataViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
    private val PLAYER_ARG_SONG_ID = "songId"

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {
            AppTheme {
                val viewModel: AuthViewModel = viewModel()
                val dataViewModel : DataViewModel = viewModel()

                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                var showBars by remember { mutableStateOf(false) }


                val _isPlaying = MutableStateFlow<Boolean>(false)
                val isPlaying : StateFlow<Boolean> = _isPlaying.asStateFlow()


                // System UI styling
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


                // Auto-navigation when login/signup is successful
                LaunchedEffect(uiState.isSuccess) {
                    if (uiState.isSuccess) {
                        navController.navigate("explore") {
                            popUpTo("auth") { inclusive = true } // clear back stack
                        }
                        viewModel.clearMessages()
                    }
                }

                // Permission request
                val context = LocalContext.current

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        dataViewModel.getAllSongs(context)
                        dataViewModel._currentSong.value = dataViewModel.songs.value[0]
                        println(dataViewModel.songs.value[0])
                    }
                }

                LaunchedEffect(Unit) {
                    // Only launch permission request once
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
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ){

                            NavHost(
                                navController = navController,
                                startDestination = "explore",
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                composable("auth") {
                                    Auth(viewModel)
                                }
                                composable("explore") {
                                    Explore(dataViewModel,navController)
                                }
                                composable("liked") {
                                    Liked(
                                        dataViewModel, navController
                                    )
                                }
                                composable("local") {
                                    Local(dataViewModel,navController)
                                }
                                composable("search") {
                                    Search(dataViewModel)
                                }

                                composable("player/{$PLAYER_ARG_SONG_ID}",arguments = listOf(
                                    navArgument(PLAYER_ARG_SONG_ID) { type = NavType.StringType }
                                )) {backStackEntry ->
                                    val songId = backStackEntry.arguments?.getString(PLAYER_ARG_SONG_ID)
                                    val song = songId?.let { dataViewModel.getSongByTitle(it) }

                                    if (song != null) {
                                        Player(
                                            s = song,
                                            dataViewModel = dataViewModel
                                        )
                                    }
                                }
                            }
                            if(showBars){
                                if(currentRoute == "explore" || currentRoute == "local") {
                                    TopBar()
                                }
                            }
                            if(showBars){
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                ) {
                                    if(dataViewModel.showPlayer.collectAsState().value){
                                        val song = dataViewModel.currentSong.collectAsState().value!!
                                        FloatingMusicTracker(
                                            song = song,
                                            pause = {dataViewModel.pauseSong()},
                                            play = {dataViewModel.unpauseSong()}
                                            )
                                    }
                                    BottomNavigationBar(
                                        modifier = Modifier

                                            .padding(horizontal = 0.dp, vertical = 0.dp)
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
                                            .shadow(
                                                elevation = 8.dp,
                                                shape = RoundedCornerShape(24.dp)
                                            )
                                            .padding(horizontal = 18.dp, vertical = 0.dp),
                                        navController = navController

                                    )
                                }
                                
                            }
                        }
                }
                }
            }
        }
    }
}
