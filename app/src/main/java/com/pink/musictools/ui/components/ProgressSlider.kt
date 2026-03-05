package com.pink.musictools.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pink.musictools.data.model.PlaybackProgress

/**
 * 播放进度滑块组件
 * 
 * 显示播放进度和时间，支持拖动跳转
 * 
 * @param progress 播放进度
 * @param onSeek 拖动回调
 * @param modifier 修饰符
 */
@Composable
fun ProgressSlider(
    progress: PlaybackProgress,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 进度条
        Slider(
            value = progress.progressPercentage,
            onValueChange = { percentage ->
                val newPosition = (percentage * progress.duration).toLong()
                onSeek(newPosition)
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // 时间显示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(progress.currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = formatTime(progress.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 格式化时间（毫秒转为 mm:ss 格式）
 */
private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
