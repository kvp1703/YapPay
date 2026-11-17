package com.yappay.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yappay.app.data.AppSettings
import com.yappay.app.domain.Category
import com.yappay.app.domain.CategoryRef
import com.yappay.app.ui.theme.AtelierTheme
import com.yappay.app.ui.theme.InterFamily
import com.yappay.app.ui.theme.serifStyle
import com.yappay.app.util.MoneyFormatter

// ------------------------------------------------------------------ categories

data class CategoryStyle(
    val icon: ImageVector,
    val light: Color,
    val dark: Color,
)

fun Category.style(): CategoryStyle = when (this) {
    Category.FOOD -> CategoryStyle(Icons.Rounded.Restaurant, Color(0xFFB4623A), Color(0xFFE3A07C))
    Category.GROCERIES -> CategoryStyle(Icons.Rounded.ShoppingCart, Color(0xFF5B7A4C), Color(0xFFA9C79A))
    Category.TRANSPORT -> CategoryStyle(Icons.Rounded.DirectionsCar, Color(0xFF3E5D84), Color(0xFF9DB6D8))
    Category.HEALTH -> CategoryStyle(Icons.Rounded.LocalHospital, Color(0xFFA04236), Color(0xFFE39A8E))
    Category.BILLS -> CategoryStyle(Icons.AutoMirrored.Rounded.ReceiptLong, Color(0xFF9C7A36), Color(0xFFD9BC7E))
    Category.HOUSING -> CategoryStyle(Icons.Rounded.Home, Color(0xFF66558A), Color(0xFFB9AAD9))
    Category.SHOPPING -> CategoryStyle(Icons.Rounded.ShoppingBag, Color(0xFF9E5574), Color(0xFFDFA1BC))
    Category.ENTERTAINMENT -> CategoryStyle(Icons.Rounded.Movie, Color(0xFF77508A), Color(0xFFC6A2D6))
    Category.EDUCATION -> CategoryStyle(Icons.Rounded.School, Color(0xFF3B7479), Color(0xFF92C9CC))
    Category.PERSONAL_CARE -> CategoryStyle(Icons.Rounded.Spa, Color(0xFF6F8148), Color(0xFFBBCC92))
    Category.TRAVEL -> CategoryStyle(Icons.Rounded.Flight, Color(0xFF485C9B), Color(0xFFA6B4E6))
    Category.GIFTS -> CategoryStyle(Icons.Rounded.Redeem, Color(0xFF95623F), Color(0xFFD9AA88))
    Category.OTHER -> CategoryStyle(Icons.Rounded.MoreHoriz, Color(0xFF7A7469), Color(0xFFB3ACA0))
}

/**
 * Built-ins keep the Atelier palette; user-created categories use the colour they were saved with.
 * The lighter variant is derived so it stays readable on both themes.
 */
fun CategoryRef.style(): CategoryStyle = fixed?.style() ?: run {
    val base = Color(color ?: 0xFF7A7469)
    CategoryStyle(
        icon = Icons.AutoMirrored.Rounded.Label,
        light = base,
        dark = lerp(base, Color.White, 0.35f),
    )
}

@Composable
fun categoryAccent(category: CategoryRef, onInk: Boolean = false): Color {
    val c = AtelierTheme.colors
    val style = category.style()
    return when {
        onInk && !c.isDark -> style.dark
        onInk && c.isDark -> style.light
        c.isDark -> style.dark
        else -> style.light
    }
}

