package com.pink.musictools.domain

import android.content.Context
import android.net.Uri
import android.util.Log
import com.pink.musictools.data.model.MusicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.tag.reference.PictureTypes
import org.jaudiotagger.tag.reference.ID3V2Version
import java.io.File
import java.io.FileOutputStream

class MetadataWriter(private val context: Context) {

    companion object {
        private const val TAG = "MetadataWriter"
    }

    sealed class WriteResult {
        object Success : WriteResult()
        data class Error(val message: String) : WriteResult()
    }

    init {
        configureTaggerOptions()
    }

    suspend fun writeMetadata(musicItem: MusicItem, targetFile: File): WriteResult = withContext(Dispatchers.IO) {
        try {
            if (targetFile.length() <= 0L) {
                return@withContext WriteResult.Error("写入元数据失败: 导出临时音频为空")
            }

            val audioFile = readAudioFile(targetFile)
            val tag = audioFile.tagOrCreateAndSetDefault

            tag.setField(FieldKey.TITLE, musicItem.title)
            tag.setField(FieldKey.ARTIST, musicItem.artist)
            tag.setField(FieldKey.ALBUM, musicItem.album)

            val normalizedLyrics = musicItem.lyrics
                ?.let { LyricsFormatter.normalizeToLrc(it) }
                ?.takeIf { it.isNotBlank() }

            // If editor lyrics are empty, keep or restore source embedded lyrics instead of deleting.
            val sourceLyrics = if (normalizedLyrics == null) {
                extractLyricsFromSourceAudio(musicItem.uri, musicItem.mimeType)
            } else {
                null
            }
            val lyricsToWrite = normalizedLyrics ?: sourceLyrics
            if (!lyricsToWrite.isNullOrBlank()) {
                writeLyricsField(tag, lyricsToWrite)
            }

            val artworkInfo = resolveArtworkForExport(musicItem)
            if (artworkInfo != null) {
                runCatching {
                    val mimeType = artworkInfo.mimeType
                        ?.takeIf { it.isNotBlank() }
                        ?: detectImageMimeType(artworkInfo.bytes)
                    val artwork = ArtworkFactory.getNew().apply {
                        setBinaryData(artworkInfo.bytes)
                        setMimeType(mimeType)
                        setDescription("")
                        setPictureType(PictureTypes.DEFAULT_ID)
                    }
                    tag.deleteArtworkField()
                    tag.setField(artwork)
                }.onFailure { error ->
                    Log.w(TAG, "Failed to set artwork", error)
                }
            }

            audioFile.commit()
            WriteResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write metadata", e)
            WriteResult.Error("写入元数据失败: ${e.message}")
        }
    }

    private fun configureTaggerOptions() {
        val options = TagOptionSingleton.getInstance()
        options.setAndroid(true)
        options.setID3V2Version(ID3V2Version.ID3_V23)
        options.setLanguage("eng")
        options.setUnsyncTags(false)
        options.setAPICDescriptionITunesCompatible(true)
    }

    private fun writeLyricsField(tag: Tag, lyrics: String) {
        if (tag is AbstractID3v2Tag) {
            val lyricField = tag.createField(FieldKey.LYRICS, lyrics)
            val lyricBody = (lyricField as? AbstractID3v2Frame)?.body as? FrameBodyUSLT
            if (lyricBody != null) {
                // USLT requires a 3-byte language code for broad player compatibility.
                lyricBody.setLanguage("eng")
                lyricBody.setDescription("")
            }
            tag.setField(lyricField)
            return
        }

        tag.setField(FieldKey.LYRICS, lyrics)
    }

