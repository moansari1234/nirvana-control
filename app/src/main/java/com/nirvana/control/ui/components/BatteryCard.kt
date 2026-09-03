package com.nirvana.control.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    DoubleBezelCard(
        modifier = modifier.fillMaxWidth(),
        innerPadding = PaddingValues(vertical = 18.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BatteryItem(label = "LEFT BUD", percent = leftBattery, isCharging = leftCharging)
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .width(1.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, SpecularBorder, Color.Transparent)
                        )
                    )
            )
            BatteryItem(label = "CHARGING CASE", percent = caseBattery, isCharging = caseCharging)
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .width(1.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, SpecularBorder, Color.Transparent)
                        )
                    )
            )
            BatteryItem(label = "RIGHT BUD", percent = rightBattery, isCharging = rightCharging)
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
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
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
                text = if (percent >= 0) "${percent}%" else "--",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (percent >= 0) TextPrimary else TextMuted
            )

            if (isCharging) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(AmberWarning.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 9.sp,
                        color = AmberWarning
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Luxury Horizontal Gauge
        val animatedProgress by animateFloatAsState(
            targetValue = if (percent >= 0) (percent.coerceIn(0, 100) / 100f) else 0f,
            label = "batteryProgress"
        )

        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DarkSurfaceHighlight)
        ) {
            val gaugeBrush = when {
                percent <= 20 -> Brush.horizontalGradient(listOf(RedDanger, AmberWarning))
                percent <= 40 -> Brush.horizontalGradient(listOf(AmberWarning, NeonGreen))
                else -> Brush.horizontalGradient(listOf(ElectricCyan, NeonGreen))
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(2.dp))
                    .background(gaugeBrush)
            )
        }
    }
}