@Composable
fun CategoryTile(
    category: CategoryRef,
    size: Dp,
    modifier: Modifier = Modifier,
    voice: Boolean = false,
    onInk: Boolean = false,
) {
    val c = AtelierTheme.colors
    val accent = categoryAccent(category, onInk)
    val background = if (onInk) accent.copy(alpha = 0.22f) else lerp(c.card, accent, 0.11f)
    val border = if (onInk) null else accent.copy(alpha = 0.20f)

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(background)
                .then(if (border != null) Modifier.border(1.dp, border, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = category.style().icon,
                contentDescription = category.label,
                tint = accent,
                modifier = Modifier.size(size * 0.5f),
            )
        }
        if (voice) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 0.dp, bottom = 0.dp)
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(c.ink)
                    .border(2.dp, c.bg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Added by voice",
                    tint = c.bg,
                    modifier = Modifier.size(9.dp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ little atoms

@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AtelierTheme.colors.muted,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        }
        Text(
            text = text.uppercase(),
            color = color,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 1.68.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GoldDiamond(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(5.dp)
            .rotate(45f)
            .background(AtelierTheme.colors.gold),
    )
}

enum class AtelierButtonKind { Primary, Secondary, Ghost, Danger }

@Composable
fun AtelierButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: AtelierButtonKind = AtelierButtonKind.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val c = AtelierTheme.colors
    val shape = CircleShape
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, label = "buttonScale")
    val background = when {
        !enabled -> c.card2
        kind == AtelierButtonKind.Primary -> c.ink
        kind == AtelierButtonKind.Danger -> c.err
        else -> Color.Transparent
    }
    val content = when {
        !enabled -> c.faint
        kind == AtelierButtonKind.Primary -> c.bg
        kind == AtelierButtonKind.Danger -> c.onErr
        kind == AtelierButtonKind.Ghost -> c.ink2
        else -> c.ink
    }

    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(54.dp)
            .clip(shape)
            .background(background)
            .then(
                if (enabled && kind == AtelierButtonKind.Secondary) {
                    Modifier.border(1.dp, c.line2, shape)
                } else {
                    Modifier
                },
            )
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = content,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            letterSpacing = (-0.15).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CircleAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    ghost: Boolean = false,
    enabled: Boolean = true,
    iconTint: Color? = null,
) {
    val c = AtelierTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.93f else 1f, label = "circleScale")
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(size)
            .clip(CircleShape)
            .background(if (ghost) Color.Transparent else c.card)
            .then(if (ghost) Modifier else Modifier.border(1.dp, c.line, CircleShape))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = (iconTint ?: c.ink).let { if (enabled) it else it.copy(alpha = 0.28f) },
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

@Composable
fun DashedPill(text: String, modifier: Modifier = Modifier) {
    val c = AtelierTheme.colors
    Row(
        modifier = modifier
            .height(36.dp)
            .drawBehind {
                drawRoundRect(
                    color = c.line2,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                    ),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                )
            }
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = c.faint,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
        )
    }
}

// ------------------------------------------------------------------ count up

@Composable
fun rememberCountUp(target: Long): Long {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) {
        anim.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        )
    }
    val value = anim.value.toLong()
    return if (target % 100L == 0L) (value / 100L) * 100L else value
}

// ------------------------------------------------------------------ snackbars

@Composable
fun AtelierSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp + bottomPadding, top = 12.dp),
    ) { data ->
        AtelierSnackbarBubble(
            message = data.visuals.message,
            actionLabel = data.visuals.actionLabel,
            onAction = { data.performAction() },
            icon = Icons.Rounded.Info,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AtelierSnackbarBubble(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Check,
) {
    val c = AtelierTheme.colors
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(c.ink)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(c.snackAct),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(16.dp))
        }
        Text(
            text = message,
            color = c.bg,
            fontFamily = InterFamily,
            fontSize = 13.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    color = c.snackAct,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ money

fun money(minor: Long, settings: AppSettings): String =
    MoneyFormatter.format(minor, settings.currencySymbol, settings.grouping)

fun amountNumber(minor: Long, settings: AppSettings): String =
    MoneyFormatter.format(minor, "", settings.grouping)

@Composable
fun AmountText(text: String, modifier: Modifier = Modifier, color: Color = AtelierTheme.colors.ink) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = serifStyle(22f),
        fontStyle = FontStyle.Normal,
    )
}

@Composable
fun HeroBrush(): Brush {
    val c = AtelierTheme.colors
    return Brush.linearGradient(listOf(c.hero, c.heroEnd))
}
