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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sprachcafe.team.data.CartItem
import org.sprachcafe.team.data.DefaultKioskData
import org.sprachcafe.team.data.ItemCategory
import org.sprachcafe.team.data.KioskItem
import org.sprachcafe.team.ui.components.BarcodeScannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KioskScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    var cart by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isCartOpen by remember { mutableStateOf(false) }
    var isScannerOpen by remember { mutableStateOf(false) }

    val filteredItems = remember(selectedCategory) {
        if (selectedCategory == null) DefaultKioskData.items
        else DefaultKioskData.items.filter { it.category == selectedCategory }
    }

    val totalCents = remember(cart) {
        cart.entries.sumOf { (id, qty) ->
            (DefaultKioskData.items.find { it.id == id }?.priceCents ?: 0) * qty
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
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF9F7F4))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "☕ Kiosk & Café Kasse",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "SprachCafé Pankow • 18 Artikel",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                IconButton(
                    onClick = { isScannerOpen = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF8B1E2D))
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Barcode scannen",
                        tint = Color.White
                    )
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            // Product Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredItems) { item ->
                    val quantityInCart = cart[item.id] ?: 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { addToCart(item.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (quantityInCart > 0) Color(0xFFFAF5EB) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = item.icon,
                                    fontSize = 28.sp
                                )

                                if (quantityInCart > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF8B1E2D)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$quantityInCart",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 2,
                                color = Color(0xFF1F2937)
                            )

                            Text(
                                text = item.unit,
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.priceEurFormatted,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF8B1E2D)
                                )

                                IconButton(
                                    onClick = { addToCart(item.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = "Hinzufügen",
                                        tint = Color(0xFF8B1E2D)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Cart Summary Bar
        if (totalItemsCount > 0) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { isCartOpen = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF8B1E2D)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$totalItemsCount",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
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
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Cart Modal Sheet
        if (isCartOpen) {
            ModalBottomSheet(
                onDismissRequest = { isCartOpen = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "🛒 Aktueller Verkauf",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color(0xFF1F2937)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    cart.forEach { (id, qty) ->
                        val item = DefaultKioskData.items.find { it.id == id }
                        if (item != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${item.priceEurFormatted} × $qty",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { removeFromCart(id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, "Weniger")
                                    }
                                    Text(
                                        text = "$qty",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    IconButton(
                                        onClick = { addToCart(id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, "Mehr")
                                    }
                                    Text(
                                        text = String.format("%.2f €", (item.priceCents * qty) / 100.0),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(60.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gesamtsumme:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = String.format("%.2f €", totalCents / 100.0),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Color(0xFF8B1E2D)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Erfolgreich verbucht! (Bar)", Toast.LENGTH_SHORT).show()
                                clearCart()
                                isCartOpen = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("💶 Bar bezahlt", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Auf Strichliste gebucht", Toast.LENGTH_SHORT).show()
                                clearCart()
                                isCartOpen = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E2D)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("📝 Strichliste", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Camera Barcode Scanner View
        if (isScannerOpen) {
            BarcodeScannerView(
                onBarcodeScanned = { scannedCode ->
                    val matchedItem = DefaultKioskData.items.find { it.barcode == scannedCode }
                    if (matchedItem != null) {
                        addToCart(matchedItem.id)
                        Toast.makeText(context, "Erkannt: ${matchedItem.name}", Toast.LENGTH_SHORT).show()
                        isScannerOpen = false
                    } else {
                        Toast.makeText(context, "Unbekannter Barcode: $scannedCode", Toast.LENGTH_LONG).show()
                    }
                },
                onClose = { isScannerOpen = false }
            )
        }
    }
}
