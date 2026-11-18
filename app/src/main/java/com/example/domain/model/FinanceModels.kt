package com.example.domain.model

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity

data class PeriodFilter(
    val year: Int? = null, // null represents "All"
    val month: String? = null // null represents "All", otherwise "Jan".."Dec"
) {
    val label: String
        get() = when {
            year == null && month == null -> "All Time"
            year != null && month == null -> "All $year"
            year == null && month != null -> "$month (All Years)"
            else -> "$month $year"
        }
}

data class DashboardSummary(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRate: Double = 0.0, // ratio 0.0..1.0
    val savingsTransferred: Double = 0.0,
    val totalInvestments: Double = 0.0,
    val totalNetWorth: Double = 0.0
)

data class CategorySpending(
    val category: CategoryEntity,
    val amount: Double,
    val percentage: Double // 0.0..1.0
)

data class MoMVariance(
    val currentLabel: String,
    val previousLabel: String,
    val currentIncome: Double,
    val previousIncome: Double,
    val currentExpense: Double,
    val previousExpense: Double,
    val currentSavings: Double,
    val previousSavings: Double
) {
    val incomeDelta: Double get() = currentIncome - previousIncome
    val expenseDelta: Double get() = currentExpense - previousExpense
    val savingsDelta: Double get() = currentSavings - previousSavings

    val incomePercentChange: Double? get() = calcPct(currentIncome, previousIncome)
    val expensePercentChange: Double? get() = calcPct(currentExpense, previousExpense)
    val savingsPercentChange: Double? get() = calcPct(currentSavings, previousSavings)

    private fun calcPct(curr: Double, prev: Double): Double? {
        return if (prev == 0.0) null else (curr - prev) / kotlin.math.abs(prev)
    }
}

data class TransactionItemUi(
    val transaction: TransactionEntity,
    val category: CategoryEntity?
)

data class TransactionDateGroup(
    val dateLabel: String,
    val dayTimestamp: Long,
    val items: List<TransactionItemUi>,
    val dailyNet: Double
)

enum class BudgetHealth {
    ON_TRACK,   // < 80%
    NEAR_LIMIT, // 80% - 100%
    OVER_BUDGET // > 100%
}

data class BudgetItemStatus(
    val category: CategoryEntity,
    val monthlyLimit: Double,
    val actualSpent: Double
) {
    val remaining: Double get() = monthlyLimit - actualSpent
    val percentUsed: Double
        get() = when {
            monthlyLimit > 0.0 -> actualSpent / monthlyLimit
            actualSpent > 0.0 -> 1.5
            else -> 0.0
        }
    val health: BudgetHealth
        get() = when {
            monthlyLimit <= 0.0 && actualSpent > 0.0 -> BudgetHealth.OVER_BUDGET
            percentUsed > 1.0 -> BudgetHealth.OVER_BUDGET
            percentUsed >= 0.8 -> BudgetHealth.NEAR_LIMIT
            else -> BudgetHealth.ON_TRACK
        }
}

data class BudgetSummary(
    val totalPlanned: Double,
    val totalSpent: Double
) {
    val remaining: Double get() = totalPlanned - totalSpent
    val percentUsed: Double
        get() = if (totalPlanned > 0.0) totalSpent / totalPlanned else 0.0
}

data class MonthlyTrendPoint(
    val month: String,
    val monthIndex: Int,
    val amount: Double
)

enum class GoalCountdownStatus {
    COMPLETED,
    HEALTHY,  // > 30 days left
    URGENT,   // 0..30 days left
    OVERDUE   // < 0 days left
}

data class GoalUiModel(
    val goal: SavingsGoalEntity,
    val daysLeft: Long,
    val status: GoalCountdownStatus
) {
    val remainingAmount: Double get() = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
    val progress: Double
        get() = if (goal.targetAmount > 0.0) (goal.savedAmount / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0
    val rawPercent: Double
        get() = if (goal.targetAmount > 0.0) goal.savedAmount / goal.targetAmount else 0.0
}
