package org.sprachcafe.team.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.sprachcafe.team.data.*
import org.sprachcafe.team.ui.components.BarcodeScannerView
import org.sprachcafe.team.ui.theme.SprachCafeRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KioskScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dbHelper = remember { TeamDatabaseHelper.getInstance(context) }
    val prefs = remember { TeamPreferences.getInstance(context) }

    var itemsList by remember { mutableStateOf<List<KioskItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    var cart by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isCartOpen by remember { mutableStateOf(false) }
    var isScannerOpen by remember { mutableStateOf(false) }

    // Dialog States
    var showPayoutDialog by remember { mutableStateOf(false) }
    var payoutCategory by remember { mutableStateOf(PayoutCategory.PURCHASE) }
    var payoutAmountInput by remember { mutableStateOf("") }
    var payoutPurpose by remember { mutableStateOf("Kiosk-Nachkauf / Vorräte") }
    var receiptArticles by remember { mutableStateOf<List<ReceiptArticleItem>>(emptyList()) }
    var isPayoutScannerOpen by remember { mutableStateOf(false) }

    // Sub-item entry inside receipt dialog
    var selectedItemForReceipt by remember { mutableStateOf<KioskItem?>(null) }
    var showArticlePickerDropdown by remember { mutableStateOf(false) }
    var articlePickerQuery by remember { mutableStateOf("") }
    var itemQtyInput by remember { mutableStateOf("1") }
    var itemCostInput by remember { mutableStateOf("") }
    var itemMhdInput by remember { mutableStateOf("") }
    var updateCatalogCostChecked by remember { mutableStateOf(true) }

    // Dialog for unknown barcode: Link or Create
    var unknownScannedBarcode by remember { mutableStateOf<String?>(null) }
    var showUnknownBarcodeDialog by remember { mutableStateOf(false) }
    var showLinkBarcodePicker by remember { mutableStateOf(false) }
    var showCreateArticleDialog by remember { mutableStateOf(false) }
    var newArticleName by remember { mutableStateOf("") }
    var newArticleCategory by remember { mutableStateOf(ItemCategory.SNACKS) }
    var newArticlePrice by remember { mutableStateOf("2.00") }
    var newArticleCost by remember { mutableStateOf("1.00") }
    var newArticleUnit by remember { mutableStateOf("Stk") }

    var showDonationDialog by remember { mutableStateOf(false) }
    var donationAmountInput by remember { mutableStateOf("") }
    var donationPurpose by remember { mutableStateOf("Allgemeine Spende") }
    var donorNameInput by remember { mutableStateOf("") }

    var activeSession by remember { mutableStateOf<CashSession?>(null) }

    // Club Member States
    var scannedClubMember by remember { mutableStateOf<ClubMemberVerification?>(null) }
    var scannedMemberQrToken by remember { mutableStateOf("") }
    var showClubMemberDialog by remember { mutableStateOf(false) }
    var isMemberRedeeming by remember { mutableStateOf(false) }

    fun refreshItems() {
        itemsList = dbHelper.getAllKioskItems()
        activeSession = dbHelper.getActiveCashSession()

        // Background sync from server
        coroutineScope.launch {
            ApiClient.fetchArticles().onSuccess { fetched ->
                dbHelper.saveKioskItems(fetched)
                itemsList = dbHelper.getAllKioskItems()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshItems()
    }

    val filteredItems = remember(selectedCategory, itemsList) {
        if (selectedCategory == null) itemsList
        else itemsList.filter { it.category == selectedCategory }
    }

    val totalCents = remember(cart, itemsList) {
        cart.entries.sumOf { (id, qty) ->
            (itemsList.find { it.id == id }?.priceCents ?: 0) * qty
        }
    }
    val totalItemsCount = remember(cart) { cart.values.sum() }

    fun addToCart(itemId: String) {
        val current = cart[itemId] ?: 0
        cart = cart + (itemId to current + 1)
    }

    fun removeFromCart(itemId: String) {
        val current = cart[itemId] ?: 0
        if (current <= 1) {
            cart = cart - itemId
        } else {
            cart = cart + (itemId to current - 1)
        }
    }

    fun clearCart() {
        cart = emptyMap()
        isCartOpen = false
    }

    fun checkoutCash() {
        if (cart.isEmpty()) return
        val sessionId = prefs.activeSessionId ?: 1L

        // Record sale transaction
        val itemsSummary = cart.map { (id, qty) ->
            val name = itemsList.find { it.id == id }?.name ?: id
            "$qty x $name"
        }.joinToString(", ")

        dbHelper.addTransaction(
            sessionId = sessionId,
            type = TransactionType.SALE,
            amountCents = totalCents,
            purpose = "Café-Verkauf ($itemsSummary)",
            itemsJson = itemsSummary
        )

        // Asynchronous background sync
        coroutineScope.launch {
            ApiClient.addTransaction(
                sessionId = sessionId,
                type = "SALE",
                amountCents = totalCents,
                purpose = "Café-Verkauf",
                donorName = null,
                itemsJson = itemsSummary
            )
        }

        Toast.makeText(context, "✅ Barzahlung über ${String.format("%.2f €", totalCents / 100.0)} gebucht!", Toast.LENGTH_SHORT).show()
        clearCart()
        refreshItems()
    }

    fun bookPayout() {
        val sessionId = prefs.activeSessionId ?: 1L
        val cleanVal = payoutAmountInput.replace(",", ".").trim()
        var cents = ((cleanVal.toDoubleOrNull() ?: 0.0) * 100).toInt()

        if (payoutCategory == PayoutCategory.PURCHASE) {
            val totalArticleCost = receiptArticles.sumOf { it.totalCostCents }
            if (cents <= 0 && totalArticleCost > 0) {
                cents = totalArticleCost
            }
            if (cents <= 0 && receiptArticles.isEmpty()) {
                Toast.makeText(context, "Bitte einen Betrag oder Artikel erfassen", Toast.LENGTH_SHORT).show()
                return
            }

            // Build JSON array of articles for stock intake
            val itemsJson = org.json.JSONArray().apply {
                receiptArticles.forEach { a ->
                    put(org.json.JSONObject().apply {
                        put("id", a.itemId)
                        put("name", a.name)
                        put("category", a.category.name)
                        put("unit", a.unit)
                        put("qty", a.qty)
                        put("costCents", a.costCents)
                        put("sellingCents", a.sellingCents)
                        put("mhd", a.mhd)
                        put("barcode", a.barcode)
                        put("updateCatalogCost", a.updateCatalogCost)
                    })
                }
            }.toString()

            val summaryText = if (receiptArticles.isNotEmpty()) {
                receiptArticles.joinToString(", ") { "${it.qty}x ${it.name}" }
            } else {
                payoutPurpose
            }

            dbHelper.addTransaction(
                sessionId = sessionId,
                type = TransactionType.PAYOUT,
                amountCents = cents,
                purpose = "Einkauf: $summaryText",
                itemsJson = itemsJson
            )

            coroutineScope.launch {
                ApiClient.addTransaction(
                    sessionId = sessionId,
                    type = "PAYOUT",
                    amountCents = cents,
                    purpose = "Wareneinkauf: $summaryText",
                    donorName = prefs.memberName ?: "Schichthelfer",
                    itemsJson = itemsJson
                )
            }

            Toast.makeText(context, "✅ Wareneinkauf über ${String.format("%.2f €", cents / 100.0)} gebucht. Bestand erhöht!", Toast.LENGTH_LONG).show()
        } else if (payoutCategory == PayoutCategory.OUTTAKE) {
            if (receiptArticles.isEmpty()) {
                Toast.makeText(context, "Bitte mindestens einen Artikel zur Entnahme auswählen", Toast.LENGTH_SHORT).show()
                return
            }

            val itemsJson = org.json.JSONArray().apply {
                receiptArticles.forEach { a ->
                    put(org.json.JSONObject().apply {
                        put("id", a.itemId)
                        put("qty", a.qty)
                    })
                }
            }.toString()

            coroutineScope.launch {
                ApiClient.addTransaction(
                    sessionId = sessionId,
                    type = "CONSUMPTION",
                    amountCents = 0,
                    purpose = payoutPurpose.ifBlank { "Warenentnahme / Event" },
                    donorName = prefs.memberName ?: "Schichthelfer",
                    itemsJson = itemsJson
                )
            }

            Toast.makeText(context, "📦 Warenentnahme gebucht. Bestand reduziert!", Toast.LENGTH_LONG).show()
        } else {
            // PURE_CASH
            if (cents <= 0) {
                Toast.makeText(context, "Bitte einen gültigen Betrag eingeben", Toast.LENGTH_SHORT).show()
                return
            }

            dbHelper.addTransaction(
                sessionId = sessionId,
                type = TransactionType.PAYOUT,
                amountCents = cents,
                purpose = payoutPurpose
            )

            coroutineScope.launch {
                ApiClient.addTransaction(
                    sessionId = sessionId,
                    type = "PAYOUT",
                    amountCents = cents,
                    purpose = payoutPurpose,
                    donorName = null,
                    itemsJson = null
                )
            }

            Toast.makeText(context, "Barauszahlung von ${String.format("%.2f €", cents / 100.0)} gebucht.", Toast.LENGTH_SHORT).show()
        }

        showPayoutDialog = false
        payoutAmountInput = ""
        receiptArticles = emptyList()
        selectedItemForReceipt = null
        refreshItems()
    }

    fun bookDonation() {
        val cleanVal = donationAmountInput.replace(",", ".").trim()
        val cents = ((cleanVal.toDoubleOrNull() ?: 0.0) * 100).toInt()
        if (cents <= 0) {
            Toast.makeText(context, "Bitte einen Betrag eingeben", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionId = prefs.activeSessionId ?: 1L
        dbHelper.addTransaction(
            sessionId = sessionId,
            type = TransactionType.DONATION,
            amountCents = cents,
            purpose = donationPurpose,
            donorName = donorNameInput.takeIf { it.isNotEmpty() }
        )

        coroutineScope.launch {
            ApiClient.addTransaction(
                sessionId = sessionId,
                type = "DONATION",
                amountCents = cents,
                purpose = donationPurpose,
                donorName = donorNameInput.takeIf { it.isNotEmpty() },
                itemsJson = null
            )
        }

        Toast.makeText(context, "💛 Spende von ${String.format("%.2f €", cents / 100.0)} erfasst. Herzlichen Dank!", Toast.LENGTH_SHORT).show()
        showDonationDialog = false
        donationAmountInput = ""
        donorNameInput = ""
        refreshItems()
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF9F7F4))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kasse & Theke",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SprachCafeRed
                    )
                    Text(
                        text = if (prefs.isCashActive) "Kasse aktiv • Helfer: ${prefs.memberName ?: "--"}" else "Schicht ohne Kasse",
                        fontSize = 12.sp,
                        color = if (prefs.isCashActive) Color(0xFF059669) else Color(0xFF6B7280)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Spende Button
                    OutlinedButton(
                        onClick = { showDonationDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB45309)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("💛 Spende", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Entnahme Button
                    OutlinedButton(
                        onClick = { showPayoutDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("📤 Entnahme", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Barcode Scanner Button
                    IconButton(
                        onClick = { isScannerOpen = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner",
                            tint = SprachCafeRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Alle Artikel") }
                    )
                }
                items(ItemCategory.values()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.labelDe) }
                    )
                }
            }

            // Product Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems) { item ->
                    val inCart = cart[item.id] ?: 0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { addToCart(item.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = item.icon, fontSize = 24.sp)
                                if (inCart > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SprachCafeRed,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = inCart.toString(),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 2,
                                color = Color(0xFF1F2937)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.priceEurFormatted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SprachCafeRed
                                )
                                Text(
                                    text = item.unit,
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Floating Cart Bar
            if (totalItemsCount > 0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isCartOpen = true },
                    color = SprachCafeRed,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Badge(containerColor = Color.White) {
                                Text(
                                    text = totalItemsCount.toString(),
                                    color = SprachCafeRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Warenkorb ansehen",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Text(
                            text = String.format("%.2f €", totalCents / 100.0),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // Barcode Scanner View
        if (isScannerOpen) {
            BarcodeScannerView(
                onBarcodeScanned = { barcode ->
                    if (barcode.startsWith("SCP-MEMBER:")) {
                        isScannerOpen = false
                        coroutineScope.launch {
                            ApiClient.verifyClubMember(barcode).onSuccess { member ->
                                scannedClubMember = member
                                scannedMemberQrToken = barcode
                                showClubMemberDialog = true
                            }.onFailure { err ->
                                Toast.makeText(context, "Ungültiger Mitgliedsausweis: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        val found = itemsList.find { it.barcode == barcode }
                        if (found != null) {
                            addToCart(found.id)
                            Toast.makeText(context, "${found.name} hinzugefügt", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Unbekannter Barcode: $barcode", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onClose = { isScannerOpen = false }
            )
        }

        // Club Member Verification & Free Coffee Redemption Dialog
        if (showClubMemberDialog && scannedClubMember != null) {
            val member = scannedClubMember!!
            AlertDialog(
                onDismissRequest = { showClubMemberDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("☕", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "Vereinsmitglied erkannt",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = SprachCafeRed
                            )
                            Text(
                                text = "${member.memberNumber} • ${member.tier}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFAF5EB),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = member.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1D1B1A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Status:", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        text = if (member.valid) "Gültig ✓" else "Inaktiv / Ausstehend",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (member.valid) Color(0xFF16A34A) else Color.Red
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Event-Rabatt:", fontSize = 12.sp, color = Color.Gray)
                                    Text("${member.eventDiscountPct} %", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Freikaffee-Guthaben:", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        text = "☕ ${member.coffeeQuotaRemaining} übrig",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (member.coffeeQuotaRemaining > 0) SprachCafeRed else Color.Gray
                                    )
                                }
                            }
                        }

                        if (member.coffeeQuotaRemaining > 0) {
                            Button(
                                onClick = {
                                    isMemberRedeeming = true
                                    coroutineScope.launch {
                                        val sessId = prefs.activeSessionId ?: 1L
                                        val res = ApiClient.redeemMemberCoffee(scannedMemberQrToken, sessId.toString())
                                        isMemberRedeeming = false
                                        if (res.isSuccess) {
                                            val remaining = res.getOrDefault(member.coffeeQuotaRemaining - 1)
                                            dbHelper.addTransaction(
                                                sessionId = sessId,
                                                type = TransactionType.SALE,
                                                amountCents = 0,
                                                purpose = "☕ Freikaffee: ${member.name} (${member.memberNumber})",
                                                donorName = member.name
                                            )
                                            Toast.makeText(
                                                context,
                                                "☕ Freikaffee gebucht! Noch $remaining Kaffees übrig.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            showClubMemberDialog = false
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Fehler: ${res.exceptionOrNull()?.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                enabled = !isMemberRedeeming,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SprachCafeRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isMemberRedeeming) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("☕ 1 Freikaffee einlösen (0,00 €)", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "Kein Freikaffee-Guthaben mehr vorhanden.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showClubMemberDialog = false }) {
                        Text("Schließen", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Cart Bottom Sheet
        if (isCartOpen) {
            ModalBottomSheet(
                onDismissRequest = { isCartOpen = false },
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aktuelle Bestellung ($totalItemsCount Artikel)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { clearCart() }) {
                            Text("Leeren", color = Color.Red)
                        }
                    }

                    cart.forEach { (id, qty) ->
                        val item = itemsList.find { it.id == id }
                        if (item != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("${item.priceEurFormatted} / ${item.unit}", fontSize = 12.sp, color = Color.Gray)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { removeFromCart(id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Minus", tint = Color.Gray)
                                    }
                                    Text(qty.toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    IconButton(
                                        onClick = { addToCart(id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Plus", tint = SprachCafeRed)
                                    }
                                }
                            }
                        }
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gesamtbetrag (Bar):", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            String.format("%.2f €", totalCents / 100.0),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = SprachCafeRed
                        )
                    }

                    Button(
                        onClick = { checkoutCash() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Bar kassiert: ${String.format("%.2f €", totalCents / 100.0)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Dialog: Entnahme buchen (Wareneinkauf / Warenentnahme / Reine Barauszahlung)
        if (showPayoutDialog) {
            val totalArticleCost = remember(receiptArticles) { receiptArticles.sumOf { it.totalCostCents } }
            val scrollState = rememberScrollState()

            AlertDialog(
                onDismissRequest = { showPayoutDialog = false },
                icon = {
                    Icon(
                        when (payoutCategory) {
                            PayoutCategory.PURCHASE -> Icons.Default.ShoppingCart
                            PayoutCategory.OUTTAKE -> Icons.Default.Inventory2
                            PayoutCategory.PURE_CASH -> Icons.Default.Payments
                        },
                        contentDescription = null,
                        tint = when (payoutCategory) {
                            PayoutCategory.PURCHASE -> Color(0xFF059669)
                            PayoutCategory.OUTTAKE -> Color(0xFFD97706)
                            PayoutCategory.PURE_CASH -> Color(0xFFDC2626)
                        },
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        when (payoutCategory) {
                            PayoutCategory.PURCHASE -> "Wareneinkauf buchen (Beleg)"
                            PayoutCategory.OUTTAKE -> "Warenentnahme buchen"
                            PayoutCategory.PURE_CASH -> "Reine Barauszahlung"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Category Selector
                        Text("Art der Buchung:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = payoutCategory == PayoutCategory.PURCHASE,
                                onClick = { payoutCategory = PayoutCategory.PURCHASE },
                                label = { Text("🛍️ Einkauf", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = payoutCategory == PayoutCategory.OUTTAKE,
                                onClick = { payoutCategory = PayoutCategory.OUTTAKE },
                                label = { Text("📦 Entnahme", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = payoutCategory == PayoutCategory.PURE_CASH,
                                onClick = { payoutCategory = PayoutCategory.PURE_CASH },
                                label = { Text("💵 Barausz.", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (payoutCategory == PayoutCategory.PURE_CASH) {
                            Text(
                                "Reine Barauszahlung ohne Waren-/Bestandsbezug. Verringert den Soll-Kassenbestand.",
                                fontSize = 12.sp,
                                color = Color(0xFF4B5563)
                            )
                            OutlinedTextField(
                                value = payoutAmountInput,
                                onValueChange = { payoutAmountInput = it },
                                label = { Text("Betrag in € *") },
                                placeholder = { Text("z. B. 15,00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Verwendungszweck:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val cashPurposes = listOf("Auslagen-Erstattung", "Reinigungsmittel", "Tresor-Einwurf", "Gebühren / Porto", "Sonstiges")
                            cashPurposes.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { chip ->
                                        FilterChip(
                                            selected = payoutPurpose == chip,
                                            onClick = { payoutPurpose = chip },
                                            label = { Text(chip, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        } else {
                            // PURCHASE or OUTTAKE: Article intake/outtake
                            Text(
                                if (payoutCategory == PayoutCategory.PURCHASE)
                                    "Erfasse gekaufte Artikel (EAN-Scan oder manuell). Der Kassenbestand sinkt um den Belegbetrag und die Lagerbestände werden aufgestockt."
                                else
                                    "Erfasse entnommene Artikel (z. B. für Kurse, Events, Eigenverbrauch). Die Lagerbestände sinken, der Kassenbestand bleibt unverändert.",
                                fontSize = 12.sp,
                                color = Color(0xFF4B5563)
                            )

                            // Action buttons: Barcode Scan & Select from list
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { isPayoutScannerOpen = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SprachCafeRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("EAN-Scan", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { showArticlePickerDropdown = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Artikel suchen", fontSize = 12.sp)
                                }
                            }

                            // Active article being configured
                            if (selectedItemForReceipt != null) {
                                val curItem = selectedItemForReceipt!!
                                val enteredCostCents = ((itemCostInput.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100).toInt()
                                val sellingPriceCents = curItem.priceCents
                                val margin = if (sellingPriceCents > 0) Math.round(((sellingPriceCents - enteredCostCents).toDouble() / sellingPriceCents) * 100).toInt() else 0
                                val suggestedVkCents = Math.round(enteredCostCents * 1.8).toInt()

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF3F4F6),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(curItem.icon, fontSize = 20.sp)
                                                Column {
                                                    Text(curItem.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Katalog-VK: ${curItem.priceEurFormatted}", fontSize = 11.sp, color = Color(0xFF6B7280))
                                                }
                                            }
                                            IconButton(
                                                onClick = { selectedItemForReceipt = null },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedTextField(
                                                value = itemQtyInput,
                                                onValueChange = { itemQtyInput = it },
                                                label = { Text("Menge (${curItem.unit})") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (payoutCategory == PayoutCategory.PURCHASE) {
                                                OutlinedTextField(
                                                    value = itemCostInput,
                                                    onValueChange = { itemCostInput = it },
                                                    label = { Text("EK (€ / ${curItem.unit})") },
                                                    placeholder = { Text("z. B. 0,90") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1.2f)
                                                )
                                            }
                                        }

                                        // EK/VK Margin Check Banner
                                        if (payoutCategory == PayoutCategory.PURCHASE && enteredCostCents > 0) {
                                            if (sellingPriceCents <= enteredCostCents || margin < 25) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFFEE2E2),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                                            Text(
                                                                if (sellingPriceCents <= enteredCostCents) "Achtung: VK liegt unter/am EK!" else "Warnung: Geringe Marge (${margin}%)!",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp,
                                                                color = Color(0xFF991B1B)
                                                            )
                                                        }
                                                        Text(
                                                            "EK: ${String.format("%.2f €", enteredCostCents / 100.0)} • VK: ${curItem.priceEurFormatted}. Empfohlener VK: ${String.format("%.2f €", suggestedVkCents / 100.0)}",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF7F1D1D)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // MHD Date input & quick buttons
                                        if (payoutCategory == PayoutCategory.PURCHASE) {
                                            OutlinedTextField(
                                                value = itemMhdInput,
                                                onValueChange = { itemMhdInput = it },
                                                label = { Text("MHD (JJJJ-MM-TT)") },
                                                placeholder = { Text("z. B. 2026-12-31") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // Quick MHD chips
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf(1 to "+1M", 3 to "+3M", 6 to "+6M", 12 to "+1J").forEach { (months, label) ->
                                                    OutlinedButton(
                                                        onClick = {
                                                            val cal = Calendar.getInstance()
                                                            cal.add(Calendar.MONTH, months)
                                                            itemMhdInput = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(cal.time)
                                                        },
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(label, fontSize = 10.sp)
                                                    }
                                                }
                                                OutlinedButton(
                                                    onClick = { itemMhdInput = "" },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Kein", fontSize = 10.sp)
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val qty = itemQtyInput.toIntOrNull() ?: 1
                                                if (qty <= 0) return@Button
                                                val cost = if (payoutCategory == PayoutCategory.PURCHASE) enteredCostCents else curItem.costCents
                                                val mhd = itemMhdInput.trim().takeIf { it.isNotEmpty() }

                                                val newArticle = ReceiptArticleItem(
                                                    itemId = curItem.id,
                                                    name = curItem.name,
                                                    category = curItem.category,
                                                    unit = curItem.unit,
                                                    qty = qty,
                                                    costCents = cost,
                                                    sellingCents = curItem.priceCents,
                                                    mhd = mhd,
                                                    barcode = curItem.barcode,
                                                    updateCatalogCost = updateCatalogCostChecked
                                                )
                                                receiptArticles = receiptArticles + newArticle
                                                selectedItemForReceipt = null
                                                itemQtyInput = "1"
                                                itemCostInput = ""
                                                itemMhdInput = ""
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Zu Beleg hinzufügen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // List of added articles
                            if (receiptArticles.isNotEmpty()) {
                                Text("Erfasste Artikel (${receiptArticles.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                receiptArticles.forEach { a ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("${a.qty}x ${a.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(
                                                    if (payoutCategory == PayoutCategory.PURCHASE)
                                                        "${a.totalCostEurFormatted} (${String.format("%.2f €", a.costCents / 100.0)} / ${a.unit})${if (a.mhd != null) " • MHD: ${a.mhd}" else ""}"
                                                    else
                                                        "${a.qty} ${a.unit} entnommen",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF6B7280)
                                                )
                                            }
                                            IconButton(
                                                onClick = { receiptArticles = receiptArticles - a },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                if (payoutCategory == PayoutCategory.PURCHASE) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFECFDF5),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Beleg-Gesamtsumme:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF065F46))
                                            Text(
                                                String.format("%.2f €", totalArticleCost / 100.0),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                    }
                                }
                            }

                            // Optional Manual Overwrite / Purpose
                            OutlinedTextField(
                                value = payoutPurpose,
                                onValueChange = { payoutPurpose = it },
                                label = { Text("Notiz / Einkaufsort") },
                                placeholder = { Text("z. B. Metro, BioMarkt, Bäckerei") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { bookPayout() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (payoutCategory) {
                                PayoutCategory.PURCHASE -> Color(0xFF059669)
                                PayoutCategory.OUTTAKE -> Color(0xFFD97706)
                                PayoutCategory.PURE_CASH -> Color(0xFFDC2626)
                            }
                        )
                    ) {
                        Text(
                            when (payoutCategory) {
                                PayoutCategory.PURCHASE -> "Einkauf buchen (${if (totalArticleCost > 0) String.format("%.2f €", totalArticleCost / 100.0) else payoutAmountInput + " €"})"
                                PayoutCategory.OUTTAKE -> "Entnahme buchen (${receiptArticles.size} Artikel)"
                                PayoutCategory.PURE_CASH -> "Auszahlung buchen"
                            }
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPayoutDialog = false }) {
                        Text("Abbrechen")
                    }
                }
            )
        }

        // Barcode Scanner Modal for Payout
        if (isPayoutScannerOpen) {
            BarcodeScannerView(
                onBarcodeScanned = { scanned ->
                    isPayoutScannerOpen = false
                    val found = itemsList.find { it.barcode == scanned }
                    if (found != null) {
                        selectedItemForReceipt = found
                        itemCostInput = String.format(Locale.US, "%.2f", found.costCents / 100.0)
                        itemQtyInput = "1"
                        Toast.makeText(context, "✅ Gefunden: ${found.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        unknownScannedBarcode = scanned
                        showUnknownBarcodeDialog = true
                    }
                },
                onClose = { isPayoutScannerOpen = false }
            )
        }

        // Dialog: Unbekannter Barcode gescannt
        if (showUnknownBarcodeDialog) {
            AlertDialog(
                onDismissRequest = { showUnknownBarcodeDialog = false },
                icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = SprachCafeRed, modifier = Modifier.size(32.dp)) },
                title = { Text("Unbekannter Barcode: $unknownScannedBarcode", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text("Dieser Barcode ist bisher keinem Artikel im Sortiment zugeordnet. Wie möchtest du ihn erfassen?", fontSize = 13.sp)
                },
                confirmButton = {
                    Button(onClick = {
                        showUnknownBarcodeDialog = false
                        showCreateArticleDialog = true
                    }, colors = ButtonDefaults.buttonColors(containerColor = SprachCafeRed)) {
                        Text("Neuen Artikel anlegen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showUnknownBarcodeDialog = false
                        showLinkBarcodePicker = true
                    }) {
                        Text("Mit Artikel verknüpfen")
                    }
                }
            )
        }

        // Dialog: Mit bestehendem Artikel verknüpfen
        if (showLinkBarcodePicker) {
            AlertDialog(
                onDismissRequest = { showLinkBarcodePicker = false },
                title = { Text("Artikel zum Verknüpfen wählen", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsList.forEach { itm ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val bc = unknownScannedBarcode ?: return@clickable
                                        coroutineScope.launch {
                                            ApiClient.linkBarcode(itm.id, bc)
                                            dbHelper.updateItemBarcode(itm.id, bc)
                                            refreshItems()
                                            selectedItemForReceipt = itm.copy(barcode = bc)
                                            itemCostInput = String.format(Locale.US, "%.2f", itm.costCents / 100.0)
                                            itemQtyInput = "1"
                                            showLinkBarcodePicker = false
                                            Toast.makeText(context, "Barcode verknüpft mit ${itm.name}!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${itm.icon} ${itm.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(itm.priceEurFormatted, fontSize = 12.sp, color = SprachCafeRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showLinkBarcodePicker = false }) { Text("Abbrechen") }
                }
            )
        }

        // Dialog: Neuen Artikel im Sortiment anlegen
        if (showCreateArticleDialog) {
            AlertDialog(
                onDismissRequest = { showCreateArticleDialog = false },
                title = { Text("Neuen Artikel anlegen", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newArticleName,
                            onValueChange = { newArticleName = it },
                            label = { Text("Artikelname *") },
                            placeholder = { Text("z. B. Bio Hafermilch 1L") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newArticlePrice,
                                onValueChange = { newArticlePrice = it },
                                label = { Text("VK in € *") },
                                placeholder = { Text("2.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newArticleCost,
                                onValueChange = { newArticleCost = it },
                                label = { Text("EK in €") },
                                placeholder = { Text("1.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text("Barcode: ${unknownScannedBarcode ?: "Keiner"}", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val name = newArticleName.trim()
                            if (name.isEmpty()) return@Button
                            val vkCents = ((newArticlePrice.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100).toInt()
                            val ekCents = ((newArticleCost.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100).toInt()
                            val newItem = KioskItem(
                                id = "item-${System.currentTimeMillis()}",
                                name = name,
                                category = newArticleCategory,
                                unit = newArticleUnit,
                                priceCents = vkCents,
                                costCents = ekCents,
                                barcode = unknownScannedBarcode,
                                icon = "📦"
                            )
                            coroutineScope.launch {
                                ApiClient.saveArticle(newItem)
                                dbHelper.upsertSingleKioskItem(newItem)
                                refreshItems()
                                selectedItemForReceipt = newItem
                                itemCostInput = String.format(Locale.US, "%.2f", ekCents / 100.0)
                                itemQtyInput = "1"
                                showCreateArticleDialog = false
                                newArticleName = ""
                                Toast.makeText(context, "✅ Artikel angelegt & ausgewählt!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SprachCafeRed)
                    ) {
                        Text("Artikel speichern")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateArticleDialog = false }) { Text("Abbrechen") }
                }
            )
        }

        // Dialog: Manuelle Artikelauswahl aus Kiosk-Sortiment
        if (showArticlePickerDropdown) {
            val filteredArticles = remember(itemsList, articlePickerQuery) {
                if (articlePickerQuery.isBlank()) itemsList
                else itemsList.filter {
                    it.name.contains(articlePickerQuery, ignoreCase = true) ||
                    (it.barcode != null && it.barcode.contains(articlePickerQuery))
                }
            }

            AlertDialog(
                onDismissRequest = { showArticlePickerDropdown = false },
                title = { Text("Artikel auswählen", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = articlePickerQuery,
                            onValueChange = { articlePickerQuery = it },
                            placeholder = { Text("Name oder Barcode suchen...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredArticles.forEach { itm ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedItemForReceipt = itm
                                            itemCostInput = String.format(Locale.US, "%.2f", itm.costCents / 100.0)
                                            itemQtyInput = "1"
                                            showArticlePickerDropdown = false
                                            articlePickerQuery = ""
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(itm.icon, fontSize = 18.sp)
                                            Text(itm.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text(itm.priceEurFormatted, fontSize = 12.sp, color = SprachCafeRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showArticlePickerDropdown = false }) { Text("Abbrechen") }
                }
            )
        }

        // Dialog: Spende buchen (Ideelle Sphäre)
        if (showDonationDialog) {
            AlertDialog(
                onDismissRequest = { showDonationDialog = false },
                icon = {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(32.dp))
                },
                title = { Text("Spende erfassen (0% MwSt)", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Spenden für den Verein fließen in den Kassenstand ein und werden der ideellen Sphäre zugeordnet.", fontSize = 13.sp, color = Color(0xFF4B5563))

                        // Quick donation chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("2,00", "5,00", "10,00", "20,00").forEach { amount ->
                                OutlinedButton(
                                    onClick = { donationAmountInput = amount },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Text("$amount €", fontSize = 12.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = donationAmountInput,
                            onValueChange = { donationAmountInput = it },
                            label = { Text("Spendenbetrag in €") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Spendenzweck:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val donationPurposes = listOf("Allgemeine Spende", "Kinderprojekte", "Kulturveranstaltung")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            donationPurposes.forEach { p ->
                                FilterChip(
                                    selected = donationPurpose == p,
                                    onClick = { donationPurpose = p },
                                    label = { Text(p, fontSize = 10.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = donorNameInput,
                            onValueChange = { donorNameInput = it },
                            label = { Text("Spendername (optional)") },
                            placeholder = { Text("Für Spendenbescheinigung") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { bookDonation() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309))
                    ) {
                        Text("Spende erfassen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDonationDialog = false }) {
                        Text("Abbrechen")
                    }
                }
            )
        }
    }
}
