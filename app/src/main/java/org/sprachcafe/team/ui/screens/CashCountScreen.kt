package org.sprachcafe.team.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.sprachcafe.team.data.*
import org.sprachcafe.team.notifications.ShiftReminderManager
import org.sprachcafe.team.ui.theme.SprachCafeRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashCountScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dbHelper = remember { TeamDatabaseHelper.getInstance(context) }
    val prefs = remember { TeamPreferences.getInstance(context) }

    var session by remember { mutableStateOf<CashSession?>(null) }
    var notesInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Coin & Note counters
    var note50 by remember { mutableStateOf(0) }
    var note20 by remember { mutableStateOf(0) }
    var note10 by remember { mutableStateOf(0) }
    var note5 by remember { mutableStateOf(0) }
    var coin200 by remember { mutableStateOf(0) }
    var coin100 by remember { mutableStateOf(0) }
    var coin50 by remember { mutableStateOf(0) }
    var coin20 by remember { mutableStateOf(0) }
    var coin10 by remember { mutableStateOf(0) }
    var coin5 by remember { mutableStateOf(0) }

    fun refreshSession() {
        session = dbHelper.getActiveCashSession()
    }

    LaunchedEffect(Unit) {
        refreshSession()
    }

    val countedCashCents = remember(note50, note20, note10, note5, coin200, coin100, coin50, coin20, coin10, coin5) {
        (note50 * 5000) + (note20 * 2000) + (note10 * 1000) + (note5 * 500) +
        (coin200 * 200) + (coin100 * 100) + (coin50 * 50) + (coin20 * 20) + (coin10 * 10) + (coin5 * 5)
    }

    val expectedCents = session?.expectedTotalCents ?: (prefs.openingFloatCents)
    val diffCents = countedCashCents - expectedCents

    fun finalizeKassensturz() {
        if (session == null && !prefs.isCashActive) {
            Toast.makeText(context, "Keine aktive Kasse zum Abschließen vorhanden.", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val sess = session
        val sessionId = sess?.id ?: (prefs.activeSessionId ?: 1L)
        val baseRetained = 5000 // 50,00 € Sockel bleibt in Kasse
        val skimRetained = if (countedCashCents > baseRetained) countedCashCents - baseRetained else 0

        // 1. Close in local DB
        dbHelper.closeCashSession(
            sessionId = sessionId,
            countedCents = countedCashCents,
            diffCents = diffCents,
            baseRetainedCents = baseRetained,
            skimRetainedCents = skimRetained,
            notes = notesInput
        )

        // 2. Sync to Server
        coroutineScope.launch {
            ApiClient.closeCashSession(
                sessionId = sessionId,
                countedCents = countedCashCents,
                diffCents = diffCents,
                baseCents = baseRetained,
                skimCents = skimRetained,
                salesCents = sess?.totalSalesCents ?: 0,
                donationsCents = sess?.totalDonationsCents ?: 0,
                libraryFeesCents = sess?.totalLibraryFeesCents ?: 0,
                payoutsCents = sess?.totalPayoutsCents ?: 0,
                expectedCents = expectedCents,
                notes = notesInput
            )

            // 3. Clear active shift and cancel reminder
            ShiftReminderManager.cancelReminder(context)
            prefs.clearShift()
            isSubmitting = false
            refreshSession()
            Toast.makeText(context, "🎉 Kassensturz erfolgreich abgeschlossen & synchronisiert!", Toast.LENGTH_LONG).show()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF9F7F4))) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Kassensturz & Schichtabschluss",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SprachCafeRed
                )
                Text(
                    text = "Zähle alle Münzen und Scheine der Café-Kasse.",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }

            // Soll vs. Ist Abrechnung Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Kassenabrechnung (Soll)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1F2937)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Anfangsbestand:", color = Color.Gray, fontSize = 13.sp)
                            Text(String.format("%.2f €", (session?.openingFloatCents ?: prefs.openingFloatCents) / 100.0), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("+ Bareinnahmen Café:", color = Color.Gray, fontSize = 13.sp)
                            Text(String.format("+%.2f €", (session?.totalSalesCents ?: 0) / 100.0), color = Color(0xFF059669), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        if ((session?.totalDonationsCents ?: 0) > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("+ Spenden (0% MwSt):", color = Color.Gray, fontSize = 13.sp)
                                Text(String.format("+%.2f €", (session?.totalDonationsCents ?: 0) / 100.0), color = Color(0xFFB45309), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        if ((session?.totalLibraryFeesCents ?: 0) > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("+ Bibliotheksgebühren:", color = Color.Gray, fontSize = 13.sp)
                                Text(String.format("+%.2f €", (session?.totalLibraryFeesCents ?: 0) / 100.0), color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        if ((session?.totalPayoutsCents ?: 0) > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("- Entnahmen / Einkäufe:", color = Color.Gray, fontSize = 13.sp)
                                Text(String.format("-%.2f €", (session?.totalPayoutsCents ?: 0) / 100.0), color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        Divider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Soll-Kassenstand:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(String.format("%.2f €", expectedCents / 100.0), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gezählter Ist-Bestand:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = String.format("%.2f €", countedCashCents / 100.0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (diffCents == 0 && countedCashCents > 0) Color(0xFF059669) else SprachCafeRed
                            )
                        }

                        // Differenz Indicator
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                countedCashCents == 0 -> Color(0xFFF3F4F6)
                                diffCents == 0 -> Color(0xFFECFDF5)
                                else -> Color(0xFFFEF2F2)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        countedCashCents == 0 -> "Noch nicht gezählt"
                                        diffCents == 0 -> "✅ Kasse stimmt exakt überein!"
                                        diffCents > 0 -> "⚠️ Überschuss in Kasse:"
                                        else -> "⚠️ Fehlbetrag in Kasse:"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diffCents == 0) Color(0xFF065F46) else Color(0xFF991B1B)
                                )

                                if (countedCashCents > 0) {
                                    Text(
                                        text = String.format("%+.2f €", diffCents / 100.0),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (diffCents == 0) Color(0xFF065F46) else Color(0xFF991B1B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scheine & Münzen Zähleingaben
            item {
                Text(text = "1. Geldscheine zählen", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DenominationField("50 €", note50, { note50 = it }, Modifier.weight(1f))
                    DenominationField("20 €", note20, { note20 = it }, Modifier.weight(1f))
                    DenominationField("10 €", note10, { note10 = it }, Modifier.weight(1f))
                    DenominationField("5 €", note5, { note5 = it }, Modifier.weight(1f))
                }
            }

            item {
                Text(text = "2. Münzen zählen", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DenominationField("2 €", coin200, { coin200 = it }, Modifier.weight(1f))
                    DenominationField("1 €", coin100, { coin100 = it }, Modifier.weight(1f))
                    DenominationField("0,50 €", coin50, { coin50 = it }, Modifier.weight(1f))
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DenominationField("0,20 €", coin20, { coin20 = it }, Modifier.weight(1f))
                    DenominationField("0,10 €", coin10, { coin10 = it }, Modifier.weight(1f))
                    DenominationField("0,05 €", coin5, { coin5 = it }, Modifier.weight(1f))
                }
            }

            // Notes / Remarks
            item {
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notiz zum Kassensturz (optional)") },
                    placeholder = { Text("z. B. 50 € Sockel in Kasse belassen, Rest im Tresor") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Finalize Button
            item {
                Button(
                    onClick = { finalizeKassensturz() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting && (session != null || prefs.isCashActive)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kassensturz abschließen & übertragen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DenominationField(
    label: String,
    count: Int,
    onCountChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
            OutlinedTextField(
                value = if (count == 0) "" else count.toString(),
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    onCountChanged(clean.toIntOrNull() ?: 0)
                },
                placeholder = { Text("0", fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
