package com.yappay.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yappay.app.ExpenseApp
import com.yappay.app.data.AppSettings
import com.yappay.app.data.CategoryRepository
import com.yappay.app.data.ExpenseEntity
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

/** One date header + the expenses of that day (newest first). */
data class DayGroup(val date: LocalDate, val totalMinor: Long, val items: List<ExpenseEntity>)

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    /** true -> "next month" arrow must be disabled. */
    val isCurrentMonth: Boolean = true,
    val monthTotalMinor: Long = 0,
    /** Always TODAY, independent of the selected month. */
    val todayTotalMinor: Long = 0,
    /** Newest day first. Empty list -> show the empty state. */
    val days: List<DayGroup> = emptyList(),
    /** User-created categories, used to label/colour the rows. */
    val customCategories: List<CategoryRef> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = month
        .flatMapLatest { m ->
            combine(
                expenses.observeExpenses(DateRanges.month(m)),
                expenses.observeTotal(DateRanges.day(LocalDate.now())),
                settingsRepository.settings,
                categories.observeAll(),
            ) { list, todayTotal, settings, custom ->
                HomeUiState(
                    month = m,
                    isCurrentMonth = m >= YearMonth.now(),
                    monthTotalMinor = list.sumOf { it.amountMinor },
                    todayTotalMinor = todayTotal,
                    days = list
                        .groupBy { DateRanges.toLocalDate(it.spentAt) }
                        .map { (date, items) -> DayGroup(date, items.sumOf { it.amountMinor }, items) },
                    customCategories = CategoryRef.customs(custom),
                    settings = settings,
                    isLoading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { if (it < YearMonth.now()) it.plusMonths(1) else it }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ExpenseApp
                HomeViewModel(
                    app.container.expenseRepository,
                    app.container.categoryRepository,
                    app.container.settingsRepository,
                )
            }
        }
    }
}
