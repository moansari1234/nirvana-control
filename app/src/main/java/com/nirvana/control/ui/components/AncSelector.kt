package com.nirvana.control.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    DoubleBezelCard(
        modifier = modifier.fillMaxWidth(),
        innerPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACTIVE NOISE CANCELLATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Acoustic Environment Control",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (currentMode) {
                            AncMode.ANC_ON -> BoatRed.copy(alpha = 0.15f)
                            AncMode.TRANSPARENCY -> ElectricCyan.copy(alpha = 0.15f)
                            AncMode.OFF -> DarkSurfaceHighlight
                        }
                    )
                    .border(
                        1.dp,
                        when (currentMode) {
                            AncMode.ANC_ON -> BoatRed.copy(alpha = 0.4f)
                            AncMode.TRANSPARENCY -> ElectricCyan.copy(alpha = 0.4f)
                            AncMode.OFF -> SpecularBorder
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = currentMode.label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = when (currentMode) {
                        AncMode.ANC_ON -> BoatRed
                        AncMode.TRANSPARENCY -> ElectricCyan
                        AncMode.OFF -> TextPrimary
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Luxury Segmented Control Track
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, SpecularBorder, RoundedCornerShape(14.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AncModeItem(
                title = "Noise Cancel",
                mode = AncMode.ANC_ON,
                isSelected = currentMode == AncMode.ANC_ON,
                activeColor = BoatRed,
                onClick = { onModeSelected(AncMode.ANC_ON) },
                modifier = Modifier.weight(1f)
            )

            AncModeItem(
                title = "Normal",
                mode = AncMode.OFF,
                isSelected = currentMode == AncMode.OFF,
                activeColor = Color(0xFF2C2C38),
                onClick = { onModeSelected(AncMode.OFF) },
                modifier = Modifier.weight(1f)
            )

            AncModeItem(
                title = "Transparency",
                mode = AncMode.TRANSPARENCY,
                isSelected = currentMode == AncMode.TRANSPARENCY,
                activeColor = ElectricCyan,
                onClick = { onModeSelected(AncMode.TRANSPARENCY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AncModeItem(
    title: String,
    mode: AncMode,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "ancBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "ancBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Precision Canvas Vector Icon
            AncVectorIcon(mode = mode, isSelected = isSelected, activeColor = activeColor)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimary else TextSecondary
            )
        }
    }
}

@Composable
private fun AncVectorIcon(
    mode: AncMode,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val tint = if (isSelected) {
        if (mode == AncMode.OFF) TextPrimary else activeColor
    } else {
        TextMuted
    }

    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)

        when (mode) {
            AncMode.ANC_ON -> {
                // Phase-cancellation opposing arcs with center acoustic null
                drawArc(
                    color = tint,
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.1f, h * 0.15f),
                    size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.7f),
                    style = stroke
                )
                drawArc(
                    color = tint,
                    startAngle = 315f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.55f, h * 0.15f),
                    size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.7f),
                    style = stroke
                )
                drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f))
            }
            AncMode.OFF -> {
                // Balanced circular bypass aperture
                drawCircle(color = tint, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.3f), end = Offset(w * 0.5f, h * 0.7f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            AncMode.TRANSPARENCY -> {
                // Ambient passthrough acoustic ripples
                drawArc(
                    color = tint,
                    startAngle = 225f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f, h * 0.1f),
                    size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.7f),
                    style = stroke
                )
                drawArc(
                    color = tint,
                    startAngle = 45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f, h * 0.2f),
                    size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.7f),
                    style = stroke
                )
                drawCircle(color = tint, radius = 2.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f))
            }
        }
    }
}
