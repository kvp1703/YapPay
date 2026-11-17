package com.yappay.app.ui.settings

import android.content.ContentResolver
import android.net.Uri
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
import com.yappay.app.export.CsvExporter
import com.yappay.app.util.GroupingStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository,
    private val contentResolver: ContentResolver,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** One-shot messages for a snackbar, e.g. "Exported 42 expenses". */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun setCurrencySymbol(value: String) {
        viewModelScope.launch { settingsRepository.setCurrencySymbol(value) }
    }

    fun setGrouping(value: GroupingStyle) {
        viewModelScope.launch { settingsRepository.setGrouping(value) }
    }

    fun setSpeechLanguage(value: String) {
        viewModelScope.launch { settingsRepository.setSpeechLanguage(value) }
    }

    fun setConfirmBeforeSave(value: Boolean) {
        viewModelScope.launch { settingsRepository.setConfirmBeforeSave(value) }
    }

    /** Suggested file name for ActivityResultContracts.CreateDocument("text/csv"). */
    fun suggestedFileName(): String = "expenses-${LocalDate.now()}.csv"

    /** Call with the Uri returned by CreateDocument. Null Uri (user cancelled) must NOT be passed. */
    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val message = try {
                val all = expenses.getAll()
                val csv = CsvExporter.buildCsv(
                    expenses = all,
                    customCategoryNames = categories.displayNames(),
                )
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                        ?: error("Could not open file")
                }
                "Exported ${all.size} expenses"
            } catch (e: Exception) {
                "Export failed: ${e.message ?: "unknown error"}"
            }
            _messages.send(message)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ExpenseApp
                SettingsViewModel(
                    app.container.settingsRepository,
                    app.container.expenseRepository,
                    app.container.categoryRepository,
                    app.contentResolver,
                )
            }
        }
    }
}
