package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month")
    fun observeBudgetsForPeriod(year: Int, month: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY year DESC, month ASC")
    fun observeAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY year DESC, month ASC")
    suspend fun getAllBudgetsOnce(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND year = :year AND month = :month LIMIT 1")
    suspend fun getBudgetForCategoryPeriod(categoryId: Long, year: Int, month: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}
