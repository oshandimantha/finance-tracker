package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.CategorySpending
import com.example.domain.model.DashboardSummary
import com.example.domain.model.FinanceFormatter
import com.example.domain.model.MoMVariance
import com.example.ui.components.CategoryVisuals
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.components.PeriodSelectorBar
import com.example.ui.theme.CurrencyTextStyleLarge
import com.example.ui.theme.CurrencyTextStyleMedium
import com.example.ui.theme.CurrencyTextStyleSmall
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.DashboardUiState

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    availableYears: List<Int>,
    onYearSelected: (Int?) -> Unit,
    onMonthSelected: (String?) -> Unit,
    onAddTransactionClick: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is DashboardUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DashboardUiState.Error -> {
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        is DashboardUiState.Success -> {
            DashboardContent(
                state = uiState,
                availableYears = availableYears,
                onYearSelected = onYearSelected,
                onMonthSelected = onMonthSelected,
                onAddTransactionClick = onAddTransactionClick,
                onNavigateToTransactions = onNavigateToTransactions,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    availableYears: List<Int>,
    onYearSelected: (Int?) -> Unit,
    onMonthSelected: (String?) -> Unit,
    onAddTransactionClick: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    var selectedSliceIndex by remember(state.categorySpending) { mutableStateOf<Int?>(null) }

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
            item {
                PeriodSelectorBar(
                    selectedYear = state.period.year,
                    selectedMonth = state.period.month,
                    availableYears = availableYears,
                    onYearSelected = onYearSelected,
                    onMonthSelected = onMonthSelected
                )
            }

            // 7 Executive KPI Cards
            item {
                ExecutiveKpiSection(
                    summary = state.summary,
                    periodLabel = state.period.label,
                    transactionCount = state.transactionCount,
                    onAddTransactionClick = onAddTransactionClick
                )
            }

            // Category Spending Breakdown (Donut + Ranked List)
            item {
                CategoryBreakdownCard(
                    items = state.categorySpending,
                    totalExpenses = state.summary.totalExpenses,
                    selectedIndex = selectedSliceIndex,
                    onSelectIndex = { selectedSliceIndex = it }
                )
            }

            // Month-on-Month Variance Card
            item {
                MoMVarianceCard(
                    variance = state.momVariance,
                    incomeColor = financeColors.income,
                    expenseColor = financeColors.expense,
                    savingsColor = financeColors.savings
                )
            }

            if (state.transactionCount == 0) {
                item {
                    EmptyDashboardPromptCard(
                        onAddTransactionClick = onAddTransactionClick,
                        onNavigateToTransactions = onNavigateToTransactions
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun ExecutiveKpiSection(
    summary: DashboardSummary,
    periodLabel: String,
    transactionCount: Int,
    onAddTransactionClick: () -> Unit
) {
    val financeColors = LocalFinanceColors.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // KPI #7: Hero Net Worth Card
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("kpi_net_worth_card")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                imageVector = Icons.Rounded.AccountBalance,
                                contentDescription = "Total Net Worth",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Total Net Worth",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "$periodLabel • $transactionCount tx",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = FinanceFormatter.formatLkr(summary.totalNetWorth),
                        style = CurrencyTextStyleLarge.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize),
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$periodLabel Net Cash + Investments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Surface(
                            onClick = onAddTransactionClick,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("dashboard_quick_add_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Quick add transaction",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Log Entry",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // KPIs #1 & #2: Total Income & Total Expenses
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiStatCard(
                title = "Total Income",
                value = FinanceFormatter.formatLkr(summary.totalIncome),
                icon = Icons.Rounded.ArrowDownward,
                accentColor = financeColors.income,
                containerColor = financeColors.incomeContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_total_income")
            )
            KpiStatCard(
                title = "Total Expenses",
                value = FinanceFormatter.formatLkr(summary.totalExpenses),
                icon = Icons.Rounded.ArrowUpward,
                accentColor = financeColors.expense,
                containerColor = financeColors.expenseContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_total_expenses")
            )
        }

        // KPIs #3 & #4: Net Savings & Savings Rate (%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val netColor = if (summary.netSavings >= 0) financeColors.income else financeColors.expense
            val netContainer = if (summary.netSavings >= 0) financeColors.incomeContainer else financeColors.expenseContainer
            KpiStatCard(
                title = "Net Savings",
                value = FinanceFormatter.formatLkr(summary.netSavings),
                icon = Icons.Rounded.AccountBalanceWallet,
                accentColor = netColor,
                containerColor = netContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_net_savings")
            )
            KpiStatCard(
                title = "Savings Rate",
                value = FinanceFormatter.formatPercent(summary.savingsRate),
                icon = Icons.Rounded.Percent,
                accentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_savings_rate")
            )
        }

        // KPIs #5 & #6: Savings Transferred & Total Investments
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiStatCard(
                title = "Savings Transferred",
                value = FinanceFormatter.formatLkr(summary.savingsTransferred),
                icon = Icons.Rounded.Savings,
                accentColor = financeColors.savings,
                containerColor = financeColors.savingsContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_savings_transferred")
            )
            KpiStatCard(
                title = "Total Investments",
                value = FinanceFormatter.formatLkr(summary.totalInvestments),
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                accentColor = financeColors.investment,
                containerColor = financeColors.investmentContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_total_investments")
            )
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = CurrencyTextStyleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    items: List<CategorySpending>,
    totalExpenses: Double,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PieChart,
                    contentDescription = "Spending breakdown",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Spending by Category",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expense entries for this period yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    InteractiveDonutChart(
                        items = items,
                        totalSpent = totalExpenses,
                        selectedIndex = selectedIndex,
                        onSelectIndex = onSelectIndex
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEachIndexed { index, item ->
                        val catColor = CategoryVisuals.parseColor(
                            item.category.hexColor,
                            MaterialTheme.colorScheme.primary
                        )
                        val isHighlighted = selectedIndex == index
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isHighlighted) catColor.copy(alpha = 0.08f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onSelectIndex(if (isHighlighted) null else index) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(catColor)
                                        )
                                        Text(
                                            text = item.category.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = FinanceFormatter.formatLkr(item.amount),
                                            style = CurrencyTextStyleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = FinanceFormatter.formatPercent(item.percentage),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { item.percentage.toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = catColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = StrokeCap.Round
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
private fun MoMVarianceCard(
    variance: MoMVariance,
    incomeColor: Color,
    expenseColor: Color,
    savingsColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mom_variance_card"),
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
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "Month on month comparison",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Month-on-Month Variance",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "${variance.currentLabel} vs ${variance.previousLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            MoMVarianceRow(
                label = "Income",
                current = variance.currentIncome,
                previous = variance.previousIncome,
                delta = variance.incomeDelta,
                percentChange = variance.incomePercentChange,
                positiveIsGood = true,
                accentColor = incomeColor
            )
            MoMVarianceRow(
                label = "Expenses",
                current = variance.currentExpense,
                previous = variance.previousExpense,
                delta = variance.expenseDelta,
                percentChange = variance.expensePercentChange,
                positiveIsGood = false,
                accentColor = expenseColor
            )
            MoMVarianceRow(
                label = "Net Savings",
                current = variance.currentSavings,
                previous = variance.previousSavings,
                delta = variance.savingsDelta,
                percentChange = variance.savingsPercentChange,
                positiveIsGood = true,
                accentColor = savingsColor
            )
        }
    }
}

@Composable
private fun MoMVarianceRow(
    label: String,
    current: Double,
    previous: Double,
    delta: Double,
    percentChange: Double?,
    positiveIsGood: Boolean,
    accentColor: Color
) {
    val financeColors = LocalFinanceColors.current
    val isFavorable = if (positiveIsGood) delta >= 0.0 else delta <= 0.0
    val badgeColor = if (delta == 0.0) MaterialTheme.colorScheme.onSurfaceVariant else if (isFavorable) financeColors.income else financeColors.expense
    val badgeBg = if (delta == 0.0) MaterialTheme.colorScheme.surfaceVariant else if (isFavorable) financeColors.incomeContainer else financeColors.expenseContainer

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Now: ${FinanceFormatter.formatCompactLkr(current)} • Prev: ${FinanceFormatter.formatCompactLkr(previous)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeBg
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (delta != 0.0) {
                    Icon(
                        imageVector = if (delta > 0) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                val pctSuffix = percentChange?.let { " (${FinanceFormatter.formatPercent(it)})" } ?: ""
                Text(
                    text = "${FinanceFormatter.formatSignedLkr(delta)}$pctSuffix",
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyDashboardPromptCard(
    onAddTransactionClick: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    AnimatedVisibility(visible = true, enter = fadeIn()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ready to track your LKR finances",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Log your first salary, expense, or savings transfer, or open Settings to load sample finance entries.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onAddTransactionClick,
                        modifier = Modifier.testTag("empty_dashboard_add_button")
                    ) {
                        Text("Add Transaction")
                    }
                    Surface(
                        onClick = onNavigateToTransactions,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Open Ledger",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
