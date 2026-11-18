package com.example.ui.viewmodel

import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.BudgetItemStatus
import com.example.domain.model.BudgetSummary
import com.example.domain.model.CategorySpending
import com.example.domain.model.DashboardSummary
import com.example.domain.model.GoalUiModel
import com.example.domain.model.MoMVariance
import com.example.domain.model.MonthlyTrendPoint
import com.example.domain.model.PeriodFilter
import com.example.domain.model.TransactionDateGroup
import com.example.domain.model.TransactionType

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val period: PeriodFilter,
        val summary: DashboardSummary,
        val categorySpending: List<CategorySpending>,
        val momVariance: MoMVariance,
        val transactionCount: Int
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState
    data class Success(
        val period: PeriodFilter,
        val searchQuery: String,
        val selectedType: TransactionType?,
        val groups: List<TransactionDateGroup>,
        val activeCategories: List<CategoryEntity>,
        val totalCount: Int
    ) : TransactionsUiState
    data class Error(val message: String) : TransactionsUiState
}

sealed interface BudgetUiState {
    data object Loading : BudgetUiState
    data class Success(
        val selectedYear: Int,
        val selectedMonth: String,
        val summary: BudgetSummary,
        val items: List<BudgetItemStatus>
    ) : BudgetUiState
    data class Error(val message: String) : BudgetUiState
}

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Success(
        val selectedYear: Int,
        val monthlyExpenses: List<MonthlyTrendPoint>,
        val expenseCategories: List<CategoryEntity>,
        val selectedCategoryId: Long?, // null = All Categories
        val categoryTrend: List<MonthlyTrendPoint>,
        val annualTotalExpense: Double,
        val averageMonthlyExpense: Double
    ) : AnalyticsUiState
    data class Error(val message: String) : AnalyticsUiState
}

sealed interface GoalsUiState {
    data object Loading : GoalsUiState
    data class Success(
        val goals: List<GoalUiModel>,
        val totalTarget: Double,
        val totalSaved: Double
    ) : GoalsUiState
    data class Error(val message: String) : GoalsUiState
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(
        val activeCategories: List<CategoryEntity>,
        val archivedCategories: List<CategoryEntity>,
        val totalTransactions: Int,
        val totalBudgets: Int,
        val totalGoals: Int
    ) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}
