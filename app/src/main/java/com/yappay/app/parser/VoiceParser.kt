package com.yappay.app.parser

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Turns a speech transcript such as "340 pizza" or "Rs. 1,200 for groceries"
 * into an amount (in minor units, e.g. paise/cents) and a description.
 *
 * PURE KOTLIN. No Android imports. Fully unit-testable on the JVM.
 * See VOICE_PARSER_SPEC.md for the full rule list.
 */
sealed interface ParseResult {
    data class Success(val amountMinor: Long, val description: String) : ParseResult
    data class Failure(val reason: FailureReason) : ParseResult
}

enum class FailureReason { EMPTY_INPUT, NO_AMOUNT, ZERO_AMOUNT, AMOUNT_TOO_LARGE }

object VoiceParser {

    /** Largest accepted amount in MAJOR units (e.g. rupees). */
    private val MAX_AMOUNT_MAJOR = BigDecimal("100000000") // 10 crore / 100 million

    private const val MAX_DESCRIPTION_LENGTH = 100

    private val SMALL_NUMBERS = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
        "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
        "nineteen" to 19, "twenty" to 20, "thirty" to 30, "forty" to 40,
        "fifty" to 50, "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90,
    )

    private const val HUNDRED = "hundred"

    /** Scale words that close a group (added to the running total). */
    private val BIG_SCALES = mapOf(
        "thousand" to BigDecimal("1000"),
        "k" to BigDecimal("1000"),
        "lakh" to BigDecimal("100000"),
        "lakhs" to BigDecimal("100000"),
        "lac" to BigDecimal("100000"),
        "lacs" to BigDecimal("100000"),
        "million" to BigDecimal("1000000"),
        "crore" to BigDecimal("10000000"),
        "crores" to BigDecimal("10000000"),
    )

    /** Words removed from the START and END of the description (never from the middle). */
    private val FILLER_WORDS = setOf(
        "for", "on", "at", "to", "in", "of", "the", "a", "an", "and", "only",
        "spent", "spend", "paid", "pay", "gave", "bought", "buy",
        "rupees", "rupee", "rs", "inr", "dollars", "dollar", "usd", "bucks",
        "euros", "euro", "eur", "pounds", "pound", "gbp",
    )

    private val NUMERIC_TOKEN = Regex("""^\d+(\.\d+)?$""")
    private val COMMA_BETWEEN_DIGITS = Regex("""(?<=\d),(?=\d)""")
    private val CURRENCY_SYMBOLS = Regex("""[₹$€£]""")
    private val APOSTROPHES = Regex("""['’]""")
    // \p{M} keeps combining marks so Devanagari words (e.g. "पिज़्ज़ा") are not broken.
    private val NOT_LETTER_DIGIT_DOT_SPACE = Regex("""[^\p{L}\p{M}\p{N}.\s]""")
    private val LONELY_DOT = Regex("""(?<!\d)\.|\.(?!\d)""")
    private val DIGIT_LETTER_BOUNDARY = Regex("""(?<=\d)(?=\p{L})|(?<=\p{L})(?=\d)""")
    private val WHITESPACE = Regex("""\s+""")

    /** Tries every alternative from the speech recognizer, returns the first Success. */
    fun parseBest(alternatives: List<String>): ParseResult {
        if (alternatives.isEmpty()) return ParseResult.Failure(FailureReason.EMPTY_INPUT)
        val results = alternatives.map { parse(it) }
        return results.firstOrNull { it is ParseResult.Success } ?: results.first()
    }

    fun parse(input: String): ParseResult {
        val tokens = tokenize(input)
        if (tokens.isEmpty()) return ParseResult.Failure(FailureReason.EMPTY_INPUT)

        val run = findAmountRun(tokens) ?: return ParseResult.Failure(FailureReason.NO_AMOUNT)

        val amountMajor = run.value
        if (amountMajor > MAX_AMOUNT_MAJOR) return ParseResult.Failure(FailureReason.AMOUNT_TOO_LARGE)

        val amountMinor = amountMajor
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact()
        if (amountMinor == 0L) return ParseResult.Failure(FailureReason.ZERO_AMOUNT)

        val remaining = tokens.subList(0, run.start) + tokens.subList(run.endExclusive, tokens.size)
        return ParseResult.Success(amountMinor, buildDescription(remaining))
    }

    // ---------------------------------------------------------------- tokenize

    internal fun tokenize(input: String): List<String> {
        var s = input.lowercase(Locale.ROOT)
        s = convertDevanagariDigits(s)
        s = COMMA_BETWEEN_DIGITS.replace(s, "")
        s = CURRENCY_SYMBOLS.replace(s, " ")
        s = APOSTROPHES.replace(s, "")
        s = NOT_LETTER_DIGIT_DOT_SPACE.replace(s, " ")
        s = LONELY_DOT.replace(s, " ")
        s = DIGIT_LETTER_BOUNDARY.replace(s, " ")
        return s.trim().split(WHITESPACE).filter { it.isNotEmpty() }
    }

    private fun convertDevanagariDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            sb.append(if (c in '\u0966'..'\u096F') ('0' + (c - '\u0966')) else c)
        }
        return sb.toString()
    }

    // ------------------------------------------------------------ amount runs

    private data class NumberRun(val start: Int, val endExclusive: Int, val value: BigDecimal)

    /**
     * Rule: the first run that STARTS WITH A DIGIT token wins.
     * If there is no digit token at all, the first run of number WORDS wins.
     */
    private fun findAmountRun(tokens: List<String>): NumberRun? {
        for (i in tokens.indices) {
            if (NUMERIC_TOKEN.matches(tokens[i])) return parseRunAt(tokens, i)
        }
        for (i in tokens.indices) {
            val run = parseRunAt(tokens, i)
            if (run != null) return run
        }
        return null
    }

    /**
     * Parses a number that starts exactly at [start]. Returns null if no number starts there.
     *
     * Grammar (informal):
     *   digitRun = NUMERIC (HUNDRED | BIG_SCALE)*
     *   wordRun  = ("a" before a scale word | SMALL | HUNDRED | BIG_SCALE except "k") ...
     *              with "and" allowed between two number words.
     */
    private fun parseRunAt(tokens: List<String>, start: Int): NumberRun? {
        var total = BigDecimal.ZERO
        var current = BigDecimal.ZERO
        var consumedAny = false
        var startedWithDigits = false
        var i = start

        while (i < tokens.size) {
            val t = tokens[i]
            val next = tokens.getOrNull(i + 1)
            when {
                NUMERIC_TOKEN.matches(t) -> {
                    if (consumedAny) break
                    current = BigDecimal(t)
                    startedWithDigits = true
                }
                t in SMALL_NUMBERS -> {
                    if (startedWithDigits) break
                    current += BigDecimal(SMALL_NUMBERS.getValue(t))
                }
                t == HUNDRED -> {
                    current = current.orOne() * BigDecimal(100)
                }
                t in BIG_SCALES -> {
                    if (t == "k" && !consumedAny) break
                    total += current.orOne() * BIG_SCALES.getValue(t)
                    current = BigDecimal.ZERO
                }
                t == "a" && !consumedAny && (next == HUNDRED || (next in BIG_SCALES && next != "k")) -> {
                    current = BigDecimal.ONE
                }
                t == "and" && consumedAny && !startedWithDigits && next != null && next in SMALL_NUMBERS -> {
                    // "two hundred and fifty": skip the "and"
                }
                else -> break
            }
            consumedAny = true
            i++
        }

        if (!consumedAny) return null
        return NumberRun(start, i, total + current)
    }

    private fun BigDecimal.orOne(): BigDecimal = if (signum() == 0) BigDecimal.ONE else this

    // ------------------------------------------------------------ description

    /**
     * Description-only helper used by the "Type instead" screen when no amount was heard:
     * "coffee at starbucks" -> "Coffee at starbucks".
     */
    fun descriptionOf(input: String): String = buildDescription(tokenize(input))

    private fun buildDescription(tokens: List<String>): String {
        var from = 0
        var to = tokens.size
        while (from < to && tokens[from] in FILLER_WORDS) from++
        while (to > from && tokens[to - 1] in FILLER_WORDS) to--
        val text = tokens.subList(from, to).joinToString(" ").take(MAX_DESCRIPTION_LENGTH).trim()
        return text.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}
