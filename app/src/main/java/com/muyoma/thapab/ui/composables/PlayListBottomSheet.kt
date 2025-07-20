package com.muyoma.thapab.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyoma.thapab.models.Playlist

@Composable
fun PlaylistBottomSheet(
    playlists: List<Playlist>,
    onAddPlaylist: () -> Unit,
    onDismiss: () -> Unit,
    onSelect : (String)->Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
    ) {
        Column {
            Text("Your Playlists", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            playlists.forEach { playlist ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                        .clickable{
                            onSelect(playlist.title)
                        }
                        .padding(12.dp)
                ) {
                    Text(playlist.title, color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.LightGray)
                    .clickable { onAddPlaylist() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Add Playlist", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
