package com.nirvana.control.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.model.SpatialAudioMode
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
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
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
                text = "Spatial Audio Studio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "360° Theater Surround with Gyroscope Head-Tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Mode Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Spatial Audio Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        SpatialAudioMode.OFF to "Off",
                        SpatialAudioMode.FIXED to "Fixed 3D",
                        SpatialAudioMode.HEAD_TRACKING to "Head-Track"
                    ).forEach { (mode, label) ->
                        val isSelected = currentMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ElectricCyan else Color.Transparent)
                                .clickable { onSetMode(mode) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PureBlack else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3D Visualizer & Recenter Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "360° Soundstage Orientation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Simulated 3D Compass Radar
                Box(
                    modifier = Modifier
                        .size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2 - 8.dp.toPx()

                        // Outer circles
                        drawCircle(
                            color = DarkSurfaceHighlight,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = DarkSurfaceVariant,
                            radius = radius * 0.65f,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Center indicator / Head direction
                        val arrowColor = if (currentMode == SpatialAudioMode.HEAD_TRACKING) ElectricCyan else TextSecondary
                        drawCircle(
                            color = arrowColor.copy(alpha = if (currentMode == SpatialAudioMode.HEAD_TRACKING) pulseAlpha else 0.4f),
                            radius = 24.dp.toPx(),
                            center = center
                        )

                        // Front facing vector line
                        val frontEnd = Offset(center.x, center.y - radius * 0.9f)
                        drawLine(
                            color = if (currentMode != SpatialAudioMode.OFF) BoatRed else TextMuted,
                            start = center,
                            end = frontEnd,
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎧", fontSize = 28.sp)
                        Text(
                            text = if (currentMode == SpatialAudioMode.HEAD_TRACKING) "CENTER" else currentMode.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentMode == SpatialAudioMode.HEAD_TRACKING) ElectricCyan else TextSecondary
                        )
                    }
                }

                // 1-Tap Recenter Button
                Button(
                    onClick = {
                        onRecenter()
                        recenteredMessageVisible = true
                    },
                    enabled = currentMode == SpatialAudioMode.HEAD_TRACKING,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = PureBlack
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎯 Recenter Head Tracking",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (recenteredMessageVisible) {
                    Text(
                        text = "✓ Soundstage calibrated to current position",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Spatial Audio Demo Tone
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3D Surround Test Sound",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Plays a 3D revolving tone across left and right channels",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = {
                        if (!isPlayingDemo) {
                            isPlayingDemo = true
                            coroutineScope.launch(Dispatchers.Default) {
                                try {
                                    val sampleRate = 44100
                                    val durationSeconds = 3.0
                                    val numFrames = (durationSeconds * sampleRate).toInt()
                                    // Stereo PCM
                                    val buffer = ShortArray(numFrames * 2)

                                    for (i in 0 until numFrames) {
                                        val angle = 2.0 * Math.PI * i / (sampleRate / 440.0)
                                        val panPhase = 2.0 * Math.PI * i / numFrames // 0 to 2pi
                                        val leftVol = (cos(panPhase) * 0.5 + 0.5).toFloat()
                                        val rightVol = (sin(panPhase) * 0.5 + 0.5).toFloat()

                                        val sample = (sin(angle) * Short.MAX_VALUE * 0.35f).toInt()
                                        buffer[i * 2] = (sample * leftVol).toInt().toShort()
                                        buffer[i * 2 + 1] = (sample * rightVol).toInt().toShort()
                                    }

                                    val track = AudioTrack.Builder()
                                        .setAudioAttributes(
                                            AudioAttributes.Builder()
                                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                .build()
                                        )
                                        .setAudioFormat(
                                            AudioFormat.Builder()
                                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                                .setSampleRate(sampleRate)
                                                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                                                .build()
                                        )
                                        .setBufferSizeInBytes(buffer.size * 2)
                                        .build()

                                    track.write(buffer, 0, buffer.size)
                                    track.play()
                                } catch (e: Exception) {
                                    // Ignore demo errors
                                } finally {
                                    isPlayingDemo = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BoatRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isPlayingDemo) "Playing..." else "▶ Play 3D Demo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
