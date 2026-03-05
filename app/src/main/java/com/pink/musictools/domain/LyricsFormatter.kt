package com.pink.musictools.domain

object LyricsFormatter {

    private val timeTagRegex = Regex("""\[(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val metadataTagRegex = Regex("""^\[(ti|ar|al|by|offset):.*]$""", RegexOption.IGNORE_CASE)

    fun formatTimestamp(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0L)
        val minutes = safeMillis / 60_000
        val seconds = (safeMillis % 60_000) / 1_000
        val milliseconds = safeMillis % 1_000
        return "[%02d:%02d.%03d]".format(minutes, seconds, milliseconds)
    }

    fun parseFirstTimestampMillis(line: String): Long? {
        val match = timeTagRegex.find(line) ?: return null
        return parseMillis(match)
    }

    fun stripLeadingTimeTags(line: String): String {
        return line.replace(Regex("""^(?:\[\d{1,2}:\d{1,2}(?:[.:]\d{1,3})?])+"""), "")
            .trimStart()
    }

    fun replaceOrPrependTimestamp(line: String, millis: Long): String {
        val lyric = stripLeadingTimeTags(line)
        return "${formatTimestamp(millis)}$lyric"
    }

    fun shiftTimestamp(line: String, offsetMillis: Long): String {
        val current = parseFirstTimestampMillis(line) ?: return line
        return replaceOrPrependTimestamp(line, current + offsetMillis)
    }

    fun normalizeToLrc(
        rawLyrics: String,
        includeMetadataTags: Boolean = false,
        title: String? = null,
        artist: String? = null,
        album: String? = null
    ): String {
        val normalizedText = rawLyrics.replace("\r\n", "\n").replace("\r", "\n")
        val lines = normalizedText.lines()

        val metadataLines = linkedSetOf<String>()
        val timedLines = mutableListOf<Pair<Long, String>>()
        val plainLines = mutableListOf<String>()

        lines.forEach { originalLine ->
            val line = originalLine.trimEnd()
            if (line.isBlank()) {
                return@forEach
            }

            if (metadataTagRegex.matches(line)) {
                metadataLines += line
                return@forEach
            }

            val timeTags = timeTagRegex.findAll(line).toList()
            if (timeTags.isEmpty()) {
                plainLines += line
                return@forEach
            }

            val lyricText = line.replace(timeTagRegex, "").trimStart()
            timeTags.forEach { tagMatch ->
                val millis = parseMillis(tagMatch)
                timedLines += millis to "${formatTimestamp(millis)}$lyricText"
            }
        }

        val result = mutableListOf<String>()

        if (includeMetadataTags) {
            if (!title.isNullOrBlank()) {
                metadataLines += "[ti:${title.trim()}]"
            }
            if (!artist.isNullOrBlank()) {
                metadataLines += "[ar:${artist.trim()}]"
            }
            if (!album.isNullOrBlank()) {
                metadataLines += "[al:${album.trim()}]"
            }
        }

        result += metadataLines

        timedLines
            .sortedBy { it.first }
            .mapTo(result) { it.second }

        result += plainLines

        return result.joinToString("\n")
    }

    /**
     * Parse an LRC string into a sorted list of (timestampMillis, lyricText) pairs.
     * Metadata tags (ti/ar/al…) and blank lines are skipped.
     * Lines with multiple timestamps are expanded into separate entries.
     */
    fun parseTimed(lyricsText: String): List<Pair<Long, String>> {
        return lyricsText.lines()
            .filter { it.isNotBlank() && !metadataTagRegex.matches(it.trim()) }
            .flatMap { line ->
                val text = stripLeadingTimeTags(line)
                if (text.isBlank()) emptyList()
                else timeTagRegex.findAll(line).map { parseMillis(it) to text }.toList()
            }
            .sortedBy { it.first }
    }

    private fun parseMillis(match: MatchResult): Long {
        val minutes = match.groupValues[1].toLongOrNull() ?: 0L
        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
        val fraction = match.groupValues.getOrNull(3).orEmpty()

        val fractionMillis = when (fraction.length) {
            0 -> 0L
            1 -> (fraction.toLongOrNull() ?: 0L) * 100L
            2 -> (fraction.toLongOrNull() ?: 0L) * 10L
            else -> fraction.take(3).toLongOrNull() ?: 0L
        }

        return minutes * 60_000L + seconds * 1_000L + fractionMillis
    }
}
