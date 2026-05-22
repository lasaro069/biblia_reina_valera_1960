package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrassGold,
    onPrimary = Color.Black,
    primaryContainer = DeepSage,
    onPrimaryContainer = LightSage,
    secondary = SageGreen,
    onSecondary = Color.White,
    secondaryContainer = NightDim,
    background = NightCharcoal,
    onBackground = NightLightText,
    surface = NightDim,
    onSurface = NightLightText,
    surfaceVariant = NightCharcoal,
    onSurfaceVariant = NightMutedText,
    outline = MutedBrown,
    outlineVariant = DividerGold
)

private val LightColorScheme = lightColorScheme(
    primary = GoldenAccent,
    onPrimary = Color.White,
    primaryContainer = LightGold,
    onPrimaryContainer = CharcoalText,
    secondary = SageGreen,
    onSecondary = Color.White,
    secondaryContainer = LightSage,
    onSecondaryContainer = DeepSage,
    background = WarmPaper,
    onBackground = CharcoalText,
    surface = ParchmentWhite,
    onSurface = CharcoalText,
    surfaceVariant = CardParchment,
    onSurfaceVariant = MutedBrown,
    outline = MutedBrown,
    outlineVariant = DividerGold
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
