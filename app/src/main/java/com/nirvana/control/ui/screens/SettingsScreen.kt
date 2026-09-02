package com.nirvana.control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.model.DeviceState
import com.nirvana.control.ui.theme.*

@Composable
fun SettingsScreen(
    deviceState: DeviceState,
    onSetAutoConnect: (Boolean) -> Unit,
    onSetInEarDetection: (Boolean) -> Unit,
    onOpenScanner: () -> Unit,
    onRefreshInfo: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Preferences, Bluetooth background wake, and device information",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Auto-Wake & Background Control Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Background & Auto-Wake",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Connect on Bluetooth Pair",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (deviceState.autoConnectEnabled)
                                "Wakes app and syncs battery when earbuds connect to phone"
                            else
                                "App stays completely dormant (0 MB RAM, 0% CPU) until opened",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (deviceState.autoConnectEnabled) TextSecondary else AmberWarning
                        )
                    }

                    Switch(
                        checked = deviceState.autoConnectEnabled,
                        onCheckedChange = onSetAutoConnect,
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

        // Device Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Device Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onOpenScanner() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bluetooth Radar Scanner", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Discover and pair nearby Nirvana Space earbuds", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Text("📡", fontSize = 20.sp)
                }

                if (deviceState.isConnected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { onRefreshInfo() }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sync Hardware Status", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Query battery levels and ANC registers", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Text("🔄", fontSize = 20.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { onDisconnect() }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Disconnect", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = RedDanger)
                            Text("Close active RFCOMM Bluetooth socket", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Text("⏏️", fontSize = 20.sp)
                    }
                }
            }
        }

        // Hardware & About Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Hardware & Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target Device", color = TextSecondary, fontSize = 13.sp)
                    Text(deviceState.deviceName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                if (deviceState.deviceAddress.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MAC Address", color = TextSecondary, fontSize = 13.sp)
                        Text(deviceState.deviceAddress, fontSize = 13.sp, color = TextSecondary)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Chipset Platform", color = TextSecondary, fontSize = 13.sp)
                    Text("Bluetrum AB Series (BT 5.3)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Transport Layer", color = TextSecondary, fontSize = 13.sp)
                    Text("RFCOMM SPP (5-Byte Framing)", fontSize = 13.sp, color = TextSecondary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Privacy Mode", color = TextSecondary, fontSize = 13.sp)
                    Text("100% Offline (Zero Trackers)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Telemetry & Logs", color = TextSecondary, fontSize = 13.sp)
                    Text("Zero Log Collection (Disabled)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("App Footprint", color = TextSecondary, fontSize = 13.sp)
                    Text("< 4 MB (Replaces 162MB App)", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
