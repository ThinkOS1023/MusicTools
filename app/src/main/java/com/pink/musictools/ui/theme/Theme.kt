package com.pink.musictools.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.pink.musictools.data.model.ColorTheme
import com.pink.musictools.data.model.ThemeMode

// ── Purple (default) ──────────────────────────────────────────────────────────
private val PurpleLightScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline
)

private val PurpleDarkScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline
)

// ── Blue ──────────────────────────────────────────────────────────────────────
private val BlueLightScheme = lightColorScheme(
    primary = md_theme_blue_light_primary,
    onPrimary = md_theme_blue_light_onPrimary,
    primaryContainer = md_theme_blue_light_primaryContainer,
    onPrimaryContainer = md_theme_blue_light_onPrimaryContainer,
    secondary = md_theme_blue_light_secondary,
    onSecondary = md_theme_blue_light_onSecondary,
    secondaryContainer = md_theme_blue_light_secondaryContainer,
    onSecondaryContainer = md_theme_blue_light_onSecondaryContainer,
    tertiary = md_theme_blue_light_tertiary,
    onTertiary = md_theme_blue_light_onTertiary,
    tertiaryContainer = md_theme_blue_light_tertiaryContainer,
    onTertiaryContainer = md_theme_blue_light_onTertiaryContainer,
    background = md_theme_blue_light_background,
    onBackground = md_theme_blue_light_onBackground,
    surface = md_theme_blue_light_surface,
    onSurface = md_theme_blue_light_onSurface,
    surfaceVariant = md_theme_blue_light_surfaceVariant,
    onSurfaceVariant = md_theme_blue_light_onSurfaceVariant,
    outline = md_theme_blue_light_outline
)

private val BlueDarkScheme = darkColorScheme(
    primary = md_theme_blue_dark_primary,
    onPrimary = md_theme_blue_dark_onPrimary,
    primaryContainer = md_theme_blue_dark_primaryContainer,
    onPrimaryContainer = md_theme_blue_dark_onPrimaryContainer,
    secondary = md_theme_blue_dark_secondary,
    onSecondary = md_theme_blue_dark_onSecondary,
    secondaryContainer = md_theme_blue_dark_secondaryContainer,
    onSecondaryContainer = md_theme_blue_dark_onSecondaryContainer,
    tertiary = md_theme_blue_dark_tertiary,
    onTertiary = md_theme_blue_dark_onTertiary,
    tertiaryContainer = md_theme_blue_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_blue_dark_onTertiaryContainer,
    background = md_theme_blue_dark_background,
    onBackground = md_theme_blue_dark_onBackground,
    surface = md_theme_blue_dark_surface,
    onSurface = md_theme_blue_dark_onSurface,
    surfaceVariant = md_theme_blue_dark_surfaceVariant,
    onSurfaceVariant = md_theme_blue_dark_onSurfaceVariant,
    outline = md_theme_blue_dark_outline
)

// ── Green ─────────────────────────────────────────────────────────────────────
private val GreenLightScheme = lightColorScheme(
    primary = md_theme_green_light_primary,
    onPrimary = md_theme_green_light_onPrimary,
    primaryContainer = md_theme_green_light_primaryContainer,
    onPrimaryContainer = md_theme_green_light_onPrimaryContainer,
    secondary = md_theme_green_light_secondary,
    onSecondary = md_theme_green_light_onSecondary,
    secondaryContainer = md_theme_green_light_secondaryContainer,
    onSecondaryContainer = md_theme_green_light_onSecondaryContainer,
    tertiary = md_theme_green_light_tertiary,
    onTertiary = md_theme_green_light_onTertiary,
    tertiaryContainer = md_theme_green_light_tertiaryContainer,
    onTertiaryContainer = md_theme_green_light_onTertiaryContainer,
    background = md_theme_green_light_background,
    onBackground = md_theme_green_light_onBackground,
    surface = md_theme_green_light_surface,
    onSurface = md_theme_green_light_onSurface,
    surfaceVariant = md_theme_green_light_surfaceVariant,
    onSurfaceVariant = md_theme_green_light_onSurfaceVariant,
    outline = md_theme_green_light_outline
)

private val GreenDarkScheme = darkColorScheme(
    primary = md_theme_green_dark_primary,
    onPrimary = md_theme_green_dark_onPrimary,
    primaryContainer = md_theme_green_dark_primaryContainer,
    onPrimaryContainer = md_theme_green_dark_onPrimaryContainer,
    secondary = md_theme_green_dark_secondary,
    onSecondary = md_theme_green_dark_onSecondary,
    secondaryContainer = md_theme_green_dark_secondaryContainer,
    onSecondaryContainer = md_theme_green_dark_onSecondaryContainer,
    tertiary = md_theme_green_dark_tertiary,
    onTertiary = md_theme_green_dark_onTertiary,
    tertiaryContainer = md_theme_green_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_green_dark_onTertiaryContainer,
    background = md_theme_green_dark_background,
    onBackground = md_theme_green_dark_onBackground,
    surface = md_theme_green_dark_surface,
    onSurface = md_theme_green_dark_onSurface,
    surfaceVariant = md_theme_green_dark_surfaceVariant,
    onSurfaceVariant = md_theme_green_dark_onSurfaceVariant,
    outline = md_theme_green_dark_outline
)

