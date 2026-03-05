package com.pink.musictools.data.model

/**
 * 播放进度数据模型
 * 
 * @property currentPosition 当前播放位置（毫秒），必须在0到duration之间
 * @property duration 总时长（毫秒），必须大于0
 * @property bufferedPosition 缓冲位置（毫秒），必须在currentPosition到duration之间
 */
data class PlaybackProgress(
    val currentPosition: Long,
    val duration: Long,
    val bufferedPosition: Long = 0L
) {
    /**
     * 播放进度百分比（0.0到1.0）
     */
    val progressPercentage: Float
        get() = if (duration > 0) {
            (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
        } else {
            0f
        }
    
    init {
        require(duration > 0) { "Duration must be greater than 0, got $duration" }
        require(currentPosition >= 0) { "Current position must be non-negative, got $currentPosition" }
        require(currentPosition <= duration) { 
            "Current position ($currentPosition) must not exceed duration ($duration)" 
        }
        require(bufferedPosition >= 0) { "Buffered position must be non-negative, got $bufferedPosition" }
        require(bufferedPosition <= duration) { 
            "Buffered position ($bufferedPosition) must not exceed duration ($duration)" 
        }
    }
}
