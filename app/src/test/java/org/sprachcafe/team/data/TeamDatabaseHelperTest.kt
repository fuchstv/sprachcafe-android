package org.sprachcafe.team.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TeamDatabaseHelperTest {

    private lateinit var dbHelper: TeamDatabaseHelper
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbHelper = TeamDatabaseHelper(context)

        // Add some test books using saveBooks
        dbHelper.saveBooks(listOf(
            LibraryBook(
                isbn = "1234567890",
                title = "Test Book One",
                author = "Author One",
                category = "Fiction",
                shelfLocation = "A1",
                isLent = false,
                lentTo = "",
                dueDate = ""
            ),
            LibraryBook(
                isbn = "0987654321",
                title = "Second Book Test",
                author = "Author Two",
                category = "Science",
                shelfLocation = "B2",
                isLent = false,
                lentTo = "",
                dueDate = ""
            ),
            LibraryBook(
                isbn = "1122334455",
                title = "A Short Book",
                author = "Author Three",
                category = "Art",
                shelfLocation = "C3",
                isLent = false,
                lentTo = "",
                dueDate = ""
            )
        ))
    }

    @After
    fun tearDown() {
        dbHelper.close()
    }

    @Test
    fun testSearchBooks_emptyQuery() {
        val results = dbHelper.searchBooks("")
        assertEquals(3, results.size)
    }

    @Test
    fun testSearchBooks_shortQuery_oneChar() {
        // Querying for "T" should match "Test Book One", "Second Book Test", "A Short Book"
        val results = dbHelper.searchBooks("T")
        assertEquals(3, results.size)
    }

    @Test
    fun testSearchBooks_longQuery() {
        val results = dbHelper.searchBooks("Second Book")
        assertEquals(1, results.size)
        assertEquals("Second Book Test", results[0].title)
    }

    @Test
    fun testSearchBooks_matchesAuthor() {
        val results = dbHelper.searchBooks("Author Two")
        assertEquals(1, results.size)
        assertEquals("Second Book Test", results[0].title)
    }

    @Test
    fun testSearchBooks_noMatch() {
        val results = dbHelper.searchBooks("xyz123")
        assertTrue(results.isEmpty())
    }

    @Test
    fun testSearchBooks_whitespaceHandling() {
        val results = dbHelper.searchBooks("  Test  ")
        assertEquals(2, results.size) // Matches "Test Book One", "Second Book Test"
    }

    @Test
    fun testDefaultKioskItems_excludesObsoleteDonationAndCoffeeBoxItems() {
        val items = dbHelper.getAllKioskItems()
        assertEquals(16, items.size)
        assertTrue(items.none { it.id == "item-17" || it.id == "item-18" })
        assertTrue(items.none { it.name.contains("Spende", ignoreCase = true) || it.name.contains("Kaffee-Kasse", ignoreCase = true) })
        assertTrue(items.none { it.category == ItemCategory.DONATIONS })
    }

    @Test
    fun testSaveKioskItems_purgesObsoleteItems() {
        // Manually insert legacy item into DB
        val cv = android.content.ContentValues().apply {
            put("id", "item-17")
            put("name", "Spende (Bücher / Kiez)")
            put("category", "DONATIONS")
            put("price_cents", 200)
            put("is_active", 1)
        }
        dbHelper.writableDatabase.insert("kiosk_items", null, cv)

        // Ensure getAllKioskItems ignores it
        val items = dbHelper.getAllKioskItems()
        assertTrue(items.none { it.id == "item-17" })

        // Save items and ensure obsolete item is purged completely from table
        dbHelper.saveKioskItems(items)
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT COUNT(*) FROM kiosk_items WHERE id = 'item-17'", null)
        cursor.use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
    }
}

