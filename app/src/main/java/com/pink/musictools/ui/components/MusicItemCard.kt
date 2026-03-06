package com.pink.musictools.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pink.musictools.data.model.MusicItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicItemCard(
    musicItem: MusicItem,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    isCurrentlyPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "itemScale"
    )

    // Left playing-bar width: animates in/out
    val barWidth by animateDpAsState(
        targetValue = if (isCurrentlyPlaying) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "playBar"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(
                interactionSource = interactionSource,
                indication        = LocalIndication.current,
                onClick           = onClick,
                onLongClick       = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress?.invoke()
                }
            )
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Playing indicator bar ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(52.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)
                )
        )

        Spacer(modifier = Modifier.width(if (isCurrentlyPlaying) 12.dp else 16.dp))

        // ── Album art ─────────────────────────────────────────────────────────
        Box(modifier = Modifier.size(46.dp)) {
            if (musicItem.albumArtUri != null) {
                AsyncImage(
                    model              = musicItem.albumArtUri,
                    contentDescription = "专辑封面",
                    modifier           = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            // Playing overlay on art
            if (isCurrentlyPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)),
                    contentAlignment = Alignment.Center
                ) { PlayingBars() }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Text ──────────────────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text      = musicItem.title,
                style     = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentlyPlaying) FontWeight.SemiBold else FontWeight.Normal,
                color     = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = buildString {
                    append(musicItem.artist)
                    if (musicItem.duration > 0) append("  ·  ${formatDuration(musicItem.duration)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── More button ───────────────────────────────────────────────────────
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "更多",
                    modifier = Modifier.size(18.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        }
    }
}

@Composable
private fun PlayingBars() {
    val transition = rememberInfiniteTransition(label = "bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.7f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "b3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(20.dp)
    ) {
        listOf(bar1, bar2, bar3).forEach { frac ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(frac)
                    .background(
                        MaterialTheme.colorScheme.onPrimary,
                        RoundedCornerShape(1.5.dp)
                    )
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
