package com.yappay.app.export

import com.yappay.app.data.ExpenseEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class CsvExporterCustomCategoryTest {

    private fun expense(category: String) = ExpenseEntity(
        id = 1,
        amountMinor = 34000,
        description = "Cat food",
        category = category,
        spentAt = LocalDateTime.of(2026, 11, 17, 19, 5).toInstant(ZoneOffset.UTC).toEpochMilli(),
        createdAt = 0,
        rawTranscript = null,
        source = "MANUAL",
    )

    @Test
    fun customCategoryNameIsUsed() = assertEquals(
        CsvExporter.HEADER + "\n" + "2026-11-17,19:05,340.00,Cat food,Pet care,MANUAL\n",
        CsvExporter.buildCsv(
            expenses = listOf(expense("CUSTOM:3")),
            zone = ZoneOffset.UTC,
            customCategoryNames = mapOf("CUSTOM:3" to "Pet care"),
        ),
    )

    @Test
    fun deletedCustomCategoryFallsBackToOther() = assertEquals(
        CsvExporter.HEADER + "\n" + "2026-11-17,19:05,340.00,Cat food,Other,MANUAL\n",
        CsvExporter.buildCsv(listOf(expense("CUSTOM:9")), ZoneOffset.UTC),
    )
}
