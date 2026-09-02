package org.sprachcafe.team.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch
import org.sprachcafe.team.data.*
import org.sprachcafe.team.ui.components.BarcodeScannerView
import org.sprachcafe.team.ui.theme.SprachCafeRed

enum class LibrarySubTab(val title: String) {
    CATALOG("Katalog & Suche"),
    COUNTER("Ausleihe / Rückgabe"),
    ACCOUNT_SERVICE("Kontoservice & Gebühr")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryServiceScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dbHelper = remember { TeamDatabaseHelper.getInstance(context) }
    val prefs = remember { TeamPreferences.getInstance(context) }

    var currentTab by remember { mutableStateOf(LibrarySubTab.CATALOG) }
    var booksList by remember { mutableStateOf<List<LibraryBook>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isScannerOpen by remember { mutableStateOf(false) }

    // Dialog states for Loan
    var selectedBookForLoan by remember { mutableStateOf<LibraryBook?>(null) }
    var borrowerNameInput by remember { mutableStateOf("") }
    var borrowerContactInput by remember { mutableStateOf("") }

    // Account Service / Fee States
    var readerNameInput by remember { mutableStateOf("") }
    var feeAmountCents by remember { mutableStateOf(1000) } // 10,00 € Jahresgebühr

    fun refreshBooks(query: String? = null) {
        booksList = if (!query.isNullOrBlank()) {
            dbHelper.searchBooks(query)
        } else {
            dbHelper.getAllBooks()
        }

        coroutineScope.launch {
            ApiClient.searchLibraryBooks(query).onSuccess { fetched ->
                if (fetched.isNotEmpty()) {
                    dbHelper.saveBooks(fetched)
                    booksList = if (!query.isNullOrBlank()) dbHelper.searchBooks(query) else dbHelper.getAllBooks()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshBooks()
    }

    fun borrowBook(book: LibraryBook) {
        if (borrowerNameInput.isBlank()) {
            Toast.makeText(context, "Bitte den Namen des Lesers eingeben", Toast.LENGTH_SHORT).show()
            return
        }

        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.GERMANY).format(java.util.Date())
        val dueCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 28) }
        val dueStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.GERMANY).format(dueCal.time)

        dbHelper.borrowBook(
            isbn = book.isbn,
            title = book.title,
            borrowerName = borrowerNameInput.trim(),
            borrowerContact = borrowerContactInput.trim(),
            loanDate = todayStr,
            dueDate = dueStr
        )

        coroutineScope.launch {
            ApiClient.borrowBook(
                isbn = book.isbn,
                title = book.title,
                borrowerName = borrowerNameInput.trim(),
                borrowerContact = borrowerContactInput.trim(),
                feeCents = 0
            )
        }

        Toast.makeText(context, "📖 '${book.title}' an ${borrowerNameInput.trim()} ausgeliehen (bis $dueStr)", Toast.LENGTH_LONG).show()
        selectedBookForLoan = null
        borrowerNameInput = ""
        borrowerContactInput = ""
        refreshBooks(searchQuery)
    }

    fun returnBook(isbn: String) {
        dbHelper.returnBook(isbn)
        coroutineScope.launch {
            ApiClient.returnBook(isbn)
        }
        Toast.makeText(context, "✅ Buch erfolgreich zurückgebucht!", Toast.LENGTH_SHORT).show()
        refreshBooks(searchQuery)
    }

    fun bookLibraryFee() {
        if (readerNameInput.isBlank()) {
            Toast.makeText(context, "Bitte den Namen des Lesers eingeben", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionId = prefs.activeSessionId ?: 1L
        dbHelper.addTransaction(
            sessionId = sessionId,
            type = TransactionType.LIBRARY_FEE,
            amountCents = feeAmountCents,
            purpose = "Bibliotheksgebühr / Jahresausweis",
            donorName = readerNameInput.trim()
        )

        coroutineScope.launch {
            ApiClient.addTransaction(
                sessionId = sessionId,
                type = "LIBRARY_FEE",
                amountCents = feeAmountCents,
                purpose = "Bibliotheksgebühr / Jahresausweis",
                donorName = readerNameInput.trim(),
                itemsJson = "Jahresbeitrag Hausbibliothek"
            )
        }

        Toast.makeText(context, "🎟️ ${String.format("%.2f €", feeAmountCents / 100.0)} Bibliotheksgebühr bar kassiert & gebucht!", Toast.LENGTH_LONG).show()
        readerNameInput = ""
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
                        text = "Hausbibliothek Service",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SprachCafeRed
                    )
                    Text(
                        text = "Kundenausleihe & Katalog (${booksList.size} Bücher geladen)",
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
                        contentDescription = "ISBN-Scan",
                        tint = SprachCafeRed
                    )
                }
            }

