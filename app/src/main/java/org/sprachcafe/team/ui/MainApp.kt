package org.sprachcafe.team.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sprachcafe.team.ui.screens.CashCountScreen
import org.sprachcafe.team.ui.screens.KioskScreen
import org.sprachcafe.team.ui.screens.LibraryScannerScreen
import org.sprachcafe.team.ui.screens.WasteProtocolScreen
import org.sprachcafe.team.ui.theme.SprachCafeRed

enum class NavDestination(
    val title: String,
    val icon: ImageVector
) {
    KIOSK("Kasse", Icons.Default.ShoppingCart),
    CASH_COUNT("Kassensturz", Icons.Default.Calculate),
    WASTE("Minderung", Icons.Default.Shield),
    LIBRARY("Bibliothek", Icons.Default.Book)
}

@Composable
fun MainApp() {
    var currentDestination by remember { mutableStateOf(NavDestination.KIOSK) }

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
                                fontSize = 11.sp,
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
            NavDestination.KIOSK -> KioskScreen(modifier = Modifier.padding(innerPadding))
            NavDestination.CASH_COUNT -> CashCountScreen(modifier = Modifier.padding(innerPadding))
            NavDestination.WASTE -> WasteProtocolScreen(modifier = Modifier.padding(innerPadding))
            NavDestination.LIBRARY -> LibraryScannerScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
