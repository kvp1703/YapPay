package com.yappay.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per expense.
 *
 * amountMinor   : Long, minor units (paise/cents). 340 rupees == 34000. Always > 0.
 * description   : What the money was for, e.g. "Pizza". Never blank.
 * category      : Category enum NAME, e.g. "FOOD". Read it back with Category.fromName(...).
 * spentAt       : Epoch millis of when the money was spent (user can change the date).
 * createdAt     : Epoch millis when the row was inserted. Never changes.
 * rawTranscript : The exact speech text that produced this row, or null for manual entries.
 * source        : "VOICE" or "MANUAL" (see ExpenseSource).
 */
@Entity(
    tableName = "expenses",
    indices = [Index(value = ["spent_at"])],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "spent_at") val spentAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "raw_transcript") val rawTranscript: String?,
    @ColumnInfo(name = "source") val source: String,
)

object ExpenseSource {
    const val VOICE = "VOICE"
    const val MANUAL = "MANUAL"
}

/** Result row for the "total per category" query. Column names MUST match the SQL aliases. */
data class CategoryTotal(
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "total") val total: Long,
)