            // SubTabs Navigation
            TabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = Color.White,
                contentColor = SprachCafeRed
            ) {
                LibrarySubTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = { Text(tab.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            when (currentTab) {
                LibrarySubTab.CATALOG -> {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            refreshBooks(it)
                        },
                        label = { Text("Titel, Autor oder ISBN suchen...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Books List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(booksList) { book ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (book.isLent) {
                                            returnBook(book.isbn)
                                        } else {
                                            selectedBookForLoan = book
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1F2937)
                                        )
                                        Text(
                                            text = "${book.author} • ${book.category ?: "Allgemein"}",
                                            fontSize = 13.sp,
                                            color = Color(0xFF4B5563)
                                        )
                                        Text(
                                            text = "ISBN: ${book.isbn} • Standort: ${book.shelfLocation ?: "Pankow"}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (book.isLent) Color(0xFFFEE2E2) else Color(0xFFECFDF5)
                                    ) {
                                        Text(
                                            text = if (book.isLent) "Ausgeliehen" else "Verfügbar",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (book.isLent) Color(0xFF991B1B) else Color(0xFF065F46),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LibrarySubTab.COUNTER -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = SprachCafeRed,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "Buch-Barcode / ISBN scannen",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Halte die Kamera über den Barcode auf der Buchrückseite, um die Ausleihe oder Rücknahme direkt durchzuführen.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { isScannerOpen = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SprachCafeRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scanner öffnen")
                            }
                        }
                    }
                }

                LibrarySubTab.ACCOUNT_SERVICE -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Kontoservice & Jahresgebühr kassieren",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                text = "Nimm die Bibliotheks-Jahresgebühr bar an der Theke ein. Der Betrag wird automatisch in die Kasse (Zweckbetrieb) eingebucht.",
                                fontSize = 13.sp,
                                color = Color(0xFF4B5563)
                            )

                            OutlinedTextField(
                                value = readerNameInput,
                                onValueChange = { readerNameInput = it },
                                label = { Text("Name des Lesers / Mitglieds") },
                                placeholder = { Text("z. B. Anna Kowalska") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Tarif / Gebühr wählen:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = feeAmountCents == 1000,
                                    onClick = { feeAmountCents = 1000 },
                                    label = { Text("10,00 € (Ermäßigt / Standard)") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = feeAmountCents == 1500,
                                    onClick = { feeAmountCents = 1500 },
                                    label = { Text("15,00 € (Familie / Förderer)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                onClick = { bookLibraryFee() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gebühr kassieren & in Kasse verbuchen (${String.format("%.2f €", feeAmountCents / 100.0)})")
                            }
                        }
                    }
                }
            }
        }

        // Loan Dialog
        if (selectedBookForLoan != null) {
            val book = selectedBookForLoan!!
            AlertDialog(
                onDismissRequest = { selectedBookForLoan = null },
                icon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = SprachCafeRed, modifier = Modifier.size(32.dp)) },
                title = { Text("Buch ausleihen", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("'${book.title}' von ${book.author}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Reguläre Leihfrist: 4 Wochen (28 Tage)", fontSize = 12.sp, color = Color.Gray)

                        OutlinedTextField(
                            value = borrowerNameInput,
                            onValueChange = { borrowerNameInput = it },
                            label = { Text("Name des Lesers *") },
                            placeholder = { Text("z. B. Jan Nowak") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = borrowerContactInput,
                            onValueChange = { borrowerContactInput = it },
                            label = { Text("Telefon oder E-Mail (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { borrowBook(book) },
                        colors = ButtonDefaults.buttonColors(containerColor = SprachCafeRed)
                    ) {
                        Text("Ausleihe bestätigen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedBookForLoan = null }) {
                        Text("Abbrechen")
                    }
                }
            )
        }

        // Barcode Scanner
        if (isScannerOpen) {
            BarcodeScannerView(
                onBarcodeScanned = { isbn ->
                    val cleanIsbn = isbn.replace("-", "").trim()
                    val found = booksList.find { it.isbn.replace("-", "").trim() == cleanIsbn }
                    if (found != null) {
                        if (found.isLent) {
                            returnBook(found.isbn)
                        } else {
                            selectedBookForLoan = found
                        }
                    } else {
                        // Create ad-hoc book for scanning
                        val adHoc = LibraryBook(isbn = isbn, title = "Buch ISBN $isbn", author = "SprachCafé Hausbibliothek")
                        selectedBookForLoan = adHoc
                    }
                },
                onClose = { isScannerOpen = false }
            )
        }
    }
}
