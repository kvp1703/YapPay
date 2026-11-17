package com.yappay.app.widget

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.speech.SpeechRecognizer
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.yappay.app.ExpenseApp
import com.yappay.app.data.AppSettings
import com.yappay.app.data.ExpenseSource
import com.yappay.app.domain.CategoryClassifier
import com.yappay.app.parser.FailureReason
import com.yappay.app.parser.ParseResult
import com.yappay.app.parser.VoiceParser
import com.yappay.app.speech.SpeechRecognizerManager
import com.yappay.app.speech.SpeechState
import com.yappay.app.util.MoneyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The widget's one-shot microphone surface.
 *
 * Why an Activity: Android app widgets can only send click PendingIntents; they cannot observe
 * touch-down/up. This transparent, no-history Activity is the smallest possible surface that can
 * own a real press-and-hold gesture. It deliberately does not use Compose or navigation.
 *
 * Latency path:
 *   widget tap (PendingIntent, no transition animation)
 *   -> Activity onCreate (one custom View)
 *   -> SpeechRecognizer prepared immediately (service bind hidden behind the first touch)
 *   -> ACTION_DOWN starts listening
 *   -> ACTION_UP stops listening
 *   -> result parsed and inserted on the application scope
 *   -> widget RemoteViews updated
 *   -> Activity finishes ~1s after showing "Saved".
 */
class QuickCaptureActivity : Activity() {

    private lateinit var view: QuickCaptureView
    private lateinit var speech: SpeechRecognizerManager
    private lateinit var app: ExpenseApp
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var started = false
    private var settings = AppSettings()
    private var settingsReady = false
    private var permissionGranted = false
    private var phase = Phase.Preparing
    private var pressedAt = 0L
    private var watchdog: Runnable? = null
    private var autoFinish: Runnable? = null

    private enum class Phase {
        Preparing,
        Ready,
        Listening,
        Finalizing,
        Saving,
        Saved,
        Error,
        Permission,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as ExpenseApp
        configureWindow()

        view = QuickCaptureView(this).apply {
            onHoldStart = ::startCapture
            onHoldEnd = ::stopCapture
            onHoldCancel = ::cancelCapture
            onCancelClick = ::cancelAndFinish
            onActionClick = ::openAppSettings
        }
        setContentView(view)

        speech = SpeechRecognizerManager(applicationContext)
        scope.launch {
            speech.state.collect(::onSpeechState)
        }

        // The AppContainer is lazy. Read settings off the main thread so the first transparent
        // frame is not delayed by Room/DataStore construction.
        scope.launch {
            settings = withContext(Dispatchers.Default) {
                runCatching {
                    app.container.settingsRepository.settings.first()
                }.getOrDefault(AppSettings())
            }
            settingsReady = true
            maybeBecomeReady()
        }

        if (hasMicrophonePermission()) {
            permissionGranted = true
            // Bind the recognition service only after the overlay has drawn its first frame.
            Choreographer.getInstance().postFrameCallback {
                if (!isFinishing && !isDestroyed) prepareSpeech()
            }
        } else {
            phase = Phase.Permission
            view.showPermissionRequest()
            requestMicrophonePermission()
        }
    }

    // ------------------------------------------------------------------ capture

    private fun startCapture() {
        if (phase != Phase.Ready && phase != Phase.Error) return
        if (!permissionGranted || !settingsReady) return

        clearWatchdog()
        clearAutoFinish()
        phase = Phase.Listening
        pressedAt = SystemClock.uptimeMillis()
        view.showListening("", 0f)
        speech.start(settings.speechLanguage)

        // The system recognizer normally times out on its own; this is only a safety net for a
        // stuck recognition service. It is removed as soon as a result/error arrives.
        watchdog = Runnable {
            if (phase == Phase.Listening) {
                speech.cancel()
                showError("No speech heard", "Hold the microphone and speak, e.g. “340 pizza”.")
            }
        }.also { handler.postDelayed(it, SPEECH_TIMEOUT_MS) }
    }

