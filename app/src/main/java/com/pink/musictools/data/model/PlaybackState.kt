package com.pink.musictools.data.model

/**
 * 播放状态密封类
 * 
 * 表示音乐播放器的各种状态，使用密封类确保类型安全
 */
sealed class PlaybackState {
    /**
     * 空闲状态 - 播放器未初始化或已重置
     */
    object Idle : PlaybackState()
    
    /**
     * 加载状态 - 正在准备音频文件
     */
    object Loading : PlaybackState()
    
    /**
     * 播放状态 - 音乐正在播放
     */
    object Playing : PlaybackState()
    
    /**
     * 暂停状态 - 音乐已暂停
     */
    object Paused : PlaybackState()
    
    /**
     * 停止状态 - 播放已停止
     */
    object Stopped : PlaybackState()
    
    /**
     * 错误状态 - 播放过程中发生错误
     * 
     * @property message 错误消息，必须非空
     */
    data class Error(val message: String) : PlaybackState() {
        init {
            require(message.isNotBlank()) { "Error message must not be blank" }
        }
    }
}
