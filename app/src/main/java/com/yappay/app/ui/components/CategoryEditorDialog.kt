package com.yappay.app.ui.components

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yappay.app.data.CategoryRepository
import com.yappay.app.domain.CategoryRef
import com.yappay.app.ui.theme.AtelierTheme
import com.yappay.app.ui.theme.InterFamily
import com.yappay.app.ui.theme.serifStyle

/**
 * Small dialog used both for "New category" and for editing an existing custom category.
 *
 * [existingNames] must contain every name that is NOT allowed (all built-in labels plus the
 * other custom labels). Duplicate checking is case-insensitive.
 */
@Composable
fun CategoryEditorDialog(
    title: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Long) -> Unit,
    initialName: String = "",
    initialColor: Long = CategoryRef.palette.first(),
) {
    val c = AtelierTheme.colors
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }
    val trimmed = name.trim()
    val duplicate = trimmed.isNotEmpty() && existingNames.any { it.equals(trimmed, ignoreCase = true) }
    val canSave = trimmed.isNotEmpty() && !duplicate

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.scrim)
                .imePadding()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(c.card)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 20.dp),
            ) {
                Text(
                    text = title,
                    color = c.ink,
                    style = serifStyle(28f, 31f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Give it a name and a colour. It will show up in every category picker.",
                    color = c.muted,
                    fontFamily = InterFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 18.1.sp,
                )

                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(CategoryRef.MAX_NAME_LENGTH) },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Pet care") },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) {
                        { Text("A category with this name already exists") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Eyebrow(
                    text = "Colour",
                    modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryRef.palette.chunked(6).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowColors.forEach { swatch ->
                                ColorSwatch(
                                    color = swatch,
                                    selected = swatch == color,
                                    onClick = { color = swatch },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
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
                        text = "Save",
                        onClick = { onSave(CategoryRepository.cleanName(trimmed), color) },
                        enabled = canSave,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Long,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AtelierTheme.colors
    val swatch = Color(color)
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) c.ink else c.line2,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClickLabel = "Choose colour", onClick = onClick)
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(swatch),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected colour",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
