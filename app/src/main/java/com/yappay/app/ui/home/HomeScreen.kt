package com.yappay.app.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yappay.app.data.AppSettings
import com.yappay.app.data.ExpenseEntity
import com.yappay.app.data.ExpenseSource
import com.yappay.app.domain.CategoryRef
import com.yappay.app.ui.components.AtelierSnackbarHost
import com.yappay.app.ui.components.CategoryTile
import com.yappay.app.ui.components.CircleAction
import com.yappay.app.ui.components.GoldDiamond
import com.yappay.app.ui.components.amountNumber
import com.yappay.app.ui.components.money
import com.yappay.app.ui.components.rememberCountUp
import com.yappay.app.ui.theme.AtelierTheme
import com.yappay.app.ui.theme.InterFamily
import com.yappay.app.ui.theme.serifStyle
import com.yappay.app.ui.voice.VoiceEntryDialogs
import com.yappay.app.ui.voice.VoiceEntryViewModel
import com.yappay.app.ui.voice.rememberMicClickHandler
import com.yappay.app.util.DateRanges
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.sin

private val TodayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
private val ShortDayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    onOpenSummary: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddManually: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onOpenDashboard: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val voiceVm: VoiceEntryViewModel = viewModel(factory = VoiceEntryViewModel.Factory)
    val onMicClick = rememberMicClickHandler(voiceVm, snackbarHostState)
    val c = AtelierTheme.colors

    var savedId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(savedId) {
        if (savedId != null) {
            delay(6_000)
            savedId = null
        }
    }

    val today = LocalDate.now()
    val empty = !state.isLoading && state.days.isEmpty()
    val daysElapsed = if (state.month == YearMonth.now()) today.dayOfMonth else state.month.lengthOfMonth()
    val averageRaw = if (daysElapsed > 0) state.monthTotalMinor / daysElapsed else 0L
    val average = ((averageRaw + 50) / 100) * 100
    val dailyValues = remember(state.month, state.days) {
        val values = LongArray(state.month.lengthOfMonth())
        state.days.forEach { day ->
            if (YearMonth.from(day.date) == state.month) {
                values[day.date.dayOfMonth - 1] = day.totalMinor
            }
        }
        val last = if (state.month == YearMonth.now()) today.dayOfMonth else state.month.lengthOfMonth()
        values.take(last.coerceAtLeast(1))
    }

    Scaffold(
        containerColor = c.bg,
        snackbarHost = {
            AtelierSnackbarHost(
                hostState = snackbarHostState,
                bottomPadding = 116.dp,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg)
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 170.dp),
            ) {
                item(key = "header") {
                    HomeHeader(
                        dateLine = TodayFormatter.format(today).uppercase(Locale.ENGLISH),
                        onOpenSettings = onOpenSettings,
                    )
                }
                item(key = "month") {
                    MonthSelector(
                        month = state.month,
                        isCurrentMonth = state.isCurrentMonth,
                        onPrevious = vm::previousMonth,
                        onNext = vm::nextMonth,
                    )
                }
                item(key = "hero") {
                    HeroCard(
                        month = state.month,
                        totalMinor = state.monthTotalMinor,
                        todayMinor = state.todayTotalMinor,
                        averageMinor = average,
                        settings = state.settings,
                        dailyValues = dailyValues,
                        empty = empty,
                    )
                }

                if (empty) {
                    item(key = "empty") {
                        EmptyHome(settings = state.settings)
                    }
                } else {
                    state.days.forEach { day ->
                        item(key = "day_${day.date}") {
                            DayHeader(day = day, settings = state.settings, today = today)
                        }
                        items(
                            count = day.items.size,
                            key = { index -> day.items[index].id },
                        ) { index ->
                            val expense = day.items[index]
                            ExpenseRow(
                                expense = expense,
                                showDivider = index > 0,
                                settings = state.settings,
                                categories = state.customCategories,
                                highlight = expense.id == savedId,
                                onClick = { onEditExpense(expense.id) },
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, c.bg.copy(alpha = 0.98f), c.bg),
                        ),
                    ),
            )

            Dock(
                onMicClick = onMicClick,
                onAddManually = onAddManually,
                onOpenSummary = onOpenSummary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp),
            )

            if (empty) {
                HintPill(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 128.dp),
                )
            }

            VoiceEntryDialogs(
                viewModel = voiceVm,
                snackbarHostState = snackbarHostState,
                onSaved = { savedId = it },
            )
        }
    }
}