    suspend fun copyAndWriteMetadata(
        sourceUri: Uri,
        targetFile: File,
        musicItem: MusicItem
    ): WriteResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            writeMetadata(musicItem, targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy and write metadata", e)
            WriteResult.Error("复制并写入元数据失败: ${e.message}")
        }
    }

    private data class ArtworkInfo(
        val bytes: ByteArray,
        val mimeType: String?
    )

    private fun resolveArtworkForExport(musicItem: MusicItem): ArtworkInfo? {
        val uriArtwork = musicItem.albumArtUri?.let { uri ->
            val bytes = readBytesFromUri(uri)
            if (bytes != null) {
                ArtworkInfo(bytes = bytes, mimeType = context.contentResolver.getType(uri))
            } else {
                null
            }
        }

        if (uriArtwork != null) {
            return uriArtwork
        }

        return extractArtworkFromSourceAudio(musicItem.uri, musicItem.mimeType)
    }

    private fun readBytesFromUri(uri: Uri): ByteArray? {
        return runCatching {
            if (uri.scheme == "file") {
                val file = java.io.File(uri.path ?: return@runCatching null)
                if (file.exists() && file.length() > 0) file.readBytes() else null
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes()
                }
            }
        }.getOrNull()
    }

    private fun extractArtworkFromSourceAudio(audioUri: Uri, mimeType: String): ArtworkInfo? {
        val tempAudioFile = createTempAudioCopy(audioUri, mimeType) ?: return null

        return try {
            val audioFile = readAudioFile(tempAudioFile)
            val artwork = audioFile.tag?.firstArtwork
            if (artwork?.binaryData != null && artwork.binaryData.isNotEmpty()) {
                ArtworkInfo(
                    bytes = artwork.binaryData,
                    mimeType = artwork.mimeType
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract artwork from source audio", e)
            null
        } finally {
            tempAudioFile.delete()
        }
    }

    private fun extractLyricsFromSourceAudio(audioUri: Uri, mimeType: String): String? {
        val tempAudioFile = createTempAudioCopy(audioUri, mimeType) ?: return null
        return try {
            val audioFile = readAudioFile(tempAudioFile)
            audioFile.tag
                ?.getFirst(FieldKey.LYRICS)
                ?.takeIf { it.isNotBlank() }
                ?.let { LyricsFormatter.normalizeToLrc(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract lyrics from source audio", e)
            null
        } finally {
            tempAudioFile.delete()
        }
    }

    private fun createTempAudioCopy(sourceUri: Uri, mimeType: String): File? {
        return try {
            val extension = resolveAudioExtension(sourceUri, mimeType)
            val tempFile = File.createTempFile("audio_meta_", extension, context.cacheDir)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            tempFile
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create temp audio copy", e)
            null
        }
    }

    private fun resolveAudioExtension(sourceUri: Uri, mimeType: String): String {
        val byMime = when (mimeType) {
            "audio/mpeg" -> ".mp3"
            "audio/mp4", "audio/x-m4a" -> ".m4a"
            "audio/flac" -> ".flac"
            "audio/ogg" -> ".ogg"
            "audio/wav", "audio/x-wav" -> ".wav"
            "audio/aac" -> ".aac"
            else -> null
        }
        if (byMime != null) {
            return byMime
        }

        val pathSegment = sourceUri.lastPathSegment.orEmpty()
        val ext = pathSegment.substringAfterLast('.', "").lowercase()
        return when {
            ext.isBlank() -> ".mp3"
            ext.startsWith(".") -> ext
            else -> ".$ext"
        }
    }

    private fun detectImageMimeType(bytes: ByteArray): String {
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte()
        ) {
            return "image/png"
        }

        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
        ) {
            return "image/jpeg"
        }

        if (bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() &&
            bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() &&
            bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() &&
            bytes[9] == 0x45.toByte() &&
            bytes[10] == 0x42.toByte() &&
            bytes[11] == 0x50.toByte()
        ) {
            return "image/webp"
        }

        return "image/jpeg"
    }

    private fun readAudioFile(file: File): AudioFile {
        return try {
            AudioFileIO.readMagic(file)
        } catch (magicError: Exception) {
            Log.w(TAG, "Magic-based audio detection failed, falling back to extension reader: ${file.name}", magicError)
            AudioFileIO.read(file)
        }
    }
}
