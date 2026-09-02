package org.sprachcafe.team.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    var searchQuery by remember { mutableStateOf("") }
    var isScannerOpen by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var notesInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        itemsList = dbHelper.getAllKioskItems()
        coroutineScope.launch {
            ApiClient.fetchArticles().onSuccess { fetched ->
                dbHelper.saveKioskItems(fetched)
                itemsList = dbHelper.getAllKioskItems()
            }
        }
    }

    val filteredItems = remember(itemsList, searchQuery) {
        if (searchQuery.isBlank()) itemsList
        else itemsList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            (it.barcode != null && it.barcode.contains(searchQuery))
        }
    }

    val countedCount = remember(countedQuantities) { countedQuantities.size }

    fun updateCount(id: String, delta: Int) {
        val current = countedQuantities[id] ?: 0
        val next = (current + delta).coerceAtLeast(0)
        countedQuantities = countedQuantities + (id to next)
    }

    fun setCount(id: String, count: Int) {
        countedQuantities = countedQuantities + (id to count.coerceAtLeast(0))
    }

    fun finalizeInventory() {
        if (countedQuantities.isEmpty()) {
            Toast.makeText(context, "Bitte erfasse zuerst Bestände für die Inventur.", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())
        val volunteer = prefs.memberName ?: "Ehrenamtlicher"

        coroutineScope.launch {
            ApiClient.submitInventory(
                countedBy = volunteer,
                date = dateStr,
                items = countedQuantities,
                notes = notesInput
            ).onSuccess {
                Toast.makeText(context, "✅ Inventurprotokoll erfolgreich an Server übertragen!", Toast.LENGTH_LONG).show()
                countedQuantities = emptyMap()
                notesInput = ""
            }.onFailure {
                Toast.makeText(context, "Lokal gespeichert. Fehler beim Cloud-Sync: ${it.message}", Toast.LENGTH_LONG).show()
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
                        text = "Inventur & Zählung",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SprachCafeRed
                    )
                    Text(
                        text = "$countedCount von ${itemsList.size} Artikeln gezählt",
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

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCounted) Color(0xFFECFDF5) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                        text = "${item.category.labelDe} • ${item.unit}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
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
                    val found = itemsList.find { it.barcode == barcode }
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
    }
}
