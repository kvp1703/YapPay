package com.yappay.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yappay.app.data.CURRENCY_SYMBOLS
import com.yappay.app.ui.components.AtelierSnackbarHost
import com.yappay.app.ui.components.CircleAction
import com.yappay.app.ui.components.Eyebrow
import com.yappay.app.ui.theme.AtelierTheme
import com.yappay.app.ui.theme.InterFamily
import com.yappay.app.ui.theme.serifStyle
import com.yappay.app.util.GroupingStyle

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenCategories: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val s by vm.settings.collectAsStateWithLifecycle()
    val c = AtelierTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportTo(uri)
    }

    LaunchedEffect(vm) {
        vm.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = c.bg,
        snackbarHost = { AtelierSnackbarHost(snackbarHostState, bottomPadding = 16.dp) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg)
                .padding(innerPadding),
        ) {
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
                    text = "Settings",
                    color = c.ink,
                    style = serifStyle(27f, 27f),
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 48.dp),
            ) {
                SettingsSection("Currency symbol") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CURRENCY_SYMBOLS.forEach { symbol ->
                            val selected = s.currencySymbol == symbol
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) c.ink else c.bg)
                                    .clickable { vm.setCurrencySymbol(symbol) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = symbol,
                                    color = if (selected) c.bg else c.ink2,
                                    style = serifStyle(24f, 24f),
                                )
                            }
                        }
                    }
                }

                SettingsSection("Number format") {
                    RadioRow(
                        label = "Indian",
                        detail = "1,00,000",
                        selected = s.grouping == GroupingStyle.INDIAN,
                        onClick = { vm.setGrouping(GroupingStyle.INDIAN) },
                    )
                    Divider()
                    RadioRow(
                        label = "International",
                        detail = "100,000",
                        selected = s.grouping == GroupingStyle.INTERNATIONAL,
                        onClick = { vm.setGrouping(GroupingStyle.INTERNATIONAL) },
                    )
                }

                SettingsSection("Speech language") {
                    LanguageRow(
                        label = "English (India)",
                        selected = s.speechLanguage == "en-IN",
                        onClick = { vm.setSpeechLanguage("en-IN") },
                    )
                    Divider()
                    LanguageRow(
                        label = "English (US)",
                        selected = s.speechLanguage == "en-US",
                        onClick = { vm.setSpeechLanguage("en-US") },
                    )
                    Divider()
                    LanguageRow(
                        label = "English (UK)",
                        selected = s.speechLanguage == "en-GB",
                        onClick = { vm.setSpeechLanguage("en-GB") },
                    )
                    Divider()
                    LanguageRow(
                        label = "Hindi (India)",
                        detail = "Numbers must be spoken as digits",
                        selected = s.speechLanguage == "hi-IN",
                        onClick = { vm.setSpeechLanguage("hi-IN") },
                    )
                }

                SettingsSection("Voice") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { vm.setConfirmBeforeSave(!s.confirmBeforeSave) }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Confirm before saving",
                                color = c.ink,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.5.sp,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Show the edit dialog after you speak. Turn off to save instantly with Undo.",
                                color = c.muted,
                                fontFamily = InterFamily,
                                fontSize = 12.5.sp,
                                lineHeight = 18.1.sp,
                            )
                        }
                        AtelierSwitch(checked = s.confirmBeforeSave)
                    }
                }

                SettingsSection("Categories") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable(onClick = onOpenCategories)
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c.goldSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Label,
                                contentDescription = null,
                                tint = c.gold,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom categories",
                                color = c.ink,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.5.sp,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Add your own categories with their own colour.",
                                color = c.muted,
                                fontFamily = InterFamily,
                                fontSize = 12.5.sp,
                                lineHeight = 18.1.sp,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = c.faint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                SettingsSection("Export") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { exportLauncher.launch(vm.suggestedFileName()) }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c.goldSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = c.gold,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            text = "Export all expenses to CSV",
                            color = c.ink,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.5.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = c.faint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                SettingsSection("About") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Version",
                            color = c.ink,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.5.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "1.0 · Atelier",
                            color = c.muted,
                            fontFamily = InterFamily,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val c = AtelierTheme.colors
    Column(modifier = Modifier.padding(top = 22.dp)) {
        Eyebrow(
            text = title,
            color = c.gold,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 10.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun LanguageRow(label: String, detail: String? = null, selected: Boolean, onClick: () -> Unit) {
    RadioRow(label = label, detail = detail, selected = selected, onClick = onClick)
}

@Composable
private fun RadioRow(label: String, detail: String? = null, selected: Boolean, onClick: () -> Unit) {
    val c = AtelierTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = c.ink,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
            )
            if (detail != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = detail,
                    color = c.muted,
                    fontFamily = InterFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 18.1.sp,
                )
            }
        }
        RadioDot(selected = selected)
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val c = AtelierTheme.colors
    Canvas(modifier = Modifier.size(20.dp)) {
        val radius = size.minDimension / 2f - 1.dp.toPx()
        drawCircle(
            color = if (selected) c.gold else c.line2,
            radius = radius,
            style = Stroke(width = 1.5.dp.toPx()),
        )
        if (selected) {
            drawCircle(color = c.card, radius = radius - 3.dp.toPx())
            drawCircle(color = c.gold, radius = radius - 5.5.dp.toPx())
        }
    }
}

@Composable
private fun AtelierSwitch(checked: Boolean) {
    val c = AtelierTheme.colors
    val offset by animateDpAsState(if (checked) 20.dp else 3.dp, label = "switchOffset")
    Box(
        modifier = Modifier
            .width(50.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(if (checked) c.gold else c.line2),
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset, y = 3.dp)
                .size(24.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.20f),
                    spotColor = Color.Black.copy(alpha = 0.20f),
                )
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = AtelierTheme.colors.line, thickness = 1.dp)
}
