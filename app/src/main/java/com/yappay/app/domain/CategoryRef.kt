package com.yappay.app.domain

import com.yappay.app.data.CategoryEntity

/**
 * A category as the UI sees it: one of the fixed [Category] values or a user-created
 * category saved in the `custom_categories` table.
 *
 * [key] is the exact string stored in `expenses.category`, e.g. "FOOD" or "CUSTOM:3".
 * Unknown / deleted keys fall back to [Category.OTHER] via [resolve].
 */
data class CategoryRef(
    val key: String,
    val label: String,
    /** ARGB colour of a user-created category, or null for a built-in one. */
    val color: Long? = null,
    /** Non-null only for the fixed [Category] values. */
    val fixed: Category? = null,
) {
    val isCustom: Boolean get() = fixed == null

    /** Numeric id inside "CUSTOM:12", or null for built-in keys. */
    fun customId(): Long? = key.removePrefix(CUSTOM_PREFIX).takeIf { it != key }?.toLongOrNull()

    companion object {
        const val CUSTOM_PREFIX = "CUSTOM:"
        const val MAX_NAME_LENGTH = 30

        /** Colours offered by the category editor (jewel tones from the Atelier palette). */
        val palette: List<Long> = listOf(
            0xFFB4623A,
            0xFFA04236,
            0xFF9E5574,
            0xFF77508A,
            0xFF66558A,
            0xFF485C9B,
            0xFF3E5D84,
            0xFF3B7479,
            0xFF5B7A4C,
            0xFF6F8148,
            0xFF9C7A36,
            0xFF95623F,
        )

        fun of(category: Category): CategoryRef =
            CategoryRef(key = category.name, label = category.displayName, fixed = category)

        fun custom(entity: CategoryEntity): CategoryRef = CategoryRef(
            key = "$CUSTOM_PREFIX${entity.id}",
            label = entity.name,
            color = entity.color,
        )

        /** Custom categories only, ready for [resolve] / [all]. */
        fun customs(entities: List<CategoryEntity>): List<CategoryRef> = entities.map(::custom)

        /** Every built-in category in declaration order, followed by the custom ones. */
        fun all(custom: List<CategoryRef> = emptyList()): List<CategoryRef> =
            Category.entries.map(::of) + custom

        /** Resolves the string stored in `expenses.category`. */
        fun resolve(stored: String?, custom: List<CategoryRef>): CategoryRef {
            val key = stored.orEmpty()
            custom.firstOrNull { it.key == key }?.let { return it }
            return Category.entries.firstOrNull { it.name == key }?.let(::of) ?: of(Category.OTHER)
        }

        /** Case-insensitive duplicate check used by the editor before saving. */
        fun isNameTaken(
            name: String,
            existing: List<CategoryRef>,
            exceptKey: String? = null,
        ): Boolean = existing.any { it.key != exceptKey && it.label.equals(name.trim(), ignoreCase = true) }
    }
}
