package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:year IS NULL OR year = :year)
          AND (:month IS NULL OR month = :month)
        ORDER BY date DESC, id DESC
        """
    )
    fun observeTransactionsByPeriod(year: Int?, month: String?): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE year = :year ORDER BY date ASC")
    fun observeTransactionsByYear(year: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: Long): Int

    @Query("DELETE FROM transactions WHERE categoryId = :categoryId")
    suspend fun deleteTransactionsByCategoryId(categoryId: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: List<Long>)

    @Query("UPDATE transactions SET categoryId = :categoryId, type = :type WHERE id IN (:ids)")
    suspend fun updateTransactionsCategoryAndType(
        ids: List<Long>,
        categoryId: Long,
        type: com.example.domain.model.TransactionType
    )

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
