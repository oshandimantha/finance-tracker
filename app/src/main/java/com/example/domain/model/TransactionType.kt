package com.example.domain.model

enum class TransactionType(
    val label: String,
    val shortLabel: String
) {
    INCOME("Income", "Income"),
    EXPENSE("Expense", "Expense"),
    SAVINGS("Savings", "Savings"),
    INVESTMENT("Investment", "Invest");

    companion object {
        fun fromString(value: String): TransactionType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXPENSE
        }
    }
}
