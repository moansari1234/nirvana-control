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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.model.KeyFunction
import com.nirvana.control.model.TouchGesture
import com.nirvana.control.ui.theme.*

@Composable
fun GesturesScreen(
    keyMappings: Map<TouchGesture, KeyFunction>,
    accidentalTouchGuard: Boolean,
    onSetGesture: (TouchGesture, KeyFunction) -> Unit,
    onSetAccidentalTouchGuard: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEarbudIsLeft by remember { mutableStateOf(true) }
    var activeGestureToEdit by remember { mutableStateOf<TouchGesture?>(null) }

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
                text = "Touch Gesture Controls",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Customize touch sensor actions independently for each earbud",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Accidental-Touch Guard Card
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
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🛡️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Accidental-Touch Guard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Disables single-tap to prevent unintended taps when adjusting earbuds",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = accidentalTouchGuard,
                    onCheckedChange = onSetAccidentalTouchGuard,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = ElectricCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
        }

        // Left / Right Earbud Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedEarbudIsLeft) BoatRed else Color.Transparent)
                    .clickable { selectedEarbudIsLeft = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Left Earbud",
                    fontSize = 13.sp,
                    fontWeight = if (selectedEarbudIsLeft) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedEarbudIsLeft) PureBlack else TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!selectedEarbudIsLeft) BoatRed else Color.Transparent)
                    .clickable { selectedEarbudIsLeft = false }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Right Earbud",
                    fontSize = 13.sp,
                    fontWeight = if (!selectedEarbudIsLeft) FontWeight.Bold else FontWeight.Medium,
                    color = if (!selectedEarbudIsLeft) PureBlack else TextSecondary
                )
            }
        }

        // Gestures List for selected earbud
        val gesturesForSelectedBud = if (selectedEarbudIsLeft) {
            listOf(
                TouchGesture.LEFT_SINGLE_TAP to "Single Tap",
                TouchGesture.LEFT_DOUBLE_TAP to "Double Tap",
                TouchGesture.LEFT_TRIPLE_TAP to "Triple Tap",
                TouchGesture.LEFT_LONG_PRESS to "Long Press / Hold"
            )
        } else {
            listOf(
                TouchGesture.RIGHT_SINGLE_TAP to "Single Tap",
                TouchGesture.RIGHT_DOUBLE_TAP to "Double Tap",
                TouchGesture.RIGHT_TRIPLE_TAP to "Triple Tap",
                TouchGesture.RIGHT_LONG_PRESS to "Long Press / Hold"
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                gesturesForSelectedBud.forEach { (gesture, label) ->
                    val currentFunction = keyMappings[gesture] ?: KeyFunction.NONE
                    val isSingleTapDisabled = (gesture == TouchGesture.LEFT_SINGLE_TAP || gesture == TouchGesture.RIGHT_SINGLE_TAP) && accidentalTouchGuard

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .clickable(enabled = !isSingleTapDisabled) { activeGestureToEdit = gesture }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSingleTapDisabled) TextMuted else TextPrimary
                            )
                            if (isSingleTapDisabled) {
                                Text(
                                    text = "Disabled by Accidental-Touch Guard",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AmberWarning
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSingleTapDisabled) DarkSurfaceHighlight else BoatRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isSingleTapDisabled) "Disabled" else currentFunction.label,
                                color = if (isSingleTapDisabled) TextMuted else BoatRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Preset Templates Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Gesture Profiles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Apply recommended button assignments for all gestures",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            onSetGesture(TouchGesture.LEFT_SINGLE_TAP, KeyFunction.PLAY_PAUSE)
                            onSetGesture(TouchGesture.RIGHT_SINGLE_TAP, KeyFunction.PLAY_PAUSE)
                            onSetGesture(TouchGesture.LEFT_DOUBLE_TAP, KeyFunction.PREVIOUS_TRACK)
                            onSetGesture(TouchGesture.RIGHT_DOUBLE_TAP, KeyFunction.NEXT_TRACK)
                            onSetGesture(TouchGesture.LEFT_TRIPLE_TAP, KeyFunction.VOLUME_DOWN)
                            onSetGesture(TouchGesture.RIGHT_TRIPLE_TAP, KeyFunction.VOLUME_UP)
                            onSetGesture(TouchGesture.LEFT_LONG_PRESS, KeyFunction.ANC_MODE)
                            onSetGesture(TouchGesture.RIGHT_LONG_PRESS, KeyFunction.ANC_MODE)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Media Profile", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onSetGesture(TouchGesture.LEFT_DOUBLE_TAP, KeyFunction.ANC_MODE)
                            onSetGesture(TouchGesture.RIGHT_DOUBLE_TAP, KeyFunction.BEAST_MODE)
                            onSetGesture(TouchGesture.LEFT_TRIPLE_TAP, KeyFunction.VOICE_ASSISTANT)
                            onSetGesture(TouchGesture.RIGHT_TRIPLE_TAP, KeyFunction.SPATIAL_AUDIO)
                            onSetGesture(TouchGesture.LEFT_LONG_PRESS, KeyFunction.ANC_MODE)
                            onSetGesture(TouchGesture.RIGHT_LONG_PRESS, KeyFunction.ANC_MODE)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gamer Profile", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // Action Picker Dialog
    activeGestureToEdit?.let { gesture ->
        AlertDialog(
            onDismissRequest = { activeGestureToEdit = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Select Action for ",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    KeyFunction.entries.forEach { func ->
                        val isSelected = keyMappings[gesture] == func
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BoatRed.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    onSetGesture(gesture, func)
                                    activeGestureToEdit = null
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = func.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) BoatRed else TextPrimary
                            )
                            if (isSelected) {
                                Text("✓", color = BoatRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { activeGestureToEdit = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}
