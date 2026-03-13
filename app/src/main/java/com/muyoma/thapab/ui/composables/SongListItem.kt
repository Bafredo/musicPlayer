package com.muyoma.thapab.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.muyoma.thapab.R
import com.muyoma.thapab.models.Song

@Composable
fun SongListItem(
    song: Song,
    playing: Boolean,
    onMore: () -> Unit,
    onItemClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(song.id, song.albumArtUri) {
        ImageRequest.Builder(context)
            .data(song.albumArtUri ?: song.coverResId)
            .placeholder(R.drawable.music)
            .error(R.drawable.music)
            .crossfade(false)
            .size(120)
            .build()
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp, 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .clickable { onItemClick() }
            .padding(8.dp, 1.dp)
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = "Album Art for ${song.title}",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .padding(vertical = 7.dp)
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            modifier = Modifier.clickable { onMore() }
        )
    }
}
