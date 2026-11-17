package com.yappay.app.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceParserTest {

    private fun ok(input: String, amountMinor: Long, description: String) {
        assertEquals("input: \"$input\"", ParseResult.Success(amountMinor, description), VoiceParser.parse(input))
    }

    private fun fail(input: String, reason: FailureReason) {
        assertEquals("input: \"$input\"", ParseResult.Failure(reason), VoiceParser.parse(input))
    }

    // ---------------------------------------------------------------- basics

    @Test fun amountThenDescription() = ok("340 pizza", 34000, "Pizza")
    @Test fun amountThenDescription2() = ok("500 dentist", 50000, "Dentist")
    @Test fun capitalisedInput() = ok("340 Pizza", 34000, "Pizza")
    @Test fun descriptionThenAmount() = ok("pizza 340", 34000, "Pizza")
    @Test fun amountInTheMiddle() = ok("coffee 120 starbucks", 12000, "Coffee starbucks")
    @Test fun multiWordDescription() = ok("uber 250 to office", 25000, "Uber to office")
    @Test fun onlyFirstDigitNumberIsAmount() = ok("340 pizza 2 coke", 34000, "Pizza 2 coke")
    @Test fun amountOnlyGivesEmptyDescription() = ok("340", 34000, "")

    // ------------------------------------------------------- currency + filler

    @Test fun rupeeSymbol() = ok("₹340 pizza", 34000, "Pizza")
    @Test fun rsWithDot() = ok("Rs. 340 pizza", 34000, "Pizza")
    @Test fun rsGluedToNumber() = ok("rs340 pizza", 34000, "Pizza")
    @Test fun rupeesWord() = ok("340 rupees pizza", 34000, "Pizza")
    @Test fun rupeesForWord() = ok("340 rupees for pizza", 34000, "Pizza")
    @Test fun spentOn() = ok("spent 340 on pizza", 34000, "Pizza")
    @Test fun paidTo() = ok("paid 500 to dentist", 50000, "Dentist")
    @Test fun trailingCurrency() = ok("pizza for 340 rupees", 34000, "Pizza")
    @Test fun onlyWord() = ok("₹ 99 only snacks", 9900, "Snacks")
    @Test fun andAfterDigits() = ok("500 and pizza", 50000, "Pizza")

    // ---------------------------------------------------------- number formats

    @Test fun westernThousands() = ok("1,200 groceries", 120000, "Groceries")
    @Test fun indianLakhCommas() = ok("1,20,000 laptop", 12000000, "Laptop")
    @Test fun decimal() = ok("12.50 coffee", 1250, "Coffee")
    @Test fun decimalRoundsHalfUp() = ok("12.555 coffee", 1256, "Coffee")
    @Test fun trailingFullStop() = ok("Petrol 2000.", 200000, "Petrol")
    @Test fun kSuffix() = ok("2k rent", 200000, "Rent")
    @Test fun decimalKSuffix() = ok("1.5k shoes", 150000, "Shoes")
    @Test fun digitThenThousandWord() = ok("2 thousand rent", 200000, "Rent")
    @Test fun digitThenLakhWord() = ok("1.5 lakh car repair", 15000000, "Car repair")

    // ------------------------------------------------------------ number words

    @Test fun wordsHundreds() = ok("three hundred forty pizza", 34000, "Pizza")
    @Test fun wordsWithAnd() = ok("two hundred and fifty chai", 25000, "Chai")
    @Test fun wordsTensUnits() = ok("twenty five rupees chai", 2500, "Chai")
    @Test fun wordsHyphenated() = ok("twenty-five chai", 2500, "Chai")
    @Test fun wordsAHundred() = ok("a hundred bus ticket", 10000, "Bus ticket")
    @Test fun wordsThousandHundred() = ok("one thousand five hundred electricity bill", 150000, "Electricity bill")
    @Test fun wordsLakh() = ok("one lakh twenty thousand bike", 12000000, "Bike")

    // ------------------------------------------- digits beat words (priority)

    @Test fun digitsWinOverLaterWords() = ok("500 one plus cover", 50000, "One plus cover")
    @Test fun digitsWinOverEarlierWords() = ok("two dosa 80", 8000, "Two dosa")

    // ------------------------------------------------------------ punctuation

    @Test fun apostropheRemoved() = ok("340 domino's", 34000, "Dominos")
    @Test fun devanagariDigitsAndWord() = ok("५०० पिज़्ज़ा", 50000, "पिज़्ज़ा")

    // --------------------------------------------------------------- failures

    @Test fun empty() = fail("", FailureReason.EMPTY_INPUT)
    @Test fun blank() = fail("   ", FailureReason.EMPTY_INPUT)
    @Test fun onlyPunctuation() = fail("!!!", FailureReason.EMPTY_INPUT)
    @Test fun noNumber() = fail("pizza", FailureReason.NO_AMOUNT)
    @Test fun loneKIsNotANumber() = fail("k mart", FailureReason.NO_AMOUNT)
    @Test fun zero() = fail("0 pizza", FailureReason.ZERO_AMOUNT)
    @Test fun zeroWord() = fail("zero pizza", FailureReason.ZERO_AMOUNT)
    @Test fun roundsToZero() = fail("0.001 pizza", FailureReason.ZERO_AMOUNT)
    @Test fun tooLarge() = fail("200000000 house", FailureReason.AMOUNT_TOO_LARGE)

    // -------------------------------------------------------------- parseBest

    @Test fun parseBestPicksFirstSuccess() {
        assertEquals(ParseResult.Success(34000, "Pizza"), VoiceParser.parseBest(listOf("pizza", "340 pizza", "350 pizza")))
    }

    @Test fun parseBestReturnsFirstFailureWhenAllFail() {
        assertEquals(ParseResult.Failure(FailureReason.NO_AMOUNT), VoiceParser.parseBest(listOf("pizza", "piece")))
    }

    @Test fun parseBestEmptyList() {
        assertEquals(ParseResult.Failure(FailureReason.EMPTY_INPUT), VoiceParser.parseBest(emptyList()))
    }
}
