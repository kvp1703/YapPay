package com.yappay.app.export

import com.yappay.app.data.ExpenseEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class CsvExporterTest {

    private fun expense(amountMinor: Long, description: String, category: String, source: String) = ExpenseEntity(
        id = 1,
        amountMinor = amountMinor,
        description = description,
        category = category,
        spentAt = LocalDateTime.of(2026, 11, 17, 19, 5).toInstant(ZoneOffset.UTC).toEpochMilli(),
        createdAt = 0,
        rawTranscript = null,
        source = source,
    )

    @Test fun headerOnlyWhenEmpty() = assertEquals(CsvExporter.HEADER + "\n", CsvExporter.buildCsv(emptyList(), ZoneOffset.UTC))

    @Test fun oneRow() = assertEquals(
        CsvExporter.HEADER + "\n" + "2026-11-17,19:05,340.00,Pizza,Food & Dining,VOICE\n",
        CsvExporter.buildCsv(listOf(expense(34000, "Pizza", "FOOD", "VOICE")), ZoneOffset.UTC),
    )

    @Test fun unknownCategoryBecomesOther() = assertEquals(
        CsvExporter.HEADER + "\n" + "2026-11-17,19:05,0.05,Gum,Other,MANUAL\n",
        CsvExporter.buildCsv(listOf(expense(5, "Gum", "GONE", "MANUAL")), ZoneOffset.UTC),
    )

    @Test fun plainValueNotQuoted() = assertEquals("Pizza", CsvExporter.escape("Pizza"))
    @Test fun commaIsQuoted() = assertEquals("\"Pizza, coke\"", CsvExporter.escape("Pizza, coke"))
    @Test fun quoteIsDoubled() = assertEquals("\"5\"\" pipe\"", CsvExporter.escape("5\" pipe"))
    @Test fun newlineIsQuoted() = assertEquals("\"a\nb\"", CsvExporter.escape("a\nb"))
}
