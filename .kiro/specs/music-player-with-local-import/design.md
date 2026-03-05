# 设计文档：音乐播放器与本地导入功能

## 概述

本功能为Android应用构建一个完整的音乐播放器工具，支持从本地存储导入音乐文件并播放。采用Material Design 3设计规范，使用Jetpack Compose构建全动态UI（无XML布局），支持深色/浅色主题切换。架构采用MVVM模式，确保高度解耦和可维护性。核心功能包括：本地音乐文件扫描与导入、音乐播放控制（播放/暂停/上一首/下一首）、播放列表管理、以及优雅的Material You风格界面。

## 系统架构

```mermaid
graph TB
    subgraph "UI Layer - Jetpack Compose"
        A[MainActivity]
        B[MusicPlayerScreen]
        C[MusicListScreen]
        D[ImportScreen]
        E[ThemeManager]
    end
    
    subgraph "ViewModel Layer"
        F[MusicPlayerViewModel]
        G[MusicLibraryViewModel]
        H[ImportViewModel]
    end
    
    subgraph "Domain Layer"
        I[PlaybackController]
        J[MusicRepository]
        K[FileScanner]
    end
    
    subgraph "Data Layer"
        L[LocalMusicDataSource]
        M[MusicDatabase]
        N[MediaPlayerService]
    end
    
    subgraph "External"
        O[Android MediaStore]
        P[File System]
        Q[Android MediaPlayer]
    end
    
    A --> B
    A --> C
    A --> D
    A --> E
    
    B --> F
    C --> G
    D --> H
    
    F --> I
    G --> J
    H --> K
    
    I --> N
    J --> L
    K --> L
    
    L --> M
    L --> O
    L --> P
    N --> Q


## 主要工作流程

```mermaid
sequenceDiagram
    participant User
    participant UI as MusicPlayerScreen
    participant VM as MusicPlayerViewModel
    participant PC as PlaybackController
    participant MS as MediaPlayerService
    participant MP as Android MediaPlayer
    
    User->>UI: 点击播放按钮
    UI->>VM: onPlayPauseClicked()
    VM->>PC: play(musicItem)
    PC->>MS: startPlayback(uri)
    MS->>MP: setDataSource(uri)
    MS->>MP: prepare()
    MS->>MP: start()
    MP-->>MS: onPrepared
    MS-->>PC: PlaybackState.Playing
    PC-->>VM: updateState(Playing)
    VM-->>UI: State更新
    UI-->>User: 显示播放中UI
```

## 组件与接口

### 组件 1: MusicPlayerViewModel

**目的**: 管理音乐播放器UI状态和用户交互逻辑

**接口**:
```kotlin
class MusicPlayerViewModel(
    private val playbackController: PlaybackController,
    private val musicRepository: MusicRepository
) : ViewModel() {
    
    val playbackState: StateFlow<PlaybackState>
    val currentMusic: StateFlow<MusicItem?>
    val progress: StateFlow<PlaybackProgress>
    val playlist: StateFlow<List<MusicItem>>
    
    fun onPlayPauseClicked()
    fun onNextClicked()
    fun onPreviousClicked()
    fun onSeekTo(position: Long)
    fun onRepeatModeChanged(mode: RepeatMode)
    fun onShuffleModeChanged(enabled: Boolean)
}
```

**职责**:
- 维护播放器UI状态（播放/暂停/停止）
- 处理用户播放控制操作
- 管理播放进度和时间显示
- 协调播放列表和当前播放项


### 组件 2: MusicLibraryViewModel

**目的**: 管理音乐库列表展示和筛选

**接口**:
```kotlin
class MusicLibraryViewModel(
    private val musicRepository: MusicRepository
) : ViewModel() {
    
    val musicList: StateFlow<List<MusicItem>>
    val isLoading: StateFlow<Boolean>
    val sortOrder: StateFlow<SortOrder>
    val searchQuery: StateFlow<String>
    
    fun loadMusicLibrary()
    fun onMusicItemClicked(item: MusicItem)
    fun onSortOrderChanged(order: SortOrder)
    fun onSearchQueryChanged(query: String)
    fun deleteMusicItem(item: MusicItem)
}
```

**职责**:
- 加载和展示音乐库列表
- 处理音乐项选择和播放
- 支持排序和搜索功能
- 管理音乐项删除操作


### 组件 3: ImportViewModel

**目的**: 管理本地音乐文件导入流程

**接口**:
```kotlin
class ImportViewModel(
    private val fileScanner: FileScanner,
    private val musicRepository: MusicRepository
) : ViewModel() {
    
    val scanProgress: StateFlow<ScanProgress>
    val discoveredFiles: StateFlow<List<AudioFile>>
    val importStatus: StateFlow<ImportStatus>
    
    fun startScan(directory: Uri)
    fun selectFiles(files: List<AudioFile>)
    fun importSelectedFiles()
    fun cancelImport()
}
```

**职责**:
- 扫描本地存储中的音频文件
- 展示发现的音频文件列表
- 处理用户选择和导入操作
- 显示导入进度和状态


### 组件 4: PlaybackController

**目的**: 核心播放控制逻辑，协调MediaPlayerService

**接口**:
```kotlin
class PlaybackController(
    private val mediaPlayerService: MediaPlayerService,
    private val musicRepository: MusicRepository
) {
    
    val playbackState: StateFlow<PlaybackState>
    val currentMusic: StateFlow<MusicItem?>
    val progress: StateFlow<PlaybackProgress>
    
    suspend fun play(music: MusicItem)
    suspend fun pause()
    suspend fun resume()
    suspend fun stop()
    suspend fun seekTo(position: Long)
    suspend fun playNext()
    suspend fun playPrevious()
    fun setRepeatMode(mode: RepeatMode)
    fun setShuffleMode(enabled: Boolean)
}
```

**职责**:
- 封装MediaPlayerService的播放控制
- 管理播放队列和播放顺序
- 处理重复和随机播放模式
- 提供播放状态和进度流


### 组件 5: MusicRepository

**目的**: 音乐数据访问层，统一数据源管理

**接口**:
```kotlin
interface MusicRepository {
    
