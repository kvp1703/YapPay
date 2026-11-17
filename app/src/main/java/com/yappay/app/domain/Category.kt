package com.yappay.app.domain

/**
 * Fixed list of categories. The enum NAME (e.g. "FOOD") is what gets stored in the database.
 * NEVER rename an enum constant after release: old rows would silently become OTHER.
 *
 * ORDER MATTERS: CategoryClassifier checks categories top to bottom and returns the first match.
 * That is why BILLS ("phone bill") is above SHOPPING ("phone").
 */
enum class Category(val displayName: String, val keywords: List<String>) {
    FOOD(
        "Food & Dining",
        listOf(
            "pizza", "burger", "sandwich", "restaurant", "hotel food", "cafe", "coffee", "tea", "chai",
            "breakfast", "lunch", "dinner", "snack", "snacks", "food", "swiggy", "zomato", "dosa",
            "idli", "biryani", "pasta", "noodles", "momos", "samosa", "juice", "icecream", "ice cream",
            "dessert", "cake", "bakery", "starbucks", "mcdonalds", "kfc", "dominos", "subway", "beer",
            "drinks", "bar", "pub", "canteen", "mess", "tiffin", "thali",
        ),
    ),
    GROCERIES(
        "Groceries",
        listOf(
            "grocery", "groceries", "vegetable", "vegetables", "veggies", "fruit", "fruits", "milk",
            "bread", "egg", "eggs", "rice", "atta", "dal", "oil", "supermarket", "kirana", "bigbasket",
            "blinkit", "zepto", "dmart", "instamart", "provisions",
        ),
    ),
    TRANSPORT(
        "Transport",
        listOf(
            "uber", "ola", "rapido", "taxi", "cab", "auto", "rickshaw", "bus", "metro", "train",
            "fuel", "petrol", "diesel", "cng", "parking", "toll", "fastag", "bike service",
            "car service",
        ),
    ),
    HEALTH(
        "Health",
        listOf(
            "doctor", "dentist", "hospital", "clinic", "medicine", "medicines", "medical", "pharmacy",
            "chemist", "lab test", "blood test", "checkup", "physio", "physiotherapy", "eye test",
            "spectacles", "glasses",
        ),
    ),
    BILLS(
        "Bills & Utilities",
        listOf(
            "electricity", "electricity bill", "water bill", "gas", "cylinder", "internet", "wifi",
            "broadband", "phone bill", "mobile bill", "recharge", "dth", "bill", "emi", "insurance",
        ),
    ),
    HOUSING(
        "Rent & Housing",
        listOf(
            "rent", "maintenance", "society", "plumber", "electrician", "carpenter", "repair",
            "furniture", "maid", "cook", "laundry", "ironing",
        ),
    ),
    SHOPPING(
        "Shopping",
        listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", "clothes", "shirt", "tshirt", "jeans",
            "dress", "shoes", "shoe", "bag", "watch", "phone", "laptop", "headphones", "earphones",
            "electronics", "shopping", "mall",
        ),
    ),
    ENTERTAINMENT(
        "Entertainment",
        listOf(
            "movie", "movies", "cinema", "pvr", "inox", "netflix", "spotify", "prime", "hotstar",
            "youtube", "concert", "game", "games", "bowling", "subscription",
        ),
    ),
    EDUCATION(
        "Education",
        listOf(
            "book", "books", "course", "tuition", "school", "college", "fees", "fee", "stationery",
            "exam", "udemy", "coaching",
        ),
    ),
    PERSONAL_CARE(
        "Personal Care",
        listOf("salon", "haircut", "hair cut", "parlour", "parlor", "spa", "gym", "cosmetics", "grooming"),
    ),
    TRAVEL(
        "Travel",
        listOf("flight", "flights", "airbnb", "trip", "vacation", "holiday", "resort", "visa", "luggage"),
    ),
    GIFTS(
        "Gifts & Donations",
        listOf("gift", "gifts", "donation", "charity", "temple", "church", "mosque", "gurudwara"),
    ),
    OTHER("Other", emptyList());

    companion object {
        /** Safe lookup for values read from the database. Unknown names map to OTHER. */
        fun fromName(name: String?): Category = entries.firstOrNull { it.name == name } ?: OTHER
    }
}
