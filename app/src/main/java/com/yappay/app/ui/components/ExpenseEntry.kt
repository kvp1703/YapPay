package com.yappay.app.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yappay.app.domain.CategoryRef
import com.yappay.app.ui.theme.AtelierTheme
import com.yappay.app.ui.theme.InterFamily
import com.yappay.app.ui.theme.serifStyle
import com.yappay.app.util.DateRanges
import java.time.LocalDate

enum class EntryBannerKind { Info, Error }

data class EntryBanner(
    val kind: EntryBannerKind,
    val icon: ImageVector,
    val text: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEntryBody(
    amountText: String,
    onAmountChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    category: CategoryRef,
    onCategoryChange: (CategoryRef) -> Unit,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    currencySymbol: String,
    canSave: Boolean,
    saveLabel: String,
    onSave: () -> Unit,
    showKeypad: Boolean,
    onToggleKeypad: (Boolean) -> Unit,
    fromVoice: Boolean,
    modifier: Modifier = Modifier,
    categoryOptions: List<CategoryRef> = CategoryRef.all(),
    onCreateCategory: (() -> Unit)? = null,
    prompt: String = "How much was it?",
    banner: EntryBanner? = null,
    onDescriptionFocusChange: (Boolean) -> Unit = {},
) {
    val c = AtelierTheme.colors
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (banner != null) {
                EntryBannerRow(banner = banner)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleKeypad(true) }
                    .padding(top = 6.dp, bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = prompt,
                    color = c.muted,
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.height(68.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = currencySymbol,
                        color = c.gold,
                        style = serifStyle(32f, 32f),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = amountText.ifBlank { "0" },
                        color = if (amountText.isBlank()) c.faint else c.ink,
                        style = serifStyle(64f, 64f, letterSpacing = -0.025f),
                        maxLines = 1,
                    )
                    if (showKeypad) {
                        BlinkingCaret()
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            DescriptionField(
                description = description,
                onDescriptionChange = onDescriptionChange,
                category = category,
                fromVoice = fromVoice,
                onDescriptionFocusChange = onDescriptionFocusChange,
            )

            Eyebrow(
                text = "Category",
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 6.dp),
            )
            CategoryChips(
                selected = category,
                options = categoryOptions,
                onSelect = onCategoryChange,
                onCreate = onCreateCategory,
            )

            Eyebrow(
                text = "When",
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 6.dp),
            )
            WhenChips(
                date = date,
                onDateChange = onDateChange,
                onPickDate = { showDatePicker = true },
            )
            Spacer(Modifier.height(8.dp))
        }

        AtelierButton(
            text = saveLabel,
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
        )

        if (showKeypad) {
            Spacer(Modifier.height(10.dp))
            AmountKeypad(
                onDigit = { digit ->
                    val value = amountText
                    if (value.length >= 9) return@AmountKeypad
                    val decimals = value.substringAfter('.', "").length
                    if (value.contains('.') && decimals >= 2) return@AmountKeypad
                    onAmountChange(if (value == "0") digit else value + digit)
                },
                onDecimal = {
                    if (!amountText.contains('.')) {
                        onAmountChange((if (amountText.isBlank()) "0" else amountText) + ".")
                    }
                },
                onBackspace = {
                    if (amountText.isNotEmpty()) onAmountChange(amountText.dropLast(1))
                },
                onDone = { onToggleKeypad(false) },
            )
        } else {
            Spacer(Modifier.height(34.dp))
        }
    }

    if (showDatePicker) {
        val todayPicker = DateRanges.toPickerMillis(LocalDate.now())
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateRanges.toPickerMillis(date),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayPicker
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateChange(DateRanges.fromPickerMillis(it)) }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun EntryBannerRow(banner: EntryBanner, modifier: Modifier = Modifier) {
    val c = AtelierTheme.colors
    val background = if (banner.kind == EntryBannerKind.Error) c.errSoft else c.goldSoft
    val foreground = if (banner.kind == EntryBannerKind.Error) c.errInk else c.goldInk
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = banner.icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
        )
        Text(
            text = banner.text,
            color = foreground,
            fontFamily = InterFamily,
            fontSize = 12.5.sp,
            lineHeight = 18.1.sp,
        )
    }
}

