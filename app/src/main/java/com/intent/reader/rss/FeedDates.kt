package com.intent.reader.rss

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

/** Tolerant RFC-822 / ISO-8601 date parsing for feed items. */
object FeedDates {

    private val rfc822Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
        "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH
    )
    private val rfc822NoSeconds: DateTimeFormatter = DateTimeFormatter.ofPattern(
        "EEE, dd MMM yyyy HH:mm Z", Locale.ENGLISH
    )

    fun parse(raw: String): Long {
        val v = raw.trim()
        if (v.isEmpty()) return 0L
        // RFC 822: "Tue, 10 Jun 2003 04:00:00 GMT"
        try {
            return ZonedDateTime.parse(v, rfc822Formatter).toInstant().toEpochMilli()
        } catch (_: Exception) { }
        try {
            return ZonedDateTime.parse(v, rfc822NoSeconds).toInstant().toEpochMilli()
        } catch (_: Exception) { }
        // ISO 8601: 2024-01-01T00:00:00Z / +08:00
        try {
            return OffsetDateTime.parse(v).toInstant().toEpochMilli()
        } catch (_: Exception) { }
        try {
            return ZonedDateTime.parse(v).toInstant().toEpochMilli()
        } catch (_: Exception) { }
        // No timezone: treat as local
        try {
            return LocalDateTime.parse(v.take(19)).atZone(TimeZone.getDefault().toZoneId())
                .toInstant().toEpochMilli()
        } catch (_: Exception) { }
        return 0L
    }
}