    fun getAllMusic(): Flow<List<MusicItem>>
    suspend fun getMusicById(id: String): MusicItem?
    suspend fun insertMusic(music: MusicItem)
    suspend fun insertMusicBatch(musicList: List<MusicItem>)
    suspend fun deleteMusic(id: String)
    suspend fun updateMusic(music: MusicItem)
    fun searchMusic(query: String): Flow<List<MusicItem>>
    fun getMusicSorted(order: SortOrder): Flow<List<MusicItem>>
}
```

**职责**:
- 提供统一的音乐数据访问接口
- 协调本地数据库和MediaStore
- 支持增删改查操作
- 提供搜索和排序功能


### 组件 6: FileScanner

**目的**: 扫描本地存储中的音频文件

**接口**:
```kotlin
class FileScanner(
    private val context: Context
) {
    
    suspend fun scanDirectory(uri: Uri): Flow<ScanResult>
    suspend fun scanMediaStore(): Flow<AudioFile>
    suspend fun extractMetadata(uri: Uri): AudioMetadata
    fun getSupportedFormats(): List<String>
}
```

**职责**:
- 扫描指定目录或使用MediaStore扫描
- 提取音频文件元数据（标题、艺术家、专辑等）
- 过滤支持的音频格式
- 提供扫描进度反馈


### 组件 7: MediaPlayerService

**目的**: Android后台服务，管理MediaPlayer生命周期

**接口**:
```kotlin
class MediaPlayerService : Service() {
    
    private val binder = MediaPlayerBinder()
    private var mediaPlayer: MediaPlayer? = null
    
    fun prepare(uri: Uri)
    fun start()
    fun pause()
    fun stop()
    fun seekTo(position: Long)
    fun release()
    fun getCurrentPosition(): Long
    fun getDuration(): Long
    fun isPlaying(): Boolean
    
    fun setOnCompletionListener(listener: () -> Unit)
    fun setOnErrorListener(listener: (error: String) -> Unit)
    
    inner class MediaPlayerBinder : Binder() {
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }
}
```

**职责**:
- 管理Android MediaPlayer实例
- 在后台保持音乐播放
- 处理播放完成和错误事件
- 支持前台服务通知


## 数据模型

### 模型 1: MusicItem

```kotlin
data class MusicItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val dateAdded: Long,
    val size: Long,
    val mimeType: String
)
```

**验证规则**:
- id必须非空且唯一
- title必须非空
- duration必须大于0
- uri必须是有效的文件URI
- dateAdded必须是有效的时间戳
- size必须大于0


### 模型 2: PlaybackState

```kotlin
sealed class PlaybackState {
    object Idle : PlaybackState()
    object Loading : PlaybackState()
    object Playing : PlaybackState()
    object Paused : PlaybackState()
    object Stopped : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}
