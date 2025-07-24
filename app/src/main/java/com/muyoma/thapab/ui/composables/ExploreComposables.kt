package com.muyoma.thapab.ui.composables

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Playlist
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import kotlinx.coroutines.flow.asStateFlow


@Composable
fun SectionHeader(title: String ,modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = Color.Gray,
        modifier = modifier.padding(12.dp,4.dp)
    )
}

@Composable
fun PlayListSectionHeader(title: String ,add : ()->Unit,modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp, 1.dp)
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
                imageVector = Icons.Default.MoreVert,
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
fun MostPlayedCarousel(songs: List<Song>, currentSong: Song?, play: (Song,List<Song>) -> Unit) {

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .padding(3.dp,1.dp),

    ) {

        items(songs.size) { index ->
            MostPlayedCard(
                song = songs[index],
                isPlaying = currentSong == songs[index] && PlayerController._isPlaying.collectAsState().value,
                play = {
                    play(songs[index],songs)
                       },
            )
        }
    }
}

@Composable
fun PlayLister(playlists : List<Playlist>,explore : (String)->Unit){
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), // 2 columns
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp, 1.dp)
            .heightIn(115.dp, 180.dp),
        contentPadding = PaddingValues(1.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(playlists.size) { item ->
            PlayListCard(playlists[item]){
                explore(it)
            }
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = if(song.coverResId != null)rememberAsyncImagePainter(model = song.coverResId)
                else painterResource(R.drawable.bg),
                contentDescription = song.title,
                modifier = Modifier
                    .height(140.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = song.title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
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
            .size(150.dp),
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
                painter = rememberAsyncImagePainter(model = song.coverResId),
                contentDescription = song.title,
                modifier = Modifier
                    .size(150.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { play() }
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
                    imageVector = if(isPlaying ) Icons.Default.Pause else Icons.Default.PlayArrow,
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
                painter = if(song.coverResId != null)rememberAsyncImagePainter(model = song.coverResId)
                else painterResource(R.drawable.bg),
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
fun PlayListCard(data : Playlist,explore : (String)->Unit){

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier

                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x79000000))
                .padding(10.dp)
                .clickable {
                    explore(data.title)
                },
        ) {
            Image(
                painter = painterResource(data.thumbnail),
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .width(100.dp)
                    .aspectRatio(1f)
            )
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = data.title,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 10.sp,
                    color = Color.Gray
                )

            }

        }

}


@Composable
fun MadeForYouHeader(title: String ,message : String,modifier: Modifier = Modifier) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp, 1.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.bg2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(5.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column{
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Gray,
                modifier = modifier
            )
            Text(
                text = message,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray,
                modifier = Modifier
            )
        }
    }
}
@Composable
fun MadeForYouCard(){
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .padding(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E1E1E))
            .padding(14.dp)
    ) {
        Row {
            Image(
                painter = painterResource(R.drawable.bg4),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(7.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(7.dp, 1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ){

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp)
                        )
                        Text(
                            "Grind Hard",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = null,
                        tint = Color.Gray
                    )

                }

                Column (
                    modifier = Modifier
                        .padding(8.dp,1.dp)
                ){
                    Text(
                        text = "Songs : 20 ",
                        color = Color.Gray
                    )
                    Text(
                        text = "J Cole, Imagine ",
                        color = Color.LightGray
                    )
                }
            }

        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp, 1.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ){
                CircularProgressIndicator(
                    trackColor = Color.DarkGray,
                    color = Color(0x9E07F6F6),
                    modifier = Modifier
                        .size(20.dp)
                )
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.Speaker,
                        null
                    )
                }
            }
            IconButton(
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(65.dp)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color(0x9E07F6F6))

                )
            }
        }
    }
}