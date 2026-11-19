package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.FinanceFormatter
import com.example.domain.model.TransactionItemUi
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryVisuals
import com.example.ui.components.PeriodSelectorBar
import com.example.ui.theme.CurrencyTextStyleMedium
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.TransactionsUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    availableYears: List<Int>,
    onYearSelected: (Int?) -> Unit,
    onMonthSelected: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTypeSelected: (TransactionType?) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onBulkDeleteTransactions: (List<TransactionEntity>) -> Unit = { list ->
        list.forEach(onDeleteTransaction)
    },
    onBulkUpdateCategory: (transactionIds: List<Long>, categoryId: Long, type: TransactionType) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Rounded.Add, contentDescription = "Add transaction") },
                text = { Text("Add Transaction") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_transaction_fab")
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is TransactionsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is TransactionsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is TransactionsUiState.Success -> {
                TransactionsContent(
                    state = uiState,
                    availableYears = availableYears,
                    onYearSelected = onYearSelected,
                    onMonthSelected = onMonthSelected,
                    onSearchQueryChange = onSearchQueryChange,
                    onTypeSelected = onTypeSelected,
                    onEditClick = onEditClick,
                    onDeleteTransaction = onDeleteTransaction,
                    onBulkDeleteTransactions = onBulkDeleteTransactions,
                    onBulkUpdateCategory = onBulkUpdateCategory,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsContent(
    state: TransactionsUiState.Success,
    availableYears: List<Int>,
    onYearSelected: (Int?) -> Unit,
    onMonthSelected: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTypeSelected: (TransactionType?) -> Unit,
    onEditClick: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onBulkDeleteTransactions: (List<TransactionEntity>) -> Unit,
    onBulkUpdateCategory: (transactionIds: List<Long>, categoryId: Long, type: TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var pendingDeleteTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showBulkCategoryDialog by remember { mutableStateOf(false) }

    val visibleTransactions = remember(state.groups) {
        state.groups.flatMap { it.items }.map { it.transaction }
    }
    val visibleIdSet = remember(visibleTransactions) {
        visibleTransactions.map { it.id }.toSet()
    }

    LaunchedEffect(visibleIdSet) {
        if (isSelectionMode) {
            val pruned = selectedIds.intersect(visibleIdSet)
            if (pruned != selectedIds) {
                selectedIds = pruned
            }
            if (visibleTransactions.isEmpty()) {
                isSelectionMode = false
                selectedIds = emptySet()
            }
        }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    val selectedTransactions = remember(visibleTransactions, selectedIds) {
        visibleTransactions.filter { it.id in selectedIds }
    }

    pendingDeleteTransaction?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            title = { Text("Delete Transaction?") },
            text = {
                Text(
                    "Remove \"${tx.description}\" (${FinanceFormatter.formatLkr(tx.amount)}) from your ledger? You can undo right after deleting."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(tx)
                        pendingDeleteTransaction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_tx_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTransaction = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBulkDeleteConfirm && selectedTransactions.isNotEmpty()) {
        val totalSelectedAmount = selectedTransactions.sumOf { it.amount }
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = {
                Text("Delete ${selectedTransactions.size} ${if (selectedTransactions.size == 1) "Transaction" else "Transactions"}?")
            },
            text = {
                Text(
                    "This will remove ${selectedTransactions.size} selected ledger ${if (selectedTransactions.size == 1) "entry" else "entries"} totaling ${FinanceFormatter.formatLkr(totalSelectedAmount)}. You can undo from the banner immediately."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBulkDeleteTransactions(selectedTransactions)
                        showBulkDeleteConfirm = false
                        isSelectionMode = false
                        selectedIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_bulk_delete_tx_button")
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBulkCategoryDialog && selectedTransactions.isNotEmpty()) {
        BulkCategoryEditDialog(
            selectedCount = selectedTransactions.size,
            categories = state.activeCategories,
            onDismiss = { showBulkCategoryDialog = false },
            onApply = { category ->
                onBulkUpdateCategory(
                    selectedTransactions.map { it.id },
                    category.id,
                    category.defaultType
                )
                showBulkCategoryDialog = false
                isSelectionMode = false
                selectedIds = emptySet()
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PeriodSelectorBar(
                    selectedYear = state.period.year,
                    selectedMonth = state.period.month,
                    availableYears = availableYears,
                    onYearSelected = onYearSelected,
                    onMonthSelected = onMonthSelected
                )

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search description, category, notes...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_search_input")
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (visibleTransactions.isNotEmpty()) {
                        item {
                            FilterChip(
                                selected = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        isSelectionMode = false
                                        selectedIds = emptySet()
                                    } else {
                                        isSelectionMode = true
                                    }
                                },
                                label = { Text(if (isSelectionMode) "Done" else "Select") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Checklist,
                                        contentDescription = "Toggle multi-select mode",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("tx_select_mode_button")
                            )
                        }
                    }
                    item {
                        FilterChip(
                            selected = state.selectedType == null,
                            onClick = { onTypeSelected(null) },
                            label = { Text("All (${state.totalCount})") },
                            modifier = Modifier.testTag("tx_filter_all")
                        )
                    }
                    items(TransactionType.entries) { type ->
                        val badgeColor = financeColors.colorFor(type)
                        FilterChip(
                            selected = state.selectedType == type,
                            onClick = {
                                onTypeSelected(if (state.selectedType == type) null else type)
                            },
                            label = { Text(type.label) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = financeColors.containerFor(type),
                                selectedLabelColor = badgeColor
                            ),
                            modifier = Modifier.testTag("tx_filter_${type.name.lowercase()}")
                        )
                    }
                }

                AnimatedVisibility(visible = isSelectionMode) {
                    LedgerSelectionActionBar(
                        selectedCount = selectedIds.size,
                        totalCount = visibleTransactions.size,
                        onClose = {
                            isSelectionMode = false
                            selectedIds = emptySet()
                        },
                        onToggleSelectAll = {
                            selectedIds = if (selectedIds.size == visibleIdSet.size) {
                                emptySet()
                            } else {
                                visibleIdSet
                            }
                        },
                        onBulkEditCategory = { showBulkCategoryDialog = true },
                        onBulkDelete = { showBulkDeleteConfirm = true }
                    )
                }
            }

            if (state.groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No matching transactions",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap '+ Add Transaction' to log an income, expense, savings, or investment entry.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val listState = rememberLazyListState()
                val topTxId = state.groups.firstOrNull()?.items?.firstOrNull()?.transaction?.id
                var previousTopTxId by remember { mutableStateOf(topTxId) }
                LaunchedEffect(topTxId) {
                    if (topTxId != null && previousTopTxId != null && topTxId != previousTopTxId &&
                        listState.firstVisibleItemIndex <= 2
                    ) {
                        listState.scrollToItem(0)
                    }
                    previousTopTxId = topTxId
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.groups.forEach { group ->
                        stickyHeader(key = "header_${group.dayTimestamp}") {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.dateLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (group.dailyNet != 0.0) {
                                        Text(
                                            text = FinanceFormatter.formatSignedLkr(group.dailyNet),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (group.dailyNet > 0) financeColors.income else financeColors.expense
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = group.items,
                            key = { it.transaction.id }
                        ) { itemUi ->
                            val tx = itemUi.transaction
                            val latestTransaction by rememberUpdatedState(tx)
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (!isSelectionMode && value == SwipeToDismissBoxValue.EndToStart) {
                                        onDeleteTransaction(latestTransaction)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = !isSelectionMode,
                                backgroundContent = {
                                    val bgColor by animateColorAsState(
                                        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer
                                        },
                                        label = "swipe_bg"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(bgColor)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete transaction",
                                            tint = Color.White
                                        )
                                    }
                                }
                            ) {
                                val isSelected = tx.id in selectedIds
                                TransactionLedgerCard(
                                    item = itemUi,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedIds = if (isSelected) {
                                                selectedIds - tx.id
                                            } else {
                                                selectedIds + tx.id
                                            }
                                        } else {
                                            onEditClick(tx)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                            selectedIds = setOf(tx.id)
                                        }
                                    },
                                    onEditClick = { onEditClick(tx) },
                                    onDeleteClick = { pendingDeleteTransaction = tx }
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
private fun LedgerSelectionActionBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onBulkEditCategory: () -> Unit,
    onBulkDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_selection_bar"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("tx_exit_select_mode")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Exit selection mode"
                    )
                }
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleSelectAll,
                    modifier = Modifier.testTag("tx_select_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SelectAll,
                        contentDescription = if (selectedCount == totalCount) "Clear selection" else "Select all",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(
                    onClick = onBulkEditCategory,
                    enabled = selectedCount > 0,
                    modifier = Modifier.testTag("tx_bulk_edit_category_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DriveFileRenameOutline,
                        contentDescription = "Change category for selected",
                        tint = if (selectedCount > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }
                    )
                }
                IconButton(
                    onClick = onBulkDelete,
                    enabled = selectedCount > 0,
                    modifier = Modifier.testTag("tx_bulk_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete selected transactions",
                        tint = if (selectedCount > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BulkCategoryEditDialog(
    selectedCount: Int,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onApply: (CategoryEntity) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    var chosenCategory by remember(categories) { mutableStateOf(categories.firstOrNull()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 460.dp)
                .testTag("tx_bulk_category_dialog"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Update $selectedCount Ledger ${if (selectedCount == 1) "Entry" else "Entries"}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Choose a category to reassign all selected transactions and align their type:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (categories.isEmpty()) {
                    Text(
                        text = "No categories available yet. Add a category in Settings or Budget first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = chosenCategory?.id == cat.id
                            val catColor = CategoryVisuals.parseColor(
                                cat.hexColor,
                                MaterialTheme.colorScheme.primary
                            )
                            val typeColor = financeColors.colorFor(cat.defaultType)
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { chosenCategory = cat }
                                    .testTag("bulk_category_option_${cat.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(catColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CategoryVisuals.iconFor(cat.icon),
                                            contentDescription = cat.name,
                                            tint = catColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = financeColors.containerFor(cat.defaultType)
                                    ) {
                                        Text(
                                            text = cat.defaultType.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = typeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { chosenCategory?.let(onApply) },
                        enabled = chosenCategory != null,
                        modifier = Modifier.testTag("bulk_category_apply_button")
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionLedgerCard(
    item: TransactionItemUi,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val tx = item.transaction
    val typeColor = financeColors.colorFor(tx.type)
    val typeBg = financeColors.containerFor(tx.type)
    val catColor = CategoryVisuals.parseColor(item.category?.hexColor, typeColor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("tx_item_${tx.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color coding bar per transaction type
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(typeColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.testTag("tx_select_checkbox_${tx.id}")
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(catColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryVisuals.iconFor(item.category?.icon),
                        contentDescription = item.category?.name ?: "Category",
                        tint = catColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tx.description,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = typeBg
                        ) {
                            Text(
                                text = tx.type.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = typeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = item.category?.name ?: "Other",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!tx.notes.isNullOrBlank()) {
                        Text(
                            text = tx.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val amountPrefix = when (tx.type) {
                    TransactionType.INCOME -> "+"
                    TransactionType.EXPENSE -> "-"
                    TransactionType.SAVINGS, TransactionType.INVESTMENT -> ""
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "$amountPrefix${FinanceFormatter.formatLkr(tx.amount)}",
                        style = CurrencyTextStyleMedium,
                        color = typeColor,
                        maxLines = 1,
                        modifier = Modifier.padding(end = if (isSelectionMode) 8.dp else 6.dp)
                    )
                    if (!isSelectionMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier.testTag("tx_edit_button_${tx.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit ${tx.description}",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.testTag("tx_delete_button_${tx.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete ${tx.description}",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
