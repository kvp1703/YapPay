package com.yappay.app.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.yappay.app.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

private data class CapturePalette(
    val cardStart: Int,
    val cardEnd: Int,
    val ink: Int,
    val muted: Int,
    val line: Int,
    val gold: Int,
    val gold2: Int,
    val scrim: Int,
    val orbStart: Int,
    val orbEnd: Int,
    val orbInk: Int,
)

private fun palette(context: Context): CapturePalette {
    val night = context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    return if (night) {
        CapturePalette(
            cardStart = 0xFF1C1A17.toInt(),
            cardEnd = 0xFF12110F.toInt(),
            ink = 0xFFF3EFE8.toInt(),
            muted = 0xFF9A9389.toInt(),
            line = 0xFF35312B.toInt(),
            gold = 0xFFD2B47C.toInt(),
            gold2 = 0xFFC7A76B.toInt(),
            scrim = 0xA6000000.toInt(),
            orbStart = 0xFFE8D09C.toInt(),
            orbEnd = 0xFFB8945A.toInt(),
            orbInk = 0xFF16140F.toInt(),
        )
    } else {
        CapturePalette(
            cardStart = 0xFFFFFDF9.toInt(),
            cardEnd = 0xFFF5EEE3.toInt(),
            ink = 0xFF16140F.toInt(),
            muted = 0xFF7D776C.toInt(),
            line = 0xFFE7E1D6.toInt(),
            gold = 0xFF97773F.toInt(),
            gold2 = 0xFFC9A96A.toInt(),
            scrim = 0x6616140F,
            orbStart = 0xFFE8D09C.toInt(),
            orbEnd = 0xFFB8945A.toInt(),
            orbInk = 0xFF16140F.toInt(),
        )
    }
}

private enum class OrbMode { MIC, CHECK }

/**
 * The complete quick-capture UI, written with plain Android views on purpose.
 *
 * Compose is great for the app, but it costs tens of milliseconds and a ComposeView/lifecycle
 * owner that this transient 1-2 second surface does not need. This view is also extremely small:
 * one card, three labels and one custom-drawn orb.
 */
@SuppressLint("SetTextI18n", "ClickableViewAccessibility")
class QuickCaptureView(context: Context) : FrameLayout(context) {

    var onHoldStart: (() -> Unit)? = null
    var onHoldEnd: (() -> Unit)? = null
    var onHoldCancel: (() -> Unit)? = null
    var onCancelClick: (() -> Unit)? = null
    var onActionClick: (() -> Unit)? = null

    private val p = palette(context)
    // Keep the brand serif (70 KB), but use the platform sans faces for the small copy. Loading
    // the 400 KB Inter weights on the pre-first-frame path is not worth the few milliseconds of
    // visual polish in a transient overlay.
    private val serif: Typeface =
        ResourcesCompat.getFont(context, R.font.instrument_serif_regular) ?: Typeface.SERIF
    private val inter: Typeface = Typeface.SANS_SERIF
    private val interMedium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private val card = LinearLayout(context)
    private val eyebrow = TextView(context)
    private val cancel = TextView(context)
    private val orb = MicOrbView(context, p)
    private val title = TextView(context)
    private val subtitle = TextView(context)
    private val hint = TextView(context)
    private val action = TextView(context)

    private var holdEnabled = false
    private var pressed = false
    private var listeningShown = false

    init {
        setBackgroundColor(p.scrim)
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "YapPay. Hold anywhere to speak."

        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.setPadding(dp(22), dp(16), dp(22), dp(18))
        card.elevation = dp(24).toFloat()
        card.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(p.cardStart, p.cardEnd),
        ).apply {
            cornerRadius = dp(30).toFloat()
            setStroke(dp(1), p.line)
        }

