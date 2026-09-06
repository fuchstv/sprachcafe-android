package org.sprachcafe.team.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.sprachcafe.team.data.*
import org.sprachcafe.team.ui.components.BarcodeScannerView
import org.sprachcafe.team.ui.theme.SprachCafeRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dbHelper = remember { TeamDatabaseHelper.getInstance(context) }
    val prefs = remember { TeamPreferences.getInstance(context) }

    var itemsList by remember { mutableStateOf<List<KioskItem>>(emptyList()) }
    var countedQuantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var itemBatches by remember { mutableStateOf<Map<String, List<InventoryBatchEntry>>>(emptyMap()) }

    var searchQuery by remember { mutableStateOf("") }
    var isScannerOpen by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var notesInput by remember { mutableStateOf("") }

    // Dialog state for batch / MHD detail
    var selectedBatchItem by remember { mutableStateOf<KioskItem?>(null) }

    LaunchedEffect(Unit) {
        itemsList = dbHelper.getAllKioskItems()
        coroutineScope.launch {
            ApiClient.fetchArticles().onSuccess { fetched ->
                dbHelper.saveKioskItems(fetched)
                itemsList = dbHelper.getAllKioskItems()
            }
        }
    }

    // Only inventory items that require stock tracking (excludes hot drinks & donations)
    val inventoryEligibleItems = remember(itemsList) {
        itemsList.filter { it.trackInventory }
    }

    val filteredItems = remember(inventoryEligibleItems, searchQuery) {
        if (searchQuery.isBlank()) inventoryEligibleItems
        else inventoryEligibleItems.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            (it.barcode != null && it.barcode.contains(searchQuery))
        }
    }

    val countedCount = remember(countedQuantities) { countedQuantities.size }

    fun updateCount(id: String, delta: Int) {
        val current = countedQuantities[id] ?: 0
        val next = (current + delta).coerceAtLeast(0)
        countedQuantities = countedQuantities + (id to next)
        // If single batch entry exists, update its quantity as well
        val batches = itemBatches[id]
        if (batches != null && batches.size == 1) {
            itemBatches = itemBatches + (id to listOf(batches[0].copy(quantity = next)))
        }
    }

    fun finalizeInventory() {
        if (countedQuantities.isEmpty()) {
            Toast.makeText(context, "Bitte erfasse zuerst Bestände für die Inventur.", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())
        val volunteer = prefs.memberName ?: "Ehrenamtlicher"

        val syncItems = countedQuantities.map { (itemId, totalQty) ->
            val batches = itemBatches[itemId]?.filter { it.quantity > 0 }
            InventorySyncItem(
                itemId = itemId,
                countedQty = totalQty,
                batches = batches?.ifEmpty { null }
            )
        }

        coroutineScope.launch {
            ApiClient.submitDetailedInventorySync(
                items = syncItems,
                countedBy = volunteer,
                notes = notesInput.ifBlank { "Inventur via Team-App" }
            ).onSuccess {
                Toast.makeText(context, "✅ Inventur erfolgreich synchronisiert & Chargen aktualisiert!", Toast.LENGTH_LONG).show()
                countedQuantities = emptyMap()
                itemBatches = emptyMap()
                notesInput = ""
                // Refresh local items
                ApiClient.fetchArticles().onSuccess { fetched ->
                    dbHelper.saveKioskItems(fetched)
                    itemsList = dbHelper.getAllKioskItems()
                }
            }.onFailure { syncErr ->
                // Fallback to simpler sync
                ApiClient.submitInventorySync(
                    countedItems = countedQuantities,
                    countedBy = volunteer,
                    notes = notesInput
                ).onSuccess {
                    Toast.makeText(context, "✅ Inventurprotokoll gespeichert (Basis-Sync).", Toast.LENGTH_LONG).show()
                    countedQuantities = emptyMap()
                    itemBatches = emptyMap()
                    notesInput = ""
                }.onFailure {
                    Toast.makeText(context, "Fehler beim Cloud-Sync: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
            isSubmitting = false
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF9F7F4))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Inventur & Chargen",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SprachCafeRed
                    )
                    Text(
                        text = "$countedCount von ${inventoryEligibleItems.size} Artikeln erfasst",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                IconButton(
                    onClick = { isScannerOpen = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Barcode-Scan",
                        tint = SprachCafeRed
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Artikel oder Barcode suchen...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Items List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems) { item ->
                    val count = countedQuantities[item.id] ?: 0
                    val isCounted = countedQuantities.containsKey(item.id)
                    val currentBatches = itemBatches[item.id] ?: emptyList()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCounted) Color(0xFFECFDF5) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(item.icon, fontSize = 24.sp)
                                    Column {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1F2937)
                                        )
                                        Text(
                                            text = "${item.category.labelDe} • Soll: ${item.stockQuantity} ${item.unit}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF4B5563)
                                        )
                                        if (isCounted) {
                                            val delta = count - item.stockQuantity
                                            val deltaText = if (delta > 0) "+$delta Mehrmenge" else if (delta < 0) "$delta Fehlbestand" else "✓ Passt genau"
                                            val deltaColor = if (delta > 0) Color(0xFF2563EB) else if (delta < 0) Color(0xFFDC2626) else Color(0xFF059669)
                                            Text(
                                                text = deltaText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = deltaColor
                                            )
                                        }
                                    }
                                }

                                // Stepper Controls
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { updateCount(item.id, -1) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("-1", fontSize = 12.sp)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCounted) Color(0xFF059669) else Color(0xFFE5E7EB),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isCounted) Color.White else Color(0xFF374151),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { updateCount(item.id, 1) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("+1", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { updateCount(item.id, 5) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("+5", fontSize = 12.sp)
                                    }
                                }
                            }

                            // Batch / MHD Button & Summary Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentBatches.isNotEmpty()) {
                                    Text(
                                        text = "🏷️ ${currentBatches.size} Charge(n) mit MHD hinterlegt",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = "Keine MHD-Chargen zugeordnet",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }

                                TextButton(
                                    onClick = { selectedBatchItem = item },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditCalendar,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = SprachCafeRed
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentBatches.isEmpty()) "+ Chargen / MHD" else "Chargen bearbeiten",
                                        fontSize = 12.sp,
                                        color = SprachCafeRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = { finalizeInventory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SprachCafeRed),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting && countedQuantities.isNotEmpty()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Inventurprotokoll abschließen ($countedCount Artikel)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Barcode Scanner Modal
        if (isScannerOpen) {
            BarcodeScannerView(
                onBarcodeScanned = { barcode ->
                    val found = inventoryEligibleItems.find { it.barcode == barcode }
                    if (found != null) {
                        updateCount(found.id, 1)
                        Toast.makeText(context, "${found.name}: +1 gezählt", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Unbekannter Barcode: $barcode", Toast.LENGTH_SHORT).show()
                    }
                },
                onClose = { isScannerOpen = false }
            )
        }

        // Batch / MHD Dialog
        selectedBatchItem?.let { item ->
            val existingBatches = itemBatches[item.id] ?: listOf(
                InventoryBatchEntry(
                    quantity = countedQuantities[item.id] ?: 0,
                    mhdDate = ""
                )
            )

            BatchDetailDialog(
                item = item,
                initialBatches = existingBatches,
                onDismiss = { selectedBatchItem = null },
                onSave = { updatedBatches ->
                    val totalFromBatches = updatedBatches.sumOf { it.quantity }
                    itemBatches = itemBatches + (item.id to updatedBatches)
                    countedQuantities = countedQuantities + (item.id to totalFromBatches)
                    selectedBatchItem = null
                }
            )
        }
    }
}

