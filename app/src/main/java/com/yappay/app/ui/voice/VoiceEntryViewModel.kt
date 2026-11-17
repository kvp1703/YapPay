package com.yappay.app.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yappay.app.ExpenseApp
import com.yappay.app.data.AppSettings
import com.yappay.app.data.CategoryRepository
import com.yappay.app.data.ExpenseRepository
import com.yappay.app.data.ExpenseSource
import com.yappay.app.data.SettingsRepository
import com.yappay.app.domain.CategoryClassifier
import com.yappay.app.domain.CategoryRef
import com.yappay.app.parser.FailureReason
import com.yappay.app.parser.ParseResult
import com.yappay.app.parser.VoiceParser
import com.yappay.app.speech.SpeechRecognizerManager
import com.yappay.app.speech.SpeechState
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Editable values of the confirm dialog. amountText is exactly what is in the text field. */
data class DraftForm(
    val amountText: String,
    val description: String,
    val category: CategoryRef,
    /** true once the user picked a category by hand; stops auto-classification overwriting it. */
    val categoryTouched: Boolean,
    val spentAt: Long,
    val rawTranscript: String?,
    val source: String,
) {
    val amountMinor: Long? get() = MoneyFormatter.parseUserInput(amountText)
    val amountError: Boolean get() = amountText.isNotBlank() && amountMinor == null
    val canSave: Boolean get() = amountMinor != null && description.isNotBlank()
}

/** What the parser has understood so far from the live transcript. */
data class VoicePreview(
    val amountMinor: Long,
    val description: String,
    val category: CategoryRef,
)

sealed interface VoiceUiState {
    data object Idle : VoiceUiState

    /** level is 0f..1f, for animating the mic. */
    data class Listening(
        val partialText: String,
        val level: Float,
        val preview: VoicePreview? = null,
    ) : VoiceUiState

    /** [typeInstead] renders the full-screen manual fallback instead of the confirm sheet. */
    data class Confirm(
        val form: DraftForm,
        val typeInstead: Boolean = false,
        val failureTranscript: String? = null,
    ) : VoiceUiState

    data class Failed(val message: String, val transcript: String?) : VoiceUiState
}

sealed interface VoiceEvent {
    /** Show a snackbar "message" with an UNDO action that calls undoSave(expenseId). */
    data class Saved(val expenseId: Long, val message: String) : VoiceEvent
}

