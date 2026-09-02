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
    var payoutAmountInput by remember { mutableStateOf("") }
    var payoutPurpose by remember { mutableStateOf("Milch / Hafermilch") }

    var showDonationDialog by remember { mutableStateOf(false) }
    var donationAmountInput by remember { mutableStateOf("") }
    var donationPurpose by remember { mutableStateOf("Allgemeine Spende") }
    var donorNameInput by remember { mutableStateOf("") }

    var activeSession by remember { mutableStateOf<CashSession?>(null) }

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
        val cleanVal = payoutAmountInput.replace(",", ".").trim()
        val cents = ((cleanVal.toDoubleOrNull() ?: 0.0) * 100).toInt()
        if (cents <= 0) {
            Toast.makeText(context, "Bitte einen gültigen Betrag eingeben", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionId = prefs.activeSessionId ?: 1L
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
        showPayoutDialog = false
        payoutAmountInput = ""
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
                    val found = itemsList.find { it.barcode == barcode }
                    if (found != null) {
                        addToCart(found.id)
                        Toast.makeText(context, "${found.name} hinzugefügt", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Unbekannter Barcode: $barcode", Toast.LENGTH_SHORT).show()
                    }
                },
                onClose = { isScannerOpen = false }
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

        // Dialog: Entnahme buchen (Barauszahlung)
        if (showPayoutDialog) {
            AlertDialog(
                onDismissRequest = { showPayoutDialog = false },
                icon = {
                    Icon(Icons.Default.MoneyOff, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp))
                },
                title = { Text("Barauszahlung / Entnahme buchen", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Trage hier Betrag und Verwendungszweck ein. Die Kasse verringert ihren Soll-Bestand entsprechend.", fontSize = 13.sp, color = Color(0xFF4B5563))

                        OutlinedTextField(
                            value = payoutAmountInput,
                            onValueChange = { payoutAmountInput = it },
                            label = { Text("Betrag in €") },
                            placeholder = { Text("z. B. 4,50") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Verwendungszweck:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        val chips = listOf("Milch / Hafermilch", "Kaffeebohnen", "Obst / Snacks", "Auslagen-Erstattung", "Tresor-Einwurf", "Sonstiges")
                        chips.chunked(2).forEach { row ->
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
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { bookPayout() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Entnahme buchen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPayoutDialog = false }) {
                        Text("Abbrechen")
                    }
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
