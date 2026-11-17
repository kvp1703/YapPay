package com.yappay.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yappay.app.data.AppSettings
import com.yappay.app.util.DateRanges
import com.yappay.app.util.MoneyFormatter
import java.time.YearMonth

private val BrandBlue = Color(0xFF2F6BFF)
private val OnBrand = Color.White

private val SlicePalette = listOf(
    Color(0xFF4A7DFF),
    Color(0xFF2BB673),
    Color(0xFFE8478B),
    Color(0xFFF5A623),
    Color(0xFF8B5CF6),
    Color(0xFF22C1C3),
    Color(0xFFEF4444),
    Color(0xFFEAB308),
)

@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    onAddManually: () -> Unit,
    onOpenSummary: () -> Unit,
) {
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
    val s by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddManually,
                containerColor = BrandBlue,
                contentColor = OnBrand,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DashboardHeader(
                monthTitle = DateRanges.MONTH_TITLE.format(YearMonth.now()),
                onBack = onBack,
            )

            TotalSpentCard(state = s)

            PeriodSelector(
                selected = s.period,
                onSelect = vm::selectPeriod,
            )

            SpendingCard(
                state = s,
                onOpenSummary = onOpenSummary,
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DashboardHeader(monthTitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun TotalSpentCard(state: DashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BrandBlue),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "Total Spent",
                style = MaterialTheme.typography.bodyMedium,
                color = OnBrand.copy(alpha = 0.85f),
            )
            Text(
                text = money(state.periodTotalMinor, state.settings),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = OnBrand,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatColumn(
                    label = "TODAY",
                    value = money(state.todayTotalMinor, state.settings),
                    icon = { modifier ->
                        Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = modifier)
                    },
                    modifier = Modifier.weight(1f),
                )
                StatColumn(
                    label = "THIS MONTH",
                    value = money(state.monthTotalMinor, state.settings),
                    icon = { modifier ->
                        Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = modifier)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    icon: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            icon(Modifier.size(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OnBrand.copy(alpha = 0.85f),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = OnBrand,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PeriodSelector(selected: DashboardPeriod, onSelect: (DashboardPeriod) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DashboardPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) BrandBlue else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .clickable { onSelect(period) }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) OnBrand else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SpendingCard(state: DashboardUiState, onOpenSummary: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = spendingTitle(state.period),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenSummary)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandBlue,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BrandBlue,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                SpendingRing(
                    slices = state.slices,
                    modifier = Modifier.size(190.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = compactMoney(state.periodTotalMinor, state.settings),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "SPENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (state.slices.isEmpty()) {
                Text(
                    text = "No expenses yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SpendingLegend(state.slices)
            }
        }
    }
}

@Composable
private fun SpendingRing(
    slices: List<DashboardSlice>,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokePx = 26.dp.toPx()
        val radius = (size.minDimension - strokePx) / 2f
        drawCircle(
            color = trackColor,
            radius = radius,
            style = Stroke(width = strokePx),
        )
        if (slices.isEmpty()) return@Canvas

        val gap = 3f
        var startAngle = -90f
        slices.forEachIndexed { index, slice ->
            val fullSweep = slice.fraction * 360f
            val sweep = (fullSweep - gap).coerceAtLeast(0f)
            if (sweep > 0f) {
                drawArc(
                    color = sliceColor(index),
                    startAngle = startAngle + gap / 2f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                    topLeft = Offset(
                        x = (size.width - radius * 2f) / 2f,
                        y = (size.height - radius * 2f) / 2f,
                    ),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                )
            }
            startAngle += fullSweep
        }
    }
}

@Composable
private fun SpendingLegend(slices: List<DashboardSlice>) {
    val legend = slices.take(4)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        legend.chunked(2).forEachIndexed { rowIndex, rowSlices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowSlices.forEachIndexed { columnIndex, slice ->
                    LegendItem(
                        slice = slice,
                        color = sliceColor(rowIndex * 2 + columnIndex),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowSlices.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendItem(slice: DashboardSlice, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = slice.category.label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun spendingTitle(period: DashboardPeriod): String = when (period) {
    DashboardPeriod.WEEK -> "Weekly Spending"
    DashboardPeriod.MONTH -> "Monthly Spending"
    DashboardPeriod.YEAR -> "Yearly Spending"
}

private fun sliceColor(index: Int): Color = SlicePalette[index % SlicePalette.size]

private fun money(minor: Long, settings: AppSettings): String =
    MoneyFormatter.format(minor, settings.currencySymbol, settings.grouping)

/** "$12,845" for small values, "$3.2k" / "$1.2M" inside the small ring. */
private fun compactMoney(minor: Long, settings: AppSettings): String {
    val symbol = settings.currencySymbol
    val major = minor / 100
    return when {
        major < 1_000 -> money(minor, settings)
        major < 1_000_000 -> {
            val whole = major / 1_000
            val tenth = (major % 1_000) / 100
            if (tenth == 0L) "$symbol${whole}k" else "$symbol$whole.${tenth}k"
        }
        else -> {
            val whole = major / 1_000_000
            val tenth = (major % 1_000_000) / 100_000
            if (tenth == 0L) "$symbol${whole}M" else "$symbol$whole.${tenth}M"
        }
    }
}