    private fun stopCapture() {
        if (phase != Phase.Listening) return

        val heldMs = SystemClock.uptimeMillis() - pressedAt
        if (heldMs < MIN_HOLD_MS) {
            // A stray tap should not start a network recognition round-trip.
            speech.cancel()
            clearWatchdog()
            phase = Phase.Ready
            view.showReady()
            return
        }

        clearWatchdog()
        phase = Phase.Finalizing
        view.showFinalizing()
        speech.stop()

        watchdog = Runnable {
            if (phase == Phase.Finalizing) {
                speech.cancel()
                showError("Speech timed out", "Hold the microphone and speak again.")
            }
        }.also { handler.postDelayed(it, FINALIZE_TIMEOUT_MS) }
    }

    private fun cancelCapture() {
        if (phase != Phase.Listening && phase != Phase.Finalizing) return
        speech.cancel()
        clearWatchdog()
        phase = Phase.Ready
        view.showReady()
    }

    // ------------------------------------------------------------------ speech

    private fun onSpeechState(state: SpeechState) {
        when (state) {
            SpeechState.Idle -> Unit

            SpeechState.Preparing -> {
                if (phase == Phase.Listening) view.showListening("", 0f)
            }

            is SpeechState.Listening -> {
                if (phase == Phase.Listening) {
                    val level = ((state.rmsDb + 2f) / 12f).coerceIn(0f, 1f)
                    view.showListening(state.partialText, level)
                }
            }

            is SpeechState.Result -> {
                speech.reset()
                if (phase == Phase.Listening || phase == Phase.Finalizing) {
                    handleResult(state.alternatives)
                }
            }

            is SpeechState.Error -> {
                speech.reset()
                if (phase == Phase.Listening || phase == Phase.Finalizing) {
                    handleError(state)
                }
            }
        }
    }

    private fun handleResult(alternatives: List<String>) {
        clearWatchdog()
        clearAutoFinish()

        val cleaned = alternatives.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) {
            showError("Didn't catch that", "Hold the microphone and speak, e.g. “340 pizza”.")
            return
        }

        // Prefer the first alternative that contains an amount; the recognizer's first result is
        // not always the one our parser understands.
        val transcript = cleaned.firstOrNull { VoiceParser.parse(it) is ParseResult.Success }
            ?: cleaned.first()

