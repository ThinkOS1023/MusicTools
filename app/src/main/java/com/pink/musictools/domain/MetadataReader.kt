package com.pink.musictools.domain

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.reference.ID3V2Version
import java.io.File
import java.io.FileOutputStream

class MetadataReader(
    private val context: Context,
    private val artworkStore: ArtworkStore
) {

    companion object {
        private const val TAG = "MetadataReader"
    }

    init {
        configureTaggerOptions()
    }

    data class EmbeddedMetadata(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val lyrics: String? = null,
        val albumArtUri: Uri? = null
    )

    suspend fun readEmbeddedMetadata(audioUri: Uri, mimeType: String): EmbeddedMetadata = withContext(Dispatchers.IO) {
        val tagMetadata = readByAudioTagger(audioUri, mimeType)
        val fallbackMetadata = readByRetriever(audioUri)

        EmbeddedMetadata(
            title = tagMetadata.title ?: fallbackMetadata.title,
            artist = tagMetadata.artist ?: fallbackMetadata.artist,
            album = tagMetadata.album ?: fallbackMetadata.album,
            lyrics = tagMetadata.lyrics,
            albumArtUri = tagMetadata.albumArtUri ?: fallbackMetadata.albumArtUri
        )
    }

    private suspend fun readByAudioTagger(audioUri: Uri, mimeType: String): EmbeddedMetadata {
        val tempFile = createTempAudioCopy(audioUri, mimeType) ?: return EmbeddedMetadata()

        return try {
            if (tempFile.length() <= 0L) {
                return EmbeddedMetadata()
            }

            val audioFile = readAudioFile(tempFile)
            val tag = audioFile.tag ?: return EmbeddedMetadata()

            val artwork = tag.firstArtwork
            val artworkUri = if (artwork?.binaryData != null && artwork.binaryData.isNotEmpty()) {
                artworkStore.persistArtworkBytes(artwork.binaryData, artwork.mimeType)
            } else {
                null
            }

            EmbeddedMetadata(
                title = tag.getFirst(FieldKey.TITLE).takeIf { it.isNotBlank() },
                artist = tag.getFirst(FieldKey.ARTIST).takeIf { it.isNotBlank() },
                album = tag.getFirst(FieldKey.ALBUM).takeIf { it.isNotBlank() },
                lyrics = extractLyrics(tag),
                albumArtUri = artworkUri
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse embedded metadata via AudioTagger: $audioUri", e)
            EmbeddedMetadata()
        } finally {
            tempFile.delete()
        }
    }

    private suspend fun readByRetriever(audioUri: Uri): EmbeddedMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, audioUri)
            val picture = retriever.embeddedPicture
            val artworkUri = if (picture != null && picture.isNotEmpty()) {
                artworkStore.persistArtworkBytes(picture, "image/jpeg")
            } else {
                null
            }

            EmbeddedMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                albumArtUri = artworkUri
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse metadata via MediaMetadataRetriever: $audioUri", e)
            EmbeddedMetadata()
        } finally {
            runCatching { retriever.release() }
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

    private fun extractLyrics(tag: Tag): String? {
        val directLyrics = runCatching {
            tag.getFirst(FieldKey.LYRICS)
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }

        if (!directLyrics.isNullOrBlank()) {
            return LyricsFormatter.normalizeToLrc(directLyrics)
        }

        if (tag is AbstractID3v2Tag) {
            val usltLyrics = runCatching { tag.getFirst("USLT") }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }

            if (!usltLyrics.isNullOrBlank()) {
                return LyricsFormatter.normalizeToLrc(usltLyrics)
            }
        }

        return null
    }

    private fun createTempAudioCopy(sourceUri: Uri, mimeType: String): File? {
        val extension = when (mimeType) {
            "audio/mpeg" -> "mp3"
            "audio/mp4", "audio/x-m4a" -> "m4a"
            "audio/flac" -> "flac"
            "audio/ogg" -> "ogg"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/aac" -> "aac"
            else -> "mp3"
        }

        return try {
            val tempFile = File.createTempFile("meta_", ".$extension", context.cacheDir)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            tempFile
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy source for metadata read", e)
            null
        }
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