@Composable
private fun HomeHeader(dateLine: String, onOpenSettings: () -> Unit) {
    val c = AtelierTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 20.dp, top = 6.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoldDiamond()
                Spacer(Modifier.width(7.dp))
                Text(
                    text = dateLine,
                    color = c.muted,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.68.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Voice ",
                    color = c.ink,
                    style = serifStyle(27f, 28f),
                )
                Text(
                    text = "Expense",
                    color = c.gold,
                    style = serifStyle(27f, 28f),
                )
            }
        }
        CircleAction(
            icon = Icons.Rounded.Settings,
            contentDescription = "Settings",
            onClick = onOpenSettings,
        )
    }
}

@Composable
fun MonthSelector(
    month: YearMonth,
    isCurrentMonth: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AtelierTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleAction(
            icon = Icons.Rounded.ChevronLeft,
            contentDescription = "Previous month",
            onClick = onPrevious,
            size = 34.dp,
            ghost = true,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                color = c.ink,
                style = serifStyle(22f, 22f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = month.year.toString(),
                color = c.muted,
                fontFamily = InterFamily,
                fontSize = 12.5.sp,
                letterSpacing = 0.5.sp,
            )
        }
        CircleAction(
            icon = Icons.Rounded.ChevronRight,
            contentDescription = "Next month",
            onClick = onNext,
            size = 34.dp,
            ghost = true,
            enabled = !isCurrentMonth,
        )
    }
}

@Composable
private fun HeroCard(
    month: YearMonth,
    totalMinor: Long,
    todayMinor: Long,
    averageMinor: Long,
    settings: AppSettings,
    dailyValues: List<Long>,
    empty: Boolean,
) {
    val c = AtelierTheme.colors
    val shape = RoundedCornerShape(26.dp)
    val displayed = rememberCountUp(totalMinor)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = c.ink.copy(alpha = 0.18f),
                spotColor = c.ink.copy(alpha = 0.18f),
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.hero, c.heroEnd)))
            .border(1.dp, c.heroLine, shape)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.gold2.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(size.width * 1.05f, -size.height * 0.10f),
                        radius = size.width * 0.72f,
                    ),
                    radius = size.width * 0.72f,
                    center = Offset(size.width * 1.05f, -size.height * 0.10f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.gold2.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(-size.width * 0.10f, size.height * 1.2f),
                        radius = size.width * 0.55f,
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(-size.width * 0.10f, size.height * 1.2f),
                )
            }
            .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 18.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Spent in ${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}",
                    color = c.heroMuted,
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    letterSpacing = 0.24.sp,
                )
                Icon(
                    imageVector = Icons.Rounded.NorthEast,
                    contentDescription = null,
                    tint = c.gold2,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = settings.currencySymbol,
                    color = c.gold2,
                    style = serifStyle(30f, 30f),
                    modifier = Modifier.padding(bottom = 7.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = amountNumber(displayed, settings),
                    color = c.onHero,
                    style = serifStyle(56f, 56f, letterSpacing = -0.02f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }

            Spacer(Modifier.height(8.dp))
            Sparkline(
                values = dailyValues,
                empty = empty,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = c.heroLine, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Today",
                        color = c.heroMuted,
                        fontFamily = InterFamily,
                        fontSize = 12.5.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = money(todayMinor, settings),
                        color = c.onHero,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Avg",
                        color = c.heroMuted,
                        fontFamily = InterFamily,
                        fontSize = 12.5.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = money(averageMinor, settings),
                        color = c.onHero,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = " / day",
                        color = c.heroMuted,
                        fontFamily = InterFamily,
                        fontSize = 12.5.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun Sparkline(values: List<Long>, empty: Boolean, modifier: Modifier = Modifier) {
    val c = AtelierTheme.colors
    val pulse by rememberInfiniteTransition(label = "sparkDot").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "sparkPulse",
    )

    Canvas(modifier = modifier) {
        if (empty || values.isEmpty() || values.all { it == 0L }) {
            drawLine(
                color = c.heroLine,
                start = Offset(0f, size.height - 6.dp.toPx()),
                end = Offset(size.width, size.height - 6.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
            )
            return@Canvas
        }

        val top = 4.dp.toPx()
        val bottom = size.height - 4.dp.toPx()
        val xPad = 5.dp.toPx()
        val usableWidth = (size.width - xPad * 2f).coerceAtLeast(1f)
        val min = values.min()
        val max = values.max()
        val span = (max - min).coerceAtLeast(1L)
        val stepX = if (values.size == 1) 0f else usableWidth / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            Offset(
                x = if (values.size == 1) size.width / 2f else xPad + index * stepX,
                y = bottom - ((value - min).toFloat() / span.toFloat()) * (bottom - top),
            )
        }

        val line = smoothPath(points)
        val fill = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                colors = listOf(c.gold2.copy(alpha = 0.28f), Color.Transparent),
                startY = 0f,
                endY = size.height,
            ),
        )
        drawPath(
            path = line,
            color = c.gold2,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
        )

        val last = points.last()
        drawCircle(c.gold2.copy(alpha = 0.20f + 0.15f * pulse), radius = (5.5f + 2.5f * pulse).dp.toPx(), center = last)
        drawCircle(c.gold2, radius = 3.dp.toPx(), center = last)
    }
}

