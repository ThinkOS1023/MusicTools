package com.pink.musictools.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pink.musictools.data.model.MusicItem
import com.pink.musictools.data.model.PlaybackProgress
import com.pink.musictools.data.model.PlaybackState
import com.pink.musictools.data.repository.MusicRepository
import com.pink.musictools.domain.ArtworkStore
import com.pink.musictools.domain.LyricsFormatter
import com.pink.musictools.domain.PlaybackController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MusicEditorViewModel(
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController,
    private val artworkStore: ArtworkStore
) : ViewModel() {

    sealed class EditorStatus {
        object Idle : EditorStatus()
        object Saving : EditorStatus()
        object Success : EditorStatus()
        data class Error(val message: String) : EditorStatus()
    }

    companion object {
        private const val AUTO_SAVE_DELAY_MS = 800L
    }

    private val _currentMusic = MutableStateFlow<MusicItem?>(null)
    val currentMusic: StateFlow<MusicItem?> = _currentMusic.asStateFlow()

    private val _editorStatus = MutableStateFlow<EditorStatus>(EditorStatus.Idle)
    val editorStatus: StateFlow<EditorStatus> = _editorStatus.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _artist = MutableStateFlow("")
    val artist: StateFlow<String> = _artist.asStateFlow()

    private val _album = MutableStateFlow("")
    val album: StateFlow<String> = _album.asStateFlow()

    private val _albumArtUri = MutableStateFlow<Uri?>(null)
    val albumArtUri: StateFlow<Uri?> = _albumArtUri.asStateFlow()

    private val _lyricsText = MutableStateFlow("")
    val lyricsText: StateFlow<String> = _lyricsText.asStateFlow()

    private val _currentLineIndex = MutableStateFlow(0)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex.asStateFlow()

    private val _musicList = MutableStateFlow<List<MusicItem>>(emptyList())
    val musicList: StateFlow<List<MusicItem>> = _musicList.asStateFlow()

    private val _currentMusicIndex = MutableStateFlow(0)
    val currentMusicIndex: StateFlow<Int> = _currentMusicIndex.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    private val _hasPrevious = MutableStateFlow(false)
    val hasPrevious: StateFlow<Boolean> = _hasPrevious.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = playbackController.playbackState
    val progress: StateFlow<PlaybackProgress> = playbackController.progress

    val isPlaying: StateFlow<Boolean> = MutableStateFlow(false).apply {
        viewModelScope.launch {
            playbackController.playbackState.collect { state ->
                value = state is PlaybackState.Playing
            }
        }
    }

    val currentPosition: StateFlow<Long> = MutableStateFlow(0L).apply {
        viewModelScope.launch {
            playbackController.progress.collect { playbackProgress ->
                value = playbackProgress.currentPosition
            }
        }
    }

    private var suppressAutoSave = false
    private var autoSaveJob: Job? = null

    fun loadMusic(musicId: String) {
        viewModelScope.launch {
            try {
                val allMusic = musicRepository.getAllMusic().first()
                _musicList.value = allMusic

                val index = allMusic.indexOfFirst { it.id == musicId }
                if (index < 0) {
                    _editorStatus.value = EditorStatus.Error("未找到对应歌曲")
                    return@launch
                }

                _currentMusicIndex.value = index
                updateNavigationState()
                loadMusicAtIndex(index)
            } catch (e: Exception) {
                _editorStatus.value = EditorStatus.Error("加载失败: ${e.message}")
            }
        }
    }

    private suspend fun loadMusicAtIndex(index: Int) {
        val music = _musicList.value.getOrNull(index) ?: return

        suppressAutoSave = true
        _currentMusic.value = music
        _title.value = music.title
        _artist.value = music.artist
        _album.value = music.album
        _albumArtUri.value = music.albumArtUri
        _lyricsText.value = music.lyrics?.let { LyricsFormatter.normalizeToLrc(it) }.orEmpty()
        _currentLineIndex.value = firstStampableLineIndex(_lyricsText.value.lines())
        suppressAutoSave = false

        playbackController.play(music)
    }

    fun loadNextMusic() {
        viewModelScope.launch {
            val nextIndex = _currentMusicIndex.value + 1
            if (nextIndex < _musicList.value.size) {
                saveChangesQuietly()
                _currentMusicIndex.value = nextIndex
                updateNavigationState()
                loadMusicAtIndex(nextIndex)
            }
        }
    }

    fun loadPreviousMusic() {
        viewModelScope.launch {
            val previousIndex = _currentMusicIndex.value - 1
            if (previousIndex >= 0) {
                saveChangesQuietly()
                _currentMusicIndex.value = previousIndex
                updateNavigationState()
                loadMusicAtIndex(previousIndex)
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
        scheduleAutoSave()
    }

    fun updateArtist(newArtist: String) {
        _artist.value = newArtist
        scheduleAutoSave()
    }

    fun updateAlbum(newAlbum: String) {
        _album.value = newAlbum
        scheduleAutoSave()
    }

    fun updateAlbumArt(uri: Uri?) {
        if (uri == null) {
            _albumArtUri.value = null
            scheduleAutoSave()
            return
        }

        viewModelScope.launch {
            try {
                val persistedUri = artworkStore.persistArtworkFromUri(uri) ?: uri
                _albumArtUri.value = persistedUri
                scheduleAutoSave()
            } catch (e: Exception) {
                _editorStatus.value = EditorStatus.Error("封面保存失败: ${e.message}")
            }
        }
    }

    fun updateLyricsText(text: String) {
        _lyricsText.value = text
        val lineCount = text.lines().size.coerceAtLeast(1)
        _currentLineIndex.value = _currentLineIndex.value.coerceIn(0, lineCount - 1)
        scheduleAutoSave()
    }

    fun importLyrics(rawText: String) {
        _lyricsText.value = LyricsFormatter.normalizeToLrc(rawText)
        _currentLineIndex.value = firstStampableLineIndex(_lyricsText.value.lines())
        scheduleAutoSave()
    }

    fun exportLyrics(): String {
        return LyricsFormatter.normalizeToLrc(
            rawLyrics = _lyricsText.value,
            includeMetadataTags = true,
            title = _title.value,
            artist = _artist.value,
            album = _album.value
        )
    }

    fun stampCurrentLine() {
        val lines = _lyricsText.value.lines().toMutableList()
        if (lines.isEmpty()) {
            return
        }

        val index = _currentLineIndex.value.coerceIn(0, lines.lastIndex)
        val line = lines[index]
        if (line.isBlank()) {
            _currentLineIndex.value = findNextStampableLine(lines, index)
            return
        }

        lines[index] = LyricsFormatter.replaceOrPrependTimestamp(line, currentPosition.value)
        _lyricsText.value = lines.joinToString("\n")
        _currentLineIndex.value = findNextStampableLine(lines, index)
        scheduleAutoSave()
    }

    fun adjustCurrentLineTimestamp(offsetMillis: Long) {
        val lines = _lyricsText.value.lines().toMutableList()
        if (lines.isEmpty()) {
            return
        }

        val index = _currentLineIndex.value.coerceIn(0, lines.lastIndex)
        val line = lines[index]
        if (line.isBlank()) {
            return
        }

        val adjustedLine = if (LyricsFormatter.parseFirstTimestampMillis(line) != null) {
            LyricsFormatter.shiftTimestamp(line, offsetMillis)
        } else {
            LyricsFormatter.replaceOrPrependTimestamp(line, currentPosition.value + offsetMillis)
        }

        lines[index] = adjustedLine
        _lyricsText.value = lines.joinToString("\n")
        scheduleAutoSave()
    }

    fun previousLine() {
        _currentLineIndex.value = (_currentLineIndex.value - 1).coerceAtLeast(0)
    }

    fun nextLine() {
        val lines = _lyricsText.value.lines()
        if (lines.isEmpty()) {
            _currentLineIndex.value = 0
            return
        }
        _currentLineIndex.value = (_currentLineIndex.value + 1).coerceAtMost(lines.lastIndex)
    }

    fun setCurrentLine(index: Int) {
        val lines = _lyricsText.value.lines()
        if (lines.isEmpty()) {
            _currentLineIndex.value = 0
            return
        }
        _currentLineIndex.value = index.coerceIn(0, lines.lastIndex)
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            playbackController.seekTo(positionMs)
        }
    }

    fun seekToLineTimestamp(lineIndex: Int) {
        val line = _lyricsText.value.lines().getOrNull(lineIndex) ?: return
        val ms = LyricsFormatter.parseFirstTimestampMillis(line) ?: return
        viewModelScope.launch { playbackController.seekTo(ms) }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            if (isPlaying.value) {
                playbackController.pause()
            } else {
                val music = _currentMusic.value ?: return@launch
                if (playbackState.value is PlaybackState.Paused) {
                    playbackController.resume()
                } else {
                    playbackController.play(music)
                }
            }
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            try {
                if (_title.value.isBlank()) {
                    _editorStatus.value = EditorStatus.Error("标题不能为空")
                    return@launch
                }

                _editorStatus.value = EditorStatus.Saving
                val updated = persistCurrentState() ?: return@launch
                _currentMusic.value = updated
                _editorStatus.value = EditorStatus.Success
            } catch (e: Exception) {
                _editorStatus.value = EditorStatus.Error("保存失败: ${e.message}")
            }
        }
    }

    fun resetStatus() {
        _editorStatus.value = EditorStatus.Idle
    }

    private fun scheduleAutoSave() {
        if (suppressAutoSave) {
            return
        }

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            saveChangesQuietly()
        }
    }

    private suspend fun saveChangesQuietly() {
        try {
            val updated = persistCurrentState() ?: return
            _currentMusic.value = updated
        } catch (_: Exception) {
            // no-op
        }
    }

    private suspend fun persistCurrentState(): MusicItem? {
        val music = _currentMusic.value ?: return null
        val normalizedLyrics = LyricsFormatter.normalizeToLrc(_lyricsText.value).ifBlank { null }

        val updatedMusic = music.copy(
            title = _title.value,
            artist = _artist.value,
            album = _album.value,
            lyrics = normalizedLyrics,
            albumArtUri = _albumArtUri.value
        )

        musicRepository.updateMusic(updatedMusic)
        return updatedMusic
    }

    private fun updateNavigationState() {
        val index = _currentMusicIndex.value
        val size = _musicList.value.size
        _hasPrevious.value = index > 0
        _hasNext.value = index < size - 1
    }

    private fun firstStampableLineIndex(lines: List<String>): Int {
        val index = lines.indexOfFirst { it.isNotBlank() }
        return if (index >= 0) index else 0
    }

    private fun findNextStampableLine(lines: List<String>, fromIndex: Int): Int {
        if (lines.isEmpty()) {
            return 0
        }

        for (index in (fromIndex + 1)..lines.lastIndex) {
            if (lines[index].isNotBlank()) {
                return index
            }
        }

        return fromIndex.coerceIn(0, lines.lastIndex)
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            saveChangesQuietly()
            playbackController.pause()
        }
        super.onCleared()
    }
}
