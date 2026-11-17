package com.yappay.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {

    private fun inr(minor: Long) = MoneyFormatter.format(minor, "₹", GroupingStyle.INDIAN)
    private fun usd(minor: Long) = MoneyFormatter.format(minor, "$", GroupingStyle.INTERNATIONAL)

    @Test fun wholeAmountHasNoDecimals() = assertEquals("₹340", inr(34000))
    @Test fun fractionShownWithTwoDigits() = assertEquals("₹12.50", inr(1250))
    @Test fun fractionLeadingZero() = assertEquals("₹12.05", inr(1205))
    @Test fun zero() = assertEquals("₹0", inr(0))
    @Test fun belowThousandNoComma() = assertEquals("₹999", inr(99900))
    @Test fun thousand() = assertEquals("₹1,000", inr(100000))
    @Test fun indianLakh() = assertEquals("₹1,20,000", inr(12000000))
    @Test fun indianLarge() = assertEquals("₹12,34,567.89", inr(123456789))
    @Test fun internationalThousands() = assertEquals("$120,000", usd(12000000))
    @Test fun internationalLarge() = assertEquals("$1,234,567.89", usd(123456789))
    @Test fun negative() = assertEquals("-₹340", inr(-34000))
    @Test fun alwaysShowDecimals() =
        assertEquals("₹340.00", MoneyFormatter.format(34000, "₹", GroupingStyle.INDIAN, alwaysShowDecimals = true))

    @Test fun parseWhole() = assertEquals(34000L, MoneyFormatter.parseUserInput("340"))
    @Test fun parseOneDecimal() = assertEquals(34050L, MoneyFormatter.parseUserInput("340.5"))
    @Test fun parseTwoDecimals() = assertEquals(1205L, MoneyFormatter.parseUserInput("12.05"))
    @Test fun parseTrailingDotWhileTyping() = assertEquals(34000L, MoneyFormatter.parseUserInput("340."))
    @Test fun parseCommas() = assertEquals(120075L, MoneyFormatter.parseUserInput("1,200.75"))
    @Test fun parseTrims() = assertEquals(9900L, MoneyFormatter.parseUserInput(" 99 "))
    @Test fun parseMax() = assertEquals(10_000_000_000L, MoneyFormatter.parseUserInput("100000000"))
    @Test fun parseEmpty() = assertNull(MoneyFormatter.parseUserInput(""))
    @Test fun parseLetters() = assertNull(MoneyFormatter.parseUserInput("abc"))
    @Test fun parseNegative() = assertNull(MoneyFormatter.parseUserInput("-5"))
    @Test fun parseThreeDecimals() = assertNull(MoneyFormatter.parseUserInput("1.234"))
    @Test fun parseZero() = assertNull(MoneyFormatter.parseUserInput("0"))
    @Test fun parseZeroDecimals() = assertNull(MoneyFormatter.parseUserInput("0.00"))
    @Test fun parseDotOnly() = assertNull(MoneyFormatter.parseUserInput("."))
    @Test fun parseTooLarge() = assertNull(MoneyFormatter.parseUserInput("100000001"))

    @Test fun inputTextWhole() = assertEquals("340", MoneyFormatter.toInputText(34000))
    @Test fun inputTextFraction() = assertEquals("12.50", MoneyFormatter.toInputText(1250))
    @Test fun inputTextLeadingZeroFraction() = assertEquals("12.05", MoneyFormatter.toInputText(1205))
    @Test fun inputTextPaise() = assertEquals("0.05", MoneyFormatter.toInputText(5))
}
