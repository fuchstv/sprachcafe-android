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
}
