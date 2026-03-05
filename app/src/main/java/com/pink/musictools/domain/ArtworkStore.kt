package com.pink.musictools.domain

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ArtworkStore(private val context: Context) {

    companion object {
        private const val ARTWORK_DIR = "artwork"
    }

    suspend fun persistArtworkFromUri(sourceUri: Uri): Uri? = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(sourceUri)
        val extension = extensionFromMimeType(mimeType)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            persistArtworkStream(input, extension)
        }
    }

    suspend fun persistArtworkBytes(bytes: ByteArray, mimeType: String?): Uri? = withContext(Dispatchers.IO) {
        val extension = extensionFromMimeType(mimeType)
        val outputFile = createArtworkFile(extension)
        FileOutputStream(outputFile).use { output ->
            output.write(bytes)
        }
        Uri.fromFile(outputFile)
    }

    private fun persistArtworkStream(input: InputStream, extension: String): Uri {
        val outputFile = createArtworkFile(extension)
        FileOutputStream(outputFile).use { output ->
            input.copyTo(output)
        }
        return Uri.fromFile(outputFile)
    }

    private fun createArtworkFile(extension: String): File {
        val folder = File(context.filesDir, ARTWORK_DIR)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return File(folder, "art_${UUID.randomUUID()}.$extension")
    }

    private fun extensionFromMimeType(mimeType: String?): String {
        return when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }
}
