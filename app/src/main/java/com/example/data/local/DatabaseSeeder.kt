package com.example.data.local

import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.FinanceFormatter
import com.example.domain.model.TransactionType

object DatabaseSeeder {

    fun defaultCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(
            name = "Salary",
            hexColor = "#15803D",
            defaultType = TransactionType.INCOME,
            notes = "Monthly salary & primary paycheck",
            icon = "payments"
        ),
        CategoryEntity(
            name = "Freelance / Bonus",
            hexColor = "#0D9488",
            defaultType = TransactionType.INCOME,
            notes = "Side income, consulting, and bonuses",
            icon = "work"
        ),
        CategoryEntity(
            name = "Housing/Rent",
            hexColor = "#DC2626",
            defaultType = TransactionType.EXPENSE,
            notes = "Rent, mortgage, and home upkeep",
            icon = "home"
        ),
        CategoryEntity(
            name = "Groceries",
            hexColor = "#EA580C",
            defaultType = TransactionType.EXPENSE,
            notes = "Supermarket, fresh produce, household essentials",
            icon = "shopping_cart"
        ),
        CategoryEntity(
            name = "Dining Out",
            hexColor = "#D97706",
            defaultType = TransactionType.EXPENSE,
            notes = "Restaurants, cafes, and takeaway",
            icon = "restaurant"
        ),
        CategoryEntity(
            name = "Transport",
            hexColor = "#0284C7",
            defaultType = TransactionType.EXPENSE,
            notes = "Commute, ride-share, public transit",
            icon = "directions_car"
        ),
        CategoryEntity(
            name = "Fuel",
            hexColor = "#0369A1",
            defaultType = TransactionType.EXPENSE,
            notes = "Petrol, diesel, and EV charging",
            icon = "local_gas_station"
        ),
        CategoryEntity(
            name = "Utilities",
            hexColor = "#0891B2",
            defaultType = TransactionType.EXPENSE,
            notes = "Electricity, water, internet, mobile bills",
            icon = "bolt"
        ),
        CategoryEntity(
            name = "Entertainment",
            hexColor = "#EC4899",
            defaultType = TransactionType.EXPENSE,
            notes = "Streaming, cinema, events, hobbies",
            icon = "movie"
        ),
        CategoryEntity(
            name = "Electronic Items",
            hexColor = "#6366F1",
            defaultType = TransactionType.EXPENSE,
            notes = "Gadgets, accessories, and tech upgrades",
            icon = "devices"
        ),
        CategoryEntity(
            name = "Healthcare",
            hexColor = "#E11D48",
            defaultType = TransactionType.EXPENSE,
            notes = "Medical visits, pharmacy, wellness",
            icon = "medical_services"
        ),
        CategoryEntity(
            name = "Other",
            hexColor = "#64748B",
            defaultType = TransactionType.EXPENSE,
            notes = "Miscellaneous expenses",
            icon = "category"
        ),
        CategoryEntity(
            name = "Savings Transfer",
            hexColor = "#2563EB",
            defaultType = TransactionType.SAVINGS,
            notes = "Transfers to savings accounts & emergency reserves",
            icon = "savings"
        ),
        CategoryEntity(
            name = "Savings/Investment",
            hexColor = "#7C3AED",
            defaultType = TransactionType.INVESTMENT,
            notes = "Unit trusts, fixed deposits, stocks, and long-term assets",
            icon = "trending_up"
        )
    )

    fun workbookSampleTransactions(categoriesByName: Map<String, CategoryEntity>): List<TransactionEntity> {
        fun catId(name: String): Long =
            categoriesByName[name]?.id
                ?: categoriesByName["Other"]?.id
                ?: categoriesByName.values.first().id

        return listOf(
            TransactionEntity(
                date = FinanceFormatter.dateToMillis(2026, 8, 6),
                description = "Monthly Salary (intake -01)",
                categoryId = catId("Salary"),
                type = TransactionType.INCOME,
                amount = 176000.0,
                notes = "August intake",
                month = "Aug",
                year = 2026
            ),
            TransactionEntity(
                date = FinanceFormatter.dateToMillis(2026, 8, 7),
                description = "Kandy travel & dining",
                categoryId = catId("Dining Out"),
                type = TransactionType.EXPENSE,
                amount = 15000.0,
                notes = "Weekend trip meals",
                month = "Aug",
                year = 2026
            ),
            TransactionEntity(
                date = FinanceFormatter.dateToMillis(2026, 8, 8),
                description = "Household & hand bill",
                categoryId = catId("Other"),
                type = TransactionType.EXPENSE,
                amount = 8600.0,
                notes = "Monthly hand bill",
                month = "Aug",
                year = 2026
            ),
            TransactionEntity(
                date = FinanceFormatter.dateToMillis(2026, 8, 9),
                description = "Charitable donation",
                categoryId = catId("Other"),
                type = TransactionType.EXPENSE,
                amount = 2000.0,
                notes = null,
                month = "Aug",
                year = 2026
            ),
            TransactionEntity(
                date = FinanceFormatter.dateToMillis(2026, 8, 10),
                description = "Monthly savings deposit",
                categoryId = catId("Savings Transfer"),
                type = TransactionType.SAVINGS,
                amount = 25000.0,
                notes = "Emergency reserve transfer",
                month = "Aug",
                year = 2026
            ),
            TransactionEntity(
                date = FinanceFormatter.dateToMillis(2026, 8, 11),
                description = "Petrol refill",
                categoryId = catId("Fuel"),
                type = TransactionType.EXPENSE,
                amount = 5500.0,
                notes = null,
                month = "Aug",
                year = 2026
            ),
            TransactionEntity(
                date = FinanceFormatter.dateToMillis(2026, 8, 14),
                description = "Unit trust investment",
                categoryId = catId("Savings/Investment"),
                type = TransactionType.INVESTMENT,
                amount = 20000.0,
                notes = "Balanced equity fund",
                month = "Aug",
                year = 2026
            ),
            TransactionEntity(
                date = FinanceFormatter.dateToMillis(2026, 9, 5),
                description = "Monthly Salary",
                categoryId = catId("Salary"),
                type = TransactionType.INCOME,
                amount = 180000.0,
                notes = null,
                month = "Sep",
                year = 2026
            ),
            TransactionEntity(
                date = FinanceFormatter.dateToMillis(2026, 9, 8),
                description = "Apartment Rent",
                categoryId = catId("Housing/Rent"),
                type = TransactionType.EXPENSE,
                amount = 45000.0,
                notes = null,
                month = "Sep",
                year = 2026
            ),
            TransactionEntity(
                date = FinanceFormatter.dateToMillis(2026, 9, 12),
                description = "Supermarket Groceries",
                categoryId = catId("Groceries"),
                type = TransactionType.EXPENSE,
                amount = 22400.0,
                notes = null,
                month = "Sep",
                year = 2026
            )
        )
    }

    fun workbookSampleBudgets(
        categoriesByName: Map<String, CategoryEntity>,
        year: Int,
        month: String
    ): List<BudgetEntity> {
        val limits = mapOf(
            "Housing/Rent" to 50000.0,
            "Groceries" to 30000.0,
            "Dining Out" to 15000.0,
            "Transport" to 12000.0,
            "Fuel" to 10000.0,
            "Utilities" to 12000.0,
            "Entertainment" to 8000.0,
            "Healthcare" to 10000.0
        )
        return limits.mapNotNull { (name, limit) ->
            categoriesByName[name]?.let { cat ->
                BudgetEntity(
                    categoryId = cat.id,
                    monthlyLimit = limit,
                    year = year,
                    month = month
                )
            }
        }
    }

    fun workbookSampleGoals(): List<SavingsGoalEntity> = listOf(
        SavingsGoalEntity(
            title = "Emergency Fund",
            targetAmount = 500000.0,
            savedAmount = 175000.0,
            targetDate = FinanceFormatter.dateToMillis(2026, 12, 31)
        ),
        SavingsGoalEntity(
            title = "Hill Country Vacation",
            targetAmount = 150000.0,
            savedAmount = 150000.0,
            targetDate = FinanceFormatter.dateToMillis(2026, 12, 15)
        ),
        SavingsGoalEntity(
            title = "New Workstation Laptop",
            targetAmount = 320000.0,
            savedAmount = 95000.0,
            targetDate = FinanceFormatter.dateToMillis(2026, 11, 30)
        )
    )
}
