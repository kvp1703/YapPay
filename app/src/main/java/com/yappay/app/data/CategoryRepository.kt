package com.yappay.app.data

import com.yappay.app.domain.CategoryRef
import kotlinx.coroutines.flow.Flow

/**
 * The only class ViewModels use for custom categories. Validation lives here so every
 * screen (Add/Edit quick-create and the Categories screen) behaves the same way.
 */
class CategoryRepository(private val dao: CategoryDao) {

    fun observeAll(): Flow<List<CategoryEntity>> = dao.observeAll()

    suspend fun getAll(): List<CategoryEntity> = dao.getAll()

    suspend fun getById(id: Long): CategoryEntity? = dao.getById(id)

    /** @throws IllegalArgumentException when the name is blank, too long or already taken. */
    suspend fun create(
        name: String,
        color: Long,
        now: Long = System.currentTimeMillis(),
    ): CategoryEntity {
        val clean = cleanName(name)
        require(clean.isNotEmpty()) { "Enter a category name" }
        require(isValidColor(color)) { "Pick a colour" }
        require(dao.findByName(clean) == null) { "A category named \"$clean\" already exists" }
        val entity = CategoryEntity(name = clean, color = color, createdAt = now)
        return entity.copy(id = dao.insert(entity))
    }

    /** @throws IllegalArgumentException when the new name is blank or belongs to another category. */
    suspend fun update(entity: CategoryEntity, name: String, color: Long) {
        val clean = cleanName(name)
        require(clean.isNotEmpty()) { "Enter a category name" }
        require(isValidColor(color)) { "Pick a colour" }
        require(dao.findByName(clean)?.let { it.id != entity.id } != true) {
            "A category named \"$clean\" already exists"
        }
        dao.update(entity.copy(name = clean, color = color))
    }

    suspend fun delete(entity: CategoryEntity) = dao.delete(entity)

    /** "CUSTOM:<id>" -> display name, for CSV export and any one-off lookup. */
    suspend fun displayNames(): Map<String, String> =
        dao.getAll().associate { "${CategoryRef.CUSTOM_PREFIX}${it.id}" to it.name }

    companion object {
        fun cleanName(name: String): String =
            name.trim().replace(Regex("\\s+"), " ").take(CategoryRef.MAX_NAME_LENGTH)

        private fun isValidColor(color: Long): Boolean = color in 0xFF000000L..0xFFFFFFFFL
    }
}
