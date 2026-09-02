package org.sprachcafe.team.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
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
import org.sprachcafe.team.data.CashCount

@Composable
fun CashCountScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var count by remember { mutableStateOf(CashCount()) }
    var notesText by remember { mutableStateOf("") }
    var volunteerName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9F7F4))
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = null,
                tint = Color(0xFF8B1E2D),
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = "🧮 Digitaler Kassensturz",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = "Tagesabschluss & Soll-Ist-Abgleich",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }

        // Summary Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gezählter Gesamtbestand:", color = Color(0xFF6B7280), fontSize = 14.sp)
                    Text(count.totalCashEurFormatted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Wechselgeldsockel (fest):", color = Color(0xFF6B7280), fontSize = 14.sp)
                    Text("- 50,00 €", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFD97706))
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tages-Reinerlös:", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(
                        text = count.revenueEurFormatted,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color(0xFF16A34A)
                    )
                }
            }
        }

        // Volunteer Name
        OutlinedTextField(
            value = volunteerName,
            onValueChange = { volunteerName = it },
            label = { Text("Name der Schichtleitung") },
            placeholder = { Text("z.B. Philipp / Dorota / Agata") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        // Notes count (Banknoten)
        Text(
            text = "Banknoten (Scheine)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF1F2937)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CashInputField("50 €", count.note50, modifier = Modifier.weight(1f)) { count = count.copy(note50 = it) }
            CashInputField("20 €", count.note20, modifier = Modifier.weight(1f)) { count = count.copy(note20 = it) }
            CashInputField("10 €", count.note10, modifier = Modifier.weight(1f)) { count = count.copy(note10 = it) }
            CashInputField("5 €", count.note5, modifier = Modifier.weight(1f)) { count = count.copy(note5 = it) }
        }

        // Coins count (Münzen)
        Text(
            text = "Münzen",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF1F2937)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CashInputField("2,00 €", count.coin200, modifier = Modifier.weight(1f)) { count = count.copy(coin200 = it) }
            CashInputField("1,00 €", count.coin100, modifier = Modifier.weight(1f)) { count = count.copy(coin100 = it) }
            CashInputField("0,50 €", count.coin50, modifier = Modifier.weight(1f)) { count = count.copy(coin50 = it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CashInputField("0,20 €", count.coin20, modifier = Modifier.weight(1f)) { count = count.copy(coin20 = it) }
            CashInputField("0,10 €", count.coin10, modifier = Modifier.weight(1f)) { count = count.copy(coin10 = it) }
            CashInputField("0,05 €", count.coin5, modifier = Modifier.weight(1f)) { count = count.copy(coin5 = it) }
        }

        // Barauslagen / Besonderheiten
        OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            label = { Text("Barauslagen / Belege / Notizen") },
            placeholder = { Text("z.B. 2x Hafermilch gekauft 2,18 € (Beleg liegt in Kasse)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = RoundedCornerShape(14.dp)
        )

        // Submit Button
        Button(
            onClick = {
                if (volunteerName.isBlank()) {
                    Toast.makeText(context, "Bitte Namen der Schichtleitung angeben", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Kassensturz (${count.totalCashEurFormatted}) erfolgreich übermittelt!", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E2D)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Text("Kassenabschluss speichern & senden", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun CashInputField(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit
) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { str ->
            val clean = str.filter { it.isDigit() }
            onValueChange(clean.toIntOrNull() ?: 0)
        },
        label = { Text(label, fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}