        val cardParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        ).apply {
            leftMargin = dp(16)
            rightMargin = dp(16)
            bottomMargin = dp(20)
        }
        addView(card, cardParams)

        buildContent()
        applyInsets(cardParams)
    }

    private fun buildContent() {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        eyebrow.text = "YAPPAY"
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        eyebrow.setTextColor(p.gold)
        eyebrow.typeface = interMedium
        eyebrow.letterSpacing = 0.16f
        eyebrow.includeFontPadding = false
        header.addView(
            eyebrow,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )

        cancel.text = "Cancel"
        cancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        cancel.setTextColor(p.muted)
        cancel.typeface = interMedium
        cancel.gravity = Gravity.CENTER
        cancel.setPadding(dp(12), dp(4), 0, dp(4))
        cancel.isClickable = true
        cancel.isFocusable = true
        cancel.contentDescription = "Cancel voice capture"
        cancel.setOnClickListener { onCancelClick?.invoke() }
        header.addView(cancel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        card.addView(header, linearParams(LinearLayout.LayoutParams.MATCH_PARENT))

        orb.contentDescription = "Microphone"
        orb.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        card.addView(
            orb,
            LinearLayout.LayoutParams(dp(112), dp(112)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(8)
            },
        )

        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 27f)
        title.setTextColor(p.ink)
        title.typeface = serif
        title.gravity = Gravity.CENTER
        title.includeFontPadding = false
        title.maxLines = 2
        title.ellipsize = TextUtils.TruncateAt.END
        title.setLineSpacing(dp(2).toFloat(), 1f)
        card.addView(title, linearParams(LinearLayout.LayoutParams.MATCH_PARENT))

        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        subtitle.setTextColor(p.muted)
        subtitle.typeface = inter
        subtitle.gravity = Gravity.CENTER
        subtitle.maxLines = 3
        subtitle.ellipsize = TextUtils.TruncateAt.END
        subtitle.setLineSpacing(dp(4).toFloat(), 1f)
        card.addView(
            subtitle,
            linearParams(LinearLayout.LayoutParams.MATCH_PARENT).apply {
                leftMargin = dp(4)
                rightMargin = dp(4)
                topMargin = dp(6)
            },
        )

        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
        hint.setTextColor(p.gold)
        hint.typeface = interMedium
        hint.gravity = Gravity.CENTER
        hint.letterSpacing = 0.14f
        hint.includeFontPadding = false
        hint.maxLines = 1
        hint.ellipsize = TextUtils.TruncateAt.END
        card.addView(
            hint,
            linearParams(LinearLayout.LayoutParams.MATCH_PARENT).apply {
                topMargin = dp(12)
            },
        )

        action.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        action.setTextColor(p.gold)
        action.typeface = interMedium
        action.gravity = Gravity.CENTER
        action.setPadding(dp(12), dp(9), dp(12), dp(9))
        action.isClickable = true
        action.isFocusable = true
        action.visibility = GONE
        action.setOnClickListener { onActionClick?.invoke() }
        card.addView(
            action,
            linearParams(LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            },
        )

        showPreparing()
    }

    private fun applyInsets(cardParams: LayoutParams) {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            cardParams.leftMargin = dp(16) + bars.left
            cardParams.rightMargin = dp(16) + bars.right
            cardParams.bottomMargin = dp(20) + bars.bottom
            card.layoutParams = cardParams
            insets
        }
        ViewCompat.requestApplyInsets(this)
    }

    // ------------------------------------------------------------------ states

    fun showPreparing() {
        render(
            titleText = "One moment",
            subtitleText = "Getting the microphone ready…",
            hintText = "",
            enabled = false,
            mode = OrbMode.MIC,
        )
    }

    fun showReady() {
        render(
            titleText = "Hold to speak",
            subtitleText = "Say the amount, then what it was for.\ne.g. “340 pizza”",
            hintText = "Hold anywhere · auto-saves",
            enabled = true,
            mode = OrbMode.MIC,
        )
    }

    fun showListening(partialText: String, level: Float) {
        holdEnabled = false
        orb.setMode(OrbMode.MIC)
        if (!listeningShown) {
            listeningShown = true
            title.text = "Listening…"
            hint.text = "RELEASE TO SAVE"
            hint.visibility = VISIBLE
            action.visibility = GONE
            cancel.text = "Cancel"
            contentDescription = "Listening. Release to save."
            orb.setLevel(0f)
        }
        val display = partialText.ifBlank { "Say the amount, then what it was for." }
        if (subtitle.text.toString() != display) subtitle.text = display
        orb.setLevel(level)
    }

    fun showFinalizing() {
        render(
            titleText = "Got it",
            subtitleText = "Finishing…",
            hintText = "",
            enabled = false,
            mode = OrbMode.MIC,
        )
    }

    fun showSaving(amount: String, description: String) {
        render(
            titleText = "Saving",
            subtitleText = "$amount · $description",
            hintText = "",
            enabled = false,
            mode = OrbMode.MIC,
        )
    }

    fun showSaved(amount: String, description: String) {
        render(
            titleText = "Saved",
            subtitleText = "$amount · $description",
            hintText = "Just now",
            enabled = false,
            mode = OrbMode.CHECK,
        )
    }

    fun showError(titleText: String, message: String, actionLabel: String? = null) {
        render(
            titleText = titleText,
            subtitleText = message,
            hintText = "Hold to try again",
            enabled = true,
            mode = OrbMode.MIC,
            actionLabel = actionLabel,
        )
    }

    fun showPermissionDenied() {
        render(
            titleText = "Microphone blocked",
            subtitleText = "Enable microphone access in Settings to use the widget.",
            hintText = "Tap settings to allow",
            enabled = false,
            mode = OrbMode.MIC,
            actionLabel = "Open settings",
        )
    }

    fun showPermissionRequest() {
        render(
            titleText = "Microphone needed",
            subtitleText = "Allow microphone access to add expenses by voice.",
            hintText = "Waiting for permission",
            enabled = false,
            mode = OrbMode.MIC,
        )
    }

    fun showUnavailable(message: String) {
        render(
            titleText = "Speech unavailable",
            subtitleText = message,
            hintText = "",
            enabled = false,
            mode = OrbMode.MIC,
        )
    }

    private fun render(
        titleText: String,
        subtitleText: String,
        hintText: String,
        enabled: Boolean,
        mode: OrbMode,
        actionLabel: String? = null,
    ) {
        listeningShown = false
        holdEnabled = enabled
        title.text = titleText
        subtitle.text = subtitleText
        hint.text = hintText.uppercase(Locale.ENGLISH)
        hint.visibility = if (hintText.isBlank()) GONE else VISIBLE
        action.visibility = if (actionLabel == null) GONE else VISIBLE
        action.text = actionLabel.orEmpty()
        cancel.text = if (enabled) "Cancel" else "Close"
        contentDescription = listOf(titleText, subtitleText, hintText)
            .filter { it.isNotBlank() }
            .joinToString(". ")
        orb.setMode(mode)
        orb.setLevel(0f)
    }

    // ------------------------------------------------------------------ touch

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!holdEnabled) return true
                pressed = true
                orb.setPressedState(true)
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onHoldStart?.invoke()
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (pressed) {
                    pressed = false
                    orb.setPressedState(false)
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onHoldEnd?.invoke()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (pressed) {
                    pressed = false
                    orb.setPressedState(false)
                    onHoldCancel?.invoke()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> return true
        }
        return true
    }

    // ------------------------------------------------------------------ helpers

    private fun linearParams(width: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}

