package com.yappay.app.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yappay.app.ExpenseApp
import com.yappay.app.data.AppSettings
import com.yappay.app.data.CategoryRepository
import com.yappay.app.data.ExpenseEntity
import com.yappay.app.data.ExpenseRepository
import com.yappay.app.data.ExpenseSource
import com.yappay.app.data.SettingsRepository
import com.yappay.app.domain.Category
import com.yappay.app.domain.CategoryClassifier
import com.yappay.app.domain.CategoryRef
import com.yappay.app.ui.navigation.Routes
import com.yappay.app.util.DateRanges
import com.yappay.app.util.MoneyFormatter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddEditUiState(
    val isLoading: Boolean = true,
    /** null -> "Add" mode. Non-null -> "Edit" mode (show the Delete action). */
    val existing: ExpenseEntity? = null,
    val amountText: String = "",
    val description: String = "",
    val category: CategoryRef = CategoryRef.of(Category.OTHER),
    val categoryTouched: Boolean = false,
    /** User-created categories, available in the chip row. */
    val customCategories: List<CategoryRef> = emptyList(),
    val spentAt: Long = System.currentTimeMillis(),
    /** true while saving/deleting: disable the buttons. */
    val isWorking: Boolean = false,
) {
    val isEdit: Boolean get() = existing != null
    val amountMinor: Long? get() = MoneyFormatter.parseUserInput(amountText)
    val amountError: Boolean get() = amountText.isNotBlank() && amountMinor == null
    val canSave: Boolean get() = !isLoading && !isWorking && amountMinor != null && description.isNotBlank()
    val date: LocalDate get() = DateRanges.toLocalDate(spentAt)
}

sealed interface AddEditEvent {
    /** Saved or deleted: the screen must call onDone() (navigate back). */
    data object Finished : AddEditEvent

    /** The id in the route does not exist any more: show a toast/snackbar and call onDone(). */
    data object NotFound : AddEditEvent

    /** A category could not be created (duplicate name, blank, ...). Show it in a snackbar. */
    data class Message(val text: String) : AddEditEvent
}

class AddEditExpenseViewModel(
    savedStateHandle: SavedStateHandle,
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Present only on the "edit/{expenseId}" route. */
    private val expenseId: Long? = savedStateHandle.get<Long>(Routes.EXPENSE_ID_ARG)

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _ui = MutableStateFlow(AddEditUiState())
    val ui: StateFlow<AddEditUiState> = _ui.asStateFlow()

    private val _events = Channel<AddEditEvent>(Channel.BUFFERED)
    val events: Flow<AddEditEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val custom = CategoryRef.customs(categories.getAll())
            if (expenseId == null) {
                _ui.value = AddEditUiState(isLoading = false, customCategories = custom)
            } else {
                val e = expenses.getById(expenseId)
                if (e == null) {
                    _events.send(AddEditEvent.NotFound)
                } else {
                    _ui.value = AddEditUiState(
                        isLoading = false,
                        existing = e,
                        amountText = MoneyFormatter.toInputText(e.amountMinor),
                        description = e.description,
                        category = CategoryRef.resolve(e.category, custom),
                        categoryTouched = true,
                        customCategories = custom,
                        spentAt = e.spentAt,
                    )
                }
            }
        }
    }

    fun onAmountChange(text: String) = _ui.update { it.copy(amountText = text) }

    fun onDescriptionChange(text: String) = _ui.update {
        it.copy(
            description = text,
            category = if (it.categoryTouched) it.category else CategoryRef.of(CategoryClassifier.classify(text)),
        )
    }

    fun onCategoryChange(category: CategoryRef) = _ui.update { it.copy(category = category, categoryTouched = true) }

    fun onDateChange(date: LocalDate) = _ui.update { it.copy(spentAt = DateRanges.withDate(it.spentAt, date)) }

    /** Creates a custom category and selects it. Errors are surfaced as a snackbar message. */
    fun addCategory(name: String, color: Long) {
        viewModelScope.launch {
            try {
                val ref = CategoryRef.custom(categories.create(name, color))
                _ui.update {
                    it.copy(
                        customCategories = it.customCategories + ref,
                        category = ref,
                        categoryTouched = true,
                    )
                }
            } catch (e: IllegalArgumentException) {
                _events.send(AddEditEvent.Message(e.message ?: "Could not add category"))
            }
        }
    }

    fun save() {
        val s = _ui.value
        val amount = s.amountMinor ?: return
        if (!s.canSave) return
        _ui.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val existing = s.existing
            if (existing == null) {
                expenses.add(
                    amountMinor = amount,
                    description = s.description,
                    category = s.category.key,
                    spentAt = s.spentAt,
                    rawTranscript = null,
                    source = ExpenseSource.MANUAL,
                )
            } else {
                expenses.update(
                    existing.copy(
                        amountMinor = amount,
                        description = s.description,
                        category = s.category.key,
                        spentAt = s.spentAt,
                    ),
                )
            }
            _events.send(AddEditEvent.Finished)
        }
    }

    /** Call only after the user confirmed the "Delete this expense?" dialog. */
    fun delete() {
        val existing = _ui.value.existing ?: return
        _ui.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            expenses.delete(existing)
            _events.send(AddEditEvent.Finished)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ExpenseApp
                AddEditExpenseViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    expenses = app.container.expenseRepository,
                    categories = app.container.categoryRepository,
                    settingsRepository = app.container.settingsRepository,
                )
            }
        }
    }
}
