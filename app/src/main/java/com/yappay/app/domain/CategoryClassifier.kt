package com.yappay.app.domain

import java.util.Locale

/**
 * Guesses a Category from a free-text description using Category.keywords.
 * PURE KOTLIN. No Android imports.
 *
 * Rules:
 *  1. Lowercase the description, replace every non-letter/non-digit with a space, collapse spaces.
 *  2. Walk Category.entries in declaration order; inside each category walk keywords in order.
 *  3. A keyword WITH a space ("phone bill") matches if the normalized text contains it as whole words.
 *  4. A keyword WITHOUT a space matches if any word equals the keyword, keyword+"s" or keyword+"es".
 *  5. First match wins. No match -> Category.OTHER.
 */
object CategoryClassifier {

    private val NON_ALNUM = Regex("""[^\p{L}\p{M}\p{N}]+""")

    fun classify(description: String): Category {
        val normalized = NON_ALNUM.replace(description.lowercase(Locale.ROOT), " ").trim()
        if (normalized.isEmpty()) return Category.OTHER
        val words = normalized.split(" ").toSet()
        val padded = " $normalized "

        for (category in Category.entries) {
            for (keyword in category.keywords) {
                val matched = if (' ' in keyword) {
                    padded.contains(" $keyword ")
                } else {
                    keyword in words || "${keyword}s" in words || "${keyword}es" in words
                }
                if (matched) return category
            }
        }
        return Category.OTHER
    }
}
