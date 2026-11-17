package com.yappay.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** [startInclusive, endExclusive) in epoch milliseconds. Use exactly these bounds in DAO queries. */
data class MillisRange(val startInclusive: Long, val endExclusive: Long)

/**
 * All timestamps in the DB are epoch millis (UTC instants).
 * Convert to the user's local day/month ONLY here and in UI formatting.
 * java.time is available natively because minSdk = 26.
 */
object DateRanges {

    fun day(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): MillisRange = MillisRange(
        date.atStartOfDay(zone).toInstant().toEpochMilli(),
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
    )

    fun month(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): MillisRange = MillisRange(
        month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
    )

    fun toLocalDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    /**
     * When the user picks a different DATE for an expense, keep the original time-of-day.
     * Used by the Add/Edit screen.
     */
    fun withDate(epochMillis: Long, newDate: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
        val time = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
        return newDate.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * Material3 DatePicker works in UTC-midnight millis, NOT local millis.
     * ALWAYS convert with these two functions when talking to DatePickerState.
     */
    fun toPickerMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromPickerMillis(utcMillis: Long): LocalDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    /** "Tue, 17 Nov" */
    val DAY_HEADER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

    /** "November 2026" */
    val MONTH_TITLE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    /** "7:05 PM" */
    val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    /** "2026-11-17" — used in CSV export. */
    val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** "19:05" — used in CSV export. */
    val ISO_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
}