@Composable
private fun DescriptionField(
    description: String,
    onDescriptionChange: (String) -> Unit,
    category: CategoryRef,
    fromVoice: Boolean,
    onDescriptionFocusChange: (Boolean) -> Unit,
) {
    val c = AtelierTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .border(1.dp, c.line, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryTile(category = category, size = 40.dp, voice = fromVoice)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Spent on",
                color = c.muted,
                fontFamily = InterFamily,
                fontSize = 11.5.sp,
            )
            Spacer(Modifier.height(2.dp))
            BasicTextField(
                value = description,
                onValueChange = { onDescriptionChange(it.take(100)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onDescriptionFocusChange(it.isFocused) },
                textStyle = interStyleLocal(
                    color = c.ink,
                    size = 15f,
                    weight = FontWeight.Medium,
                ),
                singleLine = true,
                cursorBrush = SolidColor(c.gold),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { inner ->
                    if (description.isBlank()) {
                        Text(
                            text = "What was it for?",
                            color = c.faint,
                            fontFamily = InterFamily,
                            fontSize = 15.sp,
                        )
                    }
                    inner()
                },
            )
        }
        if (fromVoice) {
            Row(
                modifier = Modifier
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.goldSoft)
                    .padding(start = 7.dp, end = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = null, tint = c.gold, modifier = Modifier.size(13.dp))
                Text(
                    text = "From voice",
                    color = c.gold,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CategoryChips(
    selected: CategoryRef,
    options: List<CategoryRef>,
    onSelect: (CategoryRef) -> Unit,
    onCreate: (() -> Unit)?,
) {
    val c = AtelierTheme.colors
    val categories = remember(selected, options) {
        listOf(selected) + options.filter { it.key != selected.key }
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.key }) { category ->
            val on = category.key == selected.key
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(if (on) c.ink else c.card)
                    .then(if (on) Modifier else Modifier.border(1.dp, c.line, CircleShape))
                    .clickable { onSelect(category) }
                    .padding(start = 5.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryTile(category = category, size = 26.dp, onInk = on)
                Text(
                    text = category.label,
                    color = if (on) c.bg else c.ink,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onCreate != null) {
            item(key = "new_category") {
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, c.line2, CircleShape)
                        .clickable(onClick = onCreate)
                        .padding(start = 10.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = c.gold,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "New category",
                        color = c.gold,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun WhenChips(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onPickDate: () -> Unit,
) {
    val c = AtelierTheme.colors
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val isOther = date != today && date != yesterday
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            WhenChip(
                text = "Today",
                selected = date == today,
                onClick = { onDateChange(today) },
            )
        }
        item {
            WhenChip(
                text = "Yesterday",
                selected = date == yesterday,
                onClick = { onDateChange(yesterday) },
            )
        }
        item {
            WhenChip(
                text = if (isOther) DateRanges.DAY_HEADER.format(date) else "Pick date",
                selected = isOther,
                onClick = onPickDate,
                icon = Icons.Rounded.CalendarMonth,
            )
        }
    }
}

@Composable
private fun WhenChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    val c = AtelierTheme.colors
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) c.ink else c.card)
            .then(if (selected) Modifier else Modifier.border(1.dp, c.line, CircleShape))
            .clickable(onClick = onClick)
            .padding(horizontal = if (icon == null) 15.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) c.bg else c.ink2,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text,
            color = if (selected) c.bg else c.ink,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun BlinkingCaret() {
    val c = AtelierTheme.colors
    val alpha by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(550), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse),
        label = "caretAlpha",
    )
    Box(
        modifier = Modifier
            .padding(start = 5.dp)
            .width(2.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(c.gold.copy(alpha = alpha)),
    )
}

@Composable
private fun AmountKeypad(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onDone: () -> Unit,
) {
    val c = AtelierTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.kb)
            .border(1.dp, c.line.copy(alpha = 0.7f))
            .padding(start = 10.dp, end = 10.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        KeyRow {
            KeyButton("1", Modifier.weight(1f)) { onDigit("1") }
            KeyButton("2", Modifier.weight(1f)) { onDigit("2") }
            KeyButton("3", Modifier.weight(1f)) { onDigit("3") }
            KeyButton(
                icon = Icons.AutoMirrored.Rounded.Backspace,
                modifier = Modifier.weight(1f),
                fn = true,
                onClick = onBackspace,
            )
        }
        KeyRow {
            KeyButton("4", Modifier.weight(1f)) { onDigit("4") }
            KeyButton("5", Modifier.weight(1f)) { onDigit("5") }
            KeyButton("6", Modifier.weight(1f)) { onDigit("6") }
            KeySpacer(Modifier.weight(1f))
        }
        KeyRow {
            KeyButton("7", Modifier.weight(1f)) { onDigit("7") }
            KeyButton("8", Modifier.weight(1f)) { onDigit("8") }
            KeyButton("9", Modifier.weight(1f)) { onDigit("9") }
            KeySpacer(Modifier.weight(1f))
        }
        KeyRow {
            KeySpacer(Modifier.weight(1f))
            KeyButton("0", Modifier.weight(1f)) { onDigit("0") }
            KeyButton(".", Modifier.weight(1f), fn = true, onClick = onDecimal)
            KeyButton(
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                modifier = Modifier.weight(1f),
                go = true,
                onClick = onDone,
            )
        }
    }
}

@Composable
private fun KeyRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        content = content,
    )
}

@Composable
private fun KeyButton(
    label: String = "",
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    fn: Boolean = false,
    go: Boolean = false,
    onClick: () -> Unit,
) {
    val c = AtelierTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "keyScale")
    val background = when {
        pressed -> c.gold
        go -> c.ink
        fn -> Color.Transparent
        else -> c.key
    }
    val foreground = when {
        pressed -> Color.White
        go -> c.bg
        fn -> c.ink2
        else -> c.ink
    }
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(43.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(22.dp))
        } else {
            Text(
                text = label,
                color = foreground,
                fontFamily = InterFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun KeySpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.height(43.dp))
}

private fun interStyleLocal(
    color: Color,
    size: Float,
    weight: FontWeight,
) = androidx.compose.ui.text.TextStyle(
    color = color,
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = (-0.09).sp,
)

@Composable
private fun EyebrowLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = AtelierTheme.colors.muted,
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        letterSpacing = 1.68.sp,
    )
}
