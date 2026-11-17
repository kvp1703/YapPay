package com.yappay.app.data

import com.yappay.app.util.MillisRange
import kotlinx.coroutines.flow.Flow

/**
 * The ONLY class ViewModels talk to for expense data. ViewModels never touch ExpenseDao directly.
 * Validation lives here so that both voice and manual paths share it.
 */
class ExpenseRepository(private val dao: ExpenseDao) {

    fun observeExpenses(range: MillisRange): Flow<List<ExpenseEntity>> =
        dao.observeBetween(range.startInclusive, range.endExclusive)

    fun observeTotal(range: MillisRange): Flow<Long> =
        dao.observeTotalBetween(range.startInclusive, range.endExclusive)

    /** One-shot total, used by the home-screen widget (no Flow collection while the app is dead). */
    suspend fun total(range: MillisRange): Long =
        dao.totalBetween(range.startInclusive, range.endExclusive)

    fun observeCategoryTotals(range: MillisRange): Flow<List<CategoryTotal>> =
        dao.observeCategoryTotals(range.startInclusive, range.endExclusive)

    suspend fun getById(id: Long): ExpenseEntity? = dao.getById(id)

    suspend fun getAll(): List<ExpenseEntity> = dao.getAll()

    /** Returns the new row id. */
    suspend fun add(
        amountMinor: Long,
        description: String,
        category: String,
        spentAt: Long,
        rawTranscript: String?,
        source: String,
        now: Long = System.currentTimeMillis(),
    ): Long {
        require(amountMinor > 0) { "amountMinor must be > 0" }
        require(description.isNotBlank()) { "description must not be blank" }
        return dao.insert(
            ExpenseEntity(
                amountMinor = amountMinor,
                description = description.trim(),
                category = category,
                spentAt = spentAt,
                createdAt = now,
                rawTranscript = rawTranscript,
                source = source,
            ),
        )
    }

    suspend fun update(expense: ExpenseEntity) {
        require(expense.id != 0L) { "cannot update an expense without id" }
        require(expense.amountMinor > 0) { "amountMinor must be > 0" }
        require(expense.description.isNotBlank()) { "description must not be blank" }
        dao.update(expense.copy(description = expense.description.trim()))
    }

    suspend fun delete(expense: ExpenseEntity) = dao.delete(expense)

    /** Used when a custom category is deleted. */
    suspend fun reassignCategory(from: String, to: String) = dao.reassignCategory(from, to)

    /** Used by the "Undo" snackbar after a delete. Re-inserts with the SAME id. */
    suspend fun restore(expense: ExpenseEntity) {
        dao.insert(expense)
    }
}
