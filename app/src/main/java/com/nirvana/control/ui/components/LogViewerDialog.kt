package com.nirvana.control.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nirvana.control.ui.theme.*
import com.nirvana.control.util.AppLog

@Composable
fun LogViewerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var logsText by remember { mutableStateOf(AppLog.getAllLogs()) }
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    // Auto-scroll to bottom on first appearance
    LaunchedEffect(logsText) {
        verticalScroll.scrollTo(verticalScroll.maxValue)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(22.dp))
                .background(DarkSurface)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(SpecularHighlight, SpecularBorder)),
                    RoundedCornerShape(22.dp)
                )
                .padding(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianCore)
                    .padding(16.dp)
            ) {
                // Hacker Terminal Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Terminal window dots
                        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(RedDanger))
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(AmberWarning))
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(NeonGreen))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DIAGNOSTIC TELEMETRY LOGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = ElectricCyan
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = { logsText = AppLog.getAllLogs() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Refresh", fontSize = 11.sp, color = TextSecondary)
                        }

                        TextButton(
                            onClick = {
                                AppLog.clear()
                                logsText = AppLog.getAllLogs()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp, color = RedDanger)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Terminal Console Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PureBlack)
                        .border(1.dp, SpecularBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                    ) {
                        logsText.lines().forEach { line ->
                            val lineCol = when {
                                line.contains("[E/") || line.contains("failed") -> RedDanger
                                line.contains("[W/") -> AmberWarning
                                line.contains(">>> SUCCESS") -> NeonGreen
                                line.contains("RX RAW") || line.contains("TX RAW") -> ElectricCyan
                                else -> TextSecondary
                            }
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = lineCol
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Nirvana Control Logs", logsText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied ${logsText.lines().size} log events!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("📋 COPY TO CLIPBOARD", color = PureBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.8.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpecularBorder),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Close", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
