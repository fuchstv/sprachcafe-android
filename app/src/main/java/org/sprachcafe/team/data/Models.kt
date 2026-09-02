package org.sprachcafe.team.data

enum class ItemCategory(val labelDe: String) {
    COLD_DRINKS("Kalte Getränke"),
    HOT_DRINKS("Warme Getränke"),
    SNACKS("Snacks & Gebäck"),
    SHOP("Kleiner Laden"),
    DONATIONS("Spenden")
}

data class KioskItem(
    val id: String,
    val name: String,
    val category: ItemCategory,
    val unit: String,
    val priceCents: Int,
    val costCents: Int,
    val taxSphere: String,
    val barcode: String? = null,
    val icon: String = "☕"
) {
    val priceEurFormatted: String
        get() = String.format("%.2f €", priceCents / 100.0)
}

data class CartItem(
    val item: KioskItem,
    val quantity: Int
) {
    val totalCents: Int get() = item.priceCents * quantity
    val totalEurFormatted: String
        get() = String.format("%.2f €", totalCents / 100.0)
}

data class CashCount(
    val baseAmountCents: Int = 5000, // 50,00 € Wechselgeldsockel
    val note50: Int = 0,
    val note20: Int = 0,
    val note10: Int = 0,
    val note5: Int = 0,
    val coin200: Int = 0, // 2 €
    val coin100: Int = 0, // 1 €
    val coin50: Int = 0,  // 0.50 €
    val coin20: Int = 0,  // 0.20 €
    val coin10: Int = 0,  // 0.10 €
    val coin5: Int = 0    // 0.05 €
) {
    val totalCashCents: Int
        get() = (note50 * 5000) + (note20 * 2000) + (note10 * 1000) + (note5 * 500) +
                (coin200 * 200) + (coin100 * 100) + (coin50 * 50) + (coin20 * 20) + (coin10 * 10) + (coin5 * 5)

    val cashWithoutBaseCents: Int
        get() = totalCashCents - baseAmountCents

    val totalCashEurFormatted: String
        get() = String.format("%.2f €", totalCashCents / 100.0)

    val revenueEurFormatted: String
        get() = String.format("%.2f €", cashWithoutBaseCents / 100.0)
}

data class WasteEntry(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val itemName: String,
    val quantity: Int,
    val reason: String, // "MHD-Ablauf", "Helferverpflegung", "Glasbruch"
    val volunteerName: String
)

data class LibraryBook(
    val isbn: String,
    val title: String,
    val author: String,
    val isLent: Boolean = false,
    val lentTo: String? = null
)
