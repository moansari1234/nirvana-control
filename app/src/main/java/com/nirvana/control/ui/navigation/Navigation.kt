package com.nirvana.control.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.ui.theme.*

enum class AppTab(val title: String, val icon: String) {
    DASHBOARD("Dashboard", "🏠"),
    EQUALIZER("Equalizer", "🎚️"),
    SPATIAL("Spatial", "🎧"),
    GESTURES("Gestures", "👆"),
    SETTINGS("Settings", "⚙️")
}

@Composable
fun NirvanaBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = DarkSurface,
        tonalElevation = 0.dp
    ) {
        AppTab.entries.forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Text(
                        text = tab.icon,
                        fontSize = if (isSelected) 20.sp else 17.sp
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BoatRed else TextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BoatRed,
                    unselectedIconColor = TextSecondary,
                    selectedTextColor = BoatRed,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = DarkSurfaceVariant
                )
            )
        }
    }
}
