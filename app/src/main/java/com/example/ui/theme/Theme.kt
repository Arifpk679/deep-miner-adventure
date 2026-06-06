package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeepMinerColorScheme = darkColorScheme(
    primary = OreGold,
    secondary = OreDiamond,
    tertiary = OreRuby,
    background = CaveDark,
    surface = CaveGrey,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun DeepMinerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DeepMinerColorScheme,
        typography = Typography,
        content = content
    )
}
