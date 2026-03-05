package com.pink.musictools.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pink.musictools.data.model.PlaybackState
import com.pink.musictools.data.model.RepeatMode

@Composable
fun PlayerControls(
    playbackState: PlaybackState,
    repeatMode: RepeatMode,
    shuffleMode: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Secondary: shuffle / repeat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpringIconButton(onClick = onShuffleClick) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "随机播放",
                    tint = if (shuffleMode) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SpringIconButton(onClick = onRepeatClick) {
                Icon(
                    imageVector = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "重复模式",
                    tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary: prev / play-pause / next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpringIconButton(onClick = onPreviousClick, size = 52.dp) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "上一首",
                    modifier = Modifier.size(30.dp)
                )
            }

            // Play / Pause — use interactionSource so onClick is always reliable
            val playSource = remember { MutableInteractionSource() }
            val playPressed by playSource.collectIsPressedAsState()
            val playScale by animateFloatAsState(
                targetValue = if (playPressed) 0.86f else 1f,
                animationSpec = spring(dampingRatio = 0.35f, stiffness = 800f),
                label = "playScale"
            )

            FilledIconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(72.dp).scale(playScale),
                enabled = playbackState !is PlaybackState.Loading,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                interactionSource = playSource
            ) {
                if (playbackState is PlaybackState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (playbackState is PlaybackState.Playing)
                            Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState is PlaybackState.Playing) "暂停" else "播放",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            SpringIconButton(onClick = onNextClick, size = 52.dp) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "下一首",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun SpringIconButton(
    onClick: () -> Unit,
    size: Dp = 44.dp,
    content: @Composable () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.78f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 800f),
        label = "btnScale"
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size).scale(scale),
        interactionSource = source
    ) {
        content()
    }
}
