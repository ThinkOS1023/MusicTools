package com.pink.musictools.data.local

import android.content.Context
import com.pink.musictools.data.model.ColorTheme
import com.pink.musictools.data.model.ThemeMode

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var openAiApiKey: String
        get() = prefs.getString(KEY_OPENAI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENAI_API_KEY, value).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.valueOf(
            prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        )
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var colorTheme: ColorTheme
        get() = ColorTheme.valueOf(
            prefs.getString(KEY_COLOR_THEME, ColorTheme.PURPLE.name) ?: ColorTheme.PURPLE.name
        )
        set(value) = prefs.edit().putString(KEY_COLOR_THEME, value.name).apply()

    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    var customColorArgb: Long
        get() = prefs.getLong(KEY_CUSTOM_COLOR, 0xFF6750A4L)
        set(value) = prefs.edit().putLong(KEY_CUSTOM_COLOR, value).apply()

    companion object {
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_COLOR_THEME = "color_theme"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_CUSTOM_COLOR = "custom_color"
    }
}
