package com.pink.musictools.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pink.musictools.data.model.AudioFile
import com.pink.musictools.data.model.MusicItem
import com.pink.musictools.data.model.ScanProgress
import com.pink.musictools.data.repository.MusicRepository
import com.pink.musictools.domain.FileScanner
import com.pink.musictools.domain.MetadataReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ImportViewModel(
    private val fileScanner: FileScanner,
    private val musicRepository: MusicRepository,
    private val metadataReader: MetadataReader
) : ViewModel() {

    sealed class ImportStatus {
        object Idle : ImportStatus()
        object Scanning : ImportStatus()
        object Importing : ImportStatus()
        data class Success(val count: Int) : ImportStatus()
        data class Error(val message: String) : ImportStatus()
    }

    private val _scanProgress = MutableStateFlow(ScanProgress(0, 0, null, false))
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _discoveredFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    val discoveredFiles: StateFlow<List<AudioFile>> = _discoveredFiles.asStateFlow()

    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val importStatus: StateFlow<ImportStatus> = _importStatus.asStateFlow()

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()

    private val _currentStage = MutableStateFlow("")
    val currentStage: StateFlow<String> = _currentStage.asStateFlow()

    private val _stageLogs = MutableStateFlow<List<String>>(emptyList())
    val stageLogs: StateFlow<List<String>> = _stageLogs.asStateFlow()

    private val logTimeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private fun appendLog(msg: String) {
        val line = "[${logTimeFmt.format(Date())}] $msg"
        _currentStage.value = msg
        val current = _stageLogs.value
        _stageLogs.value = if (current.size >= 100) current.drop(1) + line else current + line
    }

    fun startScan(directory: Uri? = null) {
        viewModelScope.launch {
            try {
                _importStatus.value = ImportStatus.Scanning
                _discoveredFiles.value = emptyList()
                _scanProgress.value = ScanProgress(0, 0, null, false)
                _stageLogs.value = emptyList()
                appendLog("开始扫描音频文件...")

                val scanFlow = if (directory != null) {
                    fileScanner.scanDirectory(directory)
                } else {
                    fileScanner.scanDirectory(Uri.EMPTY)
                }

                scanFlow.collect { result ->
                    when (result) {
                        is FileScanner.ScanResult.Progress -> {
                            _scanProgress.value = result.progress
                            if (result.progress.totalFiles > 0 && result.progress.scannedFiles == 1) {
                                appendLog("共发现 ${result.progress.totalFiles} 个音频文件，正在读取...")
                            }
                        }

                        is FileScanner.ScanResult.FileFound -> {
                            _discoveredFiles.value = _discoveredFiles.value + result.audioFile
                        }

                        is FileScanner.ScanResult.Complete -> {
                            _discoveredFiles.value = result.files
                            appendLog("扫描完成，共发现 ${result.files.size} 个文件")
                            _importStatus.value = ImportStatus.Idle
                        }

                        is FileScanner.ScanResult.Error -> {
                            appendLog("扫描出错: ${result.message}")
                            _importStatus.value = ImportStatus.Error(result.message)
                        }
                    }
                }
            } catch (e: Exception) {
                appendLog("扫描异常: ${e.message}")
                _importStatus.value = ImportStatus.Error("扫描失败: ${e.message}")
            }
        }
    }

    fun toggleFileSelection(file: AudioFile) {
        _discoveredFiles.value = _discoveredFiles.value.map {
            if (it.uri == file.uri) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }
    }

    fun selectAll(selected: Boolean) {
        _discoveredFiles.value = _discoveredFiles.value.map {
            it.copy(isSelected = selected)
        }
    }

    fun importSelectedFiles() {
        viewModelScope.launch {
            try {
                val selectedFiles = _discoveredFiles.value.filter { it.isSelected }

                if (selectedFiles.isEmpty()) {
                    _importStatus.value = ImportStatus.Error("请至少选择一个文件")
                    return@launch
                }

                _importStatus.value = ImportStatus.Importing
                _importProgress.value = 0f
                _stageLogs.value = emptyList()
                appendLog("开始导入 ${selectedFiles.size} 个文件...")

                val musicItems = mutableListOf<MusicItem>()
                var importedCount = 0

                selectedFiles.forEachIndexed { index, audioFile ->
                    try {
                        val fallbackTitle = fileNameWithoutExtension(audioFile.name)
                        appendLog("[${index + 1}/${selectedFiles.size}] 处理中: $fallbackTitle")

                        val embeddedMetadata = metadataReader.readEmbeddedMetadata(
                            audioUri = audioFile.uri,
                            mimeType = audioFile.mimeType
                        )

                        val mediaStoreAlbumArtUri = audioFile.cachedAlbumId?.let { albumId ->
                            Uri.parse("content://media/external/audio/albumart/$albumId")
                        }

                        val duration = audioFile.cachedDuration.takeIf { it > 0 } ?: 1L

                        val musicItem = MusicItem(
                            id = UUID.randomUUID().toString(),
                            title = embeddedMetadata.title
                                ?: audioFile.cachedTitle?.takeIf { it.isNotBlank() }
                                ?: fallbackTitle,
                            artist = embeddedMetadata.artist
                                ?: audioFile.cachedArtist?.takeIf { it.isNotBlank() }
                                ?: "未知艺术家",
                            album = embeddedMetadata.album
                                ?: audioFile.cachedAlbum?.takeIf { it.isNotBlank() }
                                ?: "未知专辑",
                            duration = duration,
                            uri = audioFile.uri,
                            albumArtUri = embeddedMetadata.albumArtUri ?: mediaStoreAlbumArtUri,
                            dateAdded = System.currentTimeMillis(),
                            size = audioFile.size,
                            mimeType = audioFile.mimeType,
                            lyrics = embeddedMetadata.lyrics
                        )

                        musicItems += musicItem
                        importedCount++
                        _importProgress.value = (index + 1).toFloat() / selectedFiles.size
                    } catch (e: Exception) {
                        appendLog("跳过 ${audioFile.name}: ${e.message}")
                    }
                }

                if (musicItems.isNotEmpty()) {
                    appendLog("正在写入数据库 (${musicItems.size} 条)...")
                    musicRepository.insertMusicBatch(musicItems)
                    appendLog("导入完成，共 $importedCount 首")
                    _importStatus.value = ImportStatus.Success(importedCount)
                } else {
                    appendLog("没有成功导入任何文件")
                    _importStatus.value = ImportStatus.Error("没有成功导入任何文件")
                }
            } catch (e: Exception) {
                _importStatus.value = ImportStatus.Error("导入失败: ${e.message}")
            }
        }
    }

    fun cancelImport() {
        _importStatus.value = ImportStatus.Idle
        _scanProgress.value = ScanProgress(0, 0, null, false)
        _discoveredFiles.value = emptyList()
        _importProgress.value = 0f
        _stageLogs.value = emptyList()
        _currentStage.value = ""
    }

    fun resetImportStatus() {
        _importStatus.value = ImportStatus.Idle
    }

    private fun fileNameWithoutExtension(fileName: String): String {
        return fileName.substringBeforeLast('.', fileName).ifBlank { fileName }
    }
}
