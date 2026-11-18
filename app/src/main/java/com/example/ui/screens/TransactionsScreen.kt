package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            imageVector = Icons.Rounded.ReceiptLong,
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
                LazyColumn(
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
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        onDeleteTransaction(itemUi.transaction)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
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
                                TransactionLedgerCard(
                                    item = itemUi,
                                    onClick = { onEditClick(itemUi.transaction) }
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
private fun TransactionLedgerCard(
    item: TransactionItemUi,
    onClick: () -> Unit
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
            .clickable(onClick = onClick)
            .testTag("tx_item_${tx.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
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
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
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
                Text(
                    text = "$amountPrefix${FinanceFormatter.formatLkr(tx.amount)}",
                    style = CurrencyTextStyleMedium,
                    color = typeColor,
                    maxLines = 1
                )
            }
        }
    }
}
