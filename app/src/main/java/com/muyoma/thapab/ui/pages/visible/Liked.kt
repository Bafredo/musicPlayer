package com.muyoma.thapab.ui.pages.visible

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.muyoma.thapab.R
import com.muyoma.thapab.ui.composables.SongListItem
import com.muyoma.thapab.viewmodel.DataViewModel

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun Liked(dataViewModel: DataViewModel,navController: NavController) {
    var displayImage by remember { mutableIntStateOf(0) }
    val songs = dataViewModel.songs.collectAsState().value
    val nowPlaying = dataViewModel.currentSong.collectAsState()
    val isPlaying = dataViewModel.isPlaying.collectAsState()
    val context= LocalContext.current
    fun toggleMedia(){
        if (isPlaying.value){
            dataViewModel.pauseSong()
        }else  {
            nowPlaying.value?.let { dataViewModel.playSong(context,it) }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f),
                painter = painterResource(if(displayImage == 0) R.drawable.bg else displayImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item {
                    Spacer(modifier = Modifier.height(58.dp))
                }
                items(songs) { song ->
                    SongListItem(song,if(nowPlaying.value != null) nowPlaying.value?.title == song.title else false){it->
                        if (dataViewModel.currentSong.value?.title != song.title) {
                            dataViewModel.playSong(context, song)
                        } else if(!isPlaying.value){
                            dataViewModel.playSong(context, song)
                        }

                        navController.navigate("player/${song.title}")
                    }
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .height(70.dp)
                    )
                }
            }
        }

        // Gradient overlay at the breakpoint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp) // Adjust for stronger or softer gradient
                .align(Alignment.TopCenter)
                .offset(y = (0.3f * LocalConfiguration.current.screenHeightDp).dp - 24.dp) // Center the 48dp overlay
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black,
                            Color.Black.copy(alpha = 0.8f),
                            Color.Transparent
                        ),
                        startY = Float.POSITIVE_INFINITY,
                        endY = 0f
                    )
                )
        ){
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(25.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null
                )

                Icon(
                    imageVector = if(isPlaying.value) { Icons.Default.Pause
                    } else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(55.dp)
                        .padding(4.dp)
                        .background(Color(0x9E07F6F6), CircleShape)
                        .padding(8.dp)
                        .clickable {
                            toggleMedia()
                        }
                )
            }
        }
        Row (
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
//            .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.09f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Black,
                            Color.Transparent
                        ),

                        )
                )
                .padding(17.dp, 4.dp)
        ){

        }
    }
}
