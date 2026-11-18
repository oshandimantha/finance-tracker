package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.FinanceFormatter
import com.example.ui.components.CategoryTrendLineChart
import com.example.ui.components.CategoryVisuals
import com.example.ui.components.MonthlyExpenseBarChart
import com.example.ui.components.YearPickerDialog
import com.example.ui.theme.CurrencyTextStyleMedium
import com.example.ui.theme.CurrencyTextStyleSmall
import com.example.ui.viewmodel.AnalyticsUiState

@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    availableYears: List<Int>,
    onYearSelected: (Int) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AnalyticsUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AnalyticsUiState.Error -> {
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is AnalyticsUiState.Success -> {
            AnalyticsContent(
                state = uiState,
                availableYears = availableYears,
                onYearSelected = onYearSelected,
                onCategorySelected = onCategorySelected,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AnalyticsContent(
    state: AnalyticsUiState.Success,
    availableYears: List<Int>,
    onYearSelected: (Int) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var yearExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }

    val selectedCategory = state.expenseCategories.firstOrNull { it.id == state.selectedCategoryId }
    val lineColor = CategoryVisuals.parseColor(
        selectedCategory?.hexColor,
        MaterialTheme.colorScheme.primary
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Year Selector + Annual Summary KPIs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (yearExpanded) {
                            YearPickerDialog(
                                selectedYear = state.selectedYear,
                                availableYears = availableYears,
                                allowAll = false,
                                onSelect = { yr ->
                                    if (yr != null) onYearSelected(yr)
                                    yearExpanded = false
                                },
                                onDismiss = { yearExpanded = false }
                            )
                        }

                        Surface(
                            onClick = { yearExpanded = true },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("analytics_year_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarToday,
                                        contentDescription = "Select year",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Year ${state.selectedYear}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1.4f),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Annual Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FinanceFormatter.formatCompactLkr(state.annualTotalExpense),
                                    style = CurrencyTextStyleMedium
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Monthly Avg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FinanceFormatter.formatCompactLkr(state.averageMonthlyExpense),
                                    style = CurrencyTextStyleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 12-Month Expense Bar Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = "Monthly Expense Bar Chart",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "12-Month Expense Overview",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        MonthlyExpenseBarChart(points = state.monthlyExpenses)
                    }
                }
            }

            // Category Trend Selector + Monthly Line Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ShowChart,
                                    contentDescription = "Category Trend Line Chart",
                                    tint = lineColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Category Trend",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Box {
                                Surface(
                                    onClick = { catExpanded = true },
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.testTag("analytics_category_selector")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FilterList,
                                            contentDescription = "Filter category",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = selectedCategory?.name ?: "All Categories",
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = catExpanded,
                                    onDismissRequest = { catExpanded = false },
                                    modifier = Modifier.heightIn(max = 320.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Categories") },
                                        onClick = {
                                            onCategorySelected(null)
                                            catExpanded = false
                                        }
                                    )
                                    state.expenseCategories.forEach { cat ->
                                        val cColor = CategoryVisuals.parseColor(cat.hexColor, MaterialTheme.colorScheme.primary)
                                        DropdownMenuItem(
                                            text = { Text(cat.name) },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(cColor)
                                                )
                                            },
                                            onClick = {
                                                onCategorySelected(cat.id)
                                                catExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        CategoryTrendLineChart(
                            points = state.categoryTrend,
                            lineColor = lineColor
                        )
                    }
                }
            }

            // Monthly Breakdown Table
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${selectedCategory?.name ?: "All Categories"} • Monthly Log (${state.selectedYear})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        state.categoryTrend.forEach { pt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${pt.month} ${state.selectedYear}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (pt.amount > 0.0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FinanceFormatter.formatLkr(pt.amount),
                                    style = CurrencyTextStyleSmall,
                                    color = if (pt.amount > 0.0) lineColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}
