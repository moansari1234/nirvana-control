package com.nirvana.control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirvana.control.model.KeyFunction
import com.nirvana.control.model.TouchGesture
import com.nirvana.control.ui.components.DoubleBezelCard
import com.nirvana.control.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
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
                text = "HAPTIC SURFACE MAPPING",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = BoatRed
            )
            Text(
                text = "Touch Gesture Studio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Program optical capacitive sensors per earbud",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // Accidental-Touch Guard Card
        DoubleBezelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACCIDENTAL TOUCH GUARD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Disables single-tap to eliminate inadvertent triggers when adjusting buds",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Switch(
                    checked = accidentalTouchGuard,
                    onCheckedChange = onSetAccidentalTouchGuard,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = ElectricCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceHighlight
                    )
                )
            }
        }

        // Left / Right Earbud Selector Track
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, SpecularBorder, RoundedCornerShape(14.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selectedEarbudIsLeft) BoatRed.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (selectedEarbudIsLeft) BoatRed else Color.Transparent,
                        RoundedCornerShape(11.dp)
                    )
                    .clickable { selectedEarbudIsLeft = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LEFT EARBUD",
                    fontSize = 11.sp,
                    fontWeight = if (selectedEarbudIsLeft) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    color = if (selectedEarbudIsLeft) TextPrimary else TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (!selectedEarbudIsLeft) BoatRed.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (!selectedEarbudIsLeft) BoatRed else Color.Transparent,
                        RoundedCornerShape(11.dp)
                    )
                    .clickable { selectedEarbudIsLeft = false }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RIGHT EARBUD",
                    fontSize = 11.sp,
                    fontWeight = if (!selectedEarbudIsLeft) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    color = if (!selectedEarbudIsLeft) TextPrimary else TextSecondary
                )
            }
        }

        // Gestures List Card
        val gesturesForSelectedBud = if (selectedEarbudIsLeft) {
            listOf(
                TouchGesture.LEFT_SINGLE_TAP to "Single Tap",
                TouchGesture.LEFT_DOUBLE_TAP to "Double Tap",
                TouchGesture.LEFT_TRIPLE_TAP to "Triple Tap",
                TouchGesture.LEFT_LONG_PRESS to "Long Press & Hold"
            )
        } else {
            listOf(
                TouchGesture.RIGHT_SINGLE_TAP to "Single Tap",
                TouchGesture.RIGHT_DOUBLE_TAP to "Double Tap",
                TouchGesture.RIGHT_TRIPLE_TAP to "Triple Tap",
                TouchGesture.RIGHT_LONG_PRESS to "Long Press & Hold"
            )
        }

        DoubleBezelCard {
            Text(
                text = if (selectedEarbudIsLeft) "LEFT BUD ASSIGNMENTS" else "RIGHT BUD ASSIGNMENTS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                gesturesForSelectedBud.forEach { (gesture, label) ->
                    val currentFunction = keyMappings[gesture] ?: KeyFunction.NONE
                    val isSingleTapDisabled = (gesture == TouchGesture.LEFT_SINGLE_TAP || gesture == TouchGesture.RIGHT_SINGLE_TAP) && accidentalTouchGuard

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, SpecularBorder, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSingleTapDisabled) { activeGestureToEdit = gesture }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSingleTapDisabled) TextMuted else TextPrimary
                            )
                            if (isSingleTapDisabled) {
                                Text(
                                    text = "Locked by Touch Guard",
                                    fontSize = 10.sp,
                                    color = AmberWarning
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSingleTapDisabled) DarkSurfaceHighlight else BoatRed.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (isSingleTapDisabled) SpecularBorder else BoatRed.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isSingleTapDisabled) "DISABLED" else currentFunction.label.uppercase(),
                                color = if (isSingleTapDisabled) TextMuted else BoatRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for selecting function
    if (activeGestureToEdit != null) {
        val gesture = activeGestureToEdit!!
        ModalBottomSheet(
            onDismissRequest = { activeGestureToEdit = null },
            containerColor = DarkSurface,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SELECT ACTION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = BoatRed
                )
                Text(
                    text = gesture.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                KeyFunction.entries.forEach { func ->
                    val isCurrent = keyMappings[gesture] == func
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) BoatRed.copy(alpha = 0.18f) else DarkSurfaceVariant)
                            .border(1.dp, if (isCurrent) BoatRed else SpecularBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                onSetGesture(gesture, func)
                                activeGestureToEdit = null
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = func.label,
                            fontSize = 13.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) TextPrimary else TextSecondary
                        )

                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(BoatRed)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
