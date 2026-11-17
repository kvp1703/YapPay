package com.yappay.app.util

import java.math.BigDecimal

enum class GroupingStyle {
    /** 1,20,000 */
    INDIAN,

    /** 120,000 */
    INTERNATIONAL,
}

/**
 * All money in the app is a Long of MINOR units (paise / cents). 340 rupees == 34000L.
 * NEVER use Float or Double for money anywhere in the app.
 * PURE KOTLIN. No Android imports. Deliberately does NOT use java.text.NumberFormat so that
 * output is identical on every device and in JVM unit tests.
 */
object MoneyFormatter {

    /** "340." is accepted so the field does not flash an error while the user is still typing. */
    private val USER_INPUT = Regex("""^\d+(\.\d{0,2})?$""")
    private const val MAX_MINOR = 100_000_000L * 100L

    /**
     * format(34000, "₹", INDIAN)      -> "₹340"
     * format(1250, "₹", INDIAN)       -> "₹12.50"
     * format(12000000, "₹", INDIAN)   -> "₹1,20,000"
     * format(12000000, "$", INTERNATIONAL) -> "$120,000"
     * format(34000, "₹", INDIAN, alwaysShowDecimals = true) -> "₹340.00"
     */
    fun format(
        amountMinor: Long,
        currencySymbol: String,
        grouping: GroupingStyle,
        alwaysShowDecimals: Boolean = false,
    ): String {
        val negative = amountMinor < 0
        val abs = if (negative) -amountMinor else amountMinor
        val major = abs / 100
        val minor = abs % 100
        val grouped = group(major.toString(), grouping)
        val fraction = if (minor != 0L || alwaysShowDecimals) "." + minor.toString().padStart(2, '0') else ""
        return (if (negative) "-" else "") + currencySymbol + grouped + fraction
    }

    /**
     * Parses what the user typed in the amount text field.
     * Accepts "340", "340.", "340.5", "340.50", "1,200.75", " 99 ". Commas are ignored.
     * Returns null for: empty, letters, negative, more than 2 decimals, zero, > 10 crore.
     */
    fun parseUserInput(text: String): Long? {
        val cleaned = text.trim().replace(",", "")
        if (!USER_INPUT.matches(cleaned)) return null
        val minor = BigDecimal(cleaned).movePointRight(2).toLong()
        if (minor <= 0L || minor > MAX_MINOR) return null
        return minor
    }

    /** Value to pre-fill the amount text field with. 34000 -> "340", 1250 -> "12.50", 1205 -> "12.05". */
    fun toInputText(amountMinor: Long): String {
        val major = amountMinor / 100
        val minor = amountMinor % 100
        return if (minor == 0L) major.toString() else "$major.${minor.toString().padStart(2, '0')}"
    }

    private fun group(digits: String, style: GroupingStyle): String {
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val size = if (style == GroupingStyle.INDIAN) 2 else 3
        val parts = rest.reversed().chunked(size).map { it.reversed() }.reversed()
        return parts.joinToString(",") + "," + last3
    }
}
