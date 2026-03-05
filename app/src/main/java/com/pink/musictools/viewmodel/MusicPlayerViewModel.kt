package com.pink.musictools.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pink.musictools.data.model.MusicItem
import com.pink.musictools.data.model.PlaybackProgress
import com.pink.musictools.data.model.PlaybackState
import com.pink.musictools.data.model.RepeatMode
import com.pink.musictools.data.repository.MusicRepository
import com.pink.musictools.domain.PlaybackController
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 音乐播放器ViewModel
 * 
 * 管理音乐播放器UI状态和用户交互逻辑
 * 
 * 职责：
 * - 维护播放器UI状态（播放/暂停/停止）
 * - 处理用户播放控制操作
 * - 管理播放进度和时间显示
 * - 协调播放列表和当前播放项
 * 
 * @param playbackController 播放控制器
 * @param musicRepository 音乐数据仓库
 */
class MusicPlayerViewModel(
    private val playbackController: PlaybackController,
    private val musicRepository: MusicRepository
) : ViewModel() {
    
    /**
     * 播放状态流
     */
    val playbackState: StateFlow<PlaybackState> = playbackController.playbackState
    
    /**
     * 当前播放音乐流
     */
    val currentMusic: StateFlow<MusicItem?> = playbackController.currentMusic
    
    /**
     * 播放进度流
     */
    val progress: StateFlow<PlaybackProgress> = playbackController.progress
    
    /**
     * 播放列表流
     */
    val playlist: StateFlow<List<MusicItem>> = playbackController.playlist
    
    /**
     * 重复模式流
     */
    val repeatMode: StateFlow<RepeatMode> = playbackController.repeatMode
    
    /**
     * 随机播放模式流
     */
    val shuffleMode: StateFlow<Boolean> = playbackController.shuffleMode
    
    /**
     * 处理播放/暂停按钮点击
     * 
     * 根据当前播放状态决定播放或暂停
     */
    fun onPlayPauseClicked() {
        viewModelScope.launch {
            when (playbackState.value) {
                is PlaybackState.Playing -> {
                    // 当前正在播放，执行暂停
                    playbackController.pause()
                }
                is PlaybackState.Paused -> {
                    // 当前已暂停，执行恢复播放
                    playbackController.resume()
                }
                is PlaybackState.Idle, is PlaybackState.Stopped -> {
                    // 空闲或停止状态，播放当前音乐或播放列表第一首
                    val musicToPlay = currentMusic.value ?: playlist.value.firstOrNull()
                    musicToPlay?.let {
                        playbackController.play(it)
                    }
                }
                else -> {
                    // 其他状态（Loading, Error）不处理
                }
            }
        }
    }
    
    /**
     * 处理下一首按钮点击
     */
    fun onNextClicked() {
        viewModelScope.launch {
            playbackController.playNext()
        }
    }
    
    /**
     * 处理上一首按钮点击
     */
    fun onPreviousClicked() {
        viewModelScope.launch {
            playbackController.playPrevious()
        }
    }
    
    /**
     * 处理进度条拖动
     * 
     * @param position 目标位置（毫秒）
     */
    fun onSeekTo(position: Long) {
        viewModelScope.launch {
            playbackController.seekTo(position)
        }
    }
    
    /**
     * 处理重复模式切换
     * 
     * @param mode 新的重复模式
     */
    fun onRepeatModeChanged(mode: RepeatMode) {
        playbackController.setRepeatMode(mode)
    }
    
    /**
     * 处理随机播放模式切换
     * 
     * @param enabled 是否启用随机播放
     */
    fun onShuffleModeChanged(enabled: Boolean) {
        playbackController.setShuffleMode(enabled)
    }
    
    /**
     * 播放指定音乐
     * 
     * @param music 要播放的音乐项
     */
    fun playMusic(music: MusicItem) {
        viewModelScope.launch {
            playbackController.play(music)
        }
    }
    
    /**
     * 设置播放列表
     * 
     * @param playlist 音乐列表
     */
    fun setPlaylist(playlist: List<MusicItem>) {
        playbackController.setPlaylist(playlist)
    }
    
    override fun onCleared() {
        super.onCleared()
        // 解绑服务
        playbackController.unbindService()
    }
}