@SuppressLint("ViewConstructor")
private class MicOrbView(
    context: Context,
    private val p: CapturePalette,
) : View(context) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val micIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_widget_mic)
    private val checkIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_widget_check)
    private var fillShader: Shader? = null
    private var highlightShader: Shader? = null
    private var level = 0f
    private var pressed = false
    private var mode = OrbMode.MIC

    init {
        micIcon?.let { DrawableCompat.setTint(it, p.orbInk) }
        checkIcon?.let { DrawableCompat.setTint(it, p.orbInk) }
    }

    fun setMode(value: OrbMode) {
        if (mode == value) return
        mode = value
        invalidate()
    }

    fun setLevel(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (abs(clamped - level) < 0.015f) return
        level = clamped
        invalidate()
    }

    fun setPressedState(value: Boolean) {
        if (pressed == value) return
        pressed = value
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val radius = min(w, h) / 2f - dp(1f)
        val cx = w / 2f
        val cy = h / 2f
        fillShader = LinearGradient(
            cx - radius,
            cy - radius,
            cx + radius,
            cy + radius,
            p.orbStart,
            p.orbEnd,
            Shader.TileMode.CLAMP,
        )
        highlightShader = RadialGradient(
            cx,
            cy - radius * 0.55f,
            radius * 1.15f,
            intArrayOf(0x66FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = min(width, height) / 2f - dp(1.5f)
        val active = if (mode == OrbMode.MIC) level else 0f

        ringPaint.color = withAlpha(p.gold2, 0.18f + 0.50f * active)
        ringPaint.strokeWidth = dp(1.2f)
        canvas.drawCircle(cx, cy, maxRadius * (0.97f + 0.035f * active), ringPaint)

        ringPaint.color = withAlpha(p.gold2, 0.30f + 0.30f * active)
        ringPaint.strokeWidth = dp(1f)
        canvas.drawCircle(cx, cy, maxRadius * 0.86f, ringPaint)

        val scale = if (pressed) 0.93f else 1f
        canvas.save()
        canvas.scale(scale, scale, cx, cy)
        fillPaint.shader = fillShader
        canvas.drawCircle(cx, cy, maxRadius * 0.83f, fillPaint)
        highlightPaint.shader = highlightShader
        canvas.drawCircle(cx, cy, maxRadius * 0.83f, highlightPaint)
        canvas.restore()

        val icon = if (mode == OrbMode.CHECK) checkIcon else micIcon
        val size = (maxRadius * 0.90f).toInt()
        icon?.setBounds(
            (width - size) / 2,
            (height - size) / 2,
            (width + size) / 2,
            (height + size) / 2,
        )
        icon?.draw(canvas)
    }

    private fun withAlpha(color: Int, alpha: Float): Int = Color.argb(
        (alpha.coerceIn(0f, 1f) * 255).toInt(),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
