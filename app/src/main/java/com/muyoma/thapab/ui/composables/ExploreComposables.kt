package com.muyoma.thapab.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.muyoma.thapab.models.Playlist
import com.muyoma.thapab.models.Song


@Composable
fun SectionHeader(title: String ,modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = Color.Gray,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun PlayListSectionHeader(title: String ,add : ()->Unit,modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
    ){
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Gray,
            modifier = modifier.padding(bottom = 8.dp)
        )
        IconButton(
            onClick = {
                add()
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                null
            )
        }
    }
}
@Composable
fun PlayListDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.Black,
        tonalElevation = 8.dp,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist name", color = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(playlistName) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.defaultMinSize(minWidth = 100.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.defaultMinSize(minWidth = 100.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SongCarousel(songs: List<Song>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(songs.size) { index ->
            SongCard(song = songs[index])
        }
    }
}

@Composable
fun AlbumCarousel(songs: List<Song>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(songs.size) { index ->
            AlbumCard(song = songs[index])
        }
    }
}

@Composable
fun MostPlayedCarousel(songs: List<Song>, currentSong: Song?, play: (Song) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(songs.size) { index ->
            MostPlayedCard(
                song = songs[index],
                isPlaying = currentSong == songs[index],
                play = {play(songs[index])}
            )
        }
    }
}

@Composable
fun PlayLister(playlists : List<Playlist>){
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 2 columns
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(80.dp,180.dp),
        contentPadding = PaddingValues(1.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(playlists.size) { item ->
            PlayListCard(playlists[item])
        }
    }
}

@Composable
fun SongCard(song: Song) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { /* TODO: handle click */ },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x2A243838)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = song.coverResId),
                contentDescription = song.title,
                modifier = Modifier
                    .height(140.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = song.title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
@Composable
fun MostPlayedCard(song: Song, isPlaying: Boolean, play: ()->Unit) {
    Card(
        modifier = Modifier
            .size(160.dp),
//            .shadow(1.dp, RoundedCornerShape(12.dp), true, Color.Cyan),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(0.dp,0.dp,0.dp,8.dp)
            ,
        ) {
            Image(
                painter = painterResource(id = song.coverResId),
                contentDescription = song.title,
                modifier = Modifier
                    .size(160.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { play()}
                ,
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row (
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0x6B00010E))
            ){
                Column(
                    modifier = Modifier
                        .padding(5.dp),
                    ){
                    Text(
                        text = song.title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .width(80.dp),
                        maxLines = 2
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    tint = Color.Black,
                    imageVector = if(isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .background(Color(0x9E07F6F6), CircleShape)
                        .padding(4.dp)
                )
            }
        }
    }
}
@Composable
fun AlbumCard(song: Song) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable { /* TODO: handle click */ },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .padding(0.dp,0.dp,0.dp,8.dp)

        ) {
            Image(
                painter = painterResource(id = song.coverResId),
                contentDescription = song.title,
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth()
                    .shadow(20.dp, CircleShape, true, Color.White)
                    .clip(CircleShape)
                ,
                contentScale = ContentScale.Crop
            )
            Row (
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.Center)
            ){
                Icon(
                    tint = Color.LightGray,
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .background(Color(0xBF000000), CircleShape)
                        .padding(10.dp)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = song.artist,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun PlayListCard(data : Playlist){
    Card(
        modifier = Modifier
            .width(160.dp)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        Color.Black,
                        Color.Black,
                        Color.Transparent
                    )
                )
            )
            .clickable { /* TODO: handle click */ },
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(0.dp, 0.dp, 8.dp, 0.dp)
        ) {
            Image(
                painter = painterResource(data.thumbnail),
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(RoundedCornerShape(0.dp))
                    .width(50.dp)
            )
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = data.title,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Songs : ${ data.songs }",
                    fontWeight = FontWeight.ExtraLight,
                    fontSize = 14.sp
                )
            }

        }
    }
}