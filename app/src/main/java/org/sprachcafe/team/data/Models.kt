package org.sprachcafe.team.data

data class TeamMember(
    val id: Int,
    val name: String,
    val shortCode: String,
    val email: String? = null,
    val color: String = "#8B1E2D",
    val role: String = "Mitarbeiter"
)

data class ShiftItem(
    val id: Int,
    val date: String,
    val timeSlot: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val hours: Double? = null,
    val memberName: String,
    val memberColor: String? = null,
    val location: String = "Schulzestraße (Pankow)",
    val notes: String? = null
)

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
    val unit: String = "Stk",
    val priceCents: Int,
    val costCents: Int = 0,
    val taxSphere: String = "Zweckbetrieb",
    val barcode: String? = null,
    val icon: String = "☕",
    val isActive: Boolean = true,
    val stockQuantity: Int = 0
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

enum class TransactionType(val labelDe: String) {
    SALE("Verkauf"),
    DONATION("Spende"),
    PAYOUT("Entnahme / Auslage"),
    LIBRARY_FEE("Bibliotheksgebühr")
}

enum class PayoutCategory(val labelDe: String) {
    PURCHASE("Wareneinkauf (Beleg / Bestand steigt)"),
    OUTTAKE("Warenentnahme (Event/Verbrauch / Bestand sinkt)"),
    PURE_CASH("Reine Barauszahlung (ohne Artikel)")
}

data class ReceiptArticleItem(
    val itemId: String,
    val name: String,
    val category: ItemCategory = ItemCategory.SNACKS,
    val unit: String = "Stk",
    val qty: Int = 1,
    val costCents: Int = 0,
    val sellingCents: Int = 0,
    val mhd: String? = null,
    val barcode: String? = null,
    val updateCatalogCost: Boolean = true
) {
    val totalCostCents: Int get() = costCents * qty
    val totalCostEurFormatted: String get() = String.format("%.2f €", totalCostCents / 100.0)
    val marginPercent: Int
        get() = if (sellingCents > 0) Math.round(((sellingCents - costCents).toDouble() / sellingCents) * 100).toInt() else 0
}

data class CashTransaction(
    val id: Long = 0,
    val sessionId: Long,
    val type: TransactionType,
    val amountCents: Int,
    val purpose: String? = null,
    val receiptPhotoPath: String? = null,
    val donorOrMemberName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val itemsJson: String? = null
) {
    val amountEurFormatted: String
        get() = String.format("%.2f €", amountCents / 100.0)
}

data class CashSession(
    val id: Long = 0,
    val shiftId: Int? = null,
    val volunteerName: String,
    val date: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val openingFloatCents: Int = 5000,
    val totalSalesCents: Int = 0,
    val totalDonationsCents: Int = 0,
    val totalLibraryFeesCents: Int = 0,
    val totalPayoutsCents: Int = 0,
    val countedTotalCents: Int? = null,
    val diffCents: Int? = null,
    val baseRetainedCents: Int = 5000,
    val skimRetainedCents: Int = 0,
    val status: String = "open", // 'open' or 'closed'
    val notes: String? = null
) {
    val expectedTotalCents: Int
        get() = openingFloatCents + totalSalesCents + totalDonationsCents + totalLibraryFeesCents - totalPayoutsCents

    val expectedTotalEurFormatted: String
        get() = String.format("%.2f €", expectedTotalCents / 100.0)
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

data class InventoryCountItem(
    val id: String,
    val name: String,
    val category: ItemCategory,
    val barcode: String? = null,
    val countedQuantity: Int = 0
)

data class LibraryBook(
    val isbn: String,
    val title: String,
    val author: String,
    val category: String? = null,
    val shelfLocation: String? = null,
    val isLent: Boolean = false,
    val lentTo: String? = null,
    val dueDate: String? = null
)

data class LibraryLoan(
    val id: Long = 0,
    val isbn: String,
    val title: String,
    val borrowerName: String,
    val borrowerContact: String? = null,
    val loanDate: String,
    val dueDate: String,
    val feeCents: Int = 0,
    val status: String = "active"
)

data class ClubMemberVerification(
    val valid: Boolean,
    val id: Int,
    val memberNumber: String,
    val name: String,
    val tier: String,
    val status: String,
    val validUntil: String? = null,
    val coffeeQuotaRemaining: Int = 0,
    val eventDiscountPct: Int = 0
)
