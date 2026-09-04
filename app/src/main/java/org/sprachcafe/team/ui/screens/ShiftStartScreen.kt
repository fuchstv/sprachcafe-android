package org.sprachcafe.team.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import org.sprachcafe.team.notifications.ShiftReminderManager
import org.sprachcafe.team.ui.theme.SprachCafeRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftStartScreen(
    modifier: Modifier = Modifier,
    onShiftStarted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { TeamPreferences.getInstance(context) }
    val dbHelper = remember { TeamDatabaseHelper.getInstance(context) }

    var todayShifts by remember { mutableStateOf<List<ShiftItem>>(emptyList()) }
    var todayEvents by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    var allMembers by remember { mutableStateOf<List<TeamMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMemberPicker by remember { mutableStateOf(false) }

    var selectedShift by remember { mutableStateOf<ShiftItem?>(null) }
    var showCashDialog by remember { mutableStateOf(false) }
    var openingFloatInput by remember { mutableStateOf("50,00") }
    var suggestedFloatCents by remember { mutableStateOf(5000) }

    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())
    }
    val todayReadable = remember {
        SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMANY).format(Date())
    }

    // Load shifts and members
    fun reloadData() {
        isLoading = true
        coroutineScope.launch {
            // Load local shifts first
            val localShifts = dbHelper.getTodayCachedShifts()
            if (localShifts.isNotEmpty()) {
                todayShifts = localShifts
            }

            // Sync from API
            ApiClient.fetchTodayRoster().onSuccess { roster ->
                todayShifts = roster.shifts
                todayEvents = roster.events
                dbHelper.saveShifts(roster.shifts)
            }.onFailure {
                // Fallback to fetchTodayShifts
                ApiClient.fetchTodayShifts().onSuccess { shifts ->
                    todayShifts = shifts
                    dbHelper.saveShifts(shifts)
                }
            }

            ApiClient.fetchMembers().onSuccess { members ->
                allMembers = members
                // Auto-detect member if not set
                if (prefs.memberName.isNullOrEmpty() && members.isNotEmpty()) {
                    val defaultM = members.firstOrNull()
                    if (defaultM != null) {
                        prefs.memberName = defaultM.name
                        prefs.memberCode = defaultM.shortCode
                        prefs.memberColor = defaultM.color
                    }
                }
            }

            ApiClient.fetchLatestFloat().onSuccess { floatCents ->
                suggestedFloatCents = floatCents
                openingFloatInput = String.format(Locale.GERMANY, "%.2f", floatCents / 100.0)
            }

            isLoading = false
        }
    }

    fun claimSlotAndStart(slot: EventSlotItem, event: EventItem) {
        val memberName = prefs.memberName ?: "Ehrenamtlicher"
        val memberId = allMembers.find { it.name.equals(memberName, ignoreCase = true) || it.shortCode.equals(prefs.memberCode, ignoreCase = true) }?.id

        coroutineScope.launch {
            isLoading = true
            val res = ApiClient.assignEventSlot(slot.id, memberName, memberId)
            res.onSuccess { newShiftId ->
                Toast.makeText(context, "Slot '${slot.roleName}' übernommen!", Toast.LENGTH_SHORT).show()
                // Refresh roster so UI updates
                ApiClient.fetchTodayRoster().onSuccess { roster ->
                    todayShifts = roster.shifts
                    todayEvents = roster.events
                    dbHelper.saveShifts(roster.shifts)
                }
                // Construct a ShiftItem to pass to cash dialog
                selectedShift = ShiftItem(
                    id = newShiftId,
                    date = event.date,
                    timeSlot = event.timeSlot ?: "event",
                    startTime = slot.startTime ?: event.startTimeFormatted,
                    endTime = slot.endTime ?: event.endTimeFormatted,
                    memberName = memberName,
                    location = event.location,
                    eventId = event.id,
                    eventTitle = event.summary,
                    slotId = slot.id,
                    slotRole = slot.roleName
                )
                showCashDialog = true
            }.onFailure { e ->
                Toast.makeText(context, "Fehler beim Zuweisen: ${e.message}", Toast.LENGTH_LONG).show()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        reloadData()
    }

    fun startShiftWithCash(takeCash: Boolean, floatCents: Int) {
        val shift = selectedShift
        val memberName = prefs.memberName ?: "Ehrenamtlicher"
        val startTime = shift?.startTime ?: SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date())
        val endTime = shift?.endTime ?: "18:00"

        prefs.activeShiftId = shift?.id
        prefs.activeShiftStartTime = startTime
        prefs.activeShiftEndTime = endTime
        prefs.isCashActive = takeCash
        prefs.openingFloatCents = floatCents

        if (takeCash) {
            val localSessionId = dbHelper.startCashSession(
                shiftId = shift?.id,
                volunteerName = memberName,
                date = todayDateStr,
                startTime = startTime,
                openingFloatCents = floatCents
            )
            prefs.activeSessionId = localSessionId

            // Sync with backend asynchronously
            coroutineScope.launch {
                ApiClient.startCashSession(
                    shiftId = shift?.id,
                    volunteerName = memberName,
                    date = todayDateStr,
                    startTime = startTime,
                    openingFloatCents = floatCents
                )
            }

            // Schedule alarm 15 min before shift end
            ShiftReminderManager.scheduleReminder15MinBefore(context, endTime)
        }

        showCashDialog = false
        Toast.makeText(context, "Schicht gestartet! Viel Freude im Dienst.", Toast.LENGTH_SHORT).show()
        onShiftStarted()
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF9F7F4))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Welcome & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SprachCafé Team",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SprachCafeRed
                    )
                    Text(
                        text = todayReadable,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                // Active User Badge (Clickable to switch user)
                val memberColor = try {
                    Color(android.graphics.Color.parseColor(prefs.memberColor))
                } catch (e: Exception) {
                    SprachCafeRed
                }

                Surface(
                    onClick = { showMemberPicker = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(memberColor)
                        )
                        Text(
                            text = prefs.memberName ?: "Helfer wählen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Wechseln",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Active Shift Status if already running
            if (prefs.activeShiftId != null || prefs.isCashActive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Schicht ist aktiv",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Start: ${prefs.activeShiftStartTime ?: "--"} | Kasse: ${if (prefs.isCashActive) "Übernommen (${String.format("%.2f €", prefs.openingFloatCents / 100.0)})" else "Ohne Kasse"}",
                                color = Color(0xFF047857),
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = onShiftStarted,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Zur Kasse", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (isLoading && todayShifts.isEmpty() && todayEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SprachCafeRed)
                }
            } else if (todayShifts.isEmpty() && todayEvents.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Keine Schichten oder Events für heute eingetragen",
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4B5563),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Du kannst trotzdem eine Ad-hoc-Schicht starten:",
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                selectedShift = ShiftItem(
                                    id = 0,
                                    date = todayDateStr,
                                    timeSlot = "adhoc",
                                    startTime = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date()),
                                    endTime = "18:00",
                                    memberName = prefs.memberName ?: "Ehrenamtlicher"
                                )
                                showCashDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SprachCafeRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Ad-hoc Schicht jetzt starten")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Events Section
                    if (todayEvents.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = SprachCafeRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Heutige Veranstaltungen & Event-Schichten",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )
                            }
                        }

                        items(todayEvents) { event ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Event Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = event.summary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF111827)
                                                )
                                                Surface(
                                                    color = if (event.source == "manual") Color(0xFFEFF6FF) else Color(0xFFF3F4F6),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (event.source == "manual") "Manuell" else "GCal",
                                                        fontSize = 10.sp,
                                                        color = if (event.source == "manual") Color(0xFF1D4ED8) else Color(0xFF4B5563),
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "${event.startTimeFormatted} – ${event.endTimeFormatted} Uhr",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SprachCafeRed
                                            )
                                        }
                                    }

                                    if (!event.description.isNullOrBlank()) {
                                        Text(
                                            text = event.description,
                                            fontSize = 12.sp,
                                            color = Color(0xFF6B7280),
                                            maxLines = 2
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                                    // Event Slots
                                    Text(
                                        text = "Schicht-Slots:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF374151)
                                    )

                                    if (event.slots.isEmpty()) {
                                        Text(
                                            text = "Keine Schicht-Slots für dieses Event angelegt.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            event.slots.forEach { slot ->
                                                val isAssignedToMe = prefs.memberName?.let { myName ->
                                                    slot.assignedMembers.any { it.contains(myName, ignoreCase = true) }
                                                } ?: false

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isAssignedToMe) Color(0xFFFFFBEB) else Color(0xFFF9FAFB),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isAssignedToMe) Color(0xFFFDE68A) else Color(0xFFE5E7EB)
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                Text(
                                                                    text = slot.roleName,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 13.sp,
                                                                    color = Color(0xFF1F2937)
                                                                )
                                                                if (slot.startTime != null && slot.endTime != null) {
                                                                    Text(
                                                                        text = "(${slot.startTime}–${slot.endTime})",
                                                                        fontSize = 11.sp,
                                                                        color = Color(0xFF6B7280)
                                                                    )
                                                                }
                                                            }

                                                            if (slot.assignedMembers.isNotEmpty()) {
                                                                Text(
                                                                    text = "Besetzt von: ${slot.assignedMembers.joinToString(", ")}",
                                                                    fontSize = 11.sp,
                                                                    color = if (isAssignedToMe) Color(0xFF92400E) else Color(0xFF4B5563)
                                                                )
                                                            } else {
                                                                Text(
                                                                    text = "Noch frei (${slot.assignedMembers.size}/${slot.requiredHelpers})",
                                                                    fontSize = 11.sp,
                                                                    color = Color(0xFF059669),
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }
                                                        }

                                                        if (isAssignedToMe) {
                                                            Button(
                                                                onClick = {
                                                                    val existingShift = todayShifts.find {
                                                                        it.slotId == slot.id || (it.eventId == event.id && it.memberName.contains(prefs.memberName ?: "", ignoreCase = true))
                                                                    }
                                                                    selectedShift = existingShift ?: ShiftItem(
                                                                        id = 0,
                                                                        date = event.date,
                                                                        timeSlot = event.timeSlot ?: "event",
                                                                        startTime = slot.startTime ?: event.startTimeFormatted,
                                                                        endTime = slot.endTime ?: event.endTimeFormatted,
                                                                        memberName = prefs.memberName ?: "Ehrenamtlicher",
                                                                        location = event.location,
                                                                        eventId = event.id,
                                                                        eventTitle = event.summary,
                                                                        slotId = slot.id,
                                                                        slotRole = slot.roleName
                                                                    )
                                                                    showCashDialog = true
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = SprachCafeRed),
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                            ) {
                                                                Text("Starten", fontSize = 12.sp)
                                                            }
                                                        } else if (!slot.isFull) {
                                                            Button(
                                                                onClick = {
                                                                    claimSlotAndStart(slot, event)
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                            ) {
                                                                Text("Übernehmen", fontSize = 12.sp)
                                                            }
                                                        } else {
                                                            Surface(
                                                                color = Color(0xFFE5E7EB),
                                                                shape = RoundedCornerShape(6.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Voll",
                                                                    fontSize = 11.sp,
                                                                    color = Color(0xFF6B7280),
                                                                    fontWeight = FontWeight.Medium,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Regular Shifts Section
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = if (todayEvents.isNotEmpty()) "Reguläre Café-Schichten" else "Heutige Schichten (Pankow)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }

                    if (todayShifts.isEmpty()) {
                        item {
                            Text(
                                text = "Keine regulären Café-Schichten für heute eingetragen.",
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else {
                        items(todayShifts) { shift ->
                            val isMine = prefs.memberName?.let { shift.memberName.contains(it, ignoreCase = true) } ?: false
                            val shiftColor = try {
                                shift.memberColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: SprachCafeRed
                            } catch (e: Exception) {
                                SprachCafeRed
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedShift = shift
                                        showCashDialog = true
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMine) Color(0xFFFFFBEB) else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(shiftColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = shift.memberName.take(2).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = shiftColor,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${shift.startTime ?: "--:--"} – ${shift.endTime ?: "--:--"}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = Color(0xFF1F2937)
                                                )
                                                if (isMine) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        color = Color(0xFFFDE68A),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Deine Schicht",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF92400E),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (!shift.eventTitle.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Surface(
                                                    color = Color(0xFFEFF6FF),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Event: ${shift.eventTitle}${if (!shift.slotRole.isNullOrEmpty()) " (${shift.slotRole})" else ""}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF1D4ED8),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "${shift.memberName} • ${shift.location}",
                                                fontSize = 13.sp,
                                                color = Color(0xFF6B7280)
                                            )

                                            if (!shift.notes.isNullOrEmpty()) {
                                                Text(
                                                    text = shift.notes,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF9CA3AF)
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            selectedShift = shift
                                            showCashDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isMine) SprachCafeRed else Color(0xFF4B5563)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Starten", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Spontaneous / Ad-hoc Shift Button
                    item {
                        OutlinedButton(
                            onClick = {
                                selectedShift = ShiftItem(
                                    id = 0,
                                    date = todayDateStr,
                                    timeSlot = "adhoc",
                                    startTime = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date()),
                                    endTime = "18:00",
                                    memberName = prefs.memberName ?: "Ehrenamtlicher"
                                )
                                showCashDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Spontane Ad-hoc-Schicht starten")
                        }
                    }
                }
            }
        }

        // Dialog: Kasse übernehmen & Anfangsbestand
        if (showCashDialog && selectedShift != null) {
            AlertDialog(
                onDismissRequest = { showCashDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        tint = SprachCafeRed,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Kasse für diese Schicht übernehmen?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Möchtest du für deine Schicht (${selectedShift?.startTime}–${selectedShift?.endTime} Uhr) die Kasse öffnen?",
                            fontSize = 14.sp,
                            color = Color(0xFF4B5563)
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5EB)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Vorgeschlagener Anfangsbestand:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF78350F)
                                )
                                OutlinedTextField(
                                    value = openingFloatInput,
                                    onValueChange = { openingFloatInput = it },
                                    label = { Text("Anfangsbestand (€)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Standard: 50,00 € Wechselgeldsockel",
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanVal = openingFloatInput.replace(",", ".").trim()
                            val cents = ((cleanVal.toDoubleOrNull() ?: 50.0) * 100).toInt()
                            startShiftWithCash(takeCash = true, floatCents = cents)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SprachCafeRed)
                    ) {
                        Text("Ja, Kasse übernehmen")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            startShiftWithCash(takeCash = false, floatCents = 0)
                        }
                    ) {
                        Text("Ohne Kasse starten", color = Color(0xFF6B7280))
                    }
                }
            )
        }

        // Dialog: Helfer auswählen
        if (showMemberPicker) {
            AlertDialog(
                onDismissRequest = { showMemberPicker = false },
                title = { Text("Wer nutzt die App?") },
                text = {
                    LazyColumn(
                        modifier = Modifier.height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allMembers) { member ->
                            val isSelected = prefs.memberCode == member.shortCode
                            val mColor = try {
                                Color(android.graphics.Color.parseColor(member.color))
                            } catch (e: Exception) {
                                SprachCafeRed
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        prefs.memberName = member.name
                                        prefs.memberCode = member.shortCode
                                        prefs.memberColor = member.color
                                        prefs.memberEmail = member.email
                                        showMemberPicker = false
                                        Toast.makeText(context, "Aktiv: ${member.name}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(mColor)
                                )
                                Text(
                                    text = member.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SprachCafeRed else Color(0xFF1F2937),
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SprachCafeRed
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMemberPicker = false }) {
                        Text("Schließen")
                    }
                }
            )
        }
    }
}
