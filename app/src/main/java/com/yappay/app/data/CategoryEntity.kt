package com.yappay.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-created category. The 13 built-in categories live in the [com.yappay.app.domain.Category]
 * enum and are NOT stored here.
 *
 * name  : Trimmed, 1..30 characters, unique (case-insensitive via the unique index).
 * color : ARGB colour, e.g. 0xFFB4623A. Stored as Long so the value can never flip negative.
 *
 * Expenses reference it as "CUSTOM:<id>" in `expenses.category`, so renaming is safe.
 */
@Entity(
    tableName = "custom_categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color") val color: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
