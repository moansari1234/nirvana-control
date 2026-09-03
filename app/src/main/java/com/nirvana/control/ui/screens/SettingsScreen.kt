package com.nirvana.control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.nirvana.control.ui.components.DoubleBezelCard
import com.nirvana.control.ui.theme.*

@Composable
fun SettingsScreen(
    deviceState: DeviceState,
    onSetAutoConnect: (Boolean) -> Unit,
    onSetInEarDetection: (Boolean) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenLogs: () -> Unit,
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
                text = "SYSTEM ARCHITECTURE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = BoatRed
            )
            Text(
                text = "Settings & Device Info",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Preferences, hardware telemetry, and diagnostic logs",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // Auto-Wake & Background Control Card
        DoubleBezelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AUTO-CONNECT ON AUDIO LINK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (deviceState.autoConnectEnabled)
                            "Wakes app and establishes SPP telemetry when earbuds connect to phone"
                        else
                            "App remains dormant (0 MB RAM, 0% CPU) until opened",
                        fontSize = 11.sp,
                        color = if (deviceState.autoConnectEnabled) TextSecondary else AmberWarning
                    )
                }

                Switch(
                    checked = deviceState.autoConnectEnabled,
                    onCheckedChange = onSetAutoConnect,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = ElectricCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceHighlight
                    )
                )
            }
        }

        // Hardware & Firmware Telemetry Card
        DoubleBezelCard {
            Text(
                text = "HARDWARE & FIRMWARE TELEMETRY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryRow("Target Device", deviceState.deviceName)
                TelemetryRow("MAC Address", if (deviceState.deviceAddress.isNotEmpty()) deviceState.deviceAddress else "Not Connected")
                TelemetryRow("Audio SoC", "Bluetrum BT5.3 (Dual-Mode)")
                TelemetryRow("Control Protocol", "Bluetrum SPP RFCOMM (Insecure)")
                TelemetryRow("App Version", "v1.0.0 (Release Build)")
            }
        }

        // Diagnostic Logging & Tools Card
        DoubleBezelCard {
            Text(
                text = "DIAGNOSTIC TELEMETRY LOGS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Circular 1,000-event hardware communication buffer for instant debug paste",
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenLogs,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpecularBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🔍 Open Terminal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Button(
                    onClick = onRefreshInfo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpecularBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🔄 Sync Telemetry", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ElectricCyan)
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
