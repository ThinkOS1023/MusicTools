package com.pink.musictools.domain

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.pink.musictools.data.model.MusicItem
import com.pink.musictools.data.model.PlaybackProgress
import com.pink.musictools.data.model.PlaybackState
import com.pink.musictools.data.model.RepeatMode
import com.pink.musictools.data.service.MediaPlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 播放控制器
 * 
 * 核心播放控制逻辑，协调MediaPlayerService并管理播放状态、队列和进度更新
 * 
 * 职责：
 * - 封装MediaPlayerService的播放控制
 * - 管理播放队列和播放顺序
 * - 处理重复和随机播放模式
 * - 提供播放状态和进度流
 * 
 * @param context Android上下文，用于绑定服务
 */
class PlaybackController(private val context: Context) {
    
    companion object {
        private const val TAG = "PlaybackController"
        private const val PROGRESS_UPDATE_INTERVAL = 500L // 500ms更新一次进度
    }
    
    // MediaPlayerService相关
    private var mediaPlayerService: MediaPlayerService? = null
    private var isBound = false
    
    // 播放状态
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    // 当前播放音乐
    private val _currentMusic = MutableStateFlow<MusicItem?>(null)
    val currentMusic: StateFlow<MusicItem?> = _currentMusic.asStateFlow()
    
    // 播放进度
    private val _progress = MutableStateFlow(PlaybackProgress(0L, 1L, 0L))
    val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()
    
    // 播放队列
    private val _playlist = MutableStateFlow<List<MusicItem>>(emptyList())
    val playlist: StateFlow<List<MusicItem>> = _playlist.asStateFlow()
    
    // 重复模式
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    
    // 随机播放模式
    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()
    
    // 原始播放列表（用于随机播放）
    private var originalPlaylist: List<MusicItem> = emptyList()
    
    // 进度更新协程
    private var progressUpdateJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    // Service连接回调
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            val binder = service as MediaPlayerService.MediaPlayerBinder
            mediaPlayerService = binder.getService()
            isBound = true
            
            // 设置监听器
            mediaPlayerService?.setOnCompletionListener {
                handlePlaybackCompletion()
            }
            
