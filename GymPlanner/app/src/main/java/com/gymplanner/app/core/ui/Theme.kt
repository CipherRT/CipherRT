package com.gymplanner.app.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gymplanner.app.R

/** The accent colours a person can pick in Settings. Lime is the default. */
enum class Accent(val label: String, val color: Color) {
    LIME("Lime", Color(0xFFC6FF3D)),
    ORANGE("Orange", Color(0xFFFF6B2C)),
    CYAN("Cyan", Color(0xFF3DD9FF)),
    PURPLE("Purple", Color(0xFFB18CFF));

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: LIME
    }
}

/**
 * Dark design tokens from the v2 mockups. The single [accent] is only used for
 * primary buttons, completed sets, the active tab, the streak and PRs.
 */
@Immutable
data class AppColors(
    val accent: Color,
    val onAccent: Color = Color(0xFF0E0F11),
    val background: Color = Color(0xFF0E0F11),
    val card: Color = Color(0xFF1A1C20),
    val border: Color = Color(0xFF2C2F35),
    /** Raised / active row. */
    val cardElevated: Color = Color(0xFF22252A),
    val bottomBar: Color = Color(0xFF141518),
    val separator: Color = Color(0xFF24272C),
    val label: Color = Color(0xFFF4F5F6),
    val secondaryLabel: Color = Color(0xFFA1A7B0),
    val tertiaryLabel: Color = Color(0xFF6B7079),
    /** Neutral chip / icon tile background and its text. */
    val chip: Color = Color(0xFF24272C),
    val chipText: Color = Color(0xFFD3D7DD),
    /** −/+ buttons, unselected keys, outlines of empty ticks. */
    val fill: Color = Color(0xFF2C2F35),
    val outline: Color = Color(0xFF3A3E45),
    val cardio: Color = Color(0xFF6FC3FF),
    /** Drop sets and warnings. */
    val warning: Color = Color(0xFFFFB86B),
    val warningCard: Color = Color(0xFF1F1A14),
    val warningBorder: Color = Color(0xFF3A2E1F),
    val red: Color = Color(0xFFFF6B6B),
) {
    val isDark: Boolean get() = true
    // Older names kept so every screen reads one palette.
    val green: Color get() = accent
    val orange: Color get() = warning
    val purple: Color get() = chipText
    val teal: Color get() = chipText
    val indigo: Color get() = chipText
    val pink: Color get() = chipText
}

@OptIn(ExperimentalTextApi::class)
val InterFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

@OptIn(ExperimentalTextApi::class)
val GroteskFamily = FontFamily(
    Font(R.font.space_grotesk_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.space_grotesk_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.space_grotesk_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Space Grotesk for headings and big numbers, Inter for everything else. Sizes in sp. */
@Immutable
data class AppType(
    val largeTitle: TextStyle = TextStyle(fontFamily = GroteskFamily, fontSize = 40.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    val title1: TextStyle = TextStyle(fontFamily = GroteskFamily, fontSize = 28.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold),
    val title2: TextStyle = TextStyle(fontFamily = GroteskFamily, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold),
    val title3: TextStyle = TextStyle(fontFamily = InterFamily, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    val headline: TextStyle = TextStyle(fontFamily = InterFamily, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    val body: TextStyle = TextStyle(fontFamily = InterFamily, fontSize = 16.sp, lineHeight = 22.sp),
    val callout: TextStyle = TextStyle(fontFamily = InterFamily, fontSize = 15.sp, lineHeight = 20.sp),
    val subhead: TextStyle = TextStyle(fontFamily = InterFamily, fontSize = 14.sp, lineHeight = 19.sp),
    val footnote: TextStyle = TextStyle(fontFamily = InterFamily, fontSize = 13.sp, lineHeight = 18.sp),
    val caption: TextStyle = TextStyle(fontFamily = InterFamily, fontSize = 12.sp, lineHeight = 16.sp),
    val sectionHeader: TextStyle = TextStyle(fontFamily = InterFamily, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em),
    val number: TextStyle = TextStyle(fontFamily = GroteskFamily, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    val bigNumber: TextStyle = TextStyle(fontFamily = GroteskFamily, fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
)

val LocalAppColors = staticCompositionLocalOf { AppColors(accent = Accent.LIME.color) }
val LocalAppType = staticCompositionLocalOf { AppType() }

object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
    val type: AppType @Composable get() = LocalAppType.current
}

/** Dark only (a light theme can come later). */
@Composable
fun GymTheme(accent: Accent, content: @Composable () -> Unit) {
    val c = AppColors(accent = accent.color)
    val scheme = darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, secondary = c.accent, onSecondary = c.onAccent,
        background = c.background, onBackground = c.label,
        surface = c.card, onSurface = c.label, surfaceVariant = c.cardElevated, onSurfaceVariant = c.secondaryLabel,
        surfaceContainer = c.card, surfaceContainerHigh = c.cardElevated, surfaceContainerHighest = c.cardElevated,
        surfaceContainerLow = c.card, surfaceContainerLowest = c.background,
        outline = c.border, outlineVariant = c.separator, error = c.red,
        primaryContainer = c.cardElevated, onPrimaryContainer = c.label,
        secondaryContainer = c.chip, onSecondaryContainer = c.label,
    )
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
