package com.nirvana.control.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.model.AncMode
import com.nirvana.control.model.ConnectionState
import com.nirvana.control.model.DeviceState
import com.nirvana.control.ui.components.AncSelector
import com.nirvana.control.ui.components.BatteryCard
import com.nirvana.control.ui.components.DoubleBezelCard
import com.nirvana.control.ui.theme.*
import com.nirvana.control.util.AppLog

@SuppressLint("MissingPermission")
@Composable
fun DashboardScreen(
    deviceState: DeviceState,
    pairedDevices: List<BluetoothDevice> = emptyList(),
    onSelectDevice: (BluetoothDevice) -> Unit = {},
    onSetAncMode: (AncMode) -> Unit,
    onSetGameMode: (Boolean) -> Unit,
    onSetInEarDetection: (Boolean) -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenLogs: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bluetooth Disabled Banner
        if (!deviceState.isBluetoothEnabled) {
            DoubleBezelCard(
                borderColor = RedDanger.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RedDanger.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⚡", fontSize = 16.sp, color = RedDanger)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "BLUETOOTH DISABLED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = RedDanger
                            )
                            Text(
                                text = "Enable to connect to Nirvana Space",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onEnableBluetooth,
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Turn On", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }
        }

        // Top Hero Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "boAt",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = BoatRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(TextMuted))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NIRVANA CONTROL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.6.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = deviceState.deviceName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing Status Orb
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    val statusColor = when (deviceState.connectionState) {
                        ConnectionState.CONNECTED -> NeonGreen
                        ConnectionState.CONNECTING -> AmberWarning
                        ConnectionState.SCANNING -> ElectricCyan
                        ConnectionState.DISCONNECTED -> RedDanger
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = pulseAlpha))
                            .border(1.dp, statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (deviceState.connectionState) {
                            ConnectionState.CONNECTED -> "Active Control Link"
                            ConnectionState.CONNECTING -> "Negotiating SPP..."
                            ConnectionState.SCANNING -> "Scanning Bands..."
                            ConnectionState.DISCONNECTED -> "Awaiting Connection"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (deviceState.isConnected) NeonGreen else TextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!deviceState.isConnected) {
                    Button(
                        onClick = onOpenScanner,
                        colors = ButtonDefaults.buttonColors(containerColor = BoatRed),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("RADAR SCAN", fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.8.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDisconnect,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpecularBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // When Disconnected: Paired Devices & Diagnostic Tools
        if (!deviceState.isConnected) {
            DoubleBezelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PAIRED AUDIO DEVICES (${pairedDevices.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Tap to attach control protocol",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    TextButton(
                        onClick = onConnect,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Auto-Detect", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (pairedDevices.isEmpty()) {
                    Text(
                        text = "No bonded Bluetooth devices found. Put earbuds in pairing mode and tap 'Radar Scan'.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                } else {
                    pairedDevices.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.name ?: "Nirvana Space",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = dev.address,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                    color = TextMuted
                                )
                            }

                            val isThisConnecting = deviceState.connectionState == ConnectionState.CONNECTING &&
                                    deviceState.deviceAddress == dev.address

                            Button(
                                onClick = { onSelectDevice(dev) },
                                enabled = !isThisConnecting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    disabledContainerColor = DarkSurfaceHighlight
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isThisConnecting) "Connecting..." else "Connect",
                                    color = if (isThisConnecting) TextSecondary else PureBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = SpecularBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                // Diagnostic Quick Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val logs = AppLog.getAllLogs()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Nirvana Control Logs", logs)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Logs copied (${logs.lines().size} lines)!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpecularBorder),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("📋 Copy Logs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    OutlinedButton(
                        onClick = onOpenLogs,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpecularBorder),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("🔍 View Logs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }
        }

        // Battery Status Card
        BatteryCard(
            leftBattery = deviceState.leftBattery,
            leftCharging = deviceState.leftCharging,
            rightBattery = deviceState.rightBattery,
            rightCharging = deviceState.rightCharging,
            caseBattery = deviceState.caseBattery,
            caseCharging = deviceState.caseCharging
        )

        // Active Noise Control
        AncSelector(
            currentMode = deviceState.ancMode,
            onModeSelected = onSetAncMode
        )

        // BEAST™ Mode Card
        DoubleBezelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BEAST™",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = BoatRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GAMING MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sub-60ms ultra-low latency audio synchronization",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = deviceState.gameMode,
                    onCheckedChange = onSetGameMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = BoatRed,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceHighlight
                    )
                )
            }
        }

        // In-Ear Detection Card
        DoubleBezelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SMART IN-EAR DETECTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Auto-pause playback when either earbud is removed",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Switch(
                    checked = deviceState.inEarDetection,
                    onCheckedChange = onSetInEarDetection,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = ElectricCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceHighlight
                    )
                )
            }
        }
    }
}
