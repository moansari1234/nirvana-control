package com.nirvana.control.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.ui.theme.*

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    EQUALIZER("Equalizer"),
    SPATIAL("Spatial"),
    GESTURES("Gestures"),
    SETTINGS("Settings")
}

@Composable
fun NirvanaBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    // Detached Floating Island Glass Pill
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(DarkSurface)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(SpecularHighlight, SpecularBorder, Color(0x05FFFFFF))
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(23.dp))
                    .background(ObsidianCore)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    val interactionSource = remember { MutableInteractionSource() }

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) TextPrimary else TextMuted,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "tabText"
                    )

                    val activeGlowAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(durationMillis = 250),
                        label = "tabGlow"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onTabSelected(tab) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        NavVectorIcon(tab = tab, isSelected = isSelected)

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = tab.title,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Active Glowing Indicator Dot
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) BoatRed.copy(alpha = activeGlowAlpha) else Color.Transparent
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavVectorIcon(tab: AppTab, isSelected: Boolean) {
    val tint = if (isSelected) BoatRed else TextSecondary

    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)

        when (tab) {
            AppTab.DASHBOARD -> {
                // Precision Home Monogram
                val path = androidx.compose.ui.graphics.Path()
                path.moveTo(w * 0.15f, h * 0.45f)
                path.lineTo(w * 0.5f, h * 0.18f)
                path.lineTo(w * 0.85f, h * 0.45f)
                path.lineTo(w * 0.85f, h * 0.82f)
                path.lineTo(w * 0.15f, h * 0.82f)
                path.close()
                drawPath(path, color = tint, style = stroke)
                drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(w * 0.5f, h * 0.6f))
            }
            AppTab.EQUALIZER -> {
                // Precision Dual Faders
                drawLine(color = tint, start = Offset(w * 0.32f, h * 0.15f), end = Offset(w * 0.32f, h * 0.85f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.68f, h * 0.15f), end = Offset(w * 0.68f, h * 0.85f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(color = tint, radius = 3.dp.toPx(), center = Offset(w * 0.32f, h * 0.4f))
                drawCircle(color = tint, radius = 3.dp.toPx(), center = Offset(w * 0.68f, h * 0.65f))
            }
            AppTab.SPATIAL -> {
                // 360° Spatial Sound Field
                drawCircle(color = tint, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(color = tint, radius = 2.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f))
                drawArc(
                    color = tint,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.05f, h * 0.05f),
                    size = Size(w * 0.9f, h * 0.9f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            AppTab.GESTURES -> {
                // Tactile Earbud Touch Node
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.32f, h * 0.18f),
                    size = Size(w * 0.36f, h * 0.64f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = stroke
                )
                drawLine(color = tint, start = Offset(w * 0.42f, h * 0.42f), end = Offset(w * 0.58f, h * 0.42f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.42f, h * 0.54f), end = Offset(w * 0.58f, h * 0.54f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            AppTab.SETTINGS -> {
                // Concentric Precision Gear Node
                drawCircle(color = tint, radius = w * 0.32f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f))
                for (angle in 0 until 360 step 60) {
                    val rad = Math.toRadians(angle.toDouble())
                    val sx = (w * 0.5f) + (w * 0.32f) * kotlin.math.cos(rad).toFloat()
                    val sy = (h * 0.5f) + (w * 0.32f) * kotlin.math.sin(rad).toFloat()
                    val ex = (w * 0.5f) + (w * 0.44f) * kotlin.math.cos(rad).toFloat()
                    val ey = (h * 0.5f) + (w * 0.44f) * kotlin.math.sin(rad).toFloat()
                    drawLine(color = tint, start = Offset(sx, sy), end = Offset(ex, ey), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
        }
    }
}
