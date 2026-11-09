package com.gymplanner.app.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gymplanner.app.core.data.ThemeMode

/**
 * A calm, iOS-inspired palette: grouped grey backgrounds, white/charcoal cards,
 * one blue accent, and system green/red/orange for meaning.
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val background: Color,      // grouped background behind cards
    val card: Color,            // cards / list sections
    val cardElevated: Color,    // fields inside cards, sheets
    val separator: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val accent: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val purple: Color,
    val teal: Color,
    val indigo: Color,
    val pink: Color,
    val fill: Color,            // subtle fill for keys, chips, segmented control
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF2F2F7),
    separator = Color(0xFFD1D1D6),
    label = Color(0xFF000000),
    secondaryLabel = Color(0xFF6C6C70),
    tertiaryLabel = Color(0xFFAEAEB2),
    accent = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    orange = Color(0xFFFF9500),
    purple = Color(0xFFAF52DE),
    teal = Color(0xFF30B0C7),
    indigo = Color(0xFF5856D6),
    pink = Color(0xFFFF2D55),
    fill = Color(0x1F787880),
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    cardElevated = Color(0xFF2C2C2E),
    separator = Color(0xFF38383A),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xFF98989F),
    tertiaryLabel = Color(0xFF5A5A5E),
    accent = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    orange = Color(0xFFFF9F0A),
    purple = Color(0xFFBF5AF2),
    teal = Color(0xFF40C8E0),
    indigo = Color(0xFF5E5CE6),
    pink = Color(0xFFFF375F),
    fill = Color(0x3D787880),
)

/** Text styles that follow Apple's type ramp (sizes in sp, so they scale with the system font size). */
@Immutable
data class AppType(
    val largeTitle: TextStyle = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp),
    val title1: TextStyle = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    val title2: TextStyle = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    val title3: TextStyle = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    val headline: TextStyle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    val body: TextStyle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp),
    val callout: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 21.sp),
    val subhead: TextStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
    val footnote: TextStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    val caption: TextStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    val sectionHeader: TextStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.4.sp),
    val bigNumber: TextStyle = TextStyle(fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold),
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }
val LocalAppType = staticCompositionLocalOf { AppType() }

object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
    val type: AppType @Composable get() = LocalAppType.current
}

@Composable
fun GymTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val c = if (dark) DarkAppColors else LightAppColors
    // Material components (switches, sheets, dialogs, text fields) pick up the same palette.
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = Color.White, secondary = c.accent,
            background = c.background, onBackground = c.label,
            surface = c.card, onSurface = c.label, surfaceVariant = c.cardElevated, onSurfaceVariant = c.secondaryLabel,
            surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerHighest = c.cardElevated,
            surfaceContainerLow = c.card, surfaceContainerLowest = c.background,
            outline = c.separator, outlineVariant = c.separator, error = c.red,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = Color.White, secondary = c.accent,
            background = c.background, onBackground = c.label,
            surface = c.card, onSurface = c.label, surfaceVariant = c.cardElevated, onSurfaceVariant = c.secondaryLabel,
            surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerHighest = c.cardElevated,
            surfaceContainerLow = c.background, surfaceContainerLowest = c.card,
            outline = c.separator, outlineVariant = c.separator, error = c.red,
        )
    }
    val t = AppType()
    val typography = Typography(
        displayLarge = t.bigNumber, headlineLarge = t.largeTitle, headlineMedium = t.title1, headlineSmall = t.title2,
        titleLarge = t.title3, titleMedium = t.headline, titleSmall = t.subhead,
        bodyLarge = t.body, bodyMedium = t.subhead, bodySmall = t.footnote,
        labelLarge = t.headline, labelMedium = t.footnote, labelSmall = t.caption,
    )
    CompositionLocalProvider(LocalAppColors provides c, LocalAppType provides t) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
