package com.pink.musictools.data.model

/**
 * 重复播放模式枚举
 */
enum class RepeatMode {
    /**
     * 不重复 - 播放完列表后停止
     */
    OFF,
    
    /**
     * 单曲循环 - 重复播放当前歌曲
     */
    ONE,
    
    /**
     * 列表循环 - 播放完列表后从头开始
     */
    ALL
}
