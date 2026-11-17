package com.yappay.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * All range parameters are [start, end) epoch millis -> use DateRanges.day()/month().
 * Flow-returning functions are NOT suspend. One-shot functions ARE suspend.
 */
@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query(
        "SELECT * FROM expenses WHERE spent_at >= :start AND spent_at < :end " +
            "ORDER BY spent_at DESC, id DESC",
    )
    fun observeBetween(start: Long, end: Long): Flow<List<ExpenseEntity>>

    /** COALESCE makes an empty range return 0 instead of null. */
    @Query("SELECT COALESCE(SUM(amount_minor), 0) FROM expenses WHERE spent_at >= :start AND spent_at < :end")
    fun observeTotalBetween(start: Long, end: Long): Flow<Long>

    /** One-shot variant used by the home-screen widget. */
    @Query("SELECT COALESCE(SUM(amount_minor), 0) FROM expenses WHERE spent_at >= :start AND spent_at < :end")
    suspend fun totalBetween(start: Long, end: Long): Long

    @Query(
        "SELECT category AS category, SUM(amount_minor) AS total FROM expenses " +
            "WHERE spent_at >= :start AND spent_at < :end " +
            "GROUP BY category ORDER BY total DESC",
    )
    fun observeCategoryTotals(start: Long, end: Long): Flow<List<CategoryTotal>>

    /** Used by CSV export. Oldest first. */
    @Query("SELECT * FROM expenses ORDER BY spent_at ASC, id ASC")
    suspend fun getAll(): List<ExpenseEntity>

    /** Used when a custom category is deleted: its expenses move to the replacement category. */
    @Query("UPDATE expenses SET category = :to WHERE category = :from")
    suspend fun reassignCategory(from: String, to: String)
}