```

**验证规则**:
- 状态转换必须遵循有效的状态机规则
- Error状态必须包含非空错误消息
- 从任何状态都可以转换到Error或Stopped


### 模型 3: PlaybackProgress

```kotlin
data class PlaybackProgress(
    val currentPosition: Long,
    val duration: Long,
    val bufferedPosition: Long = 0L
) {
    val progressPercentage: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
}
```

**验证规则**:
- currentPosition必须在0到duration之间
- duration必须大于0
- bufferedPosition必须在currentPosition到duration之间
- progressPercentage必须在0.0到1.0之间


### 模型 4: AudioFile

```kotlin
data class AudioFile(
    val uri: Uri,
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String,
    val isSelected: Boolean = false
)
```

**验证规则**:
- uri必须是有效的文件URI
- name必须非空
- path必须是有效的文件路径
- size必须大于0
- mimeType必须是支持的音频格式（audio/mpeg, audio/mp4, audio/flac等）


### 模型 5: ScanProgress

```kotlin
data class ScanProgress(
    val scannedFiles: Int,
    val totalFiles: Int,
    val currentFile: String?,
    val isComplete: Boolean
) {
    val progressPercentage: Float
        get() = if (totalFiles > 0) scannedFiles.toFloat() / totalFiles else 0f
}
```

**验证规则**:
- scannedFiles必须在0到totalFiles之间
- totalFiles必须大于等于0
- isComplete为true时，scannedFiles必须等于totalFiles
- progressPercentage必须在0.0到1.0之间


### 模型 6: RepeatMode 和 SortOrder

```kotlin
enum class RepeatMode {
    OFF,      // 不重复
    ONE,      // 单曲循环
    ALL       // 列表循环
}

enum class SortOrder {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    ARTIST_DESC,
    DATE_ADDED_ASC,
    DATE_ADDED_DESC,
    DURATION_ASC,
    DURATION_DESC
}
```

**验证规则**:
- RepeatMode必须是三个值之一
- SortOrder必须是八个值之一


## 关键函数的形式化规范

### 函数 1: PlaybackController.play()

```kotlin
suspend fun play(music: MusicItem): Result<Unit>
```

**前置条件**:
- music不为null且包含有效的uri
- music.uri指向的文件存在且可访问
- MediaPlayerService已绑定且可用

**后置条件**:
- 如果成功：playbackState更新为Playing，currentMusic更新为传入的music
- 如果失败：playbackState更新为Error，包含错误描述
- 播放进度开始更新
- 之前的播放（如果有）已停止

**循环不变式**: 不适用（无循环）


### 函数 2: FileScanner.scanDirectory()

```kotlin
suspend fun scanDirectory(uri: Uri): Flow<ScanResult>
```

**前置条件**:
- uri不为null且指向有效的目录
- 应用具有READ_EXTERNAL_STORAGE权限
- 目录可访问且可读

**后置条件**:
- 返回Flow发射所有发现的音频文件
- 每个发射的AudioFile都包含有效的元数据
- 扫描完成后Flow正常结束
- 如果遇到错误，Flow发射Error结果

**循环不变式**: 
- 遍历目录时，所有已处理的文件都已被验证为音频文件或非音频文件
- 已发射的AudioFile数量等于已发现的有效音频文件数量


### 函数 3: MusicRepository.insertMusicBatch()

```kotlin
suspend fun insertMusicBatch(musicList: List<MusicItem>): Result<Int>
```

**前置条件**:
- musicList不为null
- musicList中的每个MusicItem都通过验证规则
- 数据库连接可用

**后置条件**:
- 成功时返回插入的记录数量
- 所有有效的MusicItem都已插入数据库
- 重复的id会被跳过或更新（根据策略）
- 失败时返回错误信息，数据库保持一致性

**循环不变式**:
- 遍历musicList时，所有已处理的项要么已插入，要么因验证失败被跳过
- 已插入的记录数量等于成功插入的MusicItem数量


### 函数 4: MediaPlayerService.prepare()

```kotlin
fun prepare(uri: Uri): Result<Unit>
```

**前置条件**:
- uri不为null且指向有效的音频文件
- MediaPlayer实例已创建或可创建
- 文件格式被MediaPlayer支持

**后置条件**:
- 成功时MediaPlayer处于prepared状态，可以开始播放
- duration和其他元数据可用
- 失败时MediaPlayer处于idle或error状态
- 返回Result指示成功或失败原因

**循环不变式**: 不适用（无循环）


## 算法伪代码

### 主播放工作流算法

```kotlin
// 算法：处理音乐播放请求
// 输入：music - 要播放的MusicItem
// 输出：Result<Unit> - 成功或失败结果

