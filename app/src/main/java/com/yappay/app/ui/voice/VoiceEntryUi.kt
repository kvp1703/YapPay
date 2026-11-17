package com.yappay.app.ui.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yappay.app.data.AppSettings
import com.yappay.app.domain.CategoryRef
import com.yappay.app.ui.addedit.SayItPill
import com.yappay.app.ui.components.AtelierButton
import com.yappay.app.ui.components.AtelierButtonKind
import com.yappay.app.ui.components.AtelierSnackbarBubble
import com.yappay.app.ui.components.CategoryTile
import com.yappay.app.ui.components.CircleAction
import com.yappay.app.ui.components.DashedPill
import com.yappay.app.ui.components.EntryBanner
import com.yappay.app.ui.components.EntryBannerKind
import com.yappay.app.ui.components.ExpenseEntryBody
import com.yappay.app.ui.components.money
import com.yappay.app.ui.theme.AtelierTheme
import com.yappay.app.ui.theme.InstrumentSerif
import com.yappay.app.ui.theme.InterFamily
import com.yappay.app.ui.theme.serifStyle
import com.yappay.app.util.DateRanges
import com.yappay.app.util.MoneyFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun rememberMicClickHandler(
    viewModel: VoiceEntryViewModel,
    snackbarHostState: SnackbarHostState,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.startListening()
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Microphone permission is needed to add expenses by voice.",
                    actionLabel = "Settings",
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }
    }
    return {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startListening() else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

@Composable
fun VoiceEntryDialogs(
    viewModel: VoiceEntryViewModel,
    snackbarHostState: SnackbarHostState,
    onSaved: (Long) -> Unit = {},
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val categories by viewModel.categoryOptions.collectAsStateWithLifecycle()
    var saved by remember { mutableStateOf<VoiceEvent.Saved?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is VoiceEvent.Saved -> {
                    saved = event
                    onSaved(event.expenseId)
                }
            }
        }
    }

    LaunchedEffect(saved) {
        if (saved != null) {
            delay(5_000)
            saved = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = ui) {
            VoiceUiState.Idle -> Unit
            is VoiceUiState.Listening -> ListeningOverlay(
                state = state,
                settings = settings,
                onDone = viewModel::stopListening,
                onCancel = viewModel::cancelListening,
            )
            is VoiceUiState.Failed -> FailedDialog(
                state = state,
                onRetry = viewModel::onRetry,
                onTypeInstead = viewModel::onTypeInstead,
                onDismiss = viewModel::dismiss,
            )
            is VoiceUiState.Confirm -> {
                if (state.typeInstead) {
                    TypeInsteadOverlay(
                        state = state,
                        settings = settings,
                        categories = categories,
                        viewModel = viewModel,
                    )
                } else {
                    ConfirmOverlay(
                        state = state,
                        settings = settings,
                        categories = categories,
                        viewModel = viewModel,
                    )
                }
            }
        }

        saved?.let { event ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 134.dp),
            ) {
                AtelierSnackbarBubble(
                    message = event.message,
                    actionLabel = "Undo",
                    onAction = {
                        viewModel.undoSave(event.expenseId)
                        saved = null
                    },
                    icon = Icons.Rounded.Check,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Kept for source compatibility with older Home screens. */
@Composable
fun MicFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    CircleAction(
        icon = Icons.Rounded.Mic,
        contentDescription = "Add expense by voice",
        onClick = onClick,
        modifier = modifier,
        size = 64.dp,
    )
}

// ------------------------------------------------------------------ listening

@Composable
private fun ListeningOverlay(
    state: VoiceUiState.Listening,
    settings: AppSettings,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val c = AtelierTheme.colors
    val live = state.partialText.isNotBlank()
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(live) {
        seconds = 0
        if (live) {
            while (true) {
                delay(1_000)
                seconds++
            }
        }
    }
    val timer = "0:" + seconds.coerceAtLeast(0).toString().padStart(2, '0')

    FullscreenDialog(onDismissRequest = onCancel) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(c.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCancel,
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 128.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(c.card)
                    .noRippleClick {},
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(start = 22.dp, end = 22.dp, bottom = 50.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(c.line2),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            PulsingDot(active = live)
                            Text(
                                text = if (live) "Listening" else "Ready when you are",
                                color = c.ink,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                            )
                        }
                        Text(
                            text = timer,
                            color = c.muted,
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.width(12.dp))
                        CircleAction(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Cancel",
                            onClick = onCancel,
                            size = 34.dp,
                        )
                    }

                    ListeningOrb(live = live, level = state.level, onClick = onDone)
                    LiveWaveform(live = live)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 104.dp)
                            .padding(top = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (live) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    text = state.partialText,
                                    color = c.ink,
                                    style = serifStyle(31f, 35.7f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 300.dp),
                                )
                                BlinkingCaret(height = 26.dp)
                            }
                        } else {
                            Text(
                                text = "Say the amount, then what it was for.",
                                color = c.ink2,
                                style = serifStyle(25f, 30f, italic = true, letterSpacing = 0f),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                modifier = Modifier.widthIn(max = 270.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "e.g. “340 pizza”",
                                color = c.muted,
                                fontFamily = InterFamily,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    Understood(preview = state.preview, live = live, settings = settings)
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AtelierButton(
                            text = "Cancel",
                            onClick = onCancel,
                            kind = AtelierButtonKind.Secondary,
                            modifier = Modifier.weight(1f),
                        )
                        AtelierButton(
                            text = "Done",
                            onClick = onDone,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingDot(active: Boolean) {
    val c = AtelierTheme.colors
    val alpha by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(if (active) 800 else 1600), RepeatMode.Reverse),
        label = "dotAlpha",
    )
    val dot = if (active) c.gold else c.faint
    Box(
        modifier = Modifier.size(15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(15.dp)
                .clip(CircleShape)
                .background(dot.copy(alpha = 0.18f)),
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dot.copy(alpha = alpha)),
        )
    }
}

@Composable
private fun ListeningOrb(live: Boolean, level: Float, onClick: () -> Unit) {
    val c = AtelierTheme.colors
    val phase by rememberInfiniteTransition(label = "orb").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (live) 2800 else 3200, easing = LinearEasing)),
        label = "orbPhase",
    )
    val swell by rememberInfiniteTransition(label = "orbSwell").animateFloat(
        initialValue = 1f,
        targetValue = 1.085f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "orbSwellValue",
    )
    val levelAnim by animateFloatAsState(level, tween(120), label = "level")

    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 200.dp)
            .padding(top = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val base = 48.dp.toPx()
            if (live) {
                repeat(3) { index ->
                    val p = (phase + index / 3f) % 1f
                    drawCircle(
                        color = c.gold.copy(alpha = 0.75f * (1f - p)),
                        radius = base * (1f + 1.05f * p),
                        center = center,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            } else {
                val scales = listOf(1.3f, 1.6f, 1.9f)
                scales.forEachIndexed { index, scale ->
                    val breathe = ((sin((phase + index * 0.16f) * 2f * PI.toFloat()) + 1f) / 2f)
                    drawCircle(
                        color = c.line2.copy(alpha = 0.25f + 0.60f * breathe),
                        radius = base * scale,
                        center = center,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    val s = if (live) (swell + levelAnim * 0.04f) else 1f
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(c.mic, c.micEnd)))
                .clickable(onClick = onClick)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width / 2f, 0f),
                            radius = size.height * 0.7f,
                        ),
                        radius = size.height * 0.7f,
                        center = Offset(size.width / 2f, 0f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = "Stop listening",
                tint = c.onMic,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}

@Composable
private fun LiveWaveform(live: Boolean) {
    val c = AtelierTheme.colors
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "wavePhase",
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(top = 4.dp),
    ) {
        val n = 44
        val barWidth = 2.5.dp.toPx()
        val gap = 3.dp.toPx()
        val total = n * barWidth + (n - 1) * gap
        val startX = (size.width - total) / 2f
        repeat(n) { index ->
            val envelope = sin(PI * (index + 0.5) / n).toFloat()
            val baseDp = 4f + envelope * (4f + ((index * 37) % 12))
            val shape = if (live) {
                0.50f + 0.50f * abs(sin(phase + index * 0.55f))
            } else {
                1f
            }
            val factor = if (live) 1.4f * shape else 1f
            val barHeight = (baseDp * factor).coerceIn(3.dp.value, 36.dp.value).dp.toPx()
            drawRoundRect(
                color = if (live) c.gold else c.line2,
                topLeft = Offset(startX + index * (barWidth + gap), (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Understood(preview: VoicePreview?, live: Boolean, settings: AppSettings) {
    val c = AtelierTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = (if (live) "Understood" else "Will be understood").uppercase(),
            color = c.muted,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 1.68.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (live && preview != null) {
                PreviewPill(
                    text = money(preview.amountMinor, settings),
                    serif = true,
                )
                if (preview.description.isNotBlank()) {
                    PreviewPill(text = preview.description)
                }
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.bg)
                        .border(1.dp, c.line, RoundedCornerShape(18.dp))
                        .padding(start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    CategoryTile(category = preview.category, size = 24.dp)
                    Text(
                        text = preview.category.label,
                        color = c.ink,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp,
                    )
                }
            } else {
                DashedPill("Amount")
                DashedPill("Spent on")
                DashedPill("Category")
            }
        }
    }
}

@Composable
private fun PreviewPill(text: String, serif: Boolean = false) {
    val c = AtelierTheme.colors
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(c.bg)
            .border(1.dp, c.line, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = c.ink,
            style = if (serif) serifStyle(21f, 21f) else InterFamily.let {
                androidx.compose.ui.text.TextStyle(
                    fontFamily = it,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                )
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 170.dp),
        )
    }
}

// ------------------------------------------------------------------ confirm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmOverlay(
    state: VoiceUiState.Confirm,
    settings: AppSettings,
    categories: List<CategoryRef>,
    viewModel: VoiceEntryViewModel,
) {
    val c = AtelierTheme.colors
    val form = state.form
    var categoryOpen by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    FullscreenDialog(onDismissRequest = viewModel::dismiss) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(c.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = viewModel::dismiss,
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 180.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(c.card)
                    .noRippleClick {},
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(start = 22.dp, end = 22.dp, bottom = 76.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(c.line2),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Add expense",
                            color = c.ink,
                            style = serifStyle(30f, 30f),
                        )
                        Text(
                            text = "Review".uppercase(),
                            color = c.muted,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp,
                            letterSpacing = 1.68.sp,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                    form.rawTranscript?.let { heard ->
                        Spacer(Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(c.goldSoft)
                                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 11.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = c.gold,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "Heard".uppercase(),
                                    color = c.gold,
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.6.sp,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "“$heard”",
                                color = c.ink,
                                style = serifStyle(20f, 25f, italic = true, letterSpacing = 0f),
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Amount",
                                color = c.muted,
                                fontFamily = InterFamily,
                                fontSize = 12.5.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = settings.currencySymbol,
                                    color = c.gold,
                                    style = serifStyle(28f, 28f),
                                    modifier = Modifier.padding(bottom = 6.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                BasicTextField(
                                    value = form.amountText,
                                    onValueChange = { value ->
                                        viewModel.onAmountChange(value.filter { it.isDigit() || it == '.' }.take(12))
                                    },
                                    modifier = Modifier.weight(1f),
                                    textStyle = serifStyle(52f, 52f, letterSpacing = -0.02f).copy(color = c.ink),
                                    singleLine = true,
                                    cursorBrush = SolidColor(c.gold),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = c.faint,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .size(20.dp),
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(color = c.line, thickness = 1.dp)

                    ConfirmTextRow(
                        label = "Spent on",
                        value = form.description,
                        onValueChange = viewModel::onDescriptionChange,
                    )

                    Box {
                        ConfirmActionRow(
                            label = "Category",
                            onClick = { categoryOpen = true },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CategoryTile(category = form.category, size = 28.dp)
                                Text(
                                    text = form.category.label,
                                    color = c.ink,
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = c.faint,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = categoryOpen,
                            onDismissRequest = { categoryOpen = false },
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            CategoryTile(category = category, size = 26.dp)
                                            Text(
                                                text = category.label,
                                                color = c.ink,
                                                fontFamily = InterFamily,
                                                fontSize = 14.sp,
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.onCategoryChange(category)
                                        categoryOpen = false
                                    },
                                )
                            }
                        }
                    }

                    ConfirmActionRow(
                        label = "Date",
                        onClick = { showDatePicker = true },
                    ) {
                        Text(
                            text = DateRanges.DAY_HEADER.format(DateRanges.toLocalDate(form.spentAt)),
                            color = c.ink,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = c.faint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircleAction(
                            icon = Icons.Rounded.Replay,
                            contentDescription = "Try again",
                            onClick = viewModel::onRetry,
                            size = 54.dp,
                        )
                        AtelierButton(
                            text = "Cancel",
                            onClick = viewModel::dismiss,
                            kind = AtelierButtonKind.Ghost,
                        )
                        AtelierButton(
                            text = "Save",
                            onClick = viewModel::onConfirmSave,
                            enabled = form.canSave,
                            leadingIcon = Icons.Rounded.Check,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        ConfirmDatePicker(
            initial = DateRanges.toLocalDate(form.spentAt),
            onDismiss = { showDatePicker = false },
            onPick = {
                viewModel.onDateChange(it)
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun ConfirmTextRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val c = AtelierTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = c.muted,
            fontFamily = InterFamily,
            fontSize = 12.5.sp,
            modifier = Modifier.width(84.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(100)) },
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = c.ink,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                textAlign = TextAlign.End,
            ),
            singleLine = true,
            cursorBrush = SolidColor(c.gold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
    }
    androidx.compose.material3.HorizontalDivider(color = c.line, thickness = 1.dp)
}

@Composable
private fun ConfirmActionRow(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = AtelierTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = c.muted,
            fontFamily = InterFamily,
            fontSize = 12.5.sp,
            modifier = Modifier.width(84.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
    androidx.compose.material3.HorizontalDivider(color = c.line, thickness = 1.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmDatePicker(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val todayPicker = DateRanges.toPickerMillis(LocalDate.now())
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateRanges.toPickerMillis(initial),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayPicker
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onPick(DateRanges.fromPickerMillis(it)) }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = pickerState)
    }
}

// ------------------------------------------------------------------ type instead

@Composable
private fun TypeInsteadOverlay(
    state: VoiceUiState.Confirm,
    settings: AppSettings,
    categories: List<CategoryRef>,
    viewModel: VoiceEntryViewModel,
) {
    val c = AtelierTheme.colors
    val form = state.form
    var showKeypad by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = viewModel::dismiss,
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(c.bg)
                .noRippleClick {},
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.line2),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircleAction(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Cancel",
                    onClick = viewModel::dismiss,
                )
                Text(
                    text = "Type it in",
                    color = c.ink,
                    style = serifStyle(27f, 27f),
                    modifier = Modifier.weight(1f),
                )
                SayItPill(onClick = viewModel::onRetry)
            }
            val banner = if (state.failureTranscript != null) {
                EntryBanner(
                    kind = EntryBannerKind.Error,
                    icon = Icons.Rounded.MicOff,
                    text = "No amount heard in “${state.failureTranscript}”. We kept the words — just add the amount.",
                )
            } else {
                EntryBanner(
                    kind = EntryBannerKind.Info,
                    icon = Icons.Rounded.AutoAwesome,
                    text = "No amount heard — just add the amount.",
                )
            }
            ExpenseEntryBody(
                amountText = form.amountText,
                onAmountChange = viewModel::onAmountChange,
                description = form.description,
                onDescriptionChange = viewModel::onDescriptionChange,
                category = form.category,
                onCategoryChange = viewModel::onCategoryChange,
                date = DateRanges.toLocalDate(form.spentAt),
                onDateChange = viewModel::onDateChange,
                currencySymbol = settings.currencySymbol,
                canSave = form.canSave,
                saveLabel = form.amountMinor?.let {
                    "Save ${MoneyFormatter.format(it, settings.currencySymbol, settings.grouping)}"
                } ?: "Save",
                onSave = viewModel::onConfirmSave,
                showKeypad = showKeypad,
                onToggleKeypad = { showKeypad = it },
                fromVoice = form.rawTranscript != null,
                categoryOptions = categories,
                banner = banner,
                onDescriptionFocusChange = { focused ->
                    if (focused) showKeypad = false
                },
            )
        }
    }
}

// ------------------------------------------------------------------ failed

@Composable
private fun FailedDialog(
    state: VoiceUiState.Failed,
    onRetry: () -> Unit,
    onTypeInstead: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = AtelierTheme.colors
    val nudge = remember { Animatable(0f) }
    LaunchedEffect(state) {
        nudge.animateTo(-3f, tween(150))
        nudge.animateTo(3f, tween(150))
        nudge.animateTo(0f, tween(150))
    }

    FullscreenDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(c.card)
                    .noRippleClick {}
                    .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(nudge.value.dp.roundToPx(), 0) }
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(c.errSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MicOff,
                        contentDescription = null,
                        tint = c.err,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Couldn’t add expense",
                    color = c.ink,
                    style = serifStyle(28f, 31f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = quoteItalics(state.message),
                    color = c.muted,
                    fontFamily = InterFamily,
                    fontSize = 14.sp,
                    lineHeight = 21.7.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AtelierButton(
                        text = "Type instead",
                        onClick = onTypeInstead,
                        kind = AtelierButtonKind.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    AtelierButton(
                        text = "Try again",
                        onClick = onRetry,
                        leadingIcon = Icons.Rounded.Mic,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun quoteItalics(text: String) = buildAnnotatedString {
    var inQuote = false
    text.forEach { ch ->
        if (ch == '"') {
            inQuote = !inQuote
        } else if (inQuote) {
            withStyle(
                SpanStyle(
                    fontFamily = InstrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                ),
            ) {
                append(ch)
            }
        } else {
            append(ch)
        }
    }
}

// ------------------------------------------------------------------ atoms

@Composable
private fun BlinkingCaret(height: androidx.compose.ui.unit.Dp) {
    val c = AtelierTheme.colors
    val alpha by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .width(2.dp)
            .height(height)
            .clip(RoundedCornerShape(1.dp))
            .background(c.gold.copy(alpha = alpha)),
    )
}

@Composable
private fun FullscreenDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        content()
    }
}

@Composable
private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick,
)
