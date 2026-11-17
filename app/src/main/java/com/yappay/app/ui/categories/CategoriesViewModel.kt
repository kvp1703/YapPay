package com.yappay.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yappay.app.ExpenseApp
import com.yappay.app.data.CategoryRepository
import com.yappay.app.data.ExpenseRepository
import com.yappay.app.domain.Category
import com.yappay.app.domain.CategoryRef
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val custom: List<CategoryRef> = emptyList(),
    val isLoading: Boolean = true,
)

class CategoriesViewModel(
    private val categories: CategoryRepository,
    private val expenses: ExpenseRepository,
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = categories.observeAll()
        .map { list -> CategoriesUiState(custom = CategoryRef.customs(list), isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun add(name: String, color: Long) {
        viewModelScope.launch {
            try {
                categories.create(name, color)
                _messages.send("Category added")
            } catch (e: IllegalArgumentException) {
                _messages.send(e.message ?: "Could not add category")
            }
        }
    }

    fun update(ref: CategoryRef, name: String, color: Long) {
        val id = ref.customId() ?: return
        viewModelScope.launch {
            try {
                val entity = categories.getById(id) ?: return@launch
                categories.update(entity, name, color)
                _messages.send("Category updated")
            } catch (e: IllegalArgumentException) {
                _messages.send(e.message ?: "Could not update category")
            }
        }
    }

    /** Deletes the category and moves its expenses to "Other" so no row is left orphaned. */
    fun delete(ref: CategoryRef) {
        val id = ref.customId() ?: return
        viewModelScope.launch {
            val entity = categories.getById(id) ?: return@launch
            expenses.reassignCategory(ref.key, Category.OTHER.name)
            categories.delete(entity)
            _messages.send("Category deleted · its expenses moved to Other")
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ExpenseApp
                CategoriesViewModel(app.container.categoryRepository, app.container.expenseRepository)
            }
        }
    }
}
