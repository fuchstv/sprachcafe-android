package org.sprachcafe.team.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TeamDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "sprachcafe_team.db"
        const val DATABASE_VERSION = 2

        @Volatile
        private var instance: TeamDatabaseHelper? = null

        fun getInstance(context: Context): TeamDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: TeamDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Kiosk Items
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS kiosk_items (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                unit TEXT NOT NULL DEFAULT 'Stk',
                price_cents INTEGER NOT NULL,
                cost_cents INTEGER NOT NULL DEFAULT 0,
                tax_sphere TEXT NOT NULL DEFAULT 'Zweckbetrieb',
                barcode TEXT,
                icon TEXT DEFAULT '☕',
                is_active INTEGER NOT NULL DEFAULT 1,
                stock_quantity INTEGER DEFAULT 0
            );
        """.trimIndent())

        // 2. Cash Sessions
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS cash_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                shift_id INTEGER,
                volunteer_name TEXT NOT NULL,
                date TEXT NOT NULL,
                start_time TEXT,
                end_time TEXT,
                opening_float_cents INTEGER NOT NULL DEFAULT 5000,
                total_sales_cents INTEGER NOT NULL DEFAULT 0,
                total_donations_cents INTEGER NOT NULL DEFAULT 0,
                total_library_fees_cents INTEGER NOT NULL DEFAULT 0,
                total_payouts_cents INTEGER NOT NULL DEFAULT 0,
                counted_total_cents INTEGER,
                diff_cents INTEGER,
                base_retained_cents INTEGER DEFAULT 5000,
                skim_retained_cents INTEGER DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'open',
                notes TEXT
            );
        """.trimIndent())

        // 3. Cash Transactions
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS cash_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                amount_cents INTEGER NOT NULL,
                purpose TEXT,
                receipt_photo_path TEXT,
                donor_or_member_name TEXT,
                timestamp INTEGER NOT NULL,
                items_json TEXT
            );
        """.trimIndent())

        // 4. Library Books
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS library_books (
                isbn TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                author TEXT NOT NULL,
                category TEXT,
                shelf_location TEXT,
                is_lent INTEGER NOT NULL DEFAULT 0,
                lent_to TEXT,
                due_date TEXT
            );
        """.trimIndent())

        // 5. Library Loans
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS library_loans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                isbn TEXT NOT NULL,
                title TEXT NOT NULL,
                borrower_name TEXT NOT NULL,
                borrower_contact TEXT,
                loan_date TEXT NOT NULL,
                due_date TEXT NOT NULL,
                fee_cents INTEGER DEFAULT 0,
                status TEXT DEFAULT 'active'
            );
        """.trimIndent())

        // 6. Cached Shifts
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS cached_shifts (
                id INTEGER PRIMARY KEY,
                date TEXT NOT NULL,
                time_slot TEXT NOT NULL,
                start_time TEXT,
                end_time TEXT,
                hours REAL,
                member_name TEXT NOT NULL,
                member_color TEXT,
                location TEXT,
                notes TEXT
            );
        """.trimIndent())

        seedDefaultKioskItems(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS kiosk_items")
        db.execSQL("DROP TABLE IF EXISTS cash_sessions")
        db.execSQL("DROP TABLE IF EXISTS cash_transactions")
        db.execSQL("DROP TABLE IF EXISTS library_books")
        db.execSQL("DROP TABLE IF EXISTS library_loans")
        db.execSQL("DROP TABLE IF EXISTS cached_shifts")
        onCreate(db)
    }

    private fun seedDefaultKioskItems(db: SQLiteDatabase) {
        for (item in DefaultKioskData.items) {
            val cv = ContentValues().apply {
                put("id", item.id)
                put("name", item.name)
                put("category", item.category.name)
                put("unit", item.unit)
                put("price_cents", item.priceCents)
                put("cost_cents", item.costCents)
                put("tax_sphere", item.taxSphere)
                put("barcode", item.barcode)
                put("icon", item.icon)
                put("is_active", if (item.isActive) 1 else 0)
                put("stock_quantity", item.stockQuantity)
            }
            db.insertWithOnConflict("kiosk_items", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    // --- Kiosk Items DAO ---
    fun getAllKioskItems(): List<KioskItem> {
        val list = mutableListOf<KioskItem>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM kiosk_items WHERE is_active = 1 ORDER BY category ASC, name ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToKioskItem(c))
            }
        }
        return list
    }

    fun saveKioskItems(items: List<KioskItem>) {
        writableDatabase.beginTransaction()
        try {
            for (item in items) {
                val cv = ContentValues().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("category", item.category.name)
                    put("unit", item.unit)
                    put("price_cents", item.priceCents)
                    put("cost_cents", item.costCents)
                    put("tax_sphere", item.taxSphere)
                    put("barcode", item.barcode)
                    put("icon", item.icon)
                    put("is_active", if (item.isActive) 1 else 0)
                    put("stock_quantity", item.stockQuantity)
                }
                writableDatabase.insertWithOnConflict("kiosk_items", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun upsertSingleKioskItem(item: KioskItem) {
        saveKioskItems(listOf(item))
    }

    fun updateItemBarcode(itemId: String, barcode: String) {
        val cv = ContentValues().apply {
            put("barcode", barcode)
        }
        writableDatabase.update("kiosk_items", cv, "id = ?", arrayOf(itemId))
    }

    private fun cursorToKioskItem(c: Cursor): KioskItem {
        val catName = c.getString(c.getColumnIndexOrThrow("category"))
        val category = try { ItemCategory.valueOf(catName) } catch (e: Exception) { ItemCategory.COLD_DRINKS }
        return KioskItem(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            name = c.getString(c.getColumnIndexOrThrow("name")),
            category = category,
            unit = c.getString(c.getColumnIndexOrThrow("unit")),
            priceCents = c.getInt(c.getColumnIndexOrThrow("price_cents")),
            costCents = c.getInt(c.getColumnIndexOrThrow("cost_cents")),
            taxSphere = c.getString(c.getColumnIndexOrThrow("tax_sphere")),
            barcode = c.getString(c.getColumnIndexOrThrow("barcode")),
            icon = c.getString(c.getColumnIndexOrThrow("icon")),
            isActive = c.getInt(c.getColumnIndexOrThrow("is_active")) == 1,
            stockQuantity = c.getInt(c.getColumnIndexOrThrow("stock_quantity"))
        )
    }

    // --- Cash Sessions & Transactions DAO ---
    fun startCashSession(shiftId: Int?, volunteerName: String, date: String, startTime: String?, openingFloatCents: Int): Long {
        val cv = ContentValues().apply {
            put("shift_id", shiftId)
            put("volunteer_name", volunteerName)
            put("date", date)
            put("start_time", startTime)
            put("opening_float_cents", openingFloatCents)
            put("total_sales_cents", 0)
            put("total_donations_cents", 0)
            put("total_library_fees_cents", 0)
            put("total_payouts_cents", 0)
            put("status", "open")
        }
        return writableDatabase.insert("cash_sessions", null, cv)
    }

    fun getActiveCashSession(): CashSession? {
        val cursor = readableDatabase.rawQuery("SELECT * FROM cash_sessions WHERE status = 'open' ORDER BY id DESC LIMIT 1", null)
        cursor.use { c ->
            if (c.moveToNext()) {
                return cursorToCashSession(c)
            }
        }
        return null
    }

    fun getLatestClosedFloat(): Int {
        val cursor = readableDatabase.rawQuery("SELECT base_retained_cents FROM cash_sessions WHERE status = 'closed' ORDER BY id DESC LIMIT 1", null)
        cursor.use { c ->
            if (c.moveToNext()) {
                val f = c.getInt(0)
                if (f > 0) return f
            }
        }
        return 5000
    }

    fun addTransaction(sessionId: Long, type: TransactionType, amountCents: Int, purpose: String? = null, photoPath: String? = null, donorName: String? = null, itemsJson: String? = null): Long {
        val cv = ContentValues().apply {
            put("session_id", sessionId)
            put("type", type.name)
            put("amount_cents", amountCents)
            put("purpose", purpose)
            put("receipt_photo_path", photoPath)
            put("donor_or_member_name", donorName)
            put("timestamp", System.currentTimeMillis())
            put("items_json", itemsJson)
        }
        val txId = writableDatabase.insert("cash_transactions", null, cv)

        when (type) {
            TransactionType.SALE -> writableDatabase.execSQL("UPDATE cash_sessions SET total_sales_cents = total_sales_cents + ? WHERE id = ?", arrayOf(amountCents, sessionId))
            TransactionType.DONATION -> writableDatabase.execSQL("UPDATE cash_sessions SET total_donations_cents = total_donations_cents + ? WHERE id = ?", arrayOf(amountCents, sessionId))
            TransactionType.LIBRARY_FEE -> writableDatabase.execSQL("UPDATE cash_sessions SET total_library_fees_cents = total_library_fees_cents + ? WHERE id = ?", arrayOf(amountCents, sessionId))
            TransactionType.PAYOUT -> writableDatabase.execSQL("UPDATE cash_sessions SET total_payouts_cents = total_payouts_cents + ? WHERE id = ?", arrayOf(amountCents, sessionId))
        }
        return txId
    }

    fun getTransactionsForSession(sessionId: Long): List<CashTransaction> {
        val list = mutableListOf<CashTransaction>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM cash_transactions WHERE session_id = ? ORDER BY timestamp DESC", arrayOf(sessionId.toString()))
        cursor.use { c ->
            while (c.moveToNext()) {
                val typeName = c.getString(c.getColumnIndexOrThrow("type"))
                val type = try { TransactionType.valueOf(typeName) } catch (e: Exception) { TransactionType.SALE }
                list.add(
                    CashTransaction(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        sessionId = c.getLong(c.getColumnIndexOrThrow("session_id")),
                        type = type,
                        amountCents = c.getInt(c.getColumnIndexOrThrow("amount_cents")),
                        purpose = c.getString(c.getColumnIndexOrThrow("purpose")),
                        receiptPhotoPath = c.getString(c.getColumnIndexOrThrow("receipt_photo_path")),
                        donorOrMemberName = c.getString(c.getColumnIndexOrThrow("donor_or_member_name")),
                        timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp")),
                        itemsJson = c.getString(c.getColumnIndexOrThrow("items_json"))
                    )
                )
            }
        }
        return list
    }

    fun closeCashSession(sessionId: Long, countedCents: Int, diffCents: Int, baseRetainedCents: Int, skimRetainedCents: Int, notes: String?): Boolean {
        val cv = ContentValues().apply {
            put("counted_total_cents", countedCents)
            put("diff_cents", diffCents)
            put("base_retained_cents", baseRetainedCents)
            put("skim_retained_cents", skimRetainedCents)
            put("status", "closed")
            put("notes", notes)
        }
        val rows = writableDatabase.update("cash_sessions", cv, "id = ?", arrayOf(sessionId.toString()))
        return rows > 0
    }

    private fun cursorToCashSession(c: Cursor): CashSession {
        return CashSession(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            shiftId = if (c.isNull(c.getColumnIndexOrThrow("shift_id"))) null else c.getInt(c.getColumnIndexOrThrow("shift_id")),
            volunteerName = c.getString(c.getColumnIndexOrThrow("volunteer_name")),
            date = c.getString(c.getColumnIndexOrThrow("date")),
            startTime = c.getString(c.getColumnIndexOrThrow("start_time")),
            endTime = c.getString(c.getColumnIndexOrThrow("end_time")),
            openingFloatCents = c.getInt(c.getColumnIndexOrThrow("opening_float_cents")),
            totalSalesCents = c.getInt(c.getColumnIndexOrThrow("total_sales_cents")),
            totalDonationsCents = c.getInt(c.getColumnIndexOrThrow("total_donations_cents")),
            totalLibraryFeesCents = c.getInt(c.getColumnIndexOrThrow("total_library_fees_cents")),
            totalPayoutsCents = c.getInt(c.getColumnIndexOrThrow("total_payouts_cents")),
            countedTotalCents = if (c.isNull(c.getColumnIndexOrThrow("counted_total_cents"))) null else c.getInt(c.getColumnIndexOrThrow("counted_total_cents")),
            diffCents = if (c.isNull(c.getColumnIndexOrThrow("diff_cents"))) null else c.getInt(c.getColumnIndexOrThrow("diff_cents")),
            baseRetainedCents = c.getInt(c.getColumnIndexOrThrow("base_retained_cents")),
            skimRetainedCents = c.getInt(c.getColumnIndexOrThrow("skim_retained_cents")),
            status = c.getString(c.getColumnIndexOrThrow("status")),
            notes = c.getString(c.getColumnIndexOrThrow("notes"))
        )
    }

    // --- Library DAO ---
    fun searchBooks(query: String): List<LibraryBook> {
        val list = mutableListOf<LibraryBook>()
        val q = "%${query.trim()}%"
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM library_books WHERE title LIKE ? OR author LIKE ? OR isbn LIKE ? OR category LIKE ? ORDER BY title ASC LIMIT 50",
            arrayOf(q, q, q, q)
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    LibraryBook(
                        isbn = c.getString(c.getColumnIndexOrThrow("isbn")),
                        title = c.getString(c.getColumnIndexOrThrow("title")),
                        author = c.getString(c.getColumnIndexOrThrow("author")),
                        category = c.getString(c.getColumnIndexOrThrow("category")),
                        shelfLocation = c.getString(c.getColumnIndexOrThrow("shelf_location")),
                        isLent = c.getInt(c.getColumnIndexOrThrow("is_lent")) == 1,
                        lentTo = c.getString(c.getColumnIndexOrThrow("lent_to")),
                        dueDate = c.getString(c.getColumnIndexOrThrow("due_date"))
                    )
                )
            }
        }
        return list
    }

    fun getAllBooks(): List<LibraryBook> {
        val list = mutableListOf<LibraryBook>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM library_books ORDER BY title ASC LIMIT 200", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    LibraryBook(
                        isbn = c.getString(c.getColumnIndexOrThrow("isbn")),
                        title = c.getString(c.getColumnIndexOrThrow("title")),
                        author = c.getString(c.getColumnIndexOrThrow("author")),
                        category = c.getString(c.getColumnIndexOrThrow("category")),
                        shelfLocation = c.getString(c.getColumnIndexOrThrow("shelf_location")),
                        isLent = c.getInt(c.getColumnIndexOrThrow("is_lent")) == 1,
                        lentTo = c.getString(c.getColumnIndexOrThrow("lent_to")),
                        dueDate = c.getString(c.getColumnIndexOrThrow("due_date"))
                    )
                )
            }
        }
        return list
    }

    fun saveBooks(books: List<LibraryBook>) {
        writableDatabase.beginTransaction()
        try {
            for (b in books) {
                val cv = ContentValues().apply {
                    put("isbn", b.isbn)
                    put("title", b.title)
                    put("author", b.author)
                    put("category", b.category)
                    put("shelf_location", b.shelfLocation)
                    put("is_lent", if (b.isLent) 1 else 0)
                    put("lent_to", b.lentTo)
                    put("due_date", b.dueDate)
                }
                writableDatabase.insertWithOnConflict("library_books", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun borrowBook(isbn: String, title: String, borrowerName: String, borrowerContact: String?, loanDate: String, dueDate: String, feeCents: Int = 0): Long {
        val cvLoan = ContentValues().apply {
            put("isbn", isbn)
            put("title", title)
            put("borrower_name", borrowerName)
            put("borrower_contact", borrowerContact)
            put("loan_date", loanDate)
            put("due_date", dueDate)
            put("fee_cents", feeCents)
            put("status", "active")
        }
        val loanId = writableDatabase.insert("library_loans", null, cvLoan)

        val cvBook = ContentValues().apply {
            put("is_lent", 1)
            put("lent_to", borrowerName)
            put("due_date", dueDate)
        }
        writableDatabase.update("library_books", cvBook, "isbn = ?", arrayOf(isbn))
        return loanId
    }

    fun returnBook(isbn: String): Boolean {
        val cvLoan = ContentValues().apply {
            put("status", "returned")
        }
        writableDatabase.update("library_loans", cvLoan, "isbn = ? AND status = 'active'", arrayOf(isbn))

        val cvBook = ContentValues().apply {
            put("is_lent", 0)
            putNull("lent_to")
            putNull("due_date")
        }
        val rows = writableDatabase.update("library_books", cvBook, "isbn = ?", arrayOf(isbn))
        return rows > 0
    }

    // --- Cached Shifts DAO ---
    fun saveShifts(shifts: List<ShiftItem>) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("cached_shifts", null, null)
            for (s in shifts) {
                val cv = ContentValues().apply {
                    put("id", s.id)
                    put("date", s.date)
                    put("time_slot", s.timeSlot)
                    put("start_time", s.startTime)
                    put("end_time", s.endTime)
                    put("hours", s.hours)
                    put("member_name", s.memberName)
                    put("member_color", s.memberColor)
                    put("location", s.location)
                    put("notes", s.notes)
                }
                writableDatabase.insertWithOnConflict("cached_shifts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun getTodayCachedShifts(): List<ShiftItem> {
        val list = mutableListOf<ShiftItem>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM cached_shifts ORDER BY start_time ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    ShiftItem(
                        id = c.getInt(c.getColumnIndexOrThrow("id")),
                        date = c.getString(c.getColumnIndexOrThrow("date")),
                        timeSlot = c.getString(c.getColumnIndexOrThrow("time_slot")),
                        startTime = c.getString(c.getColumnIndexOrThrow("start_time")),
                        endTime = c.getString(c.getColumnIndexOrThrow("end_time")),
                        hours = if (c.isNull(c.getColumnIndexOrThrow("hours"))) null else c.getDouble(c.getColumnIndexOrThrow("hours")),
                        memberName = c.getString(c.getColumnIndexOrThrow("member_name")),
                        memberColor = c.getString(c.getColumnIndexOrThrow("member_color")),
                        location = c.getString(c.getColumnIndexOrThrow("location")),
                        notes = c.getString(c.getColumnIndexOrThrow("notes"))
                    )
                )
            }
        }
        return list
    }
}