            mediaPlayerService?.setOnErrorListener { errorMsg ->
                handlePlaybackError(errorMsg)
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            mediaPlayerService = null
            isBound = false
        }
    }
    
    init {
        // 绑定MediaPlayerService
        bindService()
    }
    
    /**
     * 绑定MediaPlayerService
     */
    private fun bindService() {
        val intent = Intent(context, MediaPlayerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d(TAG, "Binding to MediaPlayerService")
    }
    
    /**
     * 解绑MediaPlayerService
     */
    fun unbindService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            Log.d(TAG, "Unbound from MediaPlayerService")
        }
    }
    
    /**
     * 播放指定音乐
     * 
     * 前置条件：
     * - music不为null且包含有效的uri
     * - music.uri指向的文件存在且可访问
     * - MediaPlayerService已绑定且可用
     * 
     * 后置条件：
     * - 如果成功：playbackState更新为Playing，currentMusic更新为传入的music
     * - 如果失败：playbackState更新为Error，包含错误描述
     * - 播放进度开始更新
     * - 之前的播放（如果有）已停止
     * 
     * @param music 要播放的音乐项
     */
    suspend fun play(music: MusicItem) {
        Log.d(TAG, "play() called with music: ${music.title}")
        
        // 前置条件检查
        if (!isBound || mediaPlayerService == null) {
            val errorMsg = "MediaPlayerService not bound"
            Log.e(TAG, errorMsg)
            _playbackState.value = PlaybackState.Error(errorMsg)
            return
        }
        
        // 停止当前播放
        if (_currentMusic.value != null) {
            mediaPlayerService?.stop()
            _playbackState.value = PlaybackState.Stopped
        }
        
        // 更新状态为加载中
        _playbackState.value = PlaybackState.Loading
        
        // 准备新的音频文件
        val prepareSuccess = mediaPlayerService?.prepare(music.uri) ?: false
        
        if (!prepareSuccess) {
            val errorMsg = "Failed to prepare audio file: ${music.uri}"
            Log.e(TAG, errorMsg)
            _playbackState.value = PlaybackState.Error(errorMsg)
            return
        }
        
        // 设置音乐信息（用于通知显示）
        mediaPlayerService?.setMusicInfo(music.title, music.artist)
        
        // 开始播放
        val startSuccess = mediaPlayerService?.start() ?: false
        
        if (!startSuccess) {
            val errorMsg = "Failed to start playback"
            Log.e(TAG, errorMsg)
            _playbackState.value = PlaybackState.Error(errorMsg)
            return
        }
        
        // 更新当前音乐和状态
        _currentMusic.value = music
        _playbackState.value = PlaybackState.Playing
        
        // 初始化进度
        val duration = mediaPlayerService?.getDuration() ?: 0L
        _progress.value = PlaybackProgress(0L, duration, 0L)
        
        // 启动进度更新
        startProgressUpdates()
        
        Log.d(TAG, "Playback started successfully")
    }
    
    /**
     * 暂停播放
     * 
     * 前置条件：
     * - MediaPlayerService已绑定
     * - 当前正在播放音乐
     * 
     * 后置条件：
     * - playbackState更新为Paused
     * - 进度更新停止
     */
    suspend fun pause() {
        Log.d(TAG, "pause() called")
        
        if (!isBound || mediaPlayerService == null) {
            Log.w(TAG, "Cannot pause: MediaPlayerService not bound")
            return
        }
        
        val pauseSuccess = mediaPlayerService?.pause() ?: false
        
        if (pauseSuccess) {
            _playbackState.value = PlaybackState.Paused
            stopProgressUpdates()
            Log.d(TAG, "Playback paused")
        } else {
            Log.e(TAG, "Failed to pause playback")
        }
    }
    
    /**
     * 恢复播放
     * 
     * 前置条件：
     * - MediaPlayerService已绑定
     * - 当前处于暂停状态
     * 
     * 后置条件：
     * - playbackState更新为Playing
     * - 进度更新恢复
     */
    suspend fun resume() {
        Log.d(TAG, "resume() called")
        
        if (!isBound || mediaPlayerService == null) {
            Log.w(TAG, "Cannot resume: MediaPlayerService not bound")
            return
        }
        
        val startSuccess = mediaPlayerService?.start() ?: false
        
        if (startSuccess) {
            _playbackState.value = PlaybackState.Playing
            startProgressUpdates()
            Log.d(TAG, "Playback resumed")
        } else {
            Log.e(TAG, "Failed to resume playback")
        }
    }
    
    /**
     * 停止播放
     * 
     * 前置条件：
     * - MediaPlayerService已绑定
     * 
     * 后置条件：
     * - playbackState更新为Stopped
     * - currentMusic保持不变
     * - 进度更新停止
     */
    suspend fun stop() {
        Log.d(TAG, "stop() called")
        
        if (!isBound || mediaPlayerService == null) {
            Log.w(TAG, "Cannot stop: MediaPlayerService not bound")
            return
        }
        
        val stopSuccess = mediaPlayerService?.stop() ?: false
        
        if (stopSuccess) {
            _playbackState.value = PlaybackState.Stopped
            stopProgressUpdates()
            Log.d(TAG, "Playback stopped")
        } else {
            Log.e(TAG, "Failed to stop playback")
        }
    }
    
    /**
     * 跳转到指定位置
     * 
     * 前置条件：
     * - MediaPlayerService已绑定
     * - position在0到duration之间
     * 
     * 后置条件：
     * - 播放位置更新到指定位置
     * - progress更新
     * 
     * @param position 目标位置（毫秒）
     */
    suspend fun seekTo(position: Long) {
        Log.d(TAG, "seekTo() called with position: $position")
        
        if (!isBound || mediaPlayerService == null) {
            Log.w(TAG, "Cannot seek: MediaPlayerService not bound")
            return
        }
        
        val duration = mediaPlayerService?.getDuration() ?: 0L
        val clampedPosition = position.coerceIn(0L, duration)
        
        val seekSuccess = mediaPlayerService?.seekTo(clampedPosition) ?: false
        
        if (seekSuccess) {
            _progress.value = PlaybackProgress(clampedPosition, duration, clampedPosition)
            Log.d(TAG, "Seeked to position: $clampedPosition")
        } else {
            Log.e(TAG, "Failed to seek to position: $clampedPosition")
        }
    }
    
    /**
     * 播放下一首
     * 
     * 根据当前的重复模式和播放列表，播放下一首音乐
     * 
     * 前置条件：
     * - playlist不为空
     * 
     * 后置条件：
     * - 如果有下一首，开始播放下一首
     * - 如果没有下一首（RepeatMode.OFF且到达末尾），停止播放
     */
    suspend fun playNext() {
        Log.d(TAG, "playNext() called")
        
        val nextMusic = getNextMusic()
        
        if (nextMusic != null) {
            play(nextMusic)
        } else {
            // 没有下一首，停止播放
            stop()
            _playbackState.value = PlaybackState.Idle
            Log.d(TAG, "No next music, playback stopped")
        }
    }
    
    /**
     * 播放上一首
     * 
     * 根据当前的播放列表，播放上一首音乐
     * 
     * 前置条件：
     * - playlist不为空
     * 
     * 后置条件：
     * - 如果有上一首，开始播放上一首
     * - 如果没有上一首，播放第一首（RepeatMode.ALL）或停止（RepeatMode.OFF）
     */
    suspend fun playPrevious() {
        Log.d(TAG, "playPrevious() called")
        
        val previousMusic = getPreviousMusic()
        
        if (previousMusic != null) {
            play(previousMusic)
        } else {
            Log.d(TAG, "No previous music")
        }
    }
    
    /**
     * 设置重复模式
     * 
     * @param mode 重复模式（OFF, ONE, ALL）
     */
    fun setRepeatMode(mode: RepeatMode) {
        Log.d(TAG, "setRepeatMode() called with mode: $mode")
        _repeatMode.value = mode
    }
    
    /**
     * 设置随机播放模式
     * 
     * @param enabled 是否启用随机播放
     */
    fun setShuffleMode(enabled: Boolean) {
        Log.d(TAG, "setShuffleMode() called with enabled: $enabled")
        
        if (enabled && !_shuffleMode.value) {
            // 启用随机播放：保存原始列表并打乱
            originalPlaylist = _playlist.value
            val currentMusic = _currentMusic.value
            val shuffledList = _playlist.value.toMutableList()
            
            // 如果有当前播放的音乐，确保它在第一位
            if (currentMusic != null && shuffledList.contains(currentMusic)) {
                shuffledList.remove(currentMusic)
                shuffledList.shuffle()
                shuffledList.add(0, currentMusic)
            } else {
                shuffledList.shuffle()
            }
            
            _playlist.value = shuffledList
            _shuffleMode.value = true
            Log.d(TAG, "Shuffle mode enabled, playlist shuffled")
            
        } else if (!enabled && _shuffleMode.value) {
            // 禁用随机播放：恢复原始列表
            _playlist.value = originalPlaylist
            _shuffleMode.value = false
            Log.d(TAG, "Shuffle mode disabled, original playlist restored")
        }
    }
    
    /**
     * 设置播放列表
     * 
     * @param playlist 音乐列表
     */
    fun setPlaylist(playlist: List<MusicItem>) {
        Log.d(TAG, "setPlaylist() called with ${playlist.size} items")
        _playlist.value = playlist
        originalPlaylist = playlist
        
        // 如果启用了随机播放，重新打乱列表
        if (_shuffleMode.value) {
            val shuffledList = playlist.toMutableList()
            val currentMusic = _currentMusic.value
            
            if (currentMusic != null && shuffledList.contains(currentMusic)) {
                shuffledList.remove(currentMusic)
                shuffledList.shuffle()
                shuffledList.add(0, currentMusic)
            } else {
                shuffledList.shuffle()
            }
            
            _playlist.value = shuffledList
        }
    }
    
    /**
     * 获取下一首音乐
     * 
     * 根据当前的重复模式返回下一首音乐
     * 
     * @return 下一首音乐，如果没有则返回null
     */
    private fun getNextMusic(): MusicItem? {
        val currentPlaylist = _playlist.value
        val currentMusic = _currentMusic.value
        
        if (currentPlaylist.isEmpty()) {
            Log.w(TAG, "Playlist is empty")
            return null
        }
        
        return when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // 单曲循环：返回当前歌曲
                currentMusic
            }
            
            RepeatMode.ALL -> {
                // 列表循环：到达末尾时回到开头
                val currentIndex = currentPlaylist.indexOf(currentMusic)
                val nextIndex = (currentIndex + 1) % currentPlaylist.size
                currentPlaylist[nextIndex]
            }
            
            RepeatMode.OFF -> {
                // 不循环：到达末尾时返回null
                val currentIndex = currentPlaylist.indexOf(currentMusic)
                if (currentIndex < currentPlaylist.size - 1) {
                    currentPlaylist[currentIndex + 1]
                } else {
                    null
                }
            }
        }
    }
    
    /**
     * 获取上一首音乐
     * 
     * 根据当前的播放列表返回上一首音乐
     * 
     * @return 上一首音乐，如果没有则返回null
     */
    private fun getPreviousMusic(): MusicItem? {
        val currentPlaylist = _playlist.value
        val currentMusic = _currentMusic.value
        
        if (currentPlaylist.isEmpty()) {
            Log.w(TAG, "Playlist is empty")
            return null
        }
        
        val currentIndex = currentPlaylist.indexOf(currentMusic)
        
        return when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // 单曲循环：返回当前歌曲
                currentMusic
            }
            
            RepeatMode.ALL -> {
                // 列表循环：到达开头时回到末尾
                val previousIndex = if (currentIndex <= 0) {
                    currentPlaylist.size - 1
                } else {
                    currentIndex - 1
                }
                currentPlaylist[previousIndex]
            }
            
            RepeatMode.OFF -> {
                // 不循环：到达开头时返回null
                if (currentIndex > 0) {
                    currentPlaylist[currentIndex - 1]
                } else {
                    null
                }
            }
        }
    }
    
    /**
     * 启动播放进度更新协程
     */
    private fun startProgressUpdates() {
        // 停止现有的更新任务
        stopProgressUpdates()
        
        progressUpdateJob = coroutineScope.launch {
            while (isActive && _playbackState.value is PlaybackState.Playing) {
                val currentPosition = mediaPlayerService?.getCurrentPosition() ?: 0L
                val duration = mediaPlayerService?.getDuration() ?: 0L
                
                if (duration > 0) {
                    _progress.value = PlaybackProgress(currentPosition, duration, currentPosition)
                }
                
                delay(PROGRESS_UPDATE_INTERVAL)
            }
        }
        
        Log.d(TAG, "Progress updates started")
    }
    
    /**
     * 停止播放进度更新协程
     */
    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
        Log.d(TAG, "Progress updates stopped")
    }
    
    /**
     * 处理播放完成事件
     */
    private fun handlePlaybackCompletion() {
        Log.d(TAG, "Playback completed")
        // Immediately reflect completion in UI before scheduling next song
        _playbackState.value = PlaybackState.Stopped
        stopProgressUpdates()

        coroutineScope.launch {
            when (_repeatMode.value) {
                RepeatMode.ONE -> {
                    _currentMusic.value?.let { play(it) }
                }
                RepeatMode.ALL, RepeatMode.OFF -> {
                    playNext()
                }
            }
        }
    }
    
    /**
     * 处理播放错误事件
     * 
     * @param errorMsg 错误消息
     */
    private fun handlePlaybackError(errorMsg: String) {
        Log.e(TAG, "Playback error: $errorMsg")
        _playbackState.value = PlaybackState.Error(errorMsg)
        stopProgressUpdates()
    }
}
