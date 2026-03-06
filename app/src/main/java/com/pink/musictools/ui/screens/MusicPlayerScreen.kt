package com.pink.musictools.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pink.musictools.data.model.PlaybackProgress
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import com.pink.musictools.data.model.PlaybackState
import com.pink.musictools.data.model.RepeatMode
import com.pink.musictools.domain.LyricsFormatter
import com.pink.musictools.ui.components.PlayerControls
import com.pink.musictools.ui.components.ProgressSlider
import com.pink.musictools.viewmodel.MusicPlayerViewModel
import kotlin.math.abs

@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val currentMusic by viewModel.currentMusic.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()

    val isPlaying = playbackState is PlaybackState.Playing

    // Vinyl rotation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(18000, easing = LinearEasing), AnimRepeatMode.Restart
        ),
        label = "rotation"
    )
    var frozenRotation by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) { if (!isPlaying) frozenRotation = rotation }
    val displayRotation = if (isPlaying) rotation else frozenRotation

    val albumScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.86f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "albumScale"
    )

    val context = LocalContext.current

    // Lyrics state
    val parsedLyrics = remember(currentMusic?.lyrics) {
        currentMusic?.lyrics?.let { LyricsFormatter.parseTimed(it) }.orEmpty()
    }
    val currentLyricIndex = run {
        val pos = progress.currentPosition
        if (parsedLyrics.isEmpty()) 0
        else {
            var result = 0
            for (i in parsedLyrics.indices) {
                if (parsedLyrics[i].first <= pos) result = i else break
            }
            result
        }
    }
    val lyricsListState = rememberLazyListState()
    LaunchedEffect(currentLyricIndex) {
        if (parsedLyrics.isNotEmpty()) {
            lyricsListState.animateScrollToItem(
                index = currentLyricIndex.coerceIn(0, parsedLyrics.lastIndex),
                scrollOffset = -300
            )
        }
    }

    var showFullLyrics by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize().haze(hazeState)) {
        // GPU-blurred background
        if (currentMusic?.albumArtUri != null) {
            val blurRequest = remember(currentMusic?.albumArtUri) {
                ImageRequest.Builder(context)
                    .data(currentMusic?.albumArtUri)
                    .size(256)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = blurRequest,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.6f
                        scaleY = 1.6f
                        alpha = 0.60f
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = BlurEffect(60f, 60f, TileMode.Clamp)
                        }
                    },
                contentScale = ContentScale.Crop
            )
        }
        val bg = MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to bg.copy(alpha = 0.50f),
                        0.35f to bg.copy(alpha = 0.68f),
                        1.0f to bg.copy(alpha = 0.93f)
                    )
                )
        )

        // Main content — switches between normal player and full-screen lyrics
        AnimatedContent(
            targetState = showFullLyrics,
            transitionSpec = {
                if (targetState) {
                    slideInVertically(tween(340)) { it / 2 } + fadeIn(tween(300)) togetherWith
                            fadeOut(tween(200))
                } else {
                    fadeIn(tween(300)) togetherWith
                            slideOutVertically(tween(300)) { it / 2 } + fadeOut(tween(200))
                }
            },
            label = "playerMode"
        ) { fullScreen ->
            if (fullScreen) {
                FullLyricsView(
                    parsedLyrics = parsedLyrics,
                    currentIndex = currentLyricIndex,
                    isPlaying = isPlaying,
                    progress = progress,
                    listState = lyricsListState,
                    trackTitle = currentMusic?.title.orEmpty(),
                    onSeekTo = { viewModel.onSeekTo(it) },
                    onTogglePlay = { viewModel.onPlayPauseClicked() },
                    onDismiss = { showFullLyrics = false },
                    modifier = modifier
                )
            } else {
                // Normal player layout
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(40.dp))

                    // Vinyl album art
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .graphicsLayer { scaleX = albumScale; scaleY = albumScale },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .rotate(displayRotation)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        if (currentMusic?.albumArtUri != null) {
                            AsyncImage(
                                model = currentMusic?.albumArtUri,
                                contentDescription = "专辑封面",
                                modifier = Modifier
                                    .size(200.dp)
                                    .rotate(displayRotation)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .rotate(displayRotation)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MusicNote, null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Track info
                    AnimatedContent(
                        targetState = currentMusic?.id,
                        transitionSpec = {
                            fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 } togetherWith
                                    fadeOut(tween(200))
                        },
                        label = "trackInfo"
                    ) { _ ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (currentMusic != null) {
                                Text(
                                    text = currentMusic!!.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentMusic!!.artist,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    "从音乐库选择一首歌",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Waveform
                    AnimatedVisibility(
                        visible = isPlaying,
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(200))
                    ) {
                        WaveformBars(modifier = Modifier.height(24.dp).fillMaxWidth())
                    }
                    if (!isPlaying) Spacer(modifier = Modifier.height(24.dp))

                    // Compact lyrics preview — center of screen, tap to expand
                    if (parsedLyrics.isNotEmpty()) {
                        CompactLyricsPreview(
                            parsedLyrics = parsedLyrics,
                            currentIndex = currentLyricIndex,
                            onClick = { showFullLyrics = true },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Progress + Controls wrapped in frosted glass panel
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .hazeChild(
                                state = hazeState,
                                shape = RoundedCornerShape(24.dp),
                                style = HazeMaterials.regular(MaterialTheme.colorScheme.surface)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                            ProgressSlider(
                                progress = progress,
                                onSeek = { viewModel.onSeekTo(it) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            PlayerControls(
                                playbackState = playbackState,
                                repeatMode = repeatMode,
                                shuffleMode = shuffleMode,
                                onPlayPauseClick = { viewModel.onPlayPauseClicked() },
                                onPreviousClick = { viewModel.onPreviousClicked() },
                                onNextClick = { viewModel.onNextClicked() },
                                onRepeatClick = {
                                    val nextMode = when (repeatMode) {
                                        RepeatMode.OFF -> RepeatMode.ALL
                                        RepeatMode.ALL -> RepeatMode.ONE
                                        RepeatMode.ONE -> RepeatMode.OFF
                                    }
                                    viewModel.onRepeatModeChanged(nextMode)
                                },
                                onShuffleClick = { viewModel.onShuffleModeChanged(!shuffleMode) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Playback error
                    AnimatedVisibility(
                        visible = playbackState is PlaybackState.Error,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = (playbackState as? PlaybackState.Error)?.message.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Compact lyrics preview (normal player view) ──────────────────────────────

@Composable
private fun CompactLyricsPreview(
    parsedLyrics: List<Pair<Long, String>>,
    currentIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            (-2..2).forEach { offset ->
                val idx = currentIndex + offset
                val text = parsedLyrics.getOrNull(idx)?.second.orEmpty()
                val distance = abs(offset)
                val targetAlpha = when (distance) {
                    0 -> 1f
                    1 -> 0.38f
                    else -> 0.14f
                }
                val alpha by animateFloatAsState(targetAlpha, tween(300), label = "ca$offset")
                if (text.isNotBlank() || distance == 0) {
                    Text(
                        text = text,
                        color = if (distance == 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                        fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (distance == 0) 16.sp else 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp,
                                vertical = if (distance == 0) 8.dp else 3.dp
                            )
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            // Subtle expand hint
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(2.dp)
                    .background(
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        CircleShape
                    )
            )
        }
    }
}

// ── Full-screen lyrics view ───────────────────────────────────────────────────

@Composable
private fun FullLyricsView(
    parsedLyrics: List<Pair<Long, String>>,
    currentIndex: Int,
    isPlaying: Boolean,
    progress: PlaybackProgress,
    listState: androidx.compose.foundation.lazy.LazyListState,
    trackTitle: String,
    onSeekTo: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header — tap to collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown, null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Full lyrics list — tap a line to seek + collapse
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(parsedLyrics) { index, (timestamp, text) ->
                val distance = abs(index - currentIndex)
                val targetAlpha = when {
                    distance == 0 -> 1f
                    distance <= 2 -> 0.50f
                    distance <= 5 -> 0.26f
                    else -> 0.10f
                }
                val alpha by animateFloatAsState(targetAlpha, tween(300), label = "fl$index")
                Text(
                    text = text,
                    color = if (distance == 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                    fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (distance == 0) 18.sp else 15.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSeekTo(timestamp)
                            onDismiss()
                        }
                        .padding(
                            horizontal = 32.dp,
                            vertical = if (distance == 0) 10.dp else 7.dp
                        )
                )
            }
        }

        // Mini player at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProgressSlider(
                progress = progress,
                onSeek = onSeekTo,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ── Waveform bars ─────────────────────────────────────────────────────────────

@Composable
private fun WaveformBars(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val barCount = 18
    val heights = (0 until barCount).map { i ->
        transition.animateFloat(
            initialValue = 0.2f + (i % 3) * 0.15f,
            targetValue = 0.6f + (i % 4) * 0.1f,
            animationSpec = infiniteRepeatable(
                tween(400 + i * 55, easing = LinearEasing),
                AnimRepeatMode.Reverse
            ),
            label = "bar$i"
        )
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { heightFrac ->
            val h by heightFrac
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(h)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        RoundedCornerShape(1.5.dp)
                    )
            )
        }
    }
}
