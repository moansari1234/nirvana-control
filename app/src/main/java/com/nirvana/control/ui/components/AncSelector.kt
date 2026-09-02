package com.nirvana.control.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.model.AncMode
import com.nirvana.control.ui.theme.*

@Composable
fun AncSelector(
    currentMode: AncMode,
    onModeSelected: (AncMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Noise Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currentMode.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (currentMode) {
                        AncMode.ANC_ON -> BoatRed
                        AncMode.TRANSPARENCY -> ElectricCyan
                        AncMode.OFF -> TextSecondary
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AncModeItem(
                    title = "Noise Cancel",
                    icon = "🔇",
                    isSelected = currentMode == AncMode.ANC_ON,
                    selectedColor = BoatRed,
                    onClick = { onModeSelected(AncMode.ANC_ON) },
                    modifier = Modifier.weight(1f)
                )

                AncModeItem(
                    title = "Normal",
                    icon = "⏹️",
                    isSelected = currentMode == AncMode.OFF,
                    selectedColor = DarkSurfaceHighlight,
                    onClick = { onModeSelected(AncMode.OFF) },
                    modifier = Modifier.weight(1f)
                )

                AncModeItem(
                    title = "Transparency",
                    icon = "👂",
                    isSelected = currentMode == AncMode.TRANSPARENCY,
                    selectedColor = ElectricCyan,
                    onClick = { onModeSelected(AncMode.TRANSPARENCY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AncModeItem(
    title: String,
    icon: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else Color.Transparent,
        label = "ancBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) PureBlack else TextSecondary,
        label = "ancText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
        }
    }
}
