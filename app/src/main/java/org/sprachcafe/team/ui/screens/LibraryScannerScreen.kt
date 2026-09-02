package org.sprachcafe.team.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.sprachcafe.team.data.LibraryBook
import org.sprachcafe.team.ui.components.BarcodeScannerView
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun LibraryScannerScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isScannerOpen by remember { mutableStateOf(false) }
    var scannedIsbn by remember { mutableStateOf<String?>(null) }
    var detectedTitle by remember { mutableStateOf<String?>(null) }
    var detectedAuthor by remember { mutableStateOf<String?>(null) }
    var isLoadingBook by remember { mutableStateOf(false) }

    var sampleBooks by remember {
        mutableStateOf(
            listOf(
                LibraryBook("978-83-7844-906-5", "Pan Tadeusz", "Adam Mickiewicz", isLent = false),
                LibraryBook("978-83-08-04166-6", "Solaris", "Stanisław Lem", isLent = true, lentTo = "Kasia"),
                LibraryBook("978-83-7490-874-0", "Bieguni", "Olga Tokarczuk", isLent = false)
            )
        )
    }

    fun lookupIsbn(isbn: String) {
        isLoadingBook = true
        scannedIsbn = isbn
        coroutineScope.launch {
            try {
                val cleanIsbn = isbn.replace("-", "").trim()
                val url = URL("https://openlibrary.org/isbn/$cleanIsbn.json")
                val result = withContext(Dispatchers.IO) {
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        JSONObject(body)
                    } else null
                }

                if (result != null) {
                    detectedTitle = result.optString("title", "Gefundenes Buch ($isbn)")
                    detectedAuthor = "Aus OpenLibrary importiert"
                } else {
                    detectedTitle = "Buch ISBN: $isbn"
                    detectedAuthor = "SprachCafé Hausbibliothek"
                }
            } catch (e: Exception) {
                detectedTitle = "Buch ISBN: $isbn"
                detectedAuthor = "SprachCafé Hausbibliothek"
            } finally {
                isLoadingBook = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF9F7F4))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = Color(0xFF8B1E2D),
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "📚 Hausbibliothek Scanner",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = "ISBN-Scan & Regal-Verwaltung (400+ Bücher)",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            // Big Scan Action Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF8B1E2D)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ISBN Barcode auf Buchrücken scannen",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Erkennt polnische & deutsche Bücher blitzschnell mit der Handykamera",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = { isScannerOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF8B1E2D))
                            Text("Kamera-Scanner starten", color = Color(0xFF8B1E2D), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Scanned Result Card
            if (scannedIsbn != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Zuletzt gescannt:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B1E2D))

                        if (isLoadingBook) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text(detectedTitle ?: "Unbekannter Titel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(detectedAuthor ?: "", fontSize = 13.sp, color = Color(0xFF6B7280))
                            Text("ISBN: $scannedIsbn", fontSize = 11.sp, color = Color(0xFF9CA3AF))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val newBook = LibraryBook(
                                            isbn = scannedIsbn!!,
                                            title = detectedTitle ?: "Buch",
                                            author = detectedAuthor ?: "Autor",
                                            isLent = false
                                        )
                                        sampleBooks = listOf(newBook) + sampleBooks
                                        Toast.makeText(context, "Buch im Katalog aufgenommen!", Toast.LENGTH_SHORT).show()
                                        scannedIsbn = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("+ In Katalog", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Als ausgeliehen markiert", Toast.LENGTH_SHORT).show()
                                        scannedIsbn = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Ausleihen", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Book Catalog
            Text(
                text = "Katalog-Vorschau (${sampleBooks.size} Bücher)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF1F2937)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(sampleBooks) { book ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(book.author, fontSize = 12.sp, color = Color(0xFF6B7280))
                                Text("ISBN: ${book.isbn}", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (book.isLent) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = if (book.isLent) "Ausgeliehen (${book.lentTo})" else "Im Regal",
                                    color = if (book.isLent) Color(0xFF92400E) else Color(0xFF166534),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Camera Scanner Overlay
        if (isScannerOpen) {
            BarcodeScannerView(
                onBarcodeScanned = { barcode ->
                    isScannerOpen = false
                    lookupIsbn(barcode)
                    Toast.makeText(context, "ISBN erkannt: $barcode", Toast.LENGTH_SHORT).show()
                },
                onClose = { isScannerOpen = false }
            )
        }
    }
}
