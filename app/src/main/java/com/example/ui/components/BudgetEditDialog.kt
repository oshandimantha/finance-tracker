package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.BudgetItemStatus

@Composable
fun BudgetEditDialog(
    item: BudgetItemStatus,
    monthLabel: String,
    year: Int,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var limitText by remember(item) {
        mutableStateOf(
            if (item.monthlyLimit > 0.0) {
                if (item.monthlyLimit % 1.0 == 0.0) item.monthlyLimit.toLong().toString()
                else item.monthlyLimit.toString()
            } else ""
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Budget: ${item.category.name}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Set monthly spending limit for $monthLabel $year",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            limitText = input
                            errorMsg = null
                        }
                    },
                    label = { Text("Monthly Limit (LKR)") },
                    prefix = { Text("Rs ", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = if (limitText.isBlank()) 0.0 else limitText.toDoubleOrNull()
                    if (value == null || value < 0.0) {
                        errorMsg = "Enter a valid amount (0 or higher)."
                    } else {
                        onConfirm(value)
                    }
                },
                modifier = Modifier.testTag("budget_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
