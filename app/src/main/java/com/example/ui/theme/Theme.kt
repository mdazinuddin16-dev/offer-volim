package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class OfferSemanticPalette(
    val savingsAccent: Color,
    val savingsContainer: Color,
    val onSavingsContainer: Color,
    val discountBadge: Color,
    val discountBadgeContainer: Color,
    val onDiscountBadgeContainer: Color,
    val flashAmber: Color,
    val flashAmberContainer: Color,
    val cardBorder: Color
)

val LocalOfferSemanticPalette = staticCompositionLocalOf {
    OfferSemanticPalette(
        savingsAccent = EmeraldSecondary,
        savingsContainer = EmeraldContainer,
        onSavingsContainer = OnEmeraldContainer,
        discountBadge = CoralPrimary,
        discountBadgeContainer = CoralContainer,
        onDiscountBadgeContainer = OnCoralContainer,
        flashAmber = AmberTertiary,
        flashAmberContainer = AmberContainer,
        cardBorder = SubtleOutline
    )
}

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = CoralContainer,
    onPrimaryContainer = OnCoralContainer,
    secondary = EmeraldSecondary,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = OnEmeraldContainer,
    tertiary = AmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = AlabasterBg,
    onBackground = EspressoInk,
    surface = WarmPaperSurface,
    onSurface = EspressoInk,
    surfaceVariant = StoneSurfaceVariant,
    onSurfaceVariant = MutedStoneInk,
    outline = SubtleOutline,
    outlineVariant = SubtleOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralDarkPrimary,
    onPrimary = Color(0xFF280600),
    primaryContainer = CoralDarkContainer,
    onPrimaryContainer = OnCoralDarkContainer,
    secondary = EmeraldDarkSecondary,
    onSecondary = Color(0xFF00291B),
    secondaryContainer = EmeraldDarkContainer,
    onSecondaryContainer = OnEmeraldDarkContainer,
    tertiary = AmberDarkTertiary,
    onTertiary = Color(0xFF2C1800),
    tertiaryContainer = AmberDarkContainer,
    onTertiaryContainer = OnAmberDarkContainer,
    background = ObsidianBg,
    onBackground = WarmIvoryInk,
    surface = SlateSurface,
    onSurface = WarmIvoryInk,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = MutedAshInk,
    outline = DarkOutline,
    outlineVariant = DarkOutline
)

private val OfferVaultShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OfferVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticPalette = if (darkTheme) {
        OfferSemanticPalette(
            savingsAccent = EmeraldDarkSecondary,
            savingsContainer = EmeraldDarkContainer,
            onSavingsContainer = OnEmeraldDarkContainer,
            discountBadge = CoralDarkPrimary,
            discountBadgeContainer = CoralDarkContainer,
            onDiscountBadgeContainer = OnCoralDarkContainer,
            flashAmber = AmberDarkTertiary,
            flashAmberContainer = AmberDarkContainer,
            cardBorder = DarkOutline
        )
    } else {
        OfferSemanticPalette(
            savingsAccent = EmeraldSecondary,
            savingsContainer = EmeraldContainer,
            onSavingsContainer = OnEmeraldContainer,
            discountBadge = CoralPrimary,
            discountBadgeContainer = CoralContainer,
            onDiscountBadgeContainer = OnCoralContainer,
            flashAmber = AmberTertiary,
            flashAmberContainer = AmberContainer,
            cardBorder = SubtleOutline
        )
    }

    CompositionLocalProvider(LocalOfferSemanticPalette provides semanticPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OfferVaultTypography,
            shapes = OfferVaultShapes,
            content = content
        )
    }
}
