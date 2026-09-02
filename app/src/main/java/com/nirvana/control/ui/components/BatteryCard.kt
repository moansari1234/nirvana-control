package com.nirvana.control.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.ui.theme.*

@Composable
fun BatteryCard(
    leftBattery: Int,
    leftCharging: Boolean,
    rightBattery: Int,
    rightCharging: Boolean,
    caseBattery: Int,
    caseCharging: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BatteryItem(label = "Left Bud", percent = leftBattery, isCharging = leftCharging)
            Box(modifier = Modifier.height(44.dp).width(1.dp).background(DividerColor))
            BatteryItem(label = "Case", percent = caseBattery, isCharging = caseCharging)
            Box(modifier = Modifier.height(44.dp).width(1.dp).background(DividerColor))
            BatteryItem(label = "Right Bud", percent = rightBattery, isCharging = rightCharging)
        }
    }
}

@Composable
private fun BatteryItem(
    label: String,
    percent: Int,
    isCharging: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val batteryColor = when {
                percent < 0 -> TextMuted
                percent <= 20 -> RedDanger
                percent <= 40 -> AmberWarning
                else -> NeonGreen
            }

            Text(
                text = if (percent >= 0) "%" else "--",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (percent >= 0) TextPrimary else TextMuted
            )

            if (isCharging) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "⚡",
                    fontSize = 14.sp,
                    color = AmberWarning
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Compact visual level bar
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DarkSurfaceHighlight)
        ) {
            val progress = (percent.coerceIn(0, 100) / 100f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (percent >= 0) progress else 0f)
                    .background(
                        when {
                            percent <= 20 -> RedDanger
                            percent <= 40 -> AmberWarning
                            else -> NeonGreen
                        }
                    )
            )
        }
    }
}
