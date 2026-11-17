package com.yappay.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.yappay.app.R

/**
 * ATELIER design tokens (Ivory by day, Onyx by night).
 * These names mirror the CSS custom properties used by the v4 design.
 */
@Immutable
data class AtelierColors(
    val isDark: Boolean,
    val bg: Color,
    val card: Color,
    val card2: Color,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val line2: Color,
    val gold: Color,
    val gold2: Color,
    val goldSoft: Color,
    val goldInk: Color,
    val err: Color,
    val errSoft: Color,
    val errInk: Color,
    val onErr: Color,
    val hero: Color,
    val heroEnd: Color,
    val onHero: Color,
    val heroMuted: Color,
    val heroLine: Color,
    val mic: Color,
    val micEnd: Color,
    val onMic: Color,
    val glass: Color,
    val scrim: Color,
    val kb: Color,
    val key: Color,
    val keySh: Color,
    val snackAct: Color,
)

val IvoryAtelier = AtelierColors(
    isDark = false,
    bg = Color(0xFFF5F2EC),
    card = Color(0xFFFFFFFF),
    card2 = Color(0xFFECE7DE),
    ink = Color(0xFF16140F),
    ink2 = Color(0xFF3D3830),
    muted = Color(0xFF7D776C),
    faint = Color(0xFFB1AA9D),
    line = Color(0xFFE7E1D6),
    line2 = Color(0xFFD3CCBF),
    gold = Color(0xFF97773F),
    gold2 = Color(0xFFC9A96A),
    goldSoft = Color(0xFFF2EADB),
    goldInk = Color(0xFF6E5427),
    err = Color(0xFF9E3B2E),
    errSoft = Color(0xFFF6E6E1),
    errInk = Color(0xFF7A2C21),
    onErr = Color(0xFFFFFFFF),
    hero = Color(0xFF16140F),
    heroEnd = Color(0xFF16140F),
    onHero = Color(0xFFF5F2EC),
    heroMuted = Color(0xFFF5F2EC).copy(alpha = 0.56f),
    heroLine = Color(0xFFF5F2EC).copy(alpha = 0.10f),
    mic = Color(0xFF16140F),
    micEnd = Color(0xFF16140F),
    onMic = Color(0xFFF5F2EC),
    glass = Color(0xFFFFFFFF).copy(alpha = 0.72f),
    scrim = Color(0xFF16140F).copy(alpha = 0.30f),
    kb = Color(0xFFECE7DE),
    key = Color(0xFFFFFFFF),
    keySh = Color(0xFF16140F).copy(alpha = 0.10f),
    snackAct = Color(0xFFD9BC85),
)

val OnyxAtelier = AtelierColors(
    isDark = true,
    bg = Color(0xFF0C0B0A),
    card = Color(0xFF171614),
    card2 = Color(0xFF22201C),
    ink = Color(0xFFF3EFE8),
    ink2 = Color(0xFFD3CDC3),
    muted = Color(0xFF9A9389),
    faint = Color(0xFF625D55),
    line = Color(0xFF282521),
    line2 = Color(0xFF3A3631),
    gold = Color(0xFFD2B47C),
    gold2 = Color(0xFFC7A76B),
    goldSoft = Color(0xFF221D15),
    goldInk = Color(0xFFE3CC9E),
    err = Color(0xFFE3907F),
    errSoft = Color(0xFF2A1814),
    errInk = Color(0xFFF0B3A6),
    onErr = Color(0xFF1A0D0A),
    hero = Color(0xFF25221D),
    heroEnd = Color(0xFF141311),
    onHero = Color(0xFFF3EFE8),
    heroMuted = Color(0xFFF3EFE8).copy(alpha = 0.55f),
    heroLine = Color(0xFFD2B47C).copy(alpha = 0.20f),
    mic = Color(0xFFE8D09C),
    micEnd = Color(0xFFB8945A),
    onMic = Color(0xFF16140F),
    glass = Color(0xFF1C1A17).copy(alpha = 0.72f),
    scrim = Color(0xFF000000).copy(alpha = 0.55f),
    kb = Color(0xFF141311),
    key = Color(0xFF22201C),
    keySh = Color(0xFF000000).copy(alpha = 0.50f),
    snackAct = Color(0xFF8A6A33),
)

val LocalAtelierColors = staticCompositionLocalOf { IvoryAtelier }

object AtelierTheme {
    val colors: AtelierColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAtelierColors.current
}

val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

val InterFamily = FontFamily(
    Font(R.font.inter_light, FontWeight.Light, FontStyle.Normal),
    Font(R.font.inter_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.inter_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.inter_bold, FontWeight.Bold, FontStyle.Normal),
)

fun serifStyle(
    size: Float,
    lineHeight: Float = size * 1.05f,
    italic: Boolean = false,
    letterSpacing: Float = -0.012f,
): TextStyle = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.em,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

fun interStyle(
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Float = size * 1.35f,
    letterSpacing: Float = -0.006f,
): TextStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.em,
)

private val AtelierTypography = Typography(
    displayLarge = serifStyle(56f, 56f, letterSpacing = -0.02f),
    displayMedium = serifStyle(44f, 44f, letterSpacing = -0.02f),
    displaySmall = serifStyle(30f, 33f),
    headlineLarge = serifStyle(30f, 33f),
    headlineMedium = serifStyle(24f, 27f),
    headlineSmall = serifStyle(22f, 24f),
    titleLarge = interStyle(15f, FontWeight.Medium),
    titleMedium = interStyle(14.5f, FontWeight.Medium),
    titleSmall = interStyle(13f, FontWeight.Medium),
    bodyLarge = interStyle(15f),
    bodyMedium = interStyle(14f),
    bodySmall = interStyle(12.5f),
    labelLarge = interStyle(13f, FontWeight.Medium),
    labelMedium = interStyle(11f, FontWeight.Medium, letterSpacing = 0.04f),
    labelSmall = interStyle(10.5f, FontWeight.Medium, letterSpacing = 0.16f),
)

@Composable
fun YapPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (darkTheme) OnyxAtelier else IvoryAtelier
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.gold,
            onPrimary = c.bg,
            secondary = c.gold2,
            onSecondary = c.ink,
            tertiary = c.err,
            background = c.bg,
            onBackground = c.ink,
            surface = c.card,
            onSurface = c.ink,
            surfaceVariant = c.card2,
            onSurfaceVariant = c.muted,
            outline = c.line2,
            outlineVariant = c.line,
            error = c.err,
            onError = c.onErr,
        )
    } else {
        lightColorScheme(
            primary = c.gold,
            onPrimary = c.bg,
            secondary = c.gold2,
            onSecondary = c.ink,
            tertiary = c.err,
            background = c.bg,
            onBackground = c.ink,
            surface = c.card,
            onSurface = c.ink,
            surfaceVariant = c.card2,
            onSurfaceVariant = c.muted,
            outline = c.line2,
            outlineVariant = c.line,
            error = c.err,
            onError = c.onErr,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAtelierColors provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AtelierTypography,
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
