package org.sprachcafe.team.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sprachcafe.team.data.DefaultKioskData
import org.sprachcafe.team.data.WasteEntry
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteProtocolScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var entries by remember {
        mutableStateOf(
            listOf(
                WasteEntry(
                    id = "w1",
                    itemName = "Ostmost (div. Sorten)",
                    quantity = 2,
                    reason = "MHD-Ablauf / Trübung",
                    volunteerName = "Philipp Fuchs"
                ),
                WasteEntry(
                    id = "w2",
                    itemName = "Mate-Limo",
                    quantity = 4,
                    reason = "Helferverpflegung (Schicht)",
                    volunteerName = "Dorota"
                )
            )
        )
    }

    var selectedItemName by remember { mutableStateOf(DefaultKioskData.items[0].name) }
    var quantity by remember { mutableIntStateOf(1) }
    var selectedReason by remember { mutableStateOf("MHD-Ablauf / Verderb") }
    var volunteerName by remember { mutableStateOf("") }
    var isDropdownOpen by remember { mutableStateOf(false) }

    val reasons = listOf(
        "MHD-Ablauf / Verderb",
        "Helferverpflegung (Ehrenamt)",
        "Glasbruch / Transportschaden",
        "Eigenverbrauch Schicht"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9F7F4))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF8B1E2D),
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = "📋 GoBD-Minderungsprotokoll",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = "Finanzamt-Nachweis für Schwund & Helfergetränke",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }

        // Info Banner
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ℹ️", fontSize = 16.sp)
                Text(
                    text = "Schützt die Gemeinnützigkeit: Erklärt lückenlos, warum eingekaufte Ware keinen Kioskerlös erzielt hat.",
                    fontSize = 11.sp,
                    color = Color(0xFF1E40AF),
                    lineHeight = 15.sp
                )
            }
        }

        // Add Entry Form Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Neuen Eintrag erfassen",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1F2937)
                )

                // Item Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = isDropdownOpen,
                    onExpandedChange = { isDropdownOpen = !isDropdownOpen }
                ) {
                    OutlinedTextField(
                        value = selectedItemName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Artikel") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownOpen) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownOpen,
                        onDismissRequest = { isDropdownOpen = false }
                    ) {
                        DefaultKioskData.items.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.icon} ${item.name}") },
                                onClick = {
                                    selectedItemName = item.name
                                    isDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Quantity & Reason
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = "$quantity",
                        onValueChange = { quantity = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1 },
                        label = { Text("Menge") },
                        modifier = Modifier.width(90.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = volunteerName,
                        onValueChange = { volunteerName = it },
                        label = { Text("Erfasst von") },
                        placeholder = { Text("Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Reason Radio / Chips
                Text("Grund:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reasons.take(2).forEach { r ->
                        FilterChip(
                            selected = selectedReason == r,
                            onClick = { selectedReason = r },
                            label = { Text(r.split(" ")[0], fontSize = 11.sp) }
                        )
                    }
                    FilterChip(
                        selected = selectedReason == reasons[2],
                        onClick = { selectedReason = reasons[2] },
                        label = { Text("Bruch", fontSize = 11.sp) }
                    )
                }

                Button(
                    onClick = {
                        if (volunteerName.isBlank()) {
                            Toast.makeText(context, "Bitte Namen angeben", Toast.LENGTH_SHORT).show()
                        } else {
                            val newEntry = WasteEntry(
                                id = "w-${System.currentTimeMillis()}",
                                itemName = selectedItemName,
                                quantity = quantity,
                                reason = selectedReason,
                                volunteerName = volunteerName
                            )
                            entries = listOf(newEntry) + entries
                            Toast.makeText(context, "Minderungsprotokoll aktualisiert!", Toast.LENGTH_SHORT).show()
                            quantity = 1
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E2D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ Im Protokoll speichern", fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of entries
        Text(
            text = "Erfasste Minderungen (${entries.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF1F2937)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(entries) { entry ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${entry.quantity}× ${entry.itemName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${entry.reason} • ${entry.volunteerName}",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }

                        IconButton(
                            onClick = { entries = entries.filter { it.id != entry.id } }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }
}
