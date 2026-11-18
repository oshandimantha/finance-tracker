package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.BackupExportManager
import com.example.data.repository.RoomFinanceRepository
import com.example.domain.model.FinanceFormatter
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: RoomFinanceRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomFinanceRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app name from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Finance Tracker", appName)
    }

    @Test
    fun `lkr currency formatter matches Rs pattern`() {
        assertEquals("Rs 17,600.00", FinanceFormatter.formatLkr(17600.0))
        assertEquals("-Rs 5,000.00", FinanceFormatter.formatLkr(-5000.0))
        assertEquals("Rs 0.00", FinanceFormatter.formatLkr(0.0))
    }

    @Test
    fun `repository starts clean without dummy categories and supports backup roundtrip and reset`() = runTest {
        repository.ensureSeededCategories()
        assertTrue(repository.observeActiveCategories().first().isEmpty())

        val salaryCatId = repository.insertCategory(
            CategoryEntity(
                name = "Business Sales",
                hexColor = "#15803D",
                defaultType = TransactionType.INCOME,
                icon = "payments"
            )
        )

        repository.insertTransaction(
            TransactionEntity(
                date = FinanceFormatter.dateToMillis(2040, 8, 6),
                description = "Monthly Revenue",
                categoryId = salaryCatId,
                type = TransactionType.INCOME,
                amount = 176000.0,
                month = "Aug",
                year = 2040
            )
        )

        val snapshot = repository.getExportSnapshot()
        val json = BackupExportManager.generateBackupJson(snapshot)
        val parsed = BackupExportManager.parseBackupJson(json)
        assertEquals(1, parsed.categories.size)
        assertEquals(1, parsed.transactions.size)
        assertEquals(2040, parsed.transactions.first().year)
        assertEquals(176000.0, parsed.transactions.first().amount, 0.001)

        val csv = BackupExportManager.generateCsv(snapshot)
        assertTrue(csv.contains("Monthly Revenue"))
        assertTrue(csv.contains("176000.00"))

        repository.resetAllAppData()
        assertTrue(repository.observeAllTransactions().first().isEmpty())
        assertTrue(repository.observeActiveCategories().first().isEmpty())
    }
}
