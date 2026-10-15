package org.sprachcafe.team.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.sprachcafe.team.data.ItemCategory
import org.sprachcafe.team.data.KioskItem
import kotlin.system.measureNanoTime

class CartLookupBenchmarkTest {

    private fun generateItems(count: Int): List<KioskItem> {
        return (1..count).map { i ->
            KioskItem(
                id = "item-$i",
                name = "Item Name $i",
                category = ItemCategory.HOT_DRINKS,
                unit = "Stk",
                priceCents = i * 100,
                costCents = i * 50
            )
        }
    }

    private fun generateCart(itemsList: List<KioskItem>, cartSize: Int): Map<String, Int> {
        // Pick items spread out in itemsList to test lookup performance
        return (0 until cartSize).associate { i ->
            val idx = (i * 37) % itemsList.size
            itemsList[idx].id to ((i % 5) + 1)
        }
    }

    @Test
    fun benchmarkCartSummary() {
        val itemsList = generateItems(1000)
        val cart = generateCart(itemsList, 100)

        val iterations = 5000

        // Warm up JVM
        repeat(500) {
            cart.map { (id, qty) ->
                val name = itemsList.find { it.id == id }?.name ?: id
                "$qty x $name"
            }.joinToString(", ")
        }

        // Measure O(N*M) list find approach
        val timeListFind = measureNanoTime {
            repeat(iterations) {
                cart.map { (id, qty) ->
                    val name = itemsList.find { it.id == id }?.name ?: id
                    "$qty x $name"
                }.joinToString(", ")
            }
        }

        // Measure O(N + M) map lookup approach
        val timeMapLookup = measureNanoTime {
            repeat(iterations) {
                val itemsById = itemsList.associateBy { it.id }
                cart.map { (id, qty) ->
                    val name = itemsById[id]?.name ?: id
                    "$qty x $name"
                }.joinToString(", ")
            }
        }

        println("BENCHMARK RESULTS ($iterations iterations with 1,000 items, cart of 100 items):")
        println("  O(N) list find approach : ${timeListFind / 1_000_000.0} ms")
        println("  O(1) map lookup approach: ${timeMapLookup / 1_000_000.0} ms")
        println("  Speedup                 : ${String.format("%.2f", timeListFind.toDouble() / timeMapLookup)}x")

        // Correctness check
        val summary1 = cart.map { (id, qty) ->
            val name = itemsList.find { it.id == id }?.name ?: id
            "$qty x $name"
        }.joinToString(", ")

        val itemsById = itemsList.associateBy { it.id }
        val summary2 = cart.map { (id, qty) ->
            val name = itemsById[id]?.name ?: id
            "$qty x $name"
        }.joinToString(", ")

        assertEquals(summary1, summary2)
    }
}
