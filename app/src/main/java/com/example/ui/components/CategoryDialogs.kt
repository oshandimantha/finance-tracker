package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.TransactionType
import com.example.ui.theme.LocalFinanceColors

@Composable
fun CategoryEditorDialog(
    existingCategory: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long?,
        name: String,
        hexColor: String,
        defaultType: TransactionType,
        icon: String?,
        notes: String?
    ) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    var name by remember(existingCategory) { mutableStateOf(existingCategory?.name.orEmpty()) }
    var hexColor by remember(existingCategory) {
        mutableStateOf(existingCategory?.hexColor ?: CategoryVisuals.paletteHexOptions.first())
    }
    var defaultType by remember(existingCategory) {
        mutableStateOf(existingCategory?.defaultType ?: TransactionType.EXPENSE)
    }
    var selectedIcon by remember(existingCategory) {
        mutableStateOf(existingCategory?.icon ?: "category")
    }
    var notes by remember(existingCategory) { mutableStateOf(existingCategory?.notes.orEmpty()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var iconFilter by remember { mutableStateOf<Boolean?>(null) } // null = All, true = Business, false = Personal

    val selectedOption = CategoryVisuals.iconOptions.firstOrNull { it.key == selectedIcon }
    val filteredIcons = remember(iconFilter) {
        when (iconFilter) {
            true -> CategoryVisuals.iconOptions.filter { it.isBusiness }
            false -> CategoryVisuals.iconOptions.filter { !it.isBusiness }
            null -> CategoryVisuals.iconOptions
        }
    }
    val iconRows = remember(filteredIcons) { filteredIcons.chunked(5) }
    val colorRows = remember { CategoryVisuals.paletteHexOptions.chunked(6) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .heightIn(max = 680.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (existingCategory == null) "New Category" else "Edit Category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMsg = null
                        },
                        label = { Text("Category Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_name_input")
                    )

                    Text(
                        text = "Default Type",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TransactionType.entries.forEach { t ->
                            val c = financeColors.colorFor(t)
                            FilterChip(
                                selected = defaultType == t,
                                onClick = { defaultType = t },
                                label = { Text(t.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = financeColors.containerFor(t),
                                    selectedLabelColor = c
                                )
                            )
                        }
                    }

                    Text(
                        text = "Badge Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorRows.forEach { rowColors ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowColors.forEach { hex ->
                                    val color = CategoryVisuals.parseColor(hex, MaterialTheme.colorScheme.primary)
                                    val selected = hex.equals(hexColor, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .then(
                                                if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                                else Modifier
                                            )
                                            .clickable { hexColor = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected color",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = if (selectedOption != null) "Icon (${selectedOption.label})" else "Icon",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = iconFilter == null,
                            onClick = { iconFilter = null },
                            label = { Text("All (${CategoryVisuals.iconOptions.size})") }
                        )
                        FilterChip(
                            selected = iconFilter == true,
                            onClick = { iconFilter = true },
                            label = { Text("Business") }
                        )
                        FilterChip(
                            selected = iconFilter == false,
                            onClick = { iconFilter = false },
                            label = { Text("Personal") }
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconRows.forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowIcons.forEach { option ->
                                    val isSelected = selectedIcon == option.key
                                    val shape = RoundedCornerShape(10.dp)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 44.dp)
                                            .clip(shape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable { selectedIcon = option.key }
                                            .testTag("category_icon_${option.key}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = option.label,
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                repeat(5 - rowIcons.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.size(4.dp))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMsg = "Please enter a category name."
                            } else {
                                onSave(existingCategory?.id, name, hexColor, defaultType, selectedIcon, notes)
                            }
                        },
                        modifier = Modifier.testTag("category_save_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
