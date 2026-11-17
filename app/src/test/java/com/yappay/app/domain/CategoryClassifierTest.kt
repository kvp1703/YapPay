package com.yappay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryClassifierTest {

    private fun check(description: String, expected: Category) =
        assertEquals("description: \"$description\"", expected, CategoryClassifier.classify(description))

    @Test fun pizza() = check("Pizza", Category.FOOD)
    @Test fun coffeeStarbucks() = check("Coffee starbucks", Category.FOOD)
    @Test fun restaurantBillIsFoodNotBills() = check("Restaurant bill", Category.FOOD)
    @Test fun dentist() = check("Dentist", Category.HEALTH)
    @Test fun medicinesPlural() = check("Medicines", Category.HEALTH)
    @Test fun groceries() = check("Groceries", Category.GROCERIES)
    @Test fun uber() = check("Uber to office", Category.TRANSPORT)
    @Test fun busesPluralEs() = check("Buses", Category.TRANSPORT)
    @Test fun busTicket() = check("Bus ticket", Category.TRANSPORT)
    @Test fun phoneBillIsBills() = check("Phone bill", Category.BILLS)
    @Test fun electricityBill() = check("Electricity bill", Category.BILLS)
    @Test fun phoneCoverIsShopping() = check("Phone cover", Category.SHOPPING)
    @Test fun rent() = check("Rent", Category.HOUSING)
    @Test fun movieTickets() = check("Movie tickets", Category.ENTERTAINMENT)
    @Test fun haircut() = check("Haircut", Category.PERSONAL_CARE)
    @Test fun punctuationIgnored() = check("pizza!!", Category.FOOD)
    @Test fun unknown() = check("Random thing", Category.OTHER)
    @Test fun empty() = check("", Category.OTHER)

    @Test fun fromNameKnown() = assertEquals(Category.FOOD, Category.fromName("FOOD"))
    @Test fun fromNameUnknown() = assertEquals(Category.OTHER, Category.fromName("NOT_A_CATEGORY"))
    @Test fun fromNameNull() = assertEquals(Category.OTHER, Category.fromName(null))
}
