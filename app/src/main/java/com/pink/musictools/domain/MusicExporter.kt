package com.pink.musictools.domain

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.MediaStore
import android.util.Log
import com.pink.musictools.data.model.MusicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MusicExporter(
    private val context: Context,
    private val metadataWriter: MetadataWriter
) {

    companion object {
        private const val TAG = "MusicExporter"
    }

    sealed class ExportResult {
        data class Success(val uri: Uri, val path: String) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }

    suspend fun exportMusic(musicItem: MusicItem): ExportResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting export for: ${musicItem.title}")

            val fileName = "${musicItem.title} - ${musicItem.artist}.${getFileExtension(musicItem.mimeType)}"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                exportWithMediaStore(musicItem, fileName)
            } else {
                exportToExternalStorage(musicItem, fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            ExportResult.Error("Export failed: ${e.message}")
        }
    }

    private suspend fun exportWithMediaStore(
        musicItem: MusicItem,
        fileName: String
    ): ExportResult = withContext(Dispatchers.IO) {
        var pendingUri: Uri? = null
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, musicItem.mimeType)
                put(MediaStore.Audio.Media.TITLE, musicItem.title)
                put(MediaStore.Audio.Media.ARTIST, musicItem.artist)
                put(MediaStore.Audio.Media.ALBUM, musicItem.album)
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Exported")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues)
                ?: return@withContext ExportResult.Error("Failed to create export file")
            pendingUri = itemUri

            val tempFile = File.createTempFile(
                "export_",
                resolveSourceExtension(musicItem),
                context.cacheDir
            )

            try {
                val copied = resolver.openInputStream(musicItem.uri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    true
                } ?: false

                if (!copied) {
                    resolver.delete(itemUri, null, null)
                    return@withContext ExportResult.Error("Failed to read source audio file")
                }
                if (tempFile.length() <= 0L) {
                    resolver.delete(itemUri, null, null)
                    return@withContext ExportResult.Error("Failed to copy source audio content")
                }
                Log.d(TAG, "Copied source to temp: ${tempFile.name}, bytes=${tempFile.length()}")

                when (val result = metadataWriter.writeMetadata(musicItem, tempFile)) {
                    is MetadataWriter.WriteResult.Error -> {
                        Log.w(TAG, "Metadata write failed (non-fatal, exporting raw audio): ${result.message}")
                    }
                    MetadataWriter.WriteResult.Success -> {
                        Log.d(TAG, "Metadata written successfully")
                    }
                }

                val outputStream = resolver.openOutputStream(itemUri)
                if (outputStream == null) {
                    resolver.delete(itemUri, null, null)
                    return@withContext ExportResult.Error("Failed to write export file")
                }

                outputStream.use { stream ->
                    tempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(stream)
                    }
                }
            } finally {
                tempFile.delete()
            }

            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(itemUri, contentValues, null, null)
            pendingUri = null

            Log.d(TAG, "Export successful: $itemUri")
            ExportResult.Success(itemUri, Environment.DIRECTORY_MUSIC + "/Exported/$fileName")
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore export failed", e)
            pendingUri?.let { uri ->
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
            ExportResult.Error("Export failed: ${e.message}")
        }
    }

    private suspend fun exportToExternalStorage(
        musicItem: MusicItem,
        fileName: String
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val musicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "Exported"
            )

            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            val outputFile = File(musicDir, fileName)

            val copied = context.contentResolver.openInputStream(musicItem.uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                true
            } ?: false

            if (!copied) {
                return@withContext ExportResult.Error("Failed to read source audio file")
            }
            if (outputFile.length() <= 0L) {
                outputFile.delete()
                return@withContext ExportResult.Error("Failed to copy source audio content")
            }

            when (val result = metadataWriter.writeMetadata(musicItem, outputFile)) {
                is MetadataWriter.WriteResult.Error -> {
                    Log.w(TAG, "Metadata write failed (non-fatal, exporting raw audio): ${result.message}")
                }
                MetadataWriter.WriteResult.Success -> {
                    Log.d(TAG, "Metadata written successfully")
                }
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(outputFile.absolutePath),
                arrayOf(musicItem.mimeType),
                null
            )

            Log.d(TAG, "Export successful: ${outputFile.absolutePath}")
            ExportResult.Success(Uri.fromFile(outputFile), outputFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "External storage export failed", e)
            ExportResult.Error("Export failed: ${e.message}")
        }
    }

    private fun getFileExtension(mimeType: String): String {
        return when (mimeType) {
            "audio/mpeg" -> "mp3"
            "audio/mp4", "audio/x-m4a" -> "m4a"
            "audio/flac" -> "flac"
            "audio/ogg" -> "ogg"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/aac" -> "aac"
            else -> "mp3"
        }
    }

    private fun resolveSourceExtension(musicItem: MusicItem): String {
        val byDisplayName = extensionFromDisplayName(musicItem.uri)
        if (byDisplayName != null) {
            return byDisplayName
        }

        val resolverMime = context.contentResolver.getType(musicItem.uri)
        val byResolverMime = mimeTypeToExtension(resolverMime)
        if (byResolverMime != null) {
            return byResolverMime
        }

        val byMusicMime = mimeTypeToExtension(musicItem.mimeType)
        if (byMusicMime != null) {
            return byMusicMime
        }

        val pathSegment = musicItem.uri.lastPathSegment.orEmpty()
        val ext = pathSegment.substringAfterLast('.', "").lowercase()
        if (ext.isNotBlank()) {
            return ".$ext"
        }

        return ".mp3"
    }

    private fun extensionFromDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index < 0) {
                    return@use null
                }
                val displayName = cursor.getString(index).orEmpty()
                val ext = displayName.substringAfterLast('.', "").lowercase()
                if (ext.isBlank()) null else ".$ext"
            }
        }.getOrNull()
    }

    private fun mimeTypeToExtension(mimeType: String?): String? {
        return when (mimeType) {
            "audio/mpeg" -> ".mp3"
            "audio/mp4", "audio/x-m4a" -> ".m4a"
            "audio/flac" -> ".flac"
            "audio/ogg" -> ".ogg"
            "audio/wav", "audio/x-wav" -> ".wav"
            "audio/aac" -> ".aac"
            else -> null
        }
    }
}
