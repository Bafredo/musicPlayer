package com.muyoma.thapab.ui.pages.hidden

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.muyoma.thapab.audio.AudioOutputRoute
import com.muyoma.thapab.audio.AudioRouteController
import com.muyoma.thapab.models.Song
import com.muyoma.thapab.service.PlayerController
import com.muyoma.thapab.service.RepeatMode
import com.muyoma.thapab.ui.common.MusicProgressTracker
import com.muyoma.thapab.viewmodel.DataViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Player(
    s: Song,
    dataViewModel: DataViewModel,
    navController: NavController,
    imageModifier: Modifier,
    textModifier: Modifier,
    frameModifier: Modifier
) {
    val context = LocalContext.current
    val currentSong by dataViewModel.currentSong.collectAsState()
    val isLiked by dataViewModel.currentSongLiked.collectAsState()
    val repeatMode by dataViewModel.repeatMode.collectAsState()
    val shuffleEnabled by dataViewModel.shuffleEnabled.collectAsState()
    val isPlaying by dataViewModel.isPlaying.collectAsState()
    val displaySong = currentSong ?: s

    LaunchedEffect(s.id) {
        val alreadyActive = currentSong?.id == s.id || PlayerController.playingSong.value?.id == s.id
        if (!alreadyActive) {
            dataViewModel.playSong(context, s)
        }
    }

    var dragOffset by remember { mutableStateOf(0f) }
    var showOutputsSheet by remember { mutableStateOf(false) }
    var availableOutputs by remember { mutableStateOf(emptyList<AudioOutputRoute>()) }
    val swipeThreshold = 150f
    val velocityThreshold = 400f

    val glowPulse = rememberInfiniteTransition(label = "artworkPulse").animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = AnimationRepeatMode.Reverse
        ),
        label = "artworkScale"
    )

    val artworkScale = if (isPlaying) glowPulse.value else 1f
    val controlTint by animateColorAsState(
        targetValue = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "shuffleTint"
    )
    val repeatTint by animateColorAsState(
        targetValue = if (repeatMode == RepeatMode.OFF) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "repeatTint"
    )

    val titleText = remember(displaySong.id) { displaySong.title.uppercase() }
    val artistText = remember(displaySong.id) { displaySong.artist.uppercase() }
    val artworkModel = remember(displaySong.id) { displaySong.albumArtUri ?: displaySong.coverResId }
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.94f),
            MaterialTheme.colorScheme.background
        )
    )
    val panelGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f)
        )
    )

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = frameModifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close Player",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { navController.popBackStack() }
            )
            Text(
                text = if (shuffleEnabled) "MIXING THINGS UP" else "PLAYING FROM LIBRARY",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = controlTint,
                modifier = Modifier.clickable { dataViewModel.toggleShuffle() }
            )
        }

        Card(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(18.dp)
        ) {
            Box {
                AsyncImage(
                    model = artworkModel,
                    contentDescription = "Album Art for ${displaySong.title}",
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = artworkScale
                            scaleY = artworkScale
                            rotationZ = dragOffset / 24f
                            translationX = dragOffset
                        }
                        .pointerInput(displaySong.id) {
                            var startTime = 0L
                            var totalDragX = 0f

                            detectDragGestures(
                                onDragStart = {
                                    startTime = System.currentTimeMillis()
                                    totalDragX = 0f
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    val duration = System.currentTimeMillis() - startTime
                                    val velocity = if (duration > 0) totalDragX / (duration / 1000f) else 0f
                                    val isValidSwipe = abs(totalDragX) > swipeThreshold || abs(velocity) > velocityThreshold

                                    if (isValidSwipe) {
                                        if (totalDragX > 0) {
                                            dataViewModel.playPrev(context)
                                        } else {
                                            dataViewModel.playNext(context)
                                        }
                                    }

                                    dragOffset = 0f
                                }
                            ) { _, dragAmount ->
                                totalDragX += dragAmount.x
                                dragOffset = (dragOffset + dragAmount.x).coerceIn(-220f, 220f)
                            }
                        }
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = abs(dragOffset) > 24f,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 20.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (dragOffset > 0f) "Swipe right for previous" else "Swipe left for next",
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(panelGradient)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        RepeatMode.ALL -> Icons.Default.Repeat
                        RepeatMode.OFF -> Icons.Default.ArrowForward
                    },
                    contentDescription = "Repeat mode",
                    tint = repeatTint,
                    modifier = Modifier.clickable { dataViewModel.cycleRepeatMode() }
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = titleText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = textModifier.fillMaxWidth(0.92f)
                    )
                    Text(
                        text = artistText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                }

                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like Song",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable {
                        if (isLiked) dataViewModel.unlikeSong(displaySong) else dataViewModel.likeSong(displaySong)
                    }
                )
            }

            MusicProgressTracker(
                play = { dataViewModel.unpauseSong(context) },
                pause = { dataViewModel.pauseSong(context) },
                duration = dataViewModel.getDuration(),
                next = { dataViewModel.playNext(context) },
                prev = { dataViewModel.playPrev(context) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerGlassAction(
                    selected = showOutputsSheet,
                    onClick = {
                        availableOutputs = AudioRouteController.availableOutputs(context)
                        showOutputsSheet = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Headset,
                        contentDescription = "Output devices",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                PlayerGlassAction(
                    selected = shuffleEnabled,
                    onClick = { dataViewModel.toggleShuffle() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Randomize queue",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                PlayerGlassAction(
                    selected = false,
                    onClick = { dataViewModel.togglePlaylistSheet(true) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add to playlist",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showOutputsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOutputsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Choose Output",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Pick where playback should go right now.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                availableOutputs.forEach { route ->
                    val backgroundColor = if (route.isActive) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .shadow(0.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .background(backgroundColor)
                            .clickable {
                                AudioRouteController.routeTo(context, route.id)
                                availableOutputs = AudioRouteController.availableOutputs(context)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = route.name,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (route.isActive) {
                            Text(
                                text = "Active",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(bottom = 18.dp))
            }
        }
    }
}

@Composable
private fun PlayerGlassAction(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
