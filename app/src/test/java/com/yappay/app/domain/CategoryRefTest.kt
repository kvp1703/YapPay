package com.yappay.app.domain

import com.yappay.app.data.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRefTest {

    private fun custom(id: Long, name: String = "Pet care", color: Long = 0xFFB4623A) =
        CategoryRef.custom(CategoryEntity(id = id, name = name, color = color))

    @Test
    fun builtInsComeFirstThenCustoms() {
        val all = CategoryRef.all(listOf(custom(3)))
        assertEquals(Category.entries.size + 1, all.size)
        assertEquals(Category.FOOD.name, all.first().key)
        assertEquals("Pet care", all.last().label)
        assertTrue(all.last().isCustom)
        assertFalse(all.first().isCustom)
    }

    @Test
    fun resolvesCustomKey() {
        val ref = custom(7, name = "Plants")
        val resolved = CategoryRef.resolve(ref.key, listOf(ref))
        assertEquals("Plants", resolved.label)
        assertEquals(ref, resolved)
    }

    @Test
    fun resolvesBuiltInKey() {
        val resolved = CategoryRef.resolve("GROCERIES", emptyList())
        assertEquals(Category.GROCERIES.displayName, resolved.label)
        assertFalse(resolved.isCustom)
    }

    @Test
    fun unknownKeyFallsBackToOther() {
        val resolved = CategoryRef.resolve("CUSTOM:99", emptyList())
        assertEquals(Category.OTHER.name, resolved.key)
        assertEquals("OTHER", resolved.key)
    }

    @Test
    fun nullKeyFallsBackToOther() {
        assertEquals(Category.OTHER.name, CategoryRef.resolve(null, emptyList()).key)
    }

    @Test
    fun customIdParsesPrefixedKeys() {
        assertEquals(12L, custom(12).customId())
        assertNull(CategoryRef.of(Category.FOOD).customId())
        assertNull(CategoryRef.resolve("GONE", emptyList()).customId())
    }

    @Test
    fun duplicateNamesIgnoreCaseAndSelf() {
        val existing = listOf(custom(1, name = "Pet care"))
        assertTrue(CategoryRef.isNameTaken("pet care", existing))
        assertTrue(CategoryRef.isNameTaken("  PET CARE  ", existing))
        assertFalse(CategoryRef.isNameTaken("pet care", existing, exceptKey = "CUSTOM:1"))
        assertFalse(CategoryRef.isNameTaken("Plants", existing))
    }
}
