package com.nirvana.control.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.model.SpatialAudioMode
import com.nirvana.control.ui.components.DoubleBezelCard
import com.nirvana.control.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpatialStudioScreen(
    currentMode: SpatialAudioMode,
    onSetMode: (SpatialAudioMode) -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isPlayingDemo by remember { mutableStateOf(false) }
    var recenteredMessageVisible by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "spatialPulse")
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRotation"
    )

    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wavePulse"
    )

    val verticalScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .verticalScroll(verticalScroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Column {
            Text(
                text = "ACOUSTIC HOLOGRAPHY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = ElectricCyan
            )
            Text(
                text = "Spatial Audio Studio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "360° Dynamic Soundfield with Gyroscope IMU Tracking",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // Mode Selector Card
        DoubleBezelCard {
            Text(
                text = "SPATIAL PROJECTION MODE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, SpecularBorder, RoundedCornerShape(14.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SpatialModeOption(
                    title = "Stereo Bypass",
                    subtitle = "Direct 2.0 Audio",
                    isSelected = currentMode == SpatialAudioMode.OFF,
                    onClick = { onSetMode(SpatialAudioMode.OFF) },
                    modifier = Modifier.weight(1f)
                )

                SpatialModeOption(
                    title = "Fixed Spatial",
                    subtitle = "Concert Theater",
                    isSelected = currentMode == SpatialAudioMode.FIXED,
                    onClick = { onSetMode(SpatialAudioMode.FIXED) },
                    modifier = Modifier.weight(1f)
                )

                SpatialModeOption(
                    title = "Head Track",
                    subtitle = "Dynamic IMU 3D",
                    isSelected = currentMode == SpatialAudioMode.HEAD_TRACKING,
                    onClick = { onSetMode(SpatialAudioMode.HEAD_TRACKING) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Concentric 3D Gyroscope Radar Card
        DoubleBezelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SPATIAL HORIZON RADAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = if (currentMode == SpatialAudioMode.HEAD_TRACKING) "Active Gyro Tracking" else "Static Field",
                        fontSize = 11.sp,
                        color = if (currentMode == SpatialAudioMode.HEAD_TRACKING) ElectricCyan else TextMuted
                    )
                }

                if (recenteredMessageVisible) {
                    Text(
                        text = "HORIZON CALIBRATED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NeonGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Precision Radar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    val w = size.width
                    val center = Offset(w / 2f, w / 2f)
                    val r = w / 2f

                    // Concentric Range Rings
                    val ringStroke = Stroke(width = 1.dp.toPx())
                    drawCircle(color = Color(0x15FFFFFF), radius = r * 0.95f, center = center, style = ringStroke)
                    drawCircle(color = Color(0x1AFFFFFF), radius = r * 0.65f, center = center, style = ringStroke)
                    drawCircle(color = Color(0x22FFFFFF), radius = r * 0.35f, center = center, style = ringStroke)

                    // Crosshair Axes
                    drawLine(color = Color(0x15FFFFFF), start = Offset(center.x, 0f), end = Offset(center.x, w), strokeWidth = 1.dp.toPx())
                    drawLine(color = Color(0x15FFFFFF), start = Offset(0f, center.y), end = Offset(w, center.y), strokeWidth = 1.dp.toPx())

                    // Dynamic Sonar Wave (if Head Tracking enabled)
                    if (currentMode == SpatialAudioMode.HEAD_TRACKING) {
                        drawCircle(
                            color = ElectricCyan.copy(alpha = 0.15f * wavePulse),
                            radius = r * 0.85f * wavePulse,
                            center = center
                        )
                        drawCircle(
                            color = ElectricCyan.copy(alpha = 0.4f * (1f - wavePulse)),
                            radius = r * 0.85f * wavePulse,
                            center = center,
                            style = Stroke(1.5.dp.toPx())
                        )

                        // Radar Sweep Line
                        val rad = Math.toRadians(radarAngle.toDouble())
                        val endX = center.x + (r * 0.95f) * cos(rad).toFloat()
                        val endY = center.y + (r * 0.95f) * sin(rad).toFloat()
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, ElectricCyan),
                                start = center,
                                end = Offset(endX, endY)
                            ),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Center Head Reticle
                    drawCircle(color = PureBlack, radius = 16.dp.toPx(), center = center)
                    drawCircle(
                        color = if (currentMode == SpatialAudioMode.HEAD_TRACKING) ElectricCyan else TextSecondary,
                        radius = 16.dp.toPx(),
                        center = center,
                        style = Stroke(2.dp.toPx())
                    )
                    drawCircle(
                        color = if (currentMode == SpatialAudioMode.HEAD_TRACKING) ElectricCyan else BoatRed,
                        radius = 5.dp.toPx(),
                        center = center
                    )
                }

                Text(
                    text = "FRONT",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tactical Horizon Re-Center Button
            Button(
                onClick = {
                    onRecenter()
                    recenteredMessageVisible = true
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(2000)
                        recenteredMessageVisible = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = "RE-CENTER HORIZON",
                    color = PureBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
private fun SpatialModeOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (isSelected) ElectricCyan.copy(alpha = 0.18f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) ElectricCyan else Color.Transparent,
                RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimary else TextSecondary
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = if (isSelected) ElectricCyan else TextMuted
            )
        }
    }
}
