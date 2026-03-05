package com.pink.musictools.data.local

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pink.musictools.data.model.MusicItem

/**
 * Room数据库实体，用于存储音乐项信息
 */
@Entity(tableName = "music")
data class MusicEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val albumArtUri: String?,
    val dateAdded: Long,
    val size: Long,
    val mimeType: String,
    val lyrics: String? = null
) {
    /**
     * 转换为MusicItem领域模型
     */
    fun toMusicItem(): MusicItem {
        return MusicItem(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            uri = Uri.parse(uri),
            albumArtUri = albumArtUri?.let { Uri.parse(it) },
            dateAdded = dateAdded,
            size = size,
            mimeType = mimeType,
            lyrics = lyrics
        )
    }

    companion object {
        /**
         * 从MusicItem创建MusicEntity
         */
        fun fromMusicItem(musicItem: MusicItem): MusicEntity {
            return MusicEntity(
                id = musicItem.id,
                title = musicItem.title,
                artist = musicItem.artist,
                album = musicItem.album,
                duration = musicItem.duration,
                uri = musicItem.uri.toString(),
                albumArtUri = musicItem.albumArtUri?.toString(),
                dateAdded = musicItem.dateAdded,
                size = musicItem.size,
                mimeType = musicItem.mimeType,
                lyrics = musicItem.lyrics
            )
        }
    }
}
