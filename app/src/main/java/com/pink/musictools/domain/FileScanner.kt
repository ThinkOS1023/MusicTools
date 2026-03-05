package com.pink.musictools.domain

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.pink.musictools.data.model.AudioFile
import com.pink.musictools.data.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * 文件扫描器
 * 
 * 负责扫描本地存储中的音频文件，提取元数据，并提供扫描进度反馈
 * 
 * @property context Android上下文
 */
class FileScanner(private val context: Context) {
    
    private val contentResolver: ContentResolver = context.contentResolver
    
    companion object {
        private const val TAG = "FileScanner"
        
        /**
         * 支持的音频格式
         */
        private val SUPPORTED_FORMATS = listOf(
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
    
    /**
     * 扫描结果密封类
     */
    sealed class ScanResult {
        data class Progress(val progress: ScanProgress) : ScanResult()
        data class FileFound(val audioFile: AudioFile) : ScanResult()
        data class Complete(val files: List<AudioFile>) : ScanResult()
        data class Error(val message: String, val exception: Exception? = null) : ScanResult()
    }
    
    /**
     * 音频元数据
     */
    data class AudioMetadata(
        val title: String?,
        val artist: String?,
        val album: String?,
        val duration: Long,
        val albumArtUri: Uri?
    )
    
    /**
     * 扫描指定目录中的音频文件
     * 
     * @param uri 目录URI
     * @return 扫描结果流
     */
    fun scanDirectory(uri: Uri): Flow<ScanResult> = flow {
        try {
            Log.d(TAG, "Starting directory scan: $uri")
            
            // 使用MediaStore扫描更可靠
            emit(ScanResult.Progress(ScanProgress(0, 0, null, false)))
            
            val discoveredFiles = mutableListOf<AudioFile>()
            var scannedCount = 0
            
            // 查询MediaStore获取所有音频文件，同时预取元数据以避免 import 阶段调用 MediaMetadataRetriever
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
            )

            val selection = "${MediaStore.Audio.Media.MIME_TYPE} IN (${
                SUPPORTED_FORMATS.joinToString(",") { "'$it'" }
            })"

            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val totalFiles = cursor.count
                emit(ScanResult.Progress(ScanProgress(0, totalFiles, null, false)))

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        val path = cursor.getString(pathColumn)
                        val size = cursor.getLong(sizeColumn)
                        val mimeType = cursor.getString(mimeTypeColumn)

                        val fileUri = Uri.withAppendedPath(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )

                        if (size > 0 && mimeType in SUPPORTED_FORMATS) {
                            val cachedTitle = if (titleColumn >= 0) cursor.getString(titleColumn) else null
                            val cachedArtist = if (artistColumn >= 0) cursor.getString(artistColumn) else null
                            val cachedAlbum = if (albumColumn >= 0) cursor.getString(albumColumn) else null
                            val cachedDuration = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L
                            val cachedAlbumId = if (albumIdColumn >= 0) cursor.getLong(albumIdColumn) else null

                            val audioFile = AudioFile(
                                uri = fileUri,
                                name = name,
                                path = path,
                                size = size,
                                mimeType = mimeType,
                                isSelected = false,
                                cachedTitle = cachedTitle,
                                cachedArtist = cachedArtist,
                                cachedAlbum = cachedAlbum,
                                cachedDuration = cachedDuration,
                                cachedAlbumId = cachedAlbumId
                            )
                            
                            discoveredFiles.add(audioFile)
                            emit(ScanResult.FileFound(audioFile))
                        }
                        
                        scannedCount++
                        emit(ScanResult.Progress(
                            ScanProgress(scannedCount, totalFiles, name, false)
                        ))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing file", e)
                        // Continue with next file
                    }
                }
                
                emit(ScanResult.Progress(
                    ScanProgress(scannedCount, totalFiles, null, true)
                ))
            }
            
            emit(ScanResult.Complete(discoveredFiles))
            Log.d(TAG, "Directory scan complete. Found ${discoveredFiles.size} files")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during directory scan", e)
            emit(ScanResult.Error("扫描失败: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 使用MediaStore扫描所有音频文件
     * 
     * @return 音频文件流
     */
    fun scanMediaStore(): Flow<AudioFile> = flow {
        try {
            Log.d(TAG, "Starting MediaStore scan")
            
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE
            )
            
            val selection = "${MediaStore.Audio.Media.MIME_TYPE} IN (${
                SUPPORTED_FORMATS.joinToString(",") { "'$it'" }
            })"
            
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                
                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        val path = cursor.getString(pathColumn)
                        val size = cursor.getLong(sizeColumn)
                        val mimeType = cursor.getString(mimeTypeColumn)
                        
                        val fileUri = Uri.withAppendedPath(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )
                        
                        if (size > 0 && mimeType in SUPPORTED_FORMATS) {
                            emit(AudioFile(
                                uri = fileUri,
                                name = name,
                                path = path,
                                size = size,
                                mimeType = mimeType,
                                isSelected = false
                            ))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing MediaStore entry", e)
                        // Continue with next entry
                    }
                }
            }
            
            Log.d(TAG, "MediaStore scan complete")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during MediaStore scan", e)
            throw e
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 提取音频文件的元数据
     * 
     * @param uri 音频文件URI
     * @return 音频元数据
     */
    suspend fun extractMetadata(uri: Uri): AudioMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            
            // 尝试从MediaStore获取专辑封面URI
            val albumArtUri = getAlbumArtUri(uri)
            
            AudioMetadata(
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                albumArtUri = albumArtUri
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting metadata from $uri", e)
            AudioMetadata(
                title = null,
                artist = null,
                album = null,
                duration = 0L,
                albumArtUri = null
            )
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }
    }
    
    /**
     * 获取支持的音频格式列表
     * 
     * @return 支持的MIME类型列表
     */
    fun getSupportedFormats(): List<String> = SUPPORTED_FORMATS
    
    /**
     * 从MediaStore获取专辑封面URI
     * 
     * @param audioUri 音频文件URI
     * @return 专辑封面URI，如果不存在则返回null
     */
    private fun getAlbumArtUri(audioUri: Uri): Uri? {
        return try {
            val projection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
            
            contentResolver.query(
                audioUri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                    if (albumIdColumn >= 0) {
                        val albumId = cursor.getLong(albumIdColumn)
                        Uri.parse("content://media/external/audio/albumart/$albumId")
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting album art URI", e)
            null
        }
    }
}
