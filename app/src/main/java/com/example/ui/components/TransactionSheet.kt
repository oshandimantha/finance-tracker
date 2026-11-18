package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.FinanceFormatter
import com.example.domain.model.TransactionType
import com.example.ui.theme.LocalFinanceColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditorSheet(
    existingTransaction: TransactionEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long?,
        dateMillis: Long,
        description: String,
        categoryId: Long,
        type: TransactionType,
        amount: Double,
        notes: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financeColors = LocalFinanceColors.current

    var selectedType by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.type ?: TransactionType.EXPENSE)
    }
    var dateMillis by remember(existingTransaction) {
        mutableLongStateOf(existingTransaction?.date ?: System.currentTimeMillis())
    }
    var description by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.description.orEmpty())
    }
    var amountText by remember(existingTransaction) {
        mutableStateOf(
            existingTransaction?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            }.orEmpty()
        )
    }
    var notes by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.notes.orEmpty())
    }

    val matchingCategories = remember(categories, selectedType) {
        val byType = categories.filter { it.defaultType == selectedType }
        byType.ifEmpty { categories }
    }

    var selectedCategoryId by remember(existingTransaction, categories) {
        mutableLongStateOf(
            existingTransaction?.categoryId
                ?: matchingCategories.firstOrNull()?.id
                ?: categories.firstOrNull()?.id
                ?: 0L
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
        ?: matchingCategories.firstOrNull()
        ?: categories.firstOrNull()

    val emptyDescMsg = stringResource(R.string.error_empty_description)
    val invalidAmountMsg = stringResource(R.string.error_invalid_amount)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (existingTransaction == null) "Add Transaction" else "Edit Transaction",
                style = MaterialTheme.typography.headlineSmall
            )

            // Type Chips with color badges
            Text(
                text = "Transaction Type",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionType.entries.forEach { txType ->
                    val badgeColor = financeColors.colorFor(txType)
                    val isSelected = selectedType == txType
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedType = txType
                            val firstMatching = categories.firstOrNull { it.defaultType == txType }
                            if (firstMatching != null &&
                                categories.firstOrNull { it.id == selectedCategoryId }?.defaultType != txType
                            ) {
                                selectedCategoryId = firstMatching.id
                            }
                        },
                        label = {
                            Text(
                                text = txType.label,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = financeColors.containerFor(txType),
                            selectedLabelColor = badgeColor
                        ),
                        modifier = Modifier.testTag("tx_type_chip_${txType.name.lowercase()}")
                    )
                }
            }

            // Date & Category Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = { showDatePicker = true },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("tx_date_picker_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Select date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = FinanceFormatter.formatShortDate(dateMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(modifier = Modifier.weight(1.25f)) {
                    Surface(
                        onClick = { categoryExpanded = true },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("tx_category_picker")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val catColor = CategoryVisuals.parseColor(
                                    selectedCategory?.hexColor,
                                    MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Text(
                                    text = selectedCategory?.name ?: "Category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        matchingCategories.forEach { cat ->
                            val cColor = CategoryVisuals.parseColor(cat.hexColor, MaterialTheme.colorScheme.primary)
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryVisuals.iconFor(cat.icon),
                                        contentDescription = null,
                                        tint = cColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                        errorText = null
                    }
                },
                label = { Text("Amount (LKR)") },
                prefix = { Text("Rs ", fontWeight = FontWeight.Bold) },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    errorText = null
                },
                label = { Text("Description") },
                placeholder = { Text("e.g., Monthly Salary, Keells Groceries") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_description_input")
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Add extra details or invoice ref") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_notes_input")
            )

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("tx_cancel_button")
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull()
                        when {
                            parsedAmount == null || parsedAmount <= 0.0 -> errorText = invalidAmountMsg
                            description.isBlank() -> errorText = emptyDescMsg
                            selectedCategory == null -> errorText = "Please choose a category."
                            else -> onSave(
                                existingTransaction?.id,
                                dateMillis,
                                description,
                                selectedCategory.id,
                                selectedType,
                                parsedAmount,
                                notes
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .minimumInteractiveComponentSize()
                        .testTag("tx_save_button")
                ) {
                    Text(if (existingTransaction == null) "Save Entry" else "Update Entry")
                }
            }
        }
    }
}
