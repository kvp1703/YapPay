package com.yappay.app.ui.summary

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yappay.app.data.AppSettings
import com.yappay.app.domain.CategoryRef
import com.yappay.app.ui.components.CategoryTile
import com.yappay.app.ui.components.CircleAction
import com.yappay.app.ui.components.amountNumber
import com.yappay.app.ui.components.money
import com.yappay.app.ui.components.rememberCountUp
import com.yappay.app.ui.components.style
import com.yappay.app.ui.home.MonthSelector
import com.yappay.app.ui.theme.AtelierTheme
import com.yappay.app.ui.theme.InterFamily
import com.yappay.app.ui.theme.serifStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(onBack: () -> Unit) {
    val vm: SummaryViewModel = viewModel(factory = SummaryViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val c = AtelierTheme.colors

    Scaffold(containerColor = c.bg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircleAction(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                )
                Text(
                    text = "Summary",
                    color = c.ink,
                    style = serifStyle(27f, 27f),
                    modifier = Modifier.weight(1f),
                )
            }

            MonthSelector(
                month = state.month,
                isCurrentMonth = state.isCurrentMonth,
                onPrevious = vm::previousMonth,
                onNext = vm::nextMonth,
                modifier = Modifier.padding(top = 2.dp),
            )

            SummaryHero(
                monthName = state.month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                totalMinor = state.totalMinor,
                dailyAverageMinor = ((state.dailyAverageMinor + 50) / 100) * 100,
                slices = state.slices,
                settings = state.settings,
            )

            if (state.slices.isEmpty()) {
                Text(
                    text = "No expenses this month",
                    color = c.muted,
                    fontFamily = InterFamily,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                )
            } else {
                CategoryList(state.slices, state.settings)
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SummaryHero(
    monthName: String,
    totalMinor: Long,
    dailyAverageMinor: Long,
    slices: List<CategorySlice>,
    settings: AppSettings,
) {
    val c = AtelierTheme.colors
    val shape = RoundedCornerShape(26.dp)
    val displayed = rememberCountUp(totalMinor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.hero, c.heroEnd)))
            .border(1.dp, c.heroLine, shape)
            .padding(start = 22.dp, end = 18.dp, top = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Total · $monthName",
                color = c.heroMuted,
                fontFamily = InterFamily,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = settings.currencySymbol,
                    color = c.gold2,
                    style = serifStyle(24f, 24f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = amountNumber(displayed, settings),
                    color = c.onHero,
                    style = serifStyle(44f, 44f, letterSpacing = -0.02f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Daily average",
                color = c.heroMuted,
                fontFamily = InterFamily,
                fontSize = 12.5.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = money(dailyAverageMinor, settings),
                color = c.onHero,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
        }
        Donut(slices = slices, totalMinor = totalMinor)
    }
}

@Composable
private fun Donut(slices: List<CategorySlice>, totalMinor: Long) {
    val c = AtelierTheme.colors
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }

    Box(modifier = Modifier.size(116.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = c.heroLine,
                radius = radius,
                center = center,
                style = Stroke(width = stroke),
            )
            if (slices.isEmpty() || totalMinor <= 0) return@Canvas

            val circumference = (2f * PI.toFloat() * radius)
            val gapDegrees = (2.6f / circumference) * 360f
            var start = -90f
            slices.forEach { slice ->
                val fullSweep = slice.fraction * 360f
                val sweep = ((fullSweep - gapDegrees) * progress.value).coerceAtLeast(0f)
                if (sweep > 0.2f) {
                    drawArc(
                        color = slice.category.darkAccent(),
                        startAngle = start + gapDegrees / 2f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                    )
                }
                start += fullSweep
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = slices.size.toString(),
                color = c.onHero,
                style = serifStyle(28f, 28f),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "categories".uppercase(),
                color = c.heroMuted,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
            )
        }
    }
}

private fun CategoryRef.darkAccent(): Color = style().dark

@Composable
private fun CategoryList(slices: List<CategorySlice>, settings: AppSettings) {
    val c = AtelierTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 10.dp),
    ) {
        slices.forEachIndexed { index, slice ->
            val fraction = if (slices.first().totalMinor > 0) {
                slice.totalMinor.toFloat() / slices.first().totalMinor.toFloat()
            } else {
                0f
            }
            val bar by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(1100, delayMillis = 120 + index * 60),
                label = "categoryBar",
            )
            Column(modifier = Modifier.padding(top = 9.dp, bottom = 9.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CategoryTile(category = slice.category, size = 30.dp)
                    Text(
                        text = slice.category.label,
                        color = c.ink,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = money(slice.totalMinor, settings),
                        color = c.ink,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.5.sp,
                    )
                    Text(
                        text = "${(slice.fraction * 100).roundToInt()}%",
                        color = c.muted,
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        modifier = Modifier.width(36.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .padding(start = 42.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.line),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(bar.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(slice.category.darkAccent()),
                    )
                }
            }
            if (index != slices.lastIndex) {
                HorizontalDivider(color = c.line, thickness = 1.dp)
            }
        }
    }
}
