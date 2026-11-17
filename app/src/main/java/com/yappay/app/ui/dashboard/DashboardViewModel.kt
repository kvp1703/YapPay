package com.yappay.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yappay.app.ExpenseApp
import com.yappay.app.data.AppSettings
import com.yappay.app.data.CategoryRepository
import com.yappay.app.data.ExpenseRepository
import com.yappay.app.data.SettingsRepository
import com.yappay.app.domain.CategoryRef
import com.yappay.app.util.DateRanges
import com.yappay.app.util.MillisRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

enum class DashboardPeriod(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
}

data class DashboardSlice(val category: CategoryRef, val totalMinor: Long, val fraction: Float)

data class DashboardUiState(
    val period: DashboardPeriod = DashboardPeriod.MONTH,
    /** Total spent in the selected period. */
    val periodTotalMinor: Long = 0,
    /** Always today, independent of the selected period. */
    val todayTotalMinor: Long = 0,
    /** Always the current month, independent of the selected period. */
    val monthTotalMinor: Long = 0,
    /** Largest first. Empty -> the card shows "No expenses yet". */
    val slices: List<DashboardSlice> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
)

class DashboardViewModel(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val period = MutableStateFlow(DashboardPeriod.MONTH)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = period
        .flatMapLatest { p ->
            val range = periodRange(p)
            combine(
                expenses.observeTotal(range),
                expenses.observeTotal(DateRanges.day(LocalDate.now())),
                expenses.observeTotal(DateRanges.month(YearMonth.now())),
                expenses.observeCategoryTotals(range),
                combine(settingsRepository.settings, categories.observeAll()) { settings, custom ->
                    settings to CategoryRef.customs(custom)
                },
            ) { periodTotal, todayTotal, monthTotal, rows, (settings, custom) ->
                val total = rows.sumOf { it.total }
                DashboardUiState(
                    period = p,
                    periodTotalMinor = periodTotal,
                    todayTotalMinor = todayTotal,
                    monthTotalMinor = monthTotal,
                    // Unknown category names map to OTHER, so merge them back together.
                    slices = rows
                        .groupBy { CategoryRef.resolve(it.category, custom) }
                        .map { (cat, list) -> cat to list.sumOf { it.total } }
                        .sortedByDescending { it.second }
                        .map { (cat, sum) ->
                            DashboardSlice(cat, sum, if (total > 0) sum.toFloat() / total else 0f)
                        },
                    settings = settings,
                    isLoading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun selectPeriod(value: DashboardPeriod) = period.update { value }

    /** Week = last 7 days including today; Month = calendar month; Year = calendar year. */
    private fun periodRange(p: DashboardPeriod): MillisRange {
        val today = LocalDate.now()
        return when (p) {
            DashboardPeriod.WEEK -> {
                val start = today.minusDays(6)
                val endExclusive = today.plusDays(1)
                MillisRange(DateRanges.day(start).startInclusive, DateRanges.day(endExclusive).startInclusive)
            }
            DashboardPeriod.MONTH -> DateRanges.month(YearMonth.from(today))
            DashboardPeriod.YEAR -> {
                val start = today.withDayOfYear(1)
                val endExclusive = start.plusYears(1)
                MillisRange(DateRanges.day(start).startInclusive, DateRanges.day(endExclusive).startInclusive)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ExpenseApp
                DashboardViewModel(
                    app.container.expenseRepository,
                    app.container.categoryRepository,
                    app.container.settingsRepository,
                )
            }
        }
    }
}
