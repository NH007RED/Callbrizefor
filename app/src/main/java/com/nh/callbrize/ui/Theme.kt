package com.nh.callbrize.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF0A1330)
val MidNight = Color(0xFF111C44)
val Panel = Color(0xFF16245A)
val Gold = Color(0xFFE0B341)
val GoldLight = Color(0xFFF6D77A)
val GoldDim = Color(0xFF8C7430)
val Felt = Color(0xFF0B6B45)
val FeltLight = Color(0xFF138A5A)
val FeltDark = Color(0xFF084D32)
val CardWhite = Color(0xFFFAFAFA)
val CardRed = Color(0xFFD32F2F)
val CardBlack = Color(0xFF14181F)
val SoftText = Color(0xFFD0D6EE)

val BgBrush = Brush.verticalGradient(listOf(Color(0xFF0E1B4D), Navy, Color(0xFF050A1F)))

@Composable
fun CallbrizeTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Gold,
        onPrimary = Navy,
        secondary = Felt,
        background = Navy,
        onBackground = Color.White,
        surface = MidNight,
        onSurface = Color.White,
        surfaceVariant = Panel,
        onSurfaceVariant = SoftText,
        outline = GoldDim
    )
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

val GoldBrush = Brush.horizontalGradient(listOf(Color(0xFFF6D77A), Color(0xFFE0B341), Color(0xFFB88A24)))
val GoldRim = Brush.linearGradient(
    listOf(Color(0xFFF6D77A), Color(0xFF8C6A1E), Color(0xFFE0B341), Color(0xFF6B5116), Color(0xFFF6D77A))
)
val GlassBrush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF)))
val PanelBrush = Brush.verticalGradient(listOf(Color(0xE61B2A66), Color(0xE60F1A45)))