suspend fun processPlayRequest(music: MusicItem): Result<Unit> {
    // 前置条件断言
    require(music.uri.isValid()) { "Invalid music URI" }
    require(mediaPlayerService.isBound()) { "MediaPlayerService not bound" }
    
    // 步骤1：停止当前播放
    if (currentMusic.value != null) {
        mediaPlayerService.stop()
        updateState(PlaybackState.Stopped)
    }
    
    // 步骤2：准备新的音频文件
    updateState(PlaybackState.Loading)
    val prepareResult = mediaPlayerService.prepare(music.uri)
    
    if (prepareResult.isFailure) {
        updateState(PlaybackState.Error(prepareResult.error))
        return Result.failure(prepareResult.error)
    }
    
    // 步骤3：开始播放
    mediaPlayerService.start()
    updateCurrentMusic(music)
    updateState(PlaybackState.Playing)
    
    // 步骤4：启动进度更新协程
    startProgressUpdates()
    
    // 后置条件断言
    check(playbackState.value is PlaybackState.Playing)
    check(currentMusic.value == music)
    
    return Result.success(Unit)
}
```

**前置条件**:
- music已验证且包含有效uri
- MediaPlayerService已绑定

**后置条件**:
- 播放状态为Playing或Error
- 如果成功，currentMusic已更新
- 进度更新已启动

**循环不变式**: 不适用


### 文件扫描算法

```kotlin
// 算法：扫描目录中的音频文件
// 输入：directoryUri - 要扫描的目录URI
// 输出：Flow<ScanResult> - 扫描结果流

suspend fun scanDirectoryForAudio(directoryUri: Uri): Flow<ScanResult> = flow {
    // 前置条件断言
    require(hasStoragePermission()) { "Storage permission not granted" }
    require(directoryUri.isDirectory()) { "URI is not a directory" }
    
    val supportedFormats = listOf("audio/mpeg", "audio/mp4", "audio/flac", "audio/ogg")
    var scannedCount = 0
    val discoveredFiles = mutableListOf<AudioFile>()
    
    // 步骤1：获取目录中的所有文件
    val allFiles = contentResolver.queryDirectory(directoryUri)
    val totalFiles = allFiles.size
    
    emit(ScanResult.Progress(ScanProgress(0, totalFiles, null, false)))
    
    // 步骤2：遍历文件并过滤音频文件
    for (file in allFiles) {
        // 循环不变式：scannedCount == discoveredFiles.size + 已跳过的非音频文件数
        
        val mimeType = contentResolver.getType(file.uri)
        
        if (mimeType in supportedFormats) {
            // 步骤3：提取元数据
            val metadata = extractMetadata(file.uri)
            
            val audioFile = AudioFile(
                uri = file.uri,
                name = metadata.title ?: file.name,
                path = file.path,
                size = file.size,
                mimeType = mimeType
            )
            
            discoveredFiles.add(audioFile)
            emit(ScanResult.FileFound(audioFile))
        }
        
        scannedCount++
        emit(ScanResult.Progress(
            ScanProgress(scannedCount, totalFiles, file.name, false)
        ))
    }
    
    // 步骤4：完成扫描
    emit(ScanResult.Progress(
        ScanProgress(scannedCount, totalFiles, null, true)
    ))
    emit(ScanResult.Complete(discoveredFiles))
    
    // 后置条件断言
    check(scannedCount == totalFiles)
    check(discoveredFiles.all { it.mimeType in supportedFormats })
}
```

**前置条件**:
- 具有存储访问权限
- directoryUri指向有效目录

**后置条件**:
- 所有文件都已扫描
- 返回的AudioFile都是支持的格式
- Flow正常完成

**循环不变式**:
- scannedCount等于已处理的文件数量
- discoveredFiles只包含有效的音频文件
- 所有已发射的AudioFile都通过了格式验证


### 播放队列管理算法

```kotlin
// 算法：处理下一首播放
// 输入：无
// 输出：Result<MusicItem?> - 下一首音乐或null

