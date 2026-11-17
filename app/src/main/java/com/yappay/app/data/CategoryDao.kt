package com.yappay.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM custom_categories ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM custom_categories ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM custom_categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM custom_categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)
}