@Composable
private fun BatchDetailDialog(
    item: KioskItem,
    initialBatches: List<InventoryBatchEntry>,
    onDismiss: () -> Unit,
    onSave: (List<InventoryBatchEntry>) -> Unit
) {
    var batches by remember {
        mutableStateOf(
            if (initialBatches.isNotEmpty()) initialBatches.map { it.copy() }
            else listOf(InventoryBatchEntry(quantity = 0, mhdDate = ""))
        )
    }

    val totalCount = remember(batches) { batches.sumOf { it.quantity } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${item.icon} ${item.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = "Chargen & Mindesthaltbarkeit (MHD)",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }

                HorizontalDivider()

                // Batch rows list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    batches.forEachIndexed { index, batch ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Charge #${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF374151)
                                    )
                                    if (batches.size > 1) {
                                        IconButton(
                                            onClick = {
                                                batches = batches.filterIndexed { i, _ -> i != index }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Entfernen",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // MHD Field
                                    OutlinedTextField(
                                        value = batch.mhdDate ?: "",
                                        onValueChange = { newVal ->
                                            batches = batches.mapIndexed { i, b ->
                                                if (i == index) b.copy(mhdDate = newVal) else b
                                            }
                                        },
                                        label = { Text("MHD (JJJJ-MM-TT)") },
                                        placeholder = { Text("2026-12-31") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )

                                    // Quantity Field
                                    OutlinedTextField(
                                        value = if (batch.quantity == 0) "" else batch.quantity.toString(),
                                        onValueChange = { newVal ->
                                            val q = newVal.toIntOrNull() ?: 0
                                            batches = batches.mapIndexed { i, b ->
                                                if (i == index) b.copy(quantity = q) else b
                                            }
                                        },
                                        label = { Text("Menge (${item.unit})") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.0f)
                                    )
                                }

                                // Chargennummer (optional)
                                OutlinedTextField(
                                    value = batch.batchNumber ?: "",
                                    onValueChange = { newVal ->
                                        batches = batches.mapIndexed { i, b ->
                                            if (i == index) b.copy(batchNumber = newVal) else b
                                        }
                                    },
                                    label = { Text("Chargennummer / Lot (optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Add new batch button
                    OutlinedButton(
                        onClick = {
                            batches = batches + InventoryBatchEntry(quantity = 0, mhdDate = "")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Weitere Charge mit MHD hinzufügen", fontSize = 13.sp)
                    }
                }

                HorizontalDivider()

                // Total Summary & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Gesamtmenge:", fontSize = 11.sp, color = Color(0xFF6B7280))
                        Text(
                            "$totalCount ${item.unit}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SprachCafeRed
                        )
                    }

                    Button(
                        onClick = { onSave(batches) },
                        colors = ButtonDefaults.buttonColors(containerColor = SprachCafeRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Übernehmen")
                    }
                }
            }
        }
    }
}
