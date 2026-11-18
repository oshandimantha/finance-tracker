package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.BudgetHealth
import com.example.domain.model.BudgetItemStatus
import com.example.domain.model.BudgetSummary
import com.example.domain.model.FinanceFormatter
import com.example.domain.model.TransactionType
import com.example.ui.components.BudgetEditDialog
import com.example.ui.components.CategoryEditorDialog
import com.example.ui.components.CategoryVisuals
import com.example.ui.components.PeriodSelectorBar
import com.example.ui.theme.CurrencyTextStyleLarge
import com.example.ui.theme.CurrencyTextStyleMedium
import com.example.ui.theme.CurrencyTextStyleSmall
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.BudgetUiState
import kotlinx.coroutines.launch

@Composable
fun BudgetScreen(
    uiState: BudgetUiState,
    availableYears: List<Int>,
    onPeriodSelected: (Int, String) -> Unit,
    onUpdateCategoryBudget: (Long, Double) -> Unit,
    onCopyPreviousMonth: () -> Unit,
    onSaveCategory: (existingId: Long?, name: String, hexColor: String, defaultType: TransactionType, icon: String?, notes: String?) -> Unit,
    onCountCategoryTransactions: suspend (Long) -> Int,
    onDeleteCategoryPermanently: (categoryId: Long, deleteAssociatedTransactions: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var editingItem by remember { mutableStateOf<BudgetItemStatus?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategoryTxCount by remember { mutableStateOf(0) }

    if (showAddCategoryDialog) {
        CategoryEditorDialog(
            existingCategory = null,
            onDismiss = { showAddCategoryDialog = false },
            onSave = { id, name, hex, type, icon, notes ->
                onSaveCategory(id, name, hex, type, icon, notes)
                showAddCategoryDialog = false
            }
        )
    }

    deletingCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Delete \"${cat.name}\"?") },
            text = {
                Text(
                    text = if (deletingCategoryTxCount > 0) {
                        "This category is linked to $deletingCategoryTxCount transaction(s). Deleting it will permanently remove the category, its budget, and those transactions."
                    } else {
                        "This will permanently delete the \"${cat.name}\" category and its monthly budget."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategoryPermanently(cat.id, deletingCategoryTxCount > 0)
                        deletingCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("budget_confirm_delete_category")
                ) {
                    Text(if (deletingCategoryTxCount > 0) "Delete All" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCategoryDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = "Add budget category") },
                text = { Text("Add Category") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("budget_add_category_fab")
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is BudgetUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is BudgetUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is BudgetUiState.Success -> {
                editingItem?.let { item ->
                    BudgetEditDialog(
                        item = item,
                        monthLabel = uiState.selectedMonth,
                        year = uiState.selectedYear,
                        onDismiss = { editingItem = null },
                        onConfirm = { newLimit ->
                            onUpdateCategoryBudget(item.category.id, newLimit)
                            editingItem = null
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            PeriodSelectorBar(
                                selectedYear = uiState.selectedYear,
                                selectedMonth = uiState.selectedMonth,
                                availableYears = availableYears,
                                onYearSelected = { y -> if (y != null) onPeriodSelected(y, uiState.selectedMonth) },
                                onMonthSelected = { m -> if (m != null) onPeriodSelected(uiState.selectedYear, m) },
                                allowAllYear = false,
                                allowAllMonth = false
                            )
                        }

                        item {
                            BudgetSummaryBanner(
                                summary = uiState.summary,
                                month = uiState.selectedMonth,
                                year = uiState.selectedYear,
                                onCopyPreviousMonth = onCopyPreviousMonth
                            )
                        }

                        if (uiState.items.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Category,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Text(
                                            text = "No budget categories yet",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Tap '+ Add Category' to create your own expense categories and set monthly LKR limits.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = uiState.items,
                                key = { it.category.id }
                            ) { item ->
                                BudgetCategoryCard(
                                    item = item,
                                    onEditClick = { editingItem = item },
                                    onDeleteClick = {
                                        scope.launch {
                                            deletingCategoryTxCount = onCountCategoryTransactions(item.category.id)
                                            deletingCategory = item.category
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetSummaryBanner(
    summary: BudgetSummary,
    month: String,
    year: Int,
    onCopyPreviousMonth: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val ratio = summary.percentUsed.toFloat().coerceIn(0f, 1f)
    val barColor = when {
        summary.totalPlanned <= 0.0 && summary.totalSpent > 0.0 -> financeColors.expense
        summary.percentUsed > 1.0 -> financeColors.expense
        summary.percentUsed >= 0.8 -> financeColors.warning
        else -> financeColors.income
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_summary_banner"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$month $year Budget Plan",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${FinanceFormatter.formatPercent(summary.percentUsed)} of planned budget used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onCopyPreviousMonth,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("copy_prev_budget_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy previous month budgets",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Copy Prev", style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Actual Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FinanceFormatter.formatLkr(summary.totalSpent),
                        style = CurrencyTextStyleLarge,
                        color = barColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Planned Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FinanceFormatter.formatLkr(summary.totalPlanned),
                        style = CurrencyTextStyleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (summary.remaining >= 0) "Remaining to spend" else "Exceeded by",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FinanceFormatter.formatLkr(kotlin.math.abs(summary.remaining)),
                    style = CurrencyTextStyleSmall,
                    color = if (summary.remaining >= 0) financeColors.income else financeColors.expense
                )
            }
        }
    }
}

@Composable
private fun BudgetCategoryCard(
    item: BudgetItemStatus,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val catColor = CategoryVisuals.parseColor(item.category.hexColor, MaterialTheme.colorScheme.primary)

    val (statusLabel, statusIcon, statusColor, statusBg) = when (item.health) {
        BudgetHealth.ON_TRACK -> Quadruple(
            "On Track",
            Icons.Rounded.CheckCircle,
            financeColors.income,
            financeColors.incomeContainer
        )
        BudgetHealth.NEAR_LIMIT -> Quadruple(
            "Near Limit",
            Icons.Rounded.WarningAmber,
            financeColors.warning,
            financeColors.warningContainer
        )
        BudgetHealth.OVER_BUDGET -> Quadruple(
            "Over Budget",
            Icons.Rounded.ErrorOutline,
            financeColors.expense,
            financeColors.expenseContainer
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onEditClick)
            .testTag("budget_category_card_${item.category.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(catColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CategoryVisuals.iconFor(item.category.icon),
                            contentDescription = item.category.name,
                            tint = catColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = item.category.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (item.monthlyLimit > 0.0) {
                                "Limit: ${FinanceFormatter.formatLkr(item.monthlyLimit)}"
                            } else {
                                "Tap to set monthly budget"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.monthlyLimit > 0.0 || item.actualSpent > 0.0) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = statusLabel,
                                    tint = statusColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.testTag("budget_edit_button_${item.category.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit budget for ${item.category.name}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("budget_delete_category_button_${item.category.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete category ${item.category.name}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (item.monthlyLimit > 0.0 || item.actualSpent > 0.0) {
                LinearProgressIndicator(
                    progress = { item.percentUsed.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Spent: ${FinanceFormatter.formatLkr(item.actualSpent)}",
                        style = CurrencyTextStyleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (item.remaining >= 0) {
                            "Left: ${FinanceFormatter.formatLkr(item.remaining)}"
                        } else {
                            "Over: ${FinanceFormatter.formatLkr(-item.remaining)}"
                        },
                        style = CurrencyTextStyleMedium.copy(fontSize = CurrencyTextStyleSmall.fontSize),
                        color = statusColor
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
