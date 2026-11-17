package com.yappay.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Everything the UI needs to know about the microphone. */
sealed interface SpeechState {
    /** Not listening. Mic button enabled. */
    data object Idle : SpeechState

    /** Recognizer started; waiting for the user to start talking. */
    data object Preparing : SpeechState

    /**
     * User is talking.
     * partialText : best-guess transcript so far ("" until the first partial result).
     * rmsDb       : current loudness, roughly -2..10. Use it to animate the mic.
     */
    data class Listening(val partialText: String, val rmsDb: Float) : SpeechState

    /** Final alternatives, best first. Never empty. */
    data class Result(val alternatives: List<String>) : SpeechState

    data class Error(val code: Int, val message: String) : SpeechState
}

/**
 * Thin wrapper around android.speech.SpeechRecognizer that exposes a StateFlow.
 *
 * RULES (do not break them):
 *  - Create, start(), stop(), cancel() and destroy() MUST be called on the MAIN thread.
 *    ViewModel functions called from Compose click handlers are on the main thread, so that is fine.
 *  - Call destroy() exactly once, from ViewModel.onCleared().
 *  - The RECORD_AUDIO runtime permission must already be granted before calling start().
 *  - Pass the APPLICATION context (never an Activity) to avoid leaks.
 */
class SpeechRecognizerManager(private val appContext: Context) {

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    /**
     * Binds the recognizer service before the first start(). Creating a SpeechRecognizer is one of
     * the slowest parts of capture (it binds to the system recognition service); doing it while the
     * quick-capture overlay is appearing hides that latency from the press-and-hold gesture.
     *
     * MAIN THREAD ONLY. Does not enable the microphone.
     */
    fun prepare() {
        if (recognizer != null || !isAvailable()) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also {
            it.setRecognitionListener(listener)
        }
    }

    /** languageTag examples: "en-IN", "en-US", "hi-IN". */
    fun start(languageTag: String) {
        // Ignore double taps while a session is already running.
        if (_state.value is SpeechState.Preparing || _state.value is SpeechState.Listening) return
        if (!isAvailable()) {
            _state.value = SpeechState.Error(
                ERROR_NOT_AVAILABLE,
                "Speech recognition is not available on this device. Install or enable the Google app.",
            )
            return
        }
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(appContext).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            // Give the user ~1.5s of silence before deciding they are done.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        _state.value = SpeechState.Preparing
        r.startListening(intent)
    }

    /** User tapped "Done": stop recording and deliver whatever was heard. */
    fun stop() {
        recognizer?.stopListening()
    }

    /** User tapped "Cancel": throw the audio away. */
    fun cancel() {
        recognizer?.cancel()
        _state.value = SpeechState.Idle
    }

    /** Call after the UI has consumed a Result or Error. */
    fun reset() {
        _state.value = SpeechState.Idle
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        _state.value = SpeechState.Idle
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = SpeechState.Listening(partialText = "", rmsDb = 0f)
        }

        override fun onBeginningOfSpeech() {
            if (_state.value !is SpeechState.Listening) {
                _state.value = SpeechState.Listening(partialText = "", rmsDb = 0f)
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            val s = _state.value
            if (s is SpeechState.Listening) _state.value = s.copy(rmsDb = rmsdB)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            val s = _state.value
            _state.value = if (s is SpeechState.Listening) s.copy(partialText = text) else SpeechState.Listening(text, 0f)
        }

        override fun onResults(results: Bundle?) {
            val list = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.filter { it.isNotBlank() }
                .orEmpty()
            _state.value = if (list.isEmpty()) {
                SpeechState.Error(SpeechRecognizer.ERROR_NO_MATCH, messageFor(SpeechRecognizer.ERROR_NO_MATCH))
            } else {
                SpeechState.Result(list)
            }
        }

        override fun onError(error: Int) {
            // Some devices fire a late ERROR_CLIENT / ERROR_NO_MATCH after a good result or after
            // cancel(). Only report errors while a session is actually running.
            val s = _state.value
            if (s !is SpeechState.Preparing && s !is SpeechState.Listening) return
            _state.value = SpeechState.Error(error, messageFor(error))
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    companion object {
        /** Our own code (not from Android) for "no recognizer installed". */
        const val ERROR_NOT_AVAILABLE = -1

        fun messageFor(code: Int): String = when (code) {
            SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try again, e.g. \"340 pizza\"."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap the mic and speak."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Network problem. Check your connection and try again."
            SpeechRecognizer.ERROR_AUDIO -> "Microphone error. Try again."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Try again in a moment."
            SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
                "Speech service error. Try again."
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests. Wait a bit and try again."
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                "Selected speech language is not available. Change it in Settings."
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was interrupted. Try again."
            ERROR_NOT_AVAILABLE -> "Speech recognition is not available on this device."
            else -> "Speech recognition failed (code $code). Try again."
        }
    }
}
