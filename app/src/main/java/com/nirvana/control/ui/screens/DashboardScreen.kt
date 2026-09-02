package com.nirvana.control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.model.ConnectionState
import com.nirvana.control.model.DeviceState
import com.nirvana.control.ui.components.AncSelector
import com.nirvana.control.ui.components.BatteryCard
import com.nirvana.control.ui.theme.*

@Composable
fun DashboardScreen(
    deviceState: DeviceState,
    onSetAncMode: (com.nirvana.control.model.AncMode) -> Unit,
    onSetGameMode: (Boolean) -> Unit,
    onSetInEarDetection: (Boolean) -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenScanner: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RedDanger.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📡", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Bluetooth is Turned Off",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RedDanger
                            )
                            Text(
                                text = "Enable Bluetooth to connect to Nirvana Space",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    Button(
                        onClick = onEnableBluetooth,
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Turn On", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = deviceState.deviceName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (deviceState.connectionState) {
                                    ConnectionState.CONNECTED -> NeonGreen
                                    ConnectionState.CONNECTING -> AmberWarning
                                    ConnectionState.SCANNING -> ElectricCyan
                                    ConnectionState.DISCONNECTED -> RedDanger
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (deviceState.connectionState) {
                            ConnectionState.CONNECTED -> "Connected"
                            ConnectionState.CONNECTING -> "Connecting..."
                            ConnectionState.SCANNING -> "Scanning..."
                            ConnectionState.DISCONNECTED -> "Disconnected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            if (!deviceState.isConnected) {
                Button(
                    onClick = onOpenScanner,
                    colors = ButtonDefaults.buttonColors(containerColor = BoatRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Scan / Pair", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                OutlinedButton(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Disconnect", fontSize = 12.sp)
                }
            }
        }

        // Tri-Battery Status Card
        BatteryCard(
            leftBattery = deviceState.leftBattery,
            leftCharging = deviceState.leftCharging,
            rightBattery = deviceState.rightBattery,
            rightCharging = deviceState.rightCharging,
            caseBattery = deviceState.caseBattery,
            caseCharging = deviceState.caseCharging
        )

        // ANC Segmented Control
        AncSelector(
            currentMode = deviceState.ancMode,
            onModeSelected = onSetAncMode
        )

        // BEAST Mode (Low Latency Gaming)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎮", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "BEAST™ Gaming Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (deviceState.gameMode) "60ms Ultra-Low Latency active" else "Standard Latency",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (deviceState.gameMode) BoatRed else TextSecondary
                        )
                    }
                }

                Switch(
                    checked = deviceState.gameMode,
                    onCheckedChange = onSetGameMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = BoatRed,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
        }

        // In-Ear Detection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👂", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "In-Ear Detection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (deviceState.inEarDetection) "Auto-pause when removed" else "Disabled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = deviceState.inEarDetection,
                    onCheckedChange = onSetInEarDetection,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = ElectricCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
        }
    }
}
