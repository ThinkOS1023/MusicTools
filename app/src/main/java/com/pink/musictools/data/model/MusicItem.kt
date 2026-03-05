package com.pink.musictools.data.model

import android.net.Uri

/**
 * 音乐项数据模型
 * 
 * @property id 唯一标识符，必须非空且唯一
 * @property title 音乐标题，必须非空
 * @property artist 艺术家名称
 * @property album 专辑名称
 * @property duration 音乐时长（毫秒），必须大于0
 * @property uri 音乐文件URI，必须是有效的文件URI
 * @property albumArtUri 专辑封面URI，可选
 * @property dateAdded 添加日期时间戳，必须是有效的时间戳
 * @property size 文件大小（字节），必须大于0
 * @property mimeType MIME类型
 * @property lyrics 歌词内容，可选
 */
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
    val mimeType: String,
    val lyrics: String? = null
) {
    init {
        require(id.isNotBlank()) { "Music ID must not be blank" }
        require(title.isNotBlank()) { "Music title must not be blank" }
        require(duration > 0) { "Duration must be greater than 0, got $duration" }
        require(dateAdded > 0) { "Date added must be a valid timestamp, got $dateAdded" }
        require(size > 0) { "File size must be greater than 0, got $size" }
    }
}
