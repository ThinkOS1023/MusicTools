package com.pink.musictools.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val hasUpdate: Boolean
)

object UpdateChecker {

    private const val RELEASES_API =
        "https://api.github.com/repos/ThinkOS1023/MusicTools/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(currentVersion: String): Result<UpdateInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(RELEASES_API)
                    .header("Accept", "application/vnd.github+json")
                    .build()

                val body = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    response.body?.string() ?: error("Empty response")
                }

                val tagName = extractJsonString(body, "tag_name")
                    ?.trimStart('v', 'V')
                    ?: error("Missing tag_name")
                val htmlUrl = extractJsonString(body, "html_url") ?: ""
                val releaseNotes = extractJsonString(body, "body") ?: ""

                UpdateInfo(
                    latestVersion = tagName,
                    releaseUrl = htmlUrl,
                    releaseNotes = releaseNotes,
                    hasUpdate = isNewerVersion(tagName, currentVersion.trimStart('v', 'V'))
                )
            }
        }

    /** Simple semantic version comparison: returns true if candidate > current. */
    private fun isNewerVersion(candidate: String, current: String): Boolean {
        val cParts = candidate.split(".").mapNotNull { it.toIntOrNull() }
        val rParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(cParts.size, rParts.size)
        for (i in 0 until maxLen) {
            val c = cParts.getOrElse(i) { 0 }
            val r = rParts.getOrElse(i) { 0 }
            if (c > r) return true
            if (c < r) return false
        }
        return false
    }

    /** Minimal JSON string extractor — avoids a full JSON library dependency. */
    private fun extractJsonString(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        return pattern.find(json)?.groupValues?.getOrNull(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
    }
}
