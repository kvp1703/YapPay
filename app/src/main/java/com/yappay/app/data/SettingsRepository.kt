package com.yappay.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yappay.app.util.GroupingStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** This delegate MUST be top-level (one instance per process). Never create a second DataStore for "settings". */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val currencySymbol: String = "₹",
    val grouping: GroupingStyle = GroupingStyle.INDIAN,
    /** BCP-47 tag passed to the speech recognizer. */
    val speechLanguage: String = "en-IN",
    /** true: show the confirm dialog after speaking. false: save immediately + Undo snackbar. */
    val confirmBeforeSave: Boolean = true,
)

/** Options shown in the Settings screen. First value = tag, second = label. */
val SPEECH_LANGUAGES: List<Pair<String, String>> = listOf(
    "en-IN" to "English (India)",
    "en-US" to "English (US)",
    "en-GB" to "English (UK)",
    "hi-IN" to "Hindi (India) – numbers must be spoken as digits",
)

val CURRENCY_SYMBOLS: List<String> = listOf("₹", "$", "€", "£")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency_symbol")
        val GROUPING = stringPreferencesKey("grouping")
        val SPEECH_LANGUAGE = stringPreferencesKey("speech_language")
        val CONFIRM_BEFORE_SAVE = booleanPreferencesKey("confirm_before_save")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        val defaults = AppSettings()
        AppSettings(
            currencySymbol = p[Keys.CURRENCY] ?: defaults.currencySymbol,
            grouping = p[Keys.GROUPING]
                ?.let { name -> GroupingStyle.entries.firstOrNull { it.name == name } }
                ?: defaults.grouping,
            speechLanguage = p[Keys.SPEECH_LANGUAGE] ?: defaults.speechLanguage,
            confirmBeforeSave = p[Keys.CONFIRM_BEFORE_SAVE] ?: defaults.confirmBeforeSave,
        )
    }

    suspend fun setCurrencySymbol(value: String) {
        context.settingsDataStore.edit { it[Keys.CURRENCY] = value }
    }

    suspend fun setGrouping(value: GroupingStyle) {
        context.settingsDataStore.edit { it[Keys.GROUPING] = value.name }
    }

    suspend fun setSpeechLanguage(value: String) {
        context.settingsDataStore.edit { it[Keys.SPEECH_LANGUAGE] = value }
    }

    suspend fun setConfirmBeforeSave(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.CONFIRM_BEFORE_SAVE] = value }
    }
}
