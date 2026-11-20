package com.lvviagem.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Lv {
    val Bg = Color(0xFF0E1A24)
    val Surface = Color(0xFF16283A)
    val Accent = Color(0xFFF2B544)
    val Text = Color(0xFFEAF1F6)
    val Muted = Color(0xFF8FA6B8)
}

@Composable
fun LvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lv.Accent,
            onPrimary = Color(0xFF1A1200),
            background = Lv.Bg,
            onBackground = Lv.Text,
            surface = Lv.Surface,
            onSurface = Lv.Text,
            secondaryContainer = Color(0xFF24405A),
            onSecondaryContainer = Lv.Text,
        ),
        content = content,
    )
}
