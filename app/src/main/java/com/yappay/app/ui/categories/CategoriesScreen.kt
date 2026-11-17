package com.yappay.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yappay.app.domain.Category
import com.yappay.app.domain.CategoryRef
import com.yappay.app.ui.components.AtelierButton
import com.yappay.app.ui.components.AtelierButtonKind
import com.yappay.app.ui.components.AtelierSnackbarHost
import com.yappay.app.ui.components.CategoryEditorDialog
import com.yappay.app.ui.components.CategoryTile
import com.yappay.app.ui.components.CircleAction
import com.yappay.app.ui.components.Eyebrow
import com.yappay.app.ui.theme.AtelierTheme
import com.yappay.app.ui.theme.InterFamily
import com.yappay.app.ui.theme.serifStyle

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val vm: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory)
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val c = AtelierTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }

    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CategoryRef?>(null) }
    var deleting by remember { mutableStateOf<CategoryRef?>(null) }

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
                    text = "Categories",
                    color = c.ink,
                    style = serifStyle(27f, 27f),
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 40.dp),
            ) {
                Eyebrow(
                    text = "Your categories",
                    color = c.gold,
                    modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 10.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.card)
                        .border(1.dp, c.line, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp),
                ) {
                    if (ui.custom.isEmpty()) {
                        Text(
                            text = "No custom categories yet. Add one below and give it its own colour.",
                            color = c.muted,
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(vertical = 18.dp),
                        )
                    } else {
                        ui.custom.forEachIndexed { index, ref ->
                            if (index > 0) Divider()
                            CategoryRow(
                                category = ref,
                                onEdit = { editing = ref },
                                onDelete = { deleting = ref },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                AtelierButton(
                    text = "New category",
                    onClick = { creating = true },
                    leadingIcon = Icons.Rounded.Add,
                    modifier = Modifier.fillMaxWidth(),
                )

                Eyebrow(
                    text = "Built-in",
                    color = c.gold,
                    modifier = Modifier.padding(start = 6.dp, top = 26.dp, bottom = 10.dp),
                )
                BuiltInCategories()

                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (creating) {
        CategoryEditorDialog(
            title = "New category",
            existingNames = Category.entries.map { it.displayName } + ui.custom.map { it.label },
            onDismiss = { creating = false },
            onSave = { name, color ->
                creating = false
                vm.add(name, color)
            },
        )
    }

    editing?.let { ref ->
        CategoryEditorDialog(
            title = "Edit category",
            initialName = ref.label,
            initialColor = ref.color ?: CategoryRef.palette.first(),
            existingNames = Category.entries.map { it.displayName } +
                ui.custom.filter { it.key != ref.key }.map { it.label },
            onDismiss = { editing = null },
            onSave = { name, color ->
                editing = null
                vm.update(ref, name, color)
            },
        )
    }

    deleting?.let { ref ->
        DeleteCategoryDialog(
            category = ref,
            onDismiss = { deleting = null },
            onDelete = {
                deleting = null
                vm.delete(ref)
            },
        )
    }
}

@Composable
private fun CategoryRow(
    category: CategoryRef,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = AtelierTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onEdit)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryTile(category = category, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.label,
                color = c.ink,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Custom category",
                color = c.muted,
                fontFamily = InterFamily,
                fontSize = 12.sp,
            )
        }
        CircleAction(
            icon = Icons.Rounded.Edit,
            contentDescription = "Edit ${category.label}",
            onClick = onEdit,
            size = 36.dp,
            ghost = true,
        )
        CircleAction(
            icon = Icons.Rounded.Delete,
            contentDescription = "Delete ${category.label}",
            onClick = onDelete,
            size = 36.dp,
            ghost = true,
            iconTint = c.err,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuiltInCategories() {
    val c = AtelierTheme.colors
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Category.entries.forEach { category ->
            val ref = CategoryRef.of(category)
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(c.card)
                    .border(1.dp, c.line, CircleShape)
                    .padding(start = 5.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                CategoryTile(category = ref, size = 24.dp)
                Text(
                    text = ref.label,
                    color = c.ink2,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DeleteCategoryDialog(
    category: CategoryRef,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
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
                    text = "Delete “${category.label}”?",
                    color = c.ink,
                    style = serifStyle(28f, 31f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Its expenses will move to Other.",
                    color = c.muted,
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
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

@Composable
private fun Divider() {
    HorizontalDivider(color = AtelierTheme.colors.line, thickness = 1.dp)
}
