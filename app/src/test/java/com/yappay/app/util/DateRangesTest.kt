package com.yappay.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

class DateRangesTest {

    private val utc = ZoneOffset.UTC
    private val kolkata = ZoneId.of("Asia/Kolkata")

    private fun millis(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0, zone: ZoneId = utc) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    @Test fun monthRangeUtc() = assertEquals(
        MillisRange(millis(2026, 2, 1), millis(2026, 3, 1)),
        DateRanges.month(YearMonth.of(2026, 2), utc),
    )

    @Test fun decemberRollsIntoNextYear() = assertEquals(
        MillisRange(millis(2026, 12, 1), millis(2027, 1, 1)),
        DateRanges.month(YearMonth.of(2026, 12), utc),
    )

    @Test fun dayRangeInIndia() = assertEquals(
        MillisRange(millis(2026, 11, 17, zone = kolkata), millis(2026, 11, 18, zone = kolkata)),
        DateRanges.day(LocalDate.of(2026, 11, 17), kolkata),
    )

    @Test fun toLocalDateUsesZone() {
        // 2026-11-16 20:00 UTC is already 2026-11-17 01:30 in India.
        val m = millis(2026, 11, 16, 20, 0)
        assertEquals(LocalDate.of(2026, 11, 16), DateRanges.toLocalDate(m, utc))
        assertEquals(LocalDate.of(2026, 11, 17), DateRanges.toLocalDate(m, kolkata))
    }

    @Test fun withDateKeepsTimeOfDay() = assertEquals(
        millis(2026, 11, 10, 19, 5, kolkata),
        DateRanges.withDate(millis(2026, 11, 17, 19, 5, kolkata), LocalDate.of(2026, 11, 10), kolkata),
    )

    @Test fun pickerRoundTrip() {
        val d = LocalDate.of(2026, 11, 17)
        assertEquals(d, DateRanges.fromPickerMillis(DateRanges.toPickerMillis(d)))
        assertEquals(millis(2026, 11, 17), DateRanges.toPickerMillis(d))
    }
}