// ── Red ───────────────────────────────────────────────────────────────────────
private val RedLightScheme = lightColorScheme(
    primary = md_theme_red_light_primary,
    onPrimary = md_theme_red_light_onPrimary,
    primaryContainer = md_theme_red_light_primaryContainer,
    onPrimaryContainer = md_theme_red_light_onPrimaryContainer,
    secondary = md_theme_red_light_secondary,
    onSecondary = md_theme_red_light_onSecondary,
    secondaryContainer = md_theme_red_light_secondaryContainer,
    onSecondaryContainer = md_theme_red_light_onSecondaryContainer,
    tertiary = md_theme_red_light_tertiary,
    onTertiary = md_theme_red_light_onTertiary,
    tertiaryContainer = md_theme_red_light_tertiaryContainer,
    onTertiaryContainer = md_theme_red_light_onTertiaryContainer,
    background = md_theme_red_light_background,
    onBackground = md_theme_red_light_onBackground,
    surface = md_theme_red_light_surface,
    onSurface = md_theme_red_light_onSurface,
    surfaceVariant = md_theme_red_light_surfaceVariant,
    onSurfaceVariant = md_theme_red_light_onSurfaceVariant,
    outline = md_theme_red_light_outline
)

private val RedDarkScheme = darkColorScheme(
    primary = md_theme_red_dark_primary,
    onPrimary = md_theme_red_dark_onPrimary,
    primaryContainer = md_theme_red_dark_primaryContainer,
    onPrimaryContainer = md_theme_red_dark_onPrimaryContainer,
    secondary = md_theme_red_dark_secondary,
    onSecondary = md_theme_red_dark_onSecondary,
    secondaryContainer = md_theme_red_dark_secondaryContainer,
    onSecondaryContainer = md_theme_red_dark_onSecondaryContainer,
    tertiary = md_theme_red_dark_tertiary,
    onTertiary = md_theme_red_dark_onTertiary,
    tertiaryContainer = md_theme_red_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_red_dark_onTertiaryContainer,
    background = md_theme_red_dark_background,
    onBackground = md_theme_red_dark_onBackground,
    surface = md_theme_red_dark_surface,
    onSurface = md_theme_red_dark_onSurface,
    surfaceVariant = md_theme_red_dark_surfaceVariant,
    onSurfaceVariant = md_theme_red_dark_onSurfaceVariant,
    outline = md_theme_red_dark_outline
)

// ── Orange ────────────────────────────────────────────────────────────────────
private val OrangeLightScheme = lightColorScheme(
    primary = md_theme_orange_light_primary,
    onPrimary = md_theme_orange_light_onPrimary,
    primaryContainer = md_theme_orange_light_primaryContainer,
    onPrimaryContainer = md_theme_orange_light_onPrimaryContainer,
    secondary = md_theme_orange_light_secondary,
    onSecondary = md_theme_orange_light_onSecondary,
    secondaryContainer = md_theme_orange_light_secondaryContainer,
    onSecondaryContainer = md_theme_orange_light_onSecondaryContainer,
    tertiary = md_theme_orange_light_tertiary,
    onTertiary = md_theme_orange_light_onTertiary,
    tertiaryContainer = md_theme_orange_light_tertiaryContainer,
    onTertiaryContainer = md_theme_orange_light_onTertiaryContainer,
    background = md_theme_orange_light_background,
    onBackground = md_theme_orange_light_onBackground,
    surface = md_theme_orange_light_surface,
    onSurface = md_theme_orange_light_onSurface,
    surfaceVariant = md_theme_orange_light_surfaceVariant,
    onSurfaceVariant = md_theme_orange_light_onSurfaceVariant,
    outline = md_theme_orange_light_outline
)

private val OrangeDarkScheme = darkColorScheme(
    primary = md_theme_orange_dark_primary,
    onPrimary = md_theme_orange_dark_onPrimary,
    primaryContainer = md_theme_orange_dark_primaryContainer,
    onPrimaryContainer = md_theme_orange_dark_onPrimaryContainer,
    secondary = md_theme_orange_dark_secondary,
    onSecondary = md_theme_orange_dark_onSecondary,
    secondaryContainer = md_theme_orange_dark_secondaryContainer,
    onSecondaryContainer = md_theme_orange_dark_onSecondaryContainer,
    tertiary = md_theme_orange_dark_tertiary,
    onTertiary = md_theme_orange_dark_onTertiary,
    tertiaryContainer = md_theme_orange_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_orange_dark_onTertiaryContainer,
    background = md_theme_orange_dark_background,
    onBackground = md_theme_orange_dark_onBackground,
    surface = md_theme_orange_dark_surface,
    onSurface = md_theme_orange_dark_onSurface,
    surfaceVariant = md_theme_orange_dark_surfaceVariant,
    onSurfaceVariant = md_theme_orange_dark_onSurfaceVariant,
    outline = md_theme_orange_dark_outline
)

@Composable
fun MusicToolsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorTheme: ColorTheme = ColorTheme.PURPLE,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (colorTheme) {
            ColorTheme.PURPLE -> if (darkTheme) PurpleDarkScheme  else PurpleLightScheme
            ColorTheme.BLUE   -> if (darkTheme) BlueDarkScheme    else BlueLightScheme
            ColorTheme.GREEN  -> if (darkTheme) GreenDarkScheme   else GreenLightScheme
            ColorTheme.RED    -> if (darkTheme) RedDarkScheme     else RedLightScheme
            ColorTheme.ORANGE -> if (darkTheme) OrangeDarkScheme  else OrangeLightScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
