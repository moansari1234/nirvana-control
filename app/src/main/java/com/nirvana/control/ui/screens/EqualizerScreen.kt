package com.nirvana.control.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.model.*
import com.nirvana.control.ui.components.DoubleBezelCard
import com.nirvana.control.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun EqualizerScreen(
    currentGains: IntArray,
    activePresetName: String,
    onApplyGains: (IntArray, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editableGains by remember(currentGains) { mutableStateOf(currentGains.copyOf()) }
    var selectedPreset by remember(activePresetName) { mutableStateOf(activePresetName) }
    var showHearingTestDialog by remember { mutableStateOf(false) }

    val verticalScroll = rememberScrollState()
    val presetScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .verticalScroll(verticalScroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HARDWARE DSP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    color = BoatRed
                )
                Text(
                    text = "Acoustic Equalizer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Direct 10-band hardware filter (-6dB to +6dB)",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            OutlinedButton(
                onClick = {
                    val flat = IntArray(10) { 0 }
                    editableGains = flat
                    selectedPreset = "Balanced"
                    onApplyGains(flat, "Balanced")
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, SpecularBorder),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("RESET FLAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = TextSecondary)
            }
        }

        // Preset Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(presetScroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DEFAULT_EQ_PRESETS.forEach { preset ->
                val isSelected = selectedPreset == preset.name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BoatRed.copy(alpha = 0.2f) else DarkSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) BoatRed else SpecularBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedPreset = preset.name
                            editableGains = preset.gains.copyOf()
                            onApplyGains(editableGains, preset.name)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset.name.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }

        // Real-Time Frequency Spline Curve Card
        DoubleBezelCard {
            Text(
                text = "FREQUENCY RESPONSE CURVE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Frequency Spline
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PureBlack)
                    .border(1.dp, SpecularBorder, RoundedCornerShape(12.dp))
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Draw 0dB Grid Line
                drawLine(
                    color = Color(0x1FFFFFFF),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.dp.toPx()
                )

                // Build Spline Path from 10 bands
                val path = Path()
                val fillPath = Path()
                fillPath.moveTo(0f, h)

                val step = w / 9f
                for (i in 0..9) {
                    val gain = editableGains.getOrElse(i) { 0 }
                    // Gain is -6 to +6. Map to 0..h
                    val y = midY - (gain / 6f) * (midY * 0.8f)
                    val x = i * step

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevGain = editableGains.getOrElse(i - 1) { 0 }
                        val prevY = midY - (prevGain / 6f) * (midY * 0.8f)
                        val prevX = (i - 1) * step
                        val cx = (prevX + x) / 2f
                        path.cubicTo(cx, prevY, cx, y, x, y)
                        fillPath.cubicTo(cx, prevY, cx, y, x, y)
                    }
                }

                fillPath.lineTo(w, h)
                fillPath.close()

                // Gradient Fill Under Curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(BoatRed.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = h
                    )
                )

                // Spline Outline
                drawPath(
                    path = path,
                    color = BoatRed,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Band Control Dots
                for (i in 0..9) {
                    val gain = editableGains.getOrElse(i) { 0 }
                    val y = midY - (gain / 6f) * (midY * 0.8f)
                    val x = i * step
                    drawCircle(color = PureBlack, radius = 4.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = ElectricCyan, radius = 2.5.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        // 10-Band Sliders Card
        DoubleBezelCard {
            Text(
                text = "DSP FREQUENCY BANDS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0..9) {
                    val freq = EQ_BAND_LABELS.getOrElse(i) { "${i + 1}" }
                    val gain = editableGains.getOrElse(i) { 0 }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = freq,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.width(55.dp)
                        )

                        Slider(
                            value = gain.toFloat(),
                            onValueChange = { newVal ->
                                selectedPreset = "Custom"
                                val updated = editableGains.copyOf()
                                updated[i] = newVal.toInt()
                                editableGains = updated
                                onApplyGains(updated, "Custom")
                            },
                            valueRange = -6f..6f,
                            steps = 11,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan,
                                inactiveTrackColor = DarkSurfaceHighlight
                            )
                        )

                        Text(
                            text = if (gain > 0) "+${gain}dB" else "${gain}dB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gain != 0) ElectricCyan else TextSecondary,
                            modifier = Modifier.width(48.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
