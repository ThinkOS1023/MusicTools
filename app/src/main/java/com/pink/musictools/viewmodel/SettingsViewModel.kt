package com.pink.musictools.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pink.musictools.BuildConfig
import com.pink.musictools.data.local.AppPreferences
import com.pink.musictools.data.model.ColorTheme
import com.pink.musictools.data.model.ThemeMode
import com.pink.musictools.domain.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(
        val version: String,
        val releaseUrl: String,
        val releaseNotes: String
    ) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class SettingsViewModel(private val prefs: AppPreferences) : ViewModel() {

    private val _themeMode = MutableStateFlow(prefs.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _colorTheme = MutableStateFlow(prefs.colorTheme)
    val colorTheme: StateFlow<ColorTheme> = _colorTheme.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.dynamicColor)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.themeMode = mode
    }

    fun setColorTheme(theme: ColorTheme) {
        _colorTheme.value = theme
        prefs.colorTheme = theme
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs.dynamicColor = enabled
    }

    fun checkForUpdate() {
        if (_updateState.value is UpdateState.Checking) return
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            UpdateChecker.checkForUpdate(currentVersion)
                .onSuccess { info ->
                    _updateState.value = if (info.hasUpdate) {
                        UpdateState.UpdateAvailable(
                            version = info.latestVersion,
                            releaseUrl = info.releaseUrl,
                            releaseNotes = info.releaseNotes
                        )
                    } else {
                        UpdateState.UpToDate
                    }
                }
                .onFailure { e ->
                    _updateState.value = UpdateState.Error(e.message ?: "检查失败")
                }
        }
    }
}
