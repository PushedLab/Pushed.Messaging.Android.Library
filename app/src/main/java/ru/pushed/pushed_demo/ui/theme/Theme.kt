package ru.pushed.pushed_demo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgColor     = Color(0xFF0A0A0F)
val CardBg      = Color(0x0AFFFFFF)
val CardBorder  = Color(0x12FFFFFF)
val Coral       = Color(0xFFF05C52)
val CoralDark   = Color(0xFFD94840)
val GreenActive = Color(0xFF22C55E)
val RedOffline  = Color(0xFFEF4444)

@Composable
fun PushedDemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = BgColor, surface = BgColor, primary = Coral),
        content     = content
    )
}
