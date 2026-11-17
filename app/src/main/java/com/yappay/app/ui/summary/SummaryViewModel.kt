package com.yappay.app.ui.summary

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

/** fraction is 0f..1f of the month total. Use it for the bar width and "NN%" label. */
data class CategorySlice(val category: CategoryRef, val totalMinor: Long, val fraction: Float)

data class SummaryUiState(
    val month: YearMonth = YearMonth.now(),
    val isCurrentMonth: Boolean = true,
    val totalMinor: Long = 0,
    /** total / days elapsed (current month) or total / days in month (past months). */
    val dailyAverageMinor: Long = 0,
    /** Largest first. Empty -> show "No expenses this month". */
    val slices: List<CategorySlice> = emptyList(),
    val settings: AppSettings = AppSettings(),
)

class SummaryViewModel(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SummaryUiState> = month
        .flatMapLatest { m ->
            combine(
                expenses.observeCategoryTotals(DateRanges.month(m)),
                settingsRepository.settings,
                categories.observeAll(),
            ) { rows, settings, customRows ->
                val total = rows.sumOf { it.total }
                val now = YearMonth.now()
                val days = if (m == now) LocalDate.now().dayOfMonth else m.lengthOfMonth()
                val custom = CategoryRef.customs(customRows)
                SummaryUiState(
                    month = m,
                    isCurrentMonth = m >= now,
                    totalMinor = total,
                    dailyAverageMinor = if (days > 0) total / days else 0,
                    // Unknown / deleted keys resolve to OTHER, so merge those rows back together.
                    slices = rows
                        .groupBy { CategoryRef.resolve(it.category, custom) }
                        .map { (cat, list) -> cat to list.sumOf { it.total } }
                        .sortedByDescending { it.second }
                        .map { (cat, sum) ->
                            CategorySlice(cat, sum, if (total > 0) sum.toFloat() / total else 0f)
                        },
                    settings = settings,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState())

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { if (it < YearMonth.now()) it.plusMonths(1) else it }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ExpenseApp
                SummaryViewModel(
                    app.container.expenseRepository,
                    app.container.categoryRepository,
                    app.container.settingsRepository,
                )
            }
        }
    }
}
