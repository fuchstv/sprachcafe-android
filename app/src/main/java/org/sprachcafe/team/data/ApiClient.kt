package org.sprachcafe.team.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val BASE_URL = "https://team.xn--sprachcaf-j4a.org/api"
    private const val TIMEOUT_MS = 6000

    suspend fun fetchTodayShifts(): Result<List<ShiftItem>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/shifts/today")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val arr = json.optJSONArray("shifts") ?: JSONArray()
                val list = mutableListOf<ShiftItem>()
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    list.add(
                        ShiftItem(
                            id = s.getInt("id"),
                            date = s.getString("date"),
                            timeSlot = s.getString("time_slot"),
                            startTime = s.optString("start_time").takeIf { it.isNotEmpty() },
                            endTime = s.optString("end_time").takeIf { it.isNotEmpty() },
                            hours = if (s.isNull("hours")) null else s.optDouble("hours"),
                            memberName = s.optString("member_display_name", s.optString("member_name_raw")),
                            memberColor = s.optString("member_color").takeIf { it.isNotEmpty() },
                            location = s.optString("location", "Schulzestraße (Pankow)"),
                            notes = s.optString("notes").takeIf { it.isNotEmpty() }
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchMembers(): Result<List<TeamMember>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/members")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val arr = json.optJSONArray("members") ?: JSONArray()
                val list = mutableListOf<TeamMember>()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    list.add(
                        TeamMember(
                            id = m.getInt("id"),
                            name = m.getString("name"),
                            shortCode = m.getString("short_code"),
                            email = m.optString("email").takeIf { it.isNotEmpty() },
                            color = m.optString("color", "#8B1E2D"),
                            role = m.optString("role", "Mitarbeiter")
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchArticles(): Result<List<KioskItem>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/articles")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val arr = json.optJSONArray("items") ?: JSONArray()
                val list = mutableListOf<KioskItem>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val catName = item.getString("category")
                    val cat = try { ItemCategory.valueOf(catName) } catch (e: Exception) { ItemCategory.COLD_DRINKS }
                    list.add(
                        KioskItem(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            category = cat,
                            unit = item.optString("unit", "Stk"),
                            priceCents = item.getInt("price_cents"),
                            costCents = item.optInt("cost_cents", 0),
                            taxSphere = item.optString("tax_sphere", "Zweckbetrieb"),
                            barcode = item.optString("barcode").takeIf { it.isNotEmpty() },
                            icon = item.optString("icon", "☕"),
                            isActive = item.optInt("is_active", 1) == 1,
                            stockQuantity = item.optInt("stock_quantity", 0)
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLatestFloat(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/cash/latest-float")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                Result.success(json.optInt("openingFloatCents", 5000))
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startCashSession(shiftId: Int?, volunteerName: String, date: String, startTime: String?, openingFloatCents: Int): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/cash/sessions/start")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("shift_id", shiftId)
                put("volunteer_name", volunteerName)
                put("date", date)
                put("start_time", startTime)
                put("opening_float_cents", openingFloatCents)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..201) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                Result.success(json.optLong("sessionId"))
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTransaction(
        sessionId: Long,
        type: String,
        amountCents: Int,
        purpose: String?,
        donorName: String?,
        itemsJson: String?,
        receiptPhotoUrl: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/cash/sessions/$sessionId/transaction")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("type", type)
                put("amount_cents", amountCents)
                put("purpose", purpose)
                put("donor_or_member_name", donorName)
                put("items_json", itemsJson)
                if (!receiptPhotoUrl.isNullOrEmpty()) {
                    put("receipt_photo_url", receiptPhotoUrl)
                }
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..201) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                Result.success(json.optLong("transactionId"))
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun linkBarcode(itemId: String, barcode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/articles/link-barcode")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("itemId", itemId)
                put("barcode", barcode)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            if (conn.responseCode in 200..204) Result.success(true)
            else Result.failure(Exception("HTTP ${conn.responseCode}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveArticle(item: KioskItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/articles")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
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
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            if (conn.responseCode in 200..204) Result.success(true)
            else Result.failure(Exception("HTTP ${conn.responseCode}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitInventorySync(
        countedItems: Map<String, Int>,
        countedBy: String,
        notes: String? = null,
        location: String = "Schulzestraße (Pankow)"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/stock/inventory-sync")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val itemsObj = JSONObject()
            countedItems.forEach { (k, v) -> itemsObj.put(k, v) }

            val payload = JSONObject().apply {
                put("countedItems", itemsObj)
                put("countedBy", countedBy)
                put("notes", notes)
                put("location", location)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            if (conn.responseCode in 200..204) Result.success(true)
            else Result.failure(Exception("HTTP ${conn.responseCode}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeCashSession(sessionId: Long, countedCents: Int, diffCents: Int, baseCents: Int, skimCents: Int, salesCents: Int, donationsCents: Int, libraryFeesCents: Int, payoutsCents: Int, expectedCents: Int, notes: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/cash/sessions/$sessionId/close")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("counted_total_cents", countedCents)
                put("diff_cents", diffCents)
                put("base_retained_cents", baseCents)
                put("skim_retained_cents", skimCents)
                put("total_sales_cents", salesCents)
                put("total_donations_cents", donationsCents)
                put("total_library_fees_cents", libraryFeesCents)
                put("total_payouts_cents", payoutsCents)
                put("expected_total_cents", expectedCents)
                put("notes", notes)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode == 200) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitInventory(countedBy: String, date: String, items: Map<String, Int>, notes: String?): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/inventory")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("counted_by", countedBy)
                put("date", date)
                put("items", JSONObject(items))
                put("notes", notes)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..201) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                Result.success(json.optLong("logId"))
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchLibraryBooks(query: String?): Result<List<LibraryBook>> = withContext(Dispatchers.IO) {
        try {
            val qParam = if (!query.isNullOrBlank()) "?q=${java.net.URLEncoder.encode(query, "UTF-8")}" else ""
            val url = URL("$BASE_URL/library/books$qParam")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val arr = json.optJSONArray("books") ?: JSONArray()
                val list = mutableListOf<LibraryBook>()
                for (i in 0 until arr.length()) {
                    val b = arr.getJSONObject(i)
                    list.add(
                        LibraryBook(
                            isbn = b.optString("isbn").takeIf { it.isNotBlank() } ?: b.optString("signature").takeIf { it.isNotBlank() } ?: "LIB-${b.optInt("id", i + 1)}",
                            title = b.optString("title", "Ohne Titel"),
                            author = b.optString("author", "Unbekannt"),
                            category = b.optString("category").takeIf { it.isNotEmpty() },
                            shelfLocation = b.optString("shelf_location").takeIf { it.isNotEmpty() },
                            isLent = b.optInt("is_lent", 0) == 1,
                            lentTo = b.optString("lent_to").takeIf { it.isNotEmpty() },
                            dueDate = b.optString("due_date").takeIf { it.isNotEmpty() }
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun borrowBook(isbn: String, title: String, borrowerName: String, borrowerContact: String?, feeCents: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/library/loans")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("isbn", isbn)
                put("title", title)
                put("borrower_name", borrowerName)
                put("borrower_contact", borrowerContact)
                put("fee_cents", feeCents)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..201) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                Result.success(json.optString("dueDate", ""))
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun returnBook(isbn: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/library/return")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("isbn", isbn)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode == 200) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyClubMember(qrToken: String): Result<ClubMemberVerification> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(qrToken, "UTF-8")
            val url = URL("$BASE_URL/club-members/verify/$encoded")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val valid = json.optBoolean("valid", false)
                val m = json.getJSONObject("member")
                Result.success(
                    ClubMemberVerification(
                        valid = valid,
                        id = m.getInt("id"),
                        memberNumber = m.getString("member_number"),
                        name = m.getString("name"),
                        tier = m.getString("tier"),
                        status = m.getString("status"),
                        validUntil = m.optString("valid_until"),
                        coffeeQuotaRemaining = m.getInt("coffee_quota_remaining"),
                        eventDiscountPct = m.optInt("event_discount_pct", 0)
                    )
                )
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun redeemMemberCoffee(qrToken: String, sessionId: String?): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/club-members/redeem-coffee")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val payload = JSONObject().apply {
                put("qrToken", qrToken)
                if (sessionId != null) put("sessionId", sessionId)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val remaining = json.optInt("remaining", 0)
                Result.success(remaining)
            } else {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                val msg = try { JSONObject(errBody).optString("error", "Fehler") } catch (_: Exception) { "HTTP ${conn.responseCode}" }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
