package org.sprachcafe.team.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptArticleItemTest {

    @Test
    fun marginPercent_whenSellingCentsIsZero_returnsZero() {
        val item = ReceiptArticleItem(
            itemId = "item-1",
            name = "Free Sample",
            costCents = 100,
            sellingCents = 0
        )
        assertEquals(0, item.marginPercent)
    }

    @Test
    fun marginPercent_whenSellingCentsIsNegative_returnsZero() {
        val item = ReceiptArticleItem(
            itemId = "item-1",
            name = "Negative Price",
            costCents = 100,
            sellingCents = -50
        )
        assertEquals(0, item.marginPercent)
    }

    @Test
    fun marginPercent_whenProfitable_returnsCorrectPercentage() {
        val item50Percent = ReceiptArticleItem(
            itemId = "item-1",
            name = "Item 1",
            costCents = 100,
            sellingCents = 200
        )
        assertEquals(50, item50Percent.marginPercent)

        val item75Percent = ReceiptArticleItem(
            itemId = "item-2",
            name = "Item 2",
            costCents = 250,
            sellingCents = 1000
        )
        assertEquals(75, item75Percent.marginPercent)
    }

    @Test
    fun marginPercent_whenBreakEven_returnsZero() {
        val item = ReceiptArticleItem(
            itemId = "item-1",
            name = "At Cost Item",
            costCents = 250,
            sellingCents = 250
        )
        assertEquals(0, item.marginPercent)
    }

    @Test
    fun marginPercent_whenLoss_returnsNegativePercentage() {
        val item = ReceiptArticleItem(
            itemId = "item-1",
            name = "Loss Leader",
            costCents = 150,
            sellingCents = 100
        )
        assertEquals(-50, item.marginPercent)
    }

    @Test
    fun marginPercent_roundsToNearestInteger() {
        // 200 / 300 = 66.666...% -> rounds to 67%
        val itemRoundUp = ReceiptArticleItem(
            itemId = "item-1",
            name = "Round Up",
            costCents = 100,
            sellingCents = 300
        )
        assertEquals(67, itemRoundUp.marginPercent)

        // 100 / 300 = 33.333...% -> rounds to 33%
        val itemRoundDown = ReceiptArticleItem(
            itemId = "item-2",
            name = "Round Down",
            costCents = 200,
            sellingCents = 300
        )
        assertEquals(33, itemRoundDown.marginPercent)
    }

    @Test
    fun totalCost_calculatesCorrectly() {
        val item = ReceiptArticleItem(
            itemId = "item-1",
            name = "Bulk Item",
            qty = 3,
            costCents = 150,
            sellingCents = 300
        )
        assertEquals(450, item.totalCostCents)
        assertEquals("4.50 €", item.totalCostEurFormatted)
    }
}
