package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseSeeder
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.FinanceFormatter
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    fun observeActiveCategories(): Flow<List<CategoryEntity>>
    fun observeAllCategories(): Flow<List<CategoryEntity>>
    fun observeAllTransactions(): Flow<List<TransactionEntity>>
    fun observeTransactionsByPeriod(year: Int?, month: String?): Flow<List<TransactionEntity>>
    fun observeTransactionsByYear(year: Int): Flow<List<TransactionEntity>>
    fun observeBudgetsForPeriod(year: Int, month: String): Flow<List<BudgetEntity>>
    fun observeAllGoals(): Flow<List<SavingsGoalEntity>>

    suspend fun ensureSeededCategories()
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun deleteTransactions(transactions: List<TransactionEntity>)
    suspend fun bulkUpdateTransactionsCategory(
        transactionIds: List<Long>,
        categoryId: Long,
        type: TransactionType
    )

    suspend fun upsertBudget(categoryId: Long, year: Int, month: String, monthlyLimit: Double)
    suspend fun copyPreviousMonthBudgets(targetYear: Int, targetMonth: String): Int

    suspend fun insertGoal(goal: SavingsGoalEntity): Long
    suspend fun updateGoal(goal: SavingsGoalEntity)
    suspend fun deleteGoal(goal: SavingsGoalEntity)
    suspend fun depositToGoal(goalId: Long, amount: Double, alsoLogSavingsTransaction: Boolean)

    suspend fun insertCategory(category: CategoryEntity): Long
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun softDeleteCategory(categoryId: Long)
    suspend fun restoreCategory(categoryId: Long)
    suspend fun countTransactionsForCategory(categoryId: Long): Int
    suspend fun deleteCategoryPermanently(categoryId: Long, deleteAssociatedTransactions: Boolean)

    suspend fun getExportSnapshot(): BackupSnapshot
    suspend fun restoreFromSnapshot(snapshot: BackupSnapshot)
    suspend fun loadSampleWorkbookData(targetYear: Int, targetMonth: String)
    suspend fun resetAllAppData()
}

data class BackupSnapshot(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val goals: List<SavingsGoalEntity>
)

