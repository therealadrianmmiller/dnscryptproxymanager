package io.github.dnscryptmonitor

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class BlockedDomain(
    val domain: String,
    val pattern: String? = null,
    val timestamp: Long = 0,
)

object LogParser {
    private val patternRegex = Regex("""\t([^\t]*\*[^\t]*?)(?:\s*\(alias for\s+\[[^\]]*]))?(?:\t|$)""")
    private val aliasRegex = Regex("""\s\(alias for\s+\[([^\]]+)])""")
    private val timestampRegex = Regex("""^\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})]""")
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun parse(line: String): List<BlockedDomain> {
        val pattern = patternRegex.find(line)?.groupValues?.get(1)?.trim()
        val timestamp = timestampRegex.find(line.trim())?.groupValues?.get(1)
            ?.let { runCatching { LocalDateTime.parse(it, timestampFormat).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull() }
            ?: 0
        val aliases = aliasRegex.findAll(line).map { it.groupValues[1].trim() }.toList()
        if (aliases.isNotEmpty()) {
            return aliases.map { BlockedDomain(it, pattern, timestamp) }
        }
        val parts = line.trim().split('\t')
        if (parts.size < 3) return emptyList()
        val domain = parts[2].trim()
        return if (domain.contains('.') && !domain.contains(' ')) {
            listOf(BlockedDomain(domain, pattern, timestamp))
        } else {
            emptyList()
        }
    }
}