suspend fun getNextMusic(): Result<MusicItem?> {
    val currentIndex = playlist.value.indexOf(currentMusic.value)
    
    // 前置条件断言
    require(playlist.value.isNotEmpty()) { "Playlist is empty" }
    
    return when (repeatMode.value) {
        RepeatMode.ONE -> {
            // 单曲循环：返回当前歌曲
            Result.success(currentMusic.value)
        }
        
        RepeatMode.ALL -> {
            // 列表循环：到达末尾时回到开头
            val nextIndex = (currentIndex + 1) % playlist.value.size
            Result.success(playlist.value[nextIndex])
        }
        
        RepeatMode.OFF -> {
            // 不循环：到达末尾时返回null
            if (currentIndex < playlist.value.size - 1) {
                Result.success(playlist.value[currentIndex + 1])
            } else {
                Result.success(null)
            }
        }
    }
    
    // 后置条件：
    // - RepeatMode.ONE时返回当前歌曲
    // - RepeatMode.ALL时返回有效的下一首（可能是第一首）
    // - RepeatMode.OFF时返回下一首或null
}
```

**前置条件**:
- playlist不为空
- currentMusic在playlist中（如果不为null）

**后置条件**:
- 返回值符合当前RepeatMode的逻辑
- RepeatMode.ONE返回当前歌曲
- RepeatMode.ALL返回有效的下一首
- RepeatMode.OFF在末尾返回null

**循环不变式**: 不适用


## 示例用法

### 示例 1: 基本播放流程

```kotlin
// 在Composable中使用ViewModel
@Composable
fun MusicPlayerScreen(viewModel: MusicPlayerViewModel = viewModel()) {
    val playbackState by viewModel.playbackState.collectAsState()
    val currentMusic by viewModel.currentMusic.collectAsState()
    val progress by viewModel.progress.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 显示当前播放音乐信息
        currentMusic?.let { music ->
            MusicInfoCard(music)
        }
        
        // 进度条
        Slider(
            value = progress.progressPercentage,
            onValueChange = { newValue ->
                val newPosition = (newValue * progress.duration).toLong()
                viewModel.onSeekTo(newPosition)
            }
        )
        
        // 播放控制按钮
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = { viewModel.onPreviousClicked() }) {
                Icon(Icons.Default.SkipPrevious, "上一首")
            }
            
            IconButton(onClick = { viewModel.onPlayPauseClicked() }) {
                Icon(
                    if (playbackState is PlaybackState.Playing) 
                        Icons.Default.Pause 
                    else 
                        Icons.Default.PlayArrow,
                    "播放/暂停"
                )
            }
            
            IconButton(onClick = { viewModel.onNextClicked() }) {
                Icon(Icons.Default.SkipNext, "下一首")
            }
        }
    }
}
```


### 示例 2: 导入本地音乐

```kotlin
// 导入流程
@Composable
fun ImportScreen(viewModel: ImportViewModel = viewModel()) {
    val scanProgress by viewModel.scanProgress.collectAsState()
    val discoveredFiles by viewModel.discoveredFiles.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()
    
    Column {
        // 开始扫描按钮
        Button(onClick = {
            // 使用Storage Access Framework选择目录
            val directoryUri = selectDirectory()
            viewModel.startScan(directoryUri)
        }) {
            Text("扫描本地音乐")
        }
        
        // 显示扫描进度
        if (!scanProgress.isComplete) {
            LinearProgressIndicator(
                progress = scanProgress.progressPercentage
            )
            Text("已扫描: ${scanProgress.scannedFiles}/${scanProgress.totalFiles}")
        }
        
        // 显示发现的文件列表
        LazyColumn {
            items(discoveredFiles) { file ->
                AudioFileItem(
                    file = file,
                    onSelectionChanged = { selected ->
                        viewModel.selectFiles(
                            if (selected) discoveredFiles + file 
                            else discoveredFiles - file
                        )
                    }
                )
            }
        }
        
        // 导入按钮
        Button(
            onClick = { viewModel.importSelectedFiles() },
            enabled = discoveredFiles.any { it.isSelected }
        ) {
            Text("导入选中的音乐")
        }
    }
}
```
