package com.muyoma.thapab.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Song
import java.nio.file.WatchEvent

@Composable
fun SongListItem(
    song: Song,
    playing: Boolean,
    onMore: () -> Unit,
    changeImage: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp, 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (playing) Color.White else Color(0xAE161717),
                RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { changeImage(song.coverResId) },
                    onLongPress = { onMore() }
                )
            }
            .padding(8.dp, 1.dp)
    ) {
        Image(
            painter = if(song.coverResId != null)rememberAsyncImagePainter(model = song.coverResId)
            else painterResource(R.drawable.bg),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .padding(0.dp, 7.dp)
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(10.dp)
        ) {
            Text(
                text = song.title,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = Color.Gray,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontWeight = FontWeight.Light,
                color = Color.DarkGray,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            tint = Color.DarkGray,
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            modifier = Modifier
                .clickable{
                    onMore()
                }
        )
    }
}
