package com.yappay.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yappay.app.domain.Category
import com.yappay.app.util.DateRanges
import java.time.LocalDate

/**
 * The 4 inputs used BOTH by the voice confirm dialog and by the Add/Edit screen.
 * Stateless: the caller owns every value and receives every change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormFields(
    amountText: String,
    onAmountChange: (String) -> Unit,
    amountError: Boolean,
    description: String,
    onDescriptionChange: (String) -> Unit,
    category: Category,
    onCategoryChange: (Category) -> Unit,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { new -> onAmountChange(new.filter { it.isDigit() || it == '.' }) },
            label = { Text("Amount") },
            prefix = { Text(currencySymbol) },
            isError = amountError,
            supportingText = if (amountError) ({ Text("Enter a valid amount, e.g. 340 or 12.50") }) else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = description,
            onValueChange = { onDescriptionChange(it.take(100)) },
            label = { Text("Spent on") },
            placeholder = { Text("e.g. Pizza") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        ExposedDropdownMenuBox(
            expanded = categoryMenuOpen,
            onExpandedChange = { categoryMenuOpen = it },
        ) {
            OutlinedTextField(
                value = category.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuOpen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            )
            ExposedDropdownMenu(
                expanded = categoryMenuOpen,
                onDismissRequest = { categoryMenuOpen = false },
            ) {
                Category.entries.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.displayName) },
                        onClick = {
                            onCategoryChange(c)
                            categoryMenuOpen = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = DateRanges.DAY_HEADER.format(date),
            onValueChange = {},
            readOnly = true,
            label = { Text("Date") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
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
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