class VoiceEntryViewModel(
    private val speech: SpeechRecognizerManager,
    private val expenses: ExpenseRepository,
    categories: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Built-in + user-created categories, ready for the confirm dialog's picker. */
    val categoryOptions: StateFlow<List<CategoryRef>> = categories.observeAll()
        .map { list -> CategoryRef.all(CategoryRef.customs(list)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CategoryRef.all())

    private val _ui = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val ui: StateFlow<VoiceUiState> = _ui.asStateFlow()

    private val _events = Channel<VoiceEvent>(Channel.BUFFERED)
    val events: Flow<VoiceEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            speech.state.collect { s -> onSpeechState(s) }
        }
    }

    // ------------------------------------------------------------------ mic

    /** Call ONLY after RECORD_AUDIO is granted. */
    fun startListening() {
        speech.start(settings.value.speechLanguage)
    }

    /** "Done" button in the listening dialog. */
    fun stopListening() = speech.stop()

    /** "Cancel" button / dialog dismissed while listening. */
    fun cancelListening() {
        speech.cancel()
        _ui.value = VoiceUiState.Idle
    }

    private fun onSpeechState(s: SpeechState) {
        when (s) {
            SpeechState.Idle -> if (_ui.value is VoiceUiState.Listening) _ui.value = VoiceUiState.Idle
            SpeechState.Preparing -> _ui.value = VoiceUiState.Listening(partialText = "", level = 0f)
            is SpeechState.Listening -> _ui.value = VoiceUiState.Listening(
                partialText = s.partialText,
                level = ((s.rmsDb + 2f) / 12f).coerceIn(0f, 1f),
                preview = previewFor(s.partialText),
            )
            is SpeechState.Result -> {
                speech.reset()
                handleTranscripts(s.alternatives)
            }
            is SpeechState.Error -> {
                speech.reset()
                _ui.value = VoiceUiState.Failed(s.message, transcript = null)
            }
        }
    }

    private fun handleTranscripts(alternatives: List<String>) {
        val transcript = alternatives.firstOrNull { VoiceParser.parse(it) is ParseResult.Success }
            ?: alternatives.first()
        when (val result = VoiceParser.parse(transcript)) {
            is ParseResult.Failure -> _ui.value = VoiceUiState.Failed(failureMessage(result.reason, transcript), transcript)
            is ParseResult.Success -> {
                val form = DraftForm(
                    amountText = MoneyFormatter.toInputText(result.amountMinor),
                    description = result.description,
                    category = CategoryRef.of(CategoryClassifier.classify(result.description)),
                    categoryTouched = false,
                    spentAt = System.currentTimeMillis(),
                    rawTranscript = transcript,
                    source = ExpenseSource.VOICE,
                )
                if (settings.value.confirmBeforeSave || !form.canSave) {
                    _ui.value = VoiceUiState.Confirm(form)
                } else {
                    save(form)
                }
            }
        }
    }

    private fun failureMessage(reason: FailureReason, transcript: String): String = when (reason) {
        FailureReason.EMPTY_INPUT -> "Didn't catch that. Try again."
        FailureReason.NO_AMOUNT -> "Couldn't find an amount in \"$transcript\". Say the amount first, e.g. \"340 pizza\"."
        FailureReason.ZERO_AMOUNT -> "The amount must be more than zero."
        FailureReason.AMOUNT_TOO_LARGE -> "That amount is too large."
    }

    // ------------------------------------------------------------ confirm form

    private fun updateForm(transform: (DraftForm) -> DraftForm) {
        _ui.update { s -> if (s is VoiceUiState.Confirm) s.copy(form = transform(s.form)) else s }
    }

    fun onAmountChange(text: String) = updateForm { it.copy(amountText = text) }

    fun onDescriptionChange(text: String) = updateForm {
        val category = if (it.categoryTouched) it.category else CategoryRef.of(CategoryClassifier.classify(text))
        it.copy(description = text, category = category)
    }

    fun onCategoryChange(category: CategoryRef) = updateForm { it.copy(category = category, categoryTouched = true) }

    fun onDateChange(date: LocalDate) = updateForm { it.copy(spentAt = DateRanges.withDate(it.spentAt, date)) }

    fun onConfirmSave() {
        val s = _ui.value as? VoiceUiState.Confirm ?: return
        if (!s.form.canSave) return
        save(s.form)
    }

    /** Failed dialog -> "Type instead": keep the words that were heard, add the amount by hand. */
    fun onTypeInstead() {
        val transcript = (_ui.value as? VoiceUiState.Failed)?.transcript
        val description = transcript?.let { VoiceParser.descriptionOf(it) }.orEmpty()
        _ui.value = VoiceUiState.Confirm(
            form = DraftForm(
                amountText = "",
                description = description,
                category = CategoryRef.of(CategoryClassifier.classify(description)),
                categoryTouched = false,
                spentAt = System.currentTimeMillis(),
                rawTranscript = transcript,
                source = ExpenseSource.MANUAL,
            ),
            typeInstead = true,
            failureTranscript = transcript,
        )
    }

    private fun previewFor(text: String): VoicePreview? {
        if (text.isBlank()) return null
        val result = VoiceParser.parse(text) as? ParseResult.Success ?: return null
        return VoicePreview(
            amountMinor = result.amountMinor,
            description = result.description,
            category = CategoryRef.of(CategoryClassifier.classify(result.description)),
        )
    }

    /** Failed or Confirm dialog -> "Try again". */
    fun onRetry() {
        _ui.value = VoiceUiState.Idle
        startListening()
    }

    fun dismiss() {
        _ui.value = VoiceUiState.Idle
    }

    private fun save(form: DraftForm) {
        val amount = form.amountMinor ?: return
        _ui.value = VoiceUiState.Idle
        viewModelScope.launch {
            val id = expenses.add(
                amountMinor = amount,
                description = form.description,
                category = form.category.key,
                spentAt = form.spentAt,
                rawTranscript = form.rawTranscript,
                source = form.source,
            )
            val st = settings.value
            val money = MoneyFormatter.format(amount, st.currencySymbol, st.grouping)
            _events.send(VoiceEvent.Saved(id, "Saved $money · ${form.description.trim()}"))
        }
    }

    fun undoSave(expenseId: Long) {
        viewModelScope.launch {
            expenses.getById(expenseId)?.let { expenses.delete(it) }
        }
    }

    override fun onCleared() {
        speech.destroy()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ExpenseApp
                VoiceEntryViewModel(
                    speech = SpeechRecognizerManager(app.applicationContext),
                    expenses = app.container.expenseRepository,
                    categories = app.container.categoryRepository,
                    settingsRepository = app.container.settingsRepository,
                )
            }
        }
    }
}