private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val p0 = points.getOrElse(i - 1) { points[i] }
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points.getOrElse(i + 2) { p2 }
        path.cubicTo(
            x1 = p1.x + (p2.x - p0.x) / 6f,
            y1 = p1.y + (p2.y - p0.y) / 6f,
            x2 = p2.x - (p3.x - p1.x) / 6f,
            y2 = p2.y - (p3.y - p1.y) / 6f,
            x3 = p2.x,
            y3 = p2.y,
        )
    }
    return path
}

@Composable
private fun DayHeader(day: DayGroup, settings: AppSettings, today: LocalDate) {
    val c = AtelierTheme.colors
    val yesterday = today.minusDays(1)
    val title = when (day.date) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    }
    val dateLine = when (day.date) {
        today, yesterday -> DateRanges.DAY_HEADER.format(day.date)
        else -> ShortDayFormatter.format(day.date)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                color = c.ink,
                style = serifStyle(18f, 20f, italic = true, letterSpacing = 0f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = dateLine,
                color = c.muted,
                fontFamily = InterFamily,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Text(
            text = money(day.totalMinor, settings),
            color = c.muted,
            fontFamily = InterFamily,
            fontSize = 12.5.sp,
        )
    }
}

@Composable
private fun ExpenseRow(
    expense: ExpenseEntity,
    showDivider: Boolean,
    settings: AppSettings,
    categories: List<CategoryRef>,
    highlight: Boolean,
    onClick: () -> Unit,
) {
    val c = AtelierTheme.colors
    val category = CategoryRef.resolve(expense.category, categories)
    val time = DateRanges.TIME.format(java.time.Instant.ofEpochMilli(expense.spentAt).atZone(java.time.ZoneId.systemDefault()))
    val background = if (highlight) {
        Brush.horizontalGradient(
            listOf(c.gold.copy(alpha = 0.11f), Color.Transparent),
        )
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick),
    ) {
        if (highlight) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(vertical = 14.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(
                        color = c.gold,
                        shape = RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp),
                    ),
            )
        }
        Column {
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp, end = 22.dp),
                    thickness = 1.dp,
                    color = c.line,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CategoryTile(
                    category = category,
                    size = 40.dp,
                    voice = expense.source == ExpenseSource.VOICE,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.description,
                        color = c.ink,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        letterSpacing = (-0.18).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = buildAnnotatedString {
                            if (highlight) {
                                withStyle(SpanStyle(color = c.gold, fontWeight = FontWeight.Medium)) {
                                    append("Just now")
                                }
                                append(" · ")
                            }
                            append(category.label)
                            append(" · ")
                            append(time)
                        },
                        color = c.muted,
                        fontFamily = InterFamily,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = money(expense.amountMinor, settings),
                    color = c.ink,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = (-0.15).sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EmptyHome(settings: AppSettings) {
    val c = AtelierTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .border(1.dp, c.line, CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = CircleShape,
                        ambientColor = c.ink.copy(alpha = 0.08f),
                        spotColor = c.ink.copy(alpha = 0.10f),
                    )
                    .clip(CircleShape)
                    .background(c.card)
                    .border(1.dp, c.line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = c.gold,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = buildAnnotatedString {
                append("A blank ")
                withStyle(SpanStyle(color = c.gold)) { append("page") }
                append(".")
            },
            color = c.ink,
            style = serifStyle(28f, 31f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Tap the mic and say something like “340 pizza”.",
            color = c.muted,
            fontFamily = InterFamily,
            fontSize = 14.sp,
            lineHeight = 21.7.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Dock(
    onMicClick: () -> Unit,
    onAddManually: () -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        DockButton(
            icon = Icons.Rounded.Keyboard,
            description = "Add manually",
            onClick = onAddManually,
        )
        MicButton(onClick = onMicClick)
        DockButton(
            icon = Icons.Rounded.DonutLarge,
            description = "Summary",
            onClick = onOpenSummary,
        )
    }
}

@Composable
private fun DockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val c = AtelierTheme.colors
    Box(
        modifier = Modifier
            .size(52.dp)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = c.ink.copy(alpha = 0.08f),
                spotColor = c.ink.copy(alpha = 0.08f),
            )
            .clip(CircleShape)
            .background(c.glass)
            .border(1.dp, c.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = c.ink,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MicButton(onClick: () -> Unit) {
    val c = AtelierTheme.colors
    val spin by rememberInfiniteTransition(label = "micSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "micSpinAngle",
    )

    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringRadius = size.minDimension / 2f - 0.75.dp.toPx()
            drawCircle(
                color = c.line,
                radius = ringRadius,
                style = Stroke(width = 1.dp.toPx()),
            )
            rotate(spin) {
                drawArc(
                    color = c.gold2,
                    startAngle = 0f,
                    sweepAngle = 92f,
                    useCenter = false,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(size.width / 2f - ringRadius, size.height / 2f - ringRadius),
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(84.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = CircleShape,
                    ambientColor = c.ink.copy(alpha = 0.30f),
                    spotColor = c.ink.copy(alpha = 0.30f),
                )
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(c.mic, c.micEnd)))
                .clickable(onClick = onClick)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width / 2f, 0f),
                            radius = size.height * 0.7f,
                        ),
                        radius = size.height * 0.7f,
                        center = Offset(size.width / 2f, 0f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Add expense by voice",
                tint = c.onMic,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun HintPill(modifier: Modifier = Modifier) {
    val c = AtelierTheme.colors
    Box(modifier = modifier.padding(bottom = 5.dp)) {
        Row(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(c.ink)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tap and say ",
                color = c.bg,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
            Text(
                text = "“340 pizza”",
                color = c.snackAct,
                style = serifStyle(16f, 16f, italic = true),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 4.dp)
                .size(10.dp)
                .rotate(45f)
                .background(c.ink, RoundedCornerShape(2.dp)),
        )
    }
}
