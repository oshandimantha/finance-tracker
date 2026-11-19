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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput

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

    @Test
    fun `sample data exports non-empty csv excel and json and restores with bom`() = runTest {
        repository.loadSampleWorkbookData(2026, "Aug")
        val snapshot = repository.getExportSnapshot()
        assertTrue(snapshot.categories.isNotEmpty())
        assertTrue(snapshot.transactions.isNotEmpty())
        assertTrue(snapshot.budgets.isNotEmpty())
        assertTrue(snapshot.goals.isNotEmpty())

        val csv = BackupExportManager.generateCsv(snapshot)
        assertTrue(csv.toByteArray(Charsets.UTF_8).isNotEmpty())
        assertTrue(csv.contains("Monthly Salary"))

        val excel = BackupExportManager.generateExcelSpreadsheetXml(snapshot)
        assertTrue(excel.toByteArray(Charsets.UTF_8).isNotEmpty())
        assertTrue(excel.contains("<Worksheet ss:Name=\"Transactions\">"))
        assertTrue(excel.contains("<Worksheet ss:Name=\"Budget\">"))
        assertTrue(excel.contains("<Worksheet ss:Name=\"Savings Goals\">"))

        val jsonWithBom = "\uFEFF" + BackupExportManager.generateBackupJson(snapshot)
        val parsed = BackupExportManager.parseBackupJson(jsonWithBom)
        assertEquals(snapshot.transactions.size, parsed.transactions.size)
        assertEquals(snapshot.categories.size, parsed.categories.size)
        assertEquals(snapshot.budgets.size, parsed.budgets.size)
        assertEquals(snapshot.goals.size, parsed.goals.size)
    }

    @org.junit.Rule
    @JvmField
    val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun `category editor dialog renders and switches icon filters without crashing`() {
        var savedName: String? = null
        var savedIcon: String? = null
        composeRule.setContent {
            com.example.ui.theme.OshanFinanceTheme {
                com.example.ui.components.CategoryEditorDialog(
                    existingCategory = null,
                    onDismiss = {},
                    onSave = { _, name, _, _, icon, _ ->
                        savedName = name
                        savedIcon = icon
                    }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("Business")).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_icon_storefront").performScrollTo().performClick()
        composeRule.onNode(hasText("Personal")).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasText("All", substring = true)).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_name_input").performScrollTo().performTextInput("Office Rent")
        composeRule.onNodeWithTag("category_save_button").performClick()
        assertEquals("Office Rent", savedName)
        assertEquals("storefront", savedIcon)
    }

    @Test
    fun `main activity cold launch renders without crashing`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertTrue(!activity.isFinishing)
        controller.pause().stop().destroy()
    }
}
