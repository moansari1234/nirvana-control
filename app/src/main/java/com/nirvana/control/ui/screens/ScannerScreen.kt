package com.nirvana.control.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.model.ScannedDevice
import com.nirvana.control.ui.theme.*

@Composable
fun ScannerDialog(
    scannedDevices: List<ScannedDevice>,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceSelected: (ScannedDevice) -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarTransition")
    val radarRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRadius"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isScanning) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            drawCircle(
                                color = BoatRed.copy(alpha = 0.4f),
                                radius = radarRadius * (size.minDimension / 120f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = BoatRed,
                                radius = 4.dp.toPx()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isScanning) "Scanning Nearby..." else "Bluetooth Devices",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                }

                if (isScanning) {
                    TextButton(onClick = onStopScan) {
                        Text("Stop", color = TextSecondary, fontSize = 12.sp)
                    }
                } else {
                    TextButton(onClick = onStartScan) {
                        Text("Rescan", color = BoatRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (scannedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📡", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isScanning) "Searching for Nirvana Space..." else "No devices found",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(scannedDevices, key = { it.address }) { device ->
                            DeviceScanItem(
                                device = device,
                                onClick = { onDeviceSelected(device) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextMuted)
            }
        }
    )
}

@Composable
private fun DeviceScanItem(
    device: ScannedDevice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isNirvana) DarkSurfaceHighlight else DarkSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (device.isNirvana) FontWeight.Bold else FontWeight.Medium,
                        color = if (device.isNirvana) BoatRed else TextPrimary
                    )
                    if (device.isNirvana) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BoatRed.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Nirvana",
                                color = BoatRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "  •  ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // RSSI Signal indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = " dBm",
                        fontSize = 11.sp,
                        color = when {
                            device.rssi >= -60 -> NeonGreen
                            device.rssi >= -75 -> AmberWarning
                            else -> RedDanger
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (device.isNirvana) BoatRed else DarkSurfaceHighlight
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
