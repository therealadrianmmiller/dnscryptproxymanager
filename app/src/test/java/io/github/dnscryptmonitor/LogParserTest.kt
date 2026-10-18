package io.github.dnscryptmonitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogParserTest {
    @Test
    fun parsesExactDomain() {
        val result = LogParser.parse("[2025-02-11 09:10:11]\tblocked\ttelemetry.example.com")
        assertEquals("telemetry.example.com", result.single().domain)
        assertEquals(null, result.single().pattern)
        assertTrue(result.single().timestamp > 0)
    }

    @Test
    fun parsesAliasAndWildcardPattern() {
        val result = LogParser.parse(
            "[2025-02-11 09:10:11]\t*.example.com (alias for [api.example.com])\tignored",
        )
        assertEquals("api.example.com", result.single().domain)
        assertEquals("*.example.com", result.single().pattern)
    }

    @Test
    fun ignoresMalformedLines() {
        assertTrue(LogParser.parse("not a dnscrypt log line").isEmpty())
    }
}