class RoomFinanceRepository(
    private val database: AppDatabase
) : FinanceRepository {

    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val goalDao = database.savingsGoalDao()

    override fun observeActiveCategories(): Flow<List<CategoryEntity>> =
        categoryDao.observeActiveCategories()

    override fun observeAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.observeAllCategories()

    override fun observeAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.observeAllTransactions()

    override fun observeTransactionsByPeriod(year: Int?, month: String?): Flow<List<TransactionEntity>> =
        transactionDao.observeTransactionsByPeriod(year, month)

    override fun observeTransactionsByYear(year: Int): Flow<List<TransactionEntity>> =
        transactionDao.observeTransactionsByYear(year)

    override fun observeBudgetsForPeriod(year: Int, month: String): Flow<List<BudgetEntity>> =
        budgetDao.observeBudgetsForPeriod(year, month)

    override fun observeAllGoals(): Flow<List<SavingsGoalEntity>> =
        goalDao.observeAllGoals()

    override suspend fun ensureSeededCategories() {
        // No dummy categories auto-seeded; users add their own categories.
    }

    override suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
        if (transactions.isNotEmpty()) {
            transactionDao.insertTransactions(transactions)
        }
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    override suspend fun deleteTransactions(transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) return
        database.withTransaction {
            transactions.map { it.id }.chunked(500).forEach { chunk ->
                transactionDao.deleteTransactionsByIds(chunk)
            }
        }
    }

    override suspend fun bulkUpdateTransactionsCategory(
        transactionIds: List<Long>,
        categoryId: Long,
        type: TransactionType
    ) {
        if (transactionIds.isEmpty()) return
        database.withTransaction {
            transactionIds.chunked(500).forEach { chunk ->
                transactionDao.updateTransactionsCategoryAndType(chunk, categoryId, type)
            }
        }
    }

    override suspend fun upsertBudget(
        categoryId: Long,
        year: Int,
        month: String,
        monthlyLimit: Double
    ) {
        val existing = budgetDao.getBudgetForCategoryPeriod(categoryId, year, month)
        val entity = existing?.copy(monthlyLimit = monthlyLimit)
            ?: BudgetEntity(
                categoryId = categoryId,
                monthlyLimit = monthlyLimit,
                year = year,
                month = month
            )
        budgetDao.upsertBudget(entity)
    }

    override suspend fun copyPreviousMonthBudgets(targetYear: Int, targetMonth: String): Int {
        val (prevMonth, prevYear) = FinanceFormatter.previousMonthYear(targetMonth, targetYear)
        val allBudgets = budgetDao.getAllBudgetsOnce()
        val prevBudgets = allBudgets.filter { it.year == prevYear && it.month == prevMonth }
        if (prevBudgets.isEmpty()) return 0
        var copied = 0
        database.withTransaction {
            prevBudgets.forEach { prev ->
                val existing = budgetDao.getBudgetForCategoryPeriod(prev.categoryId, targetYear, targetMonth)
                if (existing == null) {
                    budgetDao.upsertBudget(
                        BudgetEntity(
                            categoryId = prev.categoryId,
                            monthlyLimit = prev.monthlyLimit,
                            year = targetYear,
                            month = targetMonth
                        )
                    )
                    copied++
                }
            }
        }
        return copied
    }

    override suspend fun insertGoal(goal: SavingsGoalEntity): Long =
        goalDao.insertGoal(goal)

    override suspend fun updateGoal(goal: SavingsGoalEntity) =
        goalDao.updateGoal(goal)

    override suspend fun deleteGoal(goal: SavingsGoalEntity) =
        goalDao.deleteGoal(goal)

    override suspend fun depositToGoal(
        goalId: Long,
        amount: Double,
        alsoLogSavingsTransaction: Boolean
    ) {
        if (amount <= 0.0) return
        database.withTransaction {
            val goal = goalDao.getGoalById(goalId) ?: return@withTransaction
            goalDao.updateGoal(goal.copy(savedAmount = goal.savedAmount + amount))

            if (alsoLogSavingsTransaction) {
                val categories = categoryDao.getAllCategoriesOnce()
                val savingsCat = categories.firstOrNull {
                    !it.isDeleted && it.defaultType == TransactionType.SAVINGS
                } ?: categories.firstOrNull { !it.isDeleted }
                if (savingsCat != null) {
                    val now = System.currentTimeMillis()
                    val (month, year) = FinanceFormatter.extractMonthAndYear(now)
                    transactionDao.insertTransaction(
                        TransactionEntity(
                            date = now,
                            description = "Goal deposit: ${goal.title}",
                            categoryId = savingsCat.id,
                            type = TransactionType.SAVINGS,
                            amount = amount,
                            notes = "Deposited toward savings goal",
                            month = month,
                            year = year
                        )
                    )
                }
            }
        }
    }

    override suspend fun insertCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category)

    override suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    override suspend fun softDeleteCategory(categoryId: Long) =
        categoryDao.softDeleteCategory(categoryId)

    override suspend fun restoreCategory(categoryId: Long) =
        categoryDao.restoreCategory(categoryId)

    override suspend fun countTransactionsForCategory(categoryId: Long): Int =
        transactionDao.countTransactionsForCategory(categoryId)

    override suspend fun deleteCategoryPermanently(
        categoryId: Long,
        deleteAssociatedTransactions: Boolean
    ) {
        database.withTransaction {
            if (deleteAssociatedTransactions) {
                transactionDao.deleteTransactionsByCategoryId(categoryId)
            }
            categoryDao.deleteCategoryById(categoryId)
        }
    }

    override suspend fun getExportSnapshot(): BackupSnapshot = BackupSnapshot(
        categories = categoryDao.getAllCategoriesOnce(),
        transactions = transactionDao.getAllTransactionsOnce(),
        budgets = budgetDao.getAllBudgetsOnce(),
        goals = goalDao.getAllGoalsOnce()
    )

    override suspend fun restoreFromSnapshot(snapshot: BackupSnapshot) {
        database.withTransaction {
            budgetDao.deleteAllBudgets()
            transactionDao.deleteAllTransactions()
            goalDao.deleteAllGoals()
            categoryDao.deleteAllCategories()

            categoryDao.insertCategories(snapshot.categories)
            transactionDao.insertTransactions(snapshot.transactions)
            budgetDao.upsertBudgets(snapshot.budgets)
            goalDao.insertGoals(snapshot.goals)
        }
    }

    override suspend fun loadSampleWorkbookData(targetYear: Int, targetMonth: String) {
        database.withTransaction {
            if (categoryDao.getAllCategoriesOnce().isEmpty()) {
                categoryDao.insertCategories(DatabaseSeeder.defaultCategories())
            }
            val cats = categoryDao.getAllCategoriesOnce().associateBy { it.name }
            transactionDao.insertTransactions(DatabaseSeeder.workbookSampleTransactions(cats))
            val sampleBudgets = DatabaseSeeder.workbookSampleBudgets(cats, targetYear, targetMonth)
            sampleBudgets.forEach { b ->
                val existing = budgetDao.getBudgetForCategoryPeriod(b.categoryId, b.year, b.month)
                if (existing == null) {
                    budgetDao.upsertBudget(b)
                }
            }
            if (goalDao.getAllGoalsOnce().isEmpty()) {
                goalDao.insertGoals(DatabaseSeeder.workbookSampleGoals())
            }
        }
    }

    override suspend fun resetAllAppData() {
        database.withTransaction {
            budgetDao.deleteAllBudgets()
            transactionDao.deleteAllTransactions()
            goalDao.deleteAllGoals()
            categoryDao.deleteAllCategories()
        }
    }
}
