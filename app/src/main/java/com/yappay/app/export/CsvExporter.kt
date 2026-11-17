package com.yappay.app.export

import com.yappay.app.data.ExpenseEntity
import com.yappay.app.domain.Category
import com.yappay.app.util.DateRanges
import java.time.Instant
import java.time.ZoneId

/**
 * Builds the CSV text. Writing it to a file is done in the Settings screen with
 * ActivityResultContracts.CreateDocument("text/csv") + contentResolver.openOutputStream(uri).
 *
 * Format (header + one line per expense, "\n" line endings, UTF-8):
 *   date,time,amount,description,category,source
 *   2026-11-17,19:05,340.00,Pizza,Food & Dining,VOICE
 */
object CsvExporter {

    const val HEADER = "date,time,amount,description,category,source"

    fun buildCsv(
        expenses: List<ExpenseEntity>,
        zone: ZoneId = ZoneId.systemDefault(),
        customCategoryNames: Map<String, String> = emptyMap(),
    ): String {
        val sb = StringBuilder(HEADER).append('\n')
        for (e in expenses) {
            val dateTime = Instant.ofEpochMilli(e.spentAt).atZone(zone)
            sb.append(DateRanges.ISO_DATE.format(dateTime)).append(',')
                .append(DateRanges.ISO_TIME.format(dateTime)).append(',')
                .append(formatAmount(e.amountMinor)).append(',')
                .append(escape(e.description)).append(',')
                .append(escape(customCategoryNames[e.category] ?: Category.fromName(e.category).displayName)).append(',')
                .append(e.source).append('\n')
        }
        return sb.toString()
    }

    /** 34000 -> "340.00". Always 2 decimals, no grouping, no currency symbol (spreadsheet friendly). */
    internal fun formatAmount(amountMinor: Long): String =
        "${amountMinor / 100}.${(amountMinor % 100).toString().padStart(2, '0')}"

    /** RFC 4180: wrap in quotes if the value contains comma, quote, CR or LF; double inner quotes. */
    internal fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
