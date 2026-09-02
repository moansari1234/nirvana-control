package com.nirvana.control.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.model.*
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "10-Band Hardware EQ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Direct DSP Parametric Equalizer (-6dB to +6dB)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            TextButton(
                onClick = {
                    val flat = IntArray(10) { 0 }
                    editableGains = flat
                    selectedPreset = "Balanced"
                    onApplyGains(flat, "Balanced")
                }
            ) {
                Text("Reset", color = TextSecondary, fontSize = 13.sp)
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
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedPreset = preset.name
                        editableGains = preset.gains.copyOf()
                        onApplyGains(editableGains, preset.name)
                    },
                    label = {
                        Text(
                            text = preset.name,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BoatRed,
                        selectedLabelColor = PureBlack,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // 10-Band Sliders Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EQ_BAND_LABELS.forEachIndexed { index, freqLabel ->
                    val gainValue = if (index < editableGains.size) editableGains[index] else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = freqLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            modifier = Modifier.width(52.dp)
                        )

                        Slider(
                            value = gainValue.toFloat(),
                            onValueChange = { newFloat ->
                                val newGains = editableGains.copyOf()
                                newGains[index] = newFloat.toInt()
                                editableGains = newGains
                                selectedPreset = "Custom"
                                onApplyGains(newGains, "Custom")
                            },
                            valueRange = -6f..6f,
                            steps = 11,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = BoatRed,
                                activeTrackColor = BoatRed,
                                inactiveTrackColor = DarkSurfaceHighlight
                            )
                        )

                        Text(
                            text = if (gainValue > 0) "+dB" else "dB",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (gainValue > 0) BoatRed else if (gainValue < 0) ElectricCyan else TextSecondary,
                            modifier = Modifier.width(44.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }
        }

        // Guided Hearing Calibration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { showHearingTestDialog = true },
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎯", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hearing Calibration Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                    Text(
                        text = "Run a guided frequency test to create your personalized hearing EQ profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Text("Start →", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    if (showHearingTestDialog) {
        HearingCalibrationDialog(
            onDismiss = { showHearingTestDialog = false },
            onComplete = { calibratedGains ->
                editableGains = calibratedGains
                selectedPreset = "Hearing Profile"
                onApplyGains(calibratedGains, "Hearing Profile")
                showHearingTestDialog = false
            }
        )
    }
}

@Composable
private fun HearingCalibrationDialog(
    onDismiss: () -> Unit,
    onComplete: (IntArray) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val testFrequencies = listOf(250, 500, 1000, 2000, 4000, 8000)
    var stepIndex by remember { mutableStateOf(0) }
    val results = remember { mutableStateListOf(0, 0, 0, 0, 0, 0) }

    fun playTone(freqHz: Int) {
        coroutineScope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 44100
                val durationSeconds = 0.5
                val numSamples = (durationSeconds * sampleRate).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / freqHz)
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * 0.3).toInt().toShort()
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
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
            } catch (e: Exception) {
                // Ignore audio generation failures
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Hearing Calibration (Step /)",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val currentFreq = testFrequencies[stepIndex]
                Text(
                    text = "Testing frequency: Hz\nTap 'Play Tone', then rate how clearly you hear it.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Button(
                    onClick = { playTone(currentFreq) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔊 Play Tone (Hz)", color = PureBlack, fontWeight = FontWeight.Bold)
                }

                Text("Clarity level:", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        "Faint (-2dB)" to 3,
                        "Normal (0dB)" to 1,
                        "Loud (+2dB)" to 0
                    ).forEach { (label, boost) ->
                        OutlinedButton(
                            onClick = {
                                results[stepIndex] = boost
                                if (stepIndex < testFrequencies.size - 1) {
                                    stepIndex++
                                } else {
                                    // Generate 10-band profile
                                    val finalGains = intArrayOf(
                                        results[0], results[0], results[0],
                                        results[1], results[2], results[2],
                                        results[3], results[4], results[5], results[5]
                                    )
                                    onComplete(finalGains)
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
