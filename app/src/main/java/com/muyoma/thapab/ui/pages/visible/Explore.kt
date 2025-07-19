package com.muyoma.thapab.ui.pages.visible

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Playlist
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.ui.composables.AlbumCarousel
import com.muyoma.thapab.ui.composables.MostPlayedCarousel
import com.muyoma.thapab.ui.composables.PlayLister
import com.muyoma.thapab.ui.composables.SectionHeader
import com.muyoma.thapab.ui.composables.SongCarousel
import com.muyoma.thapab.viewmodel.DataViewModel


@Composable
fun Explore(dataViewModel: DataViewModel,navController: NavController) {
    val samplePlaylists = dataViewModel.samplePlaylists
    val mostPlayed = dataViewModel.mostPlayed
    val mostPopular = dataViewModel.mostPopular
    val recommended = dataViewModel.recommended

    val gradientBackground = Brush.verticalGradient(
        listOf(Color(0xFF0F0F0F),Color(0xFF0F0F0F), Color.Black)
    )

    LazyColumn(
        modifier = Modifier
            .background(gradientBackground)
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item{
            Spacer(
                modifier = Modifier
                    .height(20.dp)
            )
        }
        item {
            SectionHeader("Your Playlists")
            PlayLister(samplePlaylists)
        }
        item {
            SectionHeader("Most Played")
            MostPlayedCarousel(mostPlayed)
        }

        item {
            SectionHeader("Artists")
            AlbumCarousel(mostPopular)
        }

        item {
            SectionHeader("Must try")
            SongCarousel(recommended)
        }
        item {
            Spacer(
                modifier = Modifier
                    .height(30.dp)
            )
        }
    }
}