        when (val result = VoiceParser.parse(transcript)) {
            is ParseResult.Failure -> {
                showError("Couldn't add expense", failureMessage(result.reason, transcript))
            }

            is ParseResult.Success -> {
                if (result.description.isBlank()) {
                    showError(
                        "What was it for?",
                        "Say the amount and what it was for, e.g. “340 pizza”.",
                    )
                } else {
                    save(result, transcript)
                }
            }
        }
    }

    private fun handleError(error: SpeechState.Error) {
        clearWatchdog()
        clearAutoFinish()

        if (error.code == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            permissionGranted = false
            phase = Phase.Permission
            view.showPermissionRequest()
            requestMicrophonePermission()
            return
        }
        showError("Couldn't add expense", error.message)
    }

    private fun save(result: ParseResult.Success, transcript: String) {
        clearWatchdog()
        clearAutoFinish()
        phase = Phase.Saving

        val settingsSnapshot = settings
        val amountText = MoneyFormatter.format(
            result.amountMinor,
            settingsSnapshot.currencySymbol,
            settingsSnapshot.grouping,
        )
        view.showSaving(amountText, result.description)

        val category = CategoryClassifier.classify(result.description)
        val captureApp = app
        captureApp.appScope.launch {
            try {
                // AppContainer is constructed here, on the app scope, never on the first frame.
                val container = captureApp.container
                container.expenseRepository.add(
                    amountMinor = result.amountMinor,
                    description = result.description,
                    category = category.name,
                    spentAt = System.currentTimeMillis(),
                    rawTranscript = transcript,
                    source = ExpenseSource.VOICE,
                )

                // The widget update is a convenience, never part of the save's success. A broken
                // launcher or RemoteViews inflation must not make the user re-add the expense.
                try {
                    ExpenseWidgetProvider.updateAll(applicationContext, container, settingsSnapshot)
                } catch (_: Exception) {
                    // The widget will refresh on its next update or when the app backgrounds.
                }

                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    phase = Phase.Saved
                    view.showSaved(amountText, result.description)
                    view.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            HapticFeedbackConstants.CONFIRM
                        } else {
                            HapticFeedbackConstants.LONG_PRESS
                        },
                    )
                    scheduleFinish(SAVED_LINGER_MS)
                }
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    showError("Couldn't save that", "Try again. If it keeps failing, open the app.")
                }
            }
        }
    }

    private fun failureMessage(reason: FailureReason, transcript: String): String = when (reason) {
        FailureReason.EMPTY_INPUT -> "Didn't catch that. Hold the microphone and speak again."
        FailureReason.NO_AMOUNT -> "Couldn't find an amount in “$transcript”. Try “340 pizza”."
        FailureReason.ZERO_AMOUNT -> "The amount must be more than zero."
        FailureReason.AMOUNT_TOO_LARGE -> "That amount is too large."
    }

    // ------------------------------------------------------------------ permission

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun prepareSpeech() {
        if (!speech.isAvailable()) {
            phase = Phase.Error
            view.showUnavailable(
                "Speech recognition isn't available on this device. Install or enable the Google app.",
            )
            scheduleFinish(ERROR_LINGER_MS)
            return
        }
        // Binds the system recognition service now, before the first touch.
        speech.prepare()
        maybeBecomeReady()
    }

    private fun maybeBecomeReady() {
        if (phase == Phase.Preparing && permissionGranted && settingsReady) {
            phase = Phase.Ready
            view.showReady()
        }
    }

    private fun requestMicrophonePermission() {
        if (hasMicrophonePermission()) {
            onMicrophoneGranted()
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_MICROPHONE,
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_MICROPHONE) return

        val granted = grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onMicrophoneGranted()
        } else {
            permissionGranted = false
            phase = Phase.Permission
            view.showPermissionDenied()
            scheduleFinish(PERMISSION_LINGER_MS)
        }
    }

    private fun onMicrophoneGranted() {
        if (!started) {
            finish()
            return
        }
        permissionGranted = true
        phase = Phase.Preparing
        view.showPreparing()
        prepareSpeech()
        maybeBecomeReady()
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    // ------------------------------------------------------------------ lifecycle

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        cancelAndFinish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // A second widget tap while a result/error is showing should start a fresh capture, not
        // leave the user on a message that is about to disappear.
        if (phase == Phase.Saved || phase == Phase.Error) {
            clearCallbacks()
            phase = Phase.Ready
            view.showReady()
        }
    }

    override fun onStart() {
        super.onStart()
        started = true
    }

    override fun onStop() {
        started = false
        super.onStop()
        if (isChangingConfigurations) return
        when (phase) {
            Phase.Listening, Phase.Finalizing -> {
                speech.cancel()
                clearCallbacks()
                finish()
            }

            // The system permission dialog and an in-flight background save both outlive a stop.
            Phase.Permission, Phase.Saving -> Unit

            else -> {
                clearCallbacks()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clearCallbacks()
        scope.cancel()
        if (::speech.isInitialized) speech.destroy()
    }

    private fun cancelAndFinish() {
        if (phase == Phase.Listening || phase == Phase.Finalizing) speech.cancel()
        clearCallbacks()
        finish()
    }

    private fun showError(titleText: String, message: String) {
        phase = Phase.Error
        view.showError(titleText, message)
        scheduleFinish(ERROR_LINGER_MS)
    }

    private fun scheduleFinish(delayMillis: Long) {
        clearAutoFinish()
        val runnable = Runnable { finish() }
        autoFinish = runnable
        handler.postDelayed(runnable, delayMillis)
    }

    private fun clearWatchdog() {
        watchdog?.let { handler.removeCallbacks(it) }
        watchdog = null
    }

    private fun clearAutoFinish() {
        autoFinish?.let { handler.removeCallbacks(it) }
        autoFinish = null
    }

    private fun clearCallbacks() {
        clearWatchdog()
        clearAutoFinish()
    }

    // ------------------------------------------------------------------ window

    @Suppress("DEPRECATION")
    private fun configureWindow() {
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private companion object {
        const val REQUEST_MICROPHONE = 41
        const val MIN_HOLD_MS = 280L
        const val SPEECH_TIMEOUT_MS = 12_000L
        const val FINALIZE_TIMEOUT_MS = 7_000L
        const val ERROR_LINGER_MS = 6_000L
        const val PERMISSION_LINGER_MS = 8_000L
        const val SAVED_LINGER_MS = 1_000L
    }
}
