package com.yappay.app.ui.addedit

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yappay.app.data.ExpenseSource
import com.yappay.app.domain.Category
import com.yappay.app.domain.CategoryRef
import com.yappay.app.ui.components.AtelierButton
import com.yappay.app.ui.components.AtelierButtonKind
import com.yappay.app.ui.components.AtelierSnackbarHost
import com.yappay.app.ui.components.CategoryEditorDialog
import com.yappay.app.ui.components.CategoryTile
import com.yappay.app.ui.components.CircleAction
import com.yappay.app.ui.components.EntryBanner
import com.yappay.app.ui.components.EntryBannerKind
import com.yappay.app.ui.components.ExpenseEntryBody
import com.yappay.app.ui.components.money
import com.yappay.app.ui.theme.AtelierTheme
import com.yappay.app.ui.theme.InterFamily
import com.yappay.app.ui.theme.serifStyle
import com.yappay.app.ui.voice.VoiceEntryDialogs
import com.yappay.app.ui.voice.VoiceEntryViewModel
import com.yappay.app.ui.voice.rememberMicClickHandler
import com.yappay.app.util.DateRanges
import com.yappay.app.util.MoneyFormatter
import java.time.Instant
import java.time.ZoneId

@Composable
fun AddEditExpenseScreen(onDone: () -> Unit) {
    val vm: AddEditExpenseViewModel = viewModel(factory = AddEditExpenseViewModel.Factory)
    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = AtelierTheme.colors

    var confirmDelete by remember { mutableStateOf(false) }
    var showCategoryEditor by remember { mutableStateOf(false) }
    var showKeypad by rememberSaveable { mutableStateOf(true) }
    var voiceLaunched by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val voiceVm: VoiceEntryViewModel = viewModel(factory = VoiceEntryViewModel.Factory)
    val onMicClick = rememberMicClickHandler(voiceVm, snackbarHostState)

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                AddEditEvent.Finished -> onDone()
                AddEditEvent.NotFound -> {
                    Toast.makeText(context, "Expense not found", Toast.LENGTH_SHORT).show()
                    onDone()
                }
                is AddEditEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    LaunchedEffect(ui.isLoading, ui.isEdit) {
        if (!ui.isLoading) showKeypad = !ui.isEdit
    }

    Scaffold(
        containerColor = c.bg,
        snackbarHost = { AtelierSnackbarHost(snackbarHostState, bottomPadding = 24.dp) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg)
                .padding(innerPadding),
        ) {
            if (ui.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.gold)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    AddEditHeader(
                        isEdit = ui.isEdit,
                        onBack = onDone,
                        onDelete = { confirmDelete = true },
                        onSayIt = {
                            voiceLaunched = true
                            onMicClick()
                        },
                    )

                    val banner = when {
                        !ui.isEdit -> EntryBanner(
                            kind = EntryBannerKind.Info,
                            icon = Icons.Rounded.AutoAwesome,
                            text = "Faster by voice — tap the mic and say “340 pizza”.",
                        )
                        ui.existing?.rawTranscript != null -> {
                            val time = DateRanges.TIME.format(
                                Instant.ofEpochMilli(ui.existing!!.spentAt)
                                    .atZone(ZoneId.systemDefault()),
                            )
                            EntryBanner(
                                kind = EntryBannerKind.Info,
                                icon = Icons.Rounded.GraphicEq,
                                text = "Added by voice at $time · heard “${ui.existing!!.rawTranscript}”",
                            )
                        }
                        else -> null
                    }

                    ExpenseEntryBody(
                        amountText = ui.amountText,
                        onAmountChange = vm::onAmountChange,
                        description = ui.description,
                        onDescriptionChange = vm::onDescriptionChange,
                        category = ui.category,
                        onCategoryChange = vm::onCategoryChange,
                        date = ui.date,
                        onDateChange = vm::onDateChange,
                        currencySymbol = settings.currencySymbol,
                        canSave = ui.canSave,
                        saveLabel = saveLabel(ui, settings.currencySymbol, settings.grouping),
                        onSave = vm::save,
                        showKeypad = showKeypad,
                        onToggleKeypad = { showKeypad = it },
                        fromVoice = ui.existing?.source == ExpenseSource.VOICE,
                        categoryOptions = CategoryRef.all(ui.customCategories),
                        onCreateCategory = { showCategoryEditor = true },
                        prompt = if (ui.isEdit) "Amount" else "How much was it?",
                        banner = banner,
                        onDescriptionFocusChange = { focused ->
                            if (focused) showKeypad = false
                        },
                    )
                }
            }

            VoiceEntryDialogs(
                viewModel = voiceVm,
                snackbarHostState = snackbarHostState,
                onSaved = { if (voiceLaunched) onDone() },
            )
        }
    }

    if (confirmDelete) {
        ui.existing?.let { existing ->
            DeleteExpenseDialog(
                onDismiss = { confirmDelete = false },
                onDelete = {
                    confirmDelete = false
                    vm.delete()
                },
                description = existing.description,
                amountMinor = existing.amountMinor,
                category = CategoryRef.resolve(existing.category, ui.customCategories),
                isVoice = existing.source == ExpenseSource.VOICE,
                date = DateRanges.DAY_HEADER.format(DateRanges.toLocalDate(existing.spentAt)),
                time = DateRanges.TIME.format(Instant.ofEpochMilli(existing.spentAt).atZone(ZoneId.systemDefault())),
                settings = settings.currencySymbol to settings.grouping,
            )
        }
    }

    if (showCategoryEditor) {
        CategoryEditorDialog(
            title = "New category",
            existingNames = Category.entries.map { it.displayName } + ui.customCategories.map { it.label },
            onDismiss = { showCategoryEditor = false },
            onSave = { name, color ->
                showCategoryEditor = false
                vm.addCategory(name, color)
            },
        )
    }
}

