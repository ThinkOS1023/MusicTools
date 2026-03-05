package com.pink.musictools.data.model

import android.net.Uri

/**
 * 音频文件数据模型
 * 
 * 用于表示扫描到的音频文件信息
 * 
 * @property uri 文件URI，必须是有效的文件URI
 * @property name 文件名称，必须非空
 * @property path 文件路径，必须是有效的文件路径
 * @property size 文件大小（字节），必须大于0
 * @property mimeType MIME类型，必须是支持的音频格式
 * @property isSelected 是否被选中用于导入
 */
data class AudioFile(
    val uri: Uri,
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String,
    val isSelected: Boolean = false,
    // 从 MediaStore 预取的元数据，避免 import 阶段使用 MediaMetadataRetriever
    val cachedTitle: String? = null,
    val cachedArtist: String? = null,
    val cachedAlbum: String? = null,
    val cachedDuration: Long = 0L,
    val cachedAlbumId: Long? = null
) {
    companion object {
        /**
         * 支持的音频MIME类型
         */
        val SUPPORTED_MIME_TYPES = setOf(
            "audio/mpeg",      // MP3
            "audio/mp4",       // M4A, AAC
            "audio/flac",      // FLAC
            "audio/ogg",       // OGG
            "audio/wav",       // WAV
            "audio/x-wav",     // WAV (alternative)
            "audio/aac",       // AAC
            "audio/x-m4a"      // M4A (alternative)
        )
    }
    
    init {
        require(name.isNotBlank()) { "File name must not be blank" }
        require(path.isNotBlank()) { "File path must not be blank" }
        require(size > 0) { "File size must be greater than 0, got $size" }
        require(mimeType in SUPPORTED_MIME_TYPES) { 
            "Unsupported MIME type: $mimeType. Supported types: $SUPPORTED_MIME_TYPES" 
        }
    }
}
