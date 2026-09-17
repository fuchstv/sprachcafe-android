package org.sprachcafe.team.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CashCountTest {

    @Test
    fun defaultValues_calculateZeroTotalCashAndNegativeRevenue() {
        val count = CashCount()

        assertEquals(5000, count.baseAmountCents)
        assertEquals(0, count.totalCashCents)
        assertEquals(-5000, count.cashWithoutBaseCents)
        assertEquals("0.00 €", count.totalCashEurFormatted)
        assertEquals("-50.00 €", count.revenueEurFormatted)
    }

    @Test
    fun totalCashCents_calculatesCorrectlyForNotesAndCoins() {
        val count = CashCount(
            baseAmountCents = 5000,
            note50 = 1,  // 5000 cents
            note20 = 1,  // 2000 cents
            note10 = 1,  // 1000 cents
            note5 = 1,   //  500 cents
            coin200 = 1, //  200 cents
            coin100 = 1, //  100 cents
            coin50 = 1,  //   50 cents
            coin20 = 1,  //   20 cents
            coin10 = 1,  //   10 cents
            coin5 = 1    //    5 cents
        )

        // Total = 5000 + 2000 + 1000 + 500 + 200 + 100 + 50 + 20 + 10 + 5 = 8885 cents
        assertEquals(8885, count.totalCashCents)
        assertEquals("88.85 €", count.totalCashEurFormatted)
    }

    @Test
    fun individualDenominations_calculateCorrectAmounts() {
        assertEquals(5000, CashCount(note50 = 1).totalCashCents)
        assertEquals(2000, CashCount(note20 = 1).totalCashCents)
        assertEquals(1000, CashCount(note10 = 1).totalCashCents)
        assertEquals(500, CashCount(note5 = 1).totalCashCents)
        assertEquals(200, CashCount(coin200 = 1).totalCashCents)
        assertEquals(100, CashCount(coin100 = 1).totalCashCents)
        assertEquals(50, CashCount(coin50 = 1).totalCashCents)
        assertEquals(20, CashCount(coin20 = 1).totalCashCents)
        assertEquals(10, CashCount(coin10 = 1).totalCashCents)
        assertEquals(5, CashCount(coin5 = 1).totalCashCents)
    }

    @Test
    fun cashWithoutBaseCents_subtractsBaseAmountCorrectly() {
        // Total cash = 2 * 50€ (10000 cents), base amount = 5000 cents
        val count = CashCount(
            baseAmountCents = 5000,
            note50 = 2
        )

        assertEquals(10000, count.totalCashCents)
        assertEquals(5000, count.cashWithoutBaseCents)
        assertEquals("100.00 €", count.totalCashEurFormatted)
        assertEquals("50.00 €", count.revenueEurFormatted)
    }

    @Test
    fun cashWithoutBaseCents_withCustomBaseAmount() {
        // Total cash = 10000 cents, custom base amount = 3000 cents
        val count = CashCount(
            baseAmountCents = 3000,
            note50 = 2
        )

        assertEquals(10000, count.totalCashCents)
        assertEquals(7000, count.cashWithoutBaseCents)
        assertEquals("100.00 €", count.totalCashEurFormatted)
        assertEquals("70.00 €", count.revenueEurFormatted)
    }
}