private fun saveLabel(ui: AddEditUiState, symbol: String, grouping: com.yappay.app.util.GroupingStyle): String {
    val amount = ui.amountMinor
    return if (ui.canSave && amount != null) "Save ${MoneyFormatter.format(amount, symbol, grouping)}" else "Save"
}

@Composable
private fun AddEditHeader(
    isEdit: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSayIt: () -> Unit,
) {
    val c = AtelierTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircleAction(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
        )
        Text(
            text = if (isEdit) "Edit expense" else "Add expense",
            color = c.ink,
            style = serifStyle(27f, 27f),
            modifier = Modifier.weight(1f),
        )
        if (isEdit) {
            CircleAction(
                icon = Icons.Rounded.Delete,
                contentDescription = "Delete",
                onClick = onDelete,
            )
        } else {
            SayItPill(onClick = onSayIt)
        }
    }
}

@Composable
fun SayItPill(onClick: () -> Unit) {
    val c = AtelierTheme.colors
    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(c.goldSoft)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.Mic, contentDescription = null, tint = c.gold, modifier = Modifier.size(18.dp))
        Text(
            text = "Say it",
            color = c.gold,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun DeleteExpenseDialog(
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    description: String,
    amountMinor: Long,
    category: CategoryRef,
    isVoice: Boolean,
    date: String,
    time: String,
    settings: Pair<String, com.yappay.app.util.GroupingStyle>,
) {
    val c = AtelierTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.scrim)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(c.card)
                    .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(c.errSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = c.err,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Delete this expense?",
                    color = c.ink,
                    style = serifStyle(28f, 31f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.bg)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CategoryTile(category = category, size = 38.dp, voice = isVoice)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = description,
                            color = c.ink,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "$date · $time",
                            color = c.muted,
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                        )
                    }
                    Text(
                        text = MoneyFormatter.format(amountMinor, settings.first, settings.second),
                        color = c.ink,
                        style = serifStyle(22f, 22f),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AtelierButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        kind = AtelierButtonKind.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    AtelierButton(
                        text = "Delete",
                        onClick = onDelete,
                        kind = AtelierButtonKind.Danger,
                        leadingIcon = Icons.Rounded.Delete,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
