package com.nirvana.control.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nirvana.control.ui.theme.DarkSurface
import com.nirvana.control.ui.theme.ObsidianCore
import com.nirvana.control.ui.theme.SpecularBorder

@Composable
fun DoubleBezelCard(
    modifier: Modifier = Modifier,
    outerRadius: Dp = 22.dp,
    innerRadius: Dp = 18.dp,
    borderColor: Color = SpecularBorder,
    innerPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(outerRadius))
            .background(DarkSurface)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(borderColor, Color(0x08FFFFFF))
                ),
                shape = RoundedCornerShape(outerRadius)
            )
            .padding(1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(innerRadius))
                .background(ObsidianCore)
                .padding(innerPadding),
            content = content
        )
    }
}
