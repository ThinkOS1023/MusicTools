package com.pink.musictools.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

// Nav bar height constant — used externally to set Scaffold padding
val NavBarHeightDp = 72.dp

private data class NavDestination(
    val icon: ImageVector,
    val label: String
)

@Composable
fun <T> FloatingNavBar(
    items: List<Triple<T, ImageVector, String>>,
    selected: T,
    hazeState: HazeState,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val navShape = RoundedCornerShape(36.dp)
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // ── Frosted glass pill background ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NavBarHeightDp)
                .clip(navShape)
                // haze blur of screen content behind the pill
                .hazeChild(
                    state = hazeState,
                    shape = navShape,
                    style = HazeStyle(
                        blurRadius = 28.dp,
                        tint = surface.copy(alpha = 0.52f)
                    )
                )
                // subtle glass rim border
                .border(
                    width = 0.8.dp,
                    color = outline.copy(alpha = 0.28f),
                    shape = navShape
                )
        )

        // ── Nav items row ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NavBarHeightDp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (screen, icon, label) ->
                NavItem(
                    icon = icon,
                    label = label,
                    selected = selected == screen,
                    onClick = { onSelect(screen) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // ── Spring scale on press + slight lift when selected ────────────────────
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed  -> 0.80f
            selected   -> 1.04f
            else       -> 1.00f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "navScale"
    )

    // ── Icon color ────────────────────────────────────────────────────────────
    val iconColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(220),
        label = "navIconColor"
    )

    // ── Pill indicator behind icon ────────────────────────────────────────────
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 48.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "pillWidth"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(220),
        label = "pillAlpha"
    )

    // ── Label alpha (reserved space to avoid layout shifts) ──────────────────
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(200),
        label = "labelAlpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon layer — pill indicator sits behind icon
            Box(contentAlignment = Alignment.Center) {
                // Animated pill indicator
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(
                            MaterialTheme.colorScheme.primaryContainer
                                .copy(alpha = pillAlpha)
                        )
                )

                Icon(
                    imageVector    = icon,
                    contentDescription = label,
                    tint           = iconColor,
                    modifier       = Modifier.size(22.dp)
                )
            }

            // Label — always occupies space (12dp), just fades in/out
            Box(
                modifier = Modifier
                    .height(14.dp)
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = labelAlpha),
                    maxLines = 1
                )
            }
        }
    }
}
