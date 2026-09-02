package org.sprachcafe.team.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sprachcafe.team.data.TeamPreferences
import org.sprachcafe.team.ui.screens.*
import org.sprachcafe.team.ui.theme.SprachCafeRed

enum class NavDestination(
    val title: String,
    val icon: ImageVector
) {
    SHIFT("Schicht", Icons.Default.Badge),
    KIOSK("Kasse", Icons.Default.ShoppingCart),
    CASH_COUNT("Kassensturz", Icons.Default.Calculate),
    INVENTORY("Inventur", Icons.Default.Inventory2),
    LIBRARY("Bibliothek", Icons.Default.MenuBook)
}

@Composable
fun MainApp(
    initialDestination: NavDestination? = null
) {
    val context = LocalContext.current
    val prefs = remember { TeamPreferences.getInstance(context) }

    val defaultDest = remember {
        initialDestination ?: if (prefs.activeShiftId != null || prefs.isCashActive) {
            NavDestination.KIOSK
        } else {
            NavDestination.SHIFT
        }
    }

    var currentDestination by remember { mutableStateOf(defaultDest) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavDestination.values().forEach { destination ->
                    val selected = currentDestination == destination
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SprachCafeRed,
                            selectedTextColor = SprachCafeRed,
                            indicatorColor = Color(0xFFFAF5EB),
                            unselectedIconColor = Color(0xFF9CA3AF),
                            unselectedTextColor = Color(0xFF9CA3AF)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentDestination) {
            NavDestination.SHIFT -> ShiftStartScreen(
                modifier = Modifier.padding(innerPadding),
                onShiftStarted = { currentDestination = NavDestination.KIOSK }
            )
            NavDestination.KIOSK -> KioskScreen(modifier = Modifier.padding(innerPadding))
            NavDestination.CASH_COUNT -> CashCountScreen(modifier = Modifier.padding(innerPadding))
            NavDestination.INVENTORY -> InventoryScreen(modifier = Modifier.padding(innerPadding))
            NavDestination.LIBRARY -> LibraryServiceScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
