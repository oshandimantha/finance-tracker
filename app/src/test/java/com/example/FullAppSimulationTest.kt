package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.repository.BackupExportManager
import com.example.data.repository.RoomFinanceRepository
import com.example.ui.navigation.OshanFinanceApp
import com.example.ui.theme.OshanFinanceTheme
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-420dpi")
class FullAppSimulationTest {

    @Rule
    @JvmField
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: RoomFinanceRepository
    private lateinit var appPreferences: AppPreferences
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomFinanceRepository(db)
        appPreferences = AppPreferences(context)
        runBlocking {
            appPreferences.setAppBranding(
                AppPreferences.DEFAULT_APP_NAME,
                AppPreferences.DEFAULT_APP_SUBTITLE
            )
            appPreferences.setAppLockEnabled(false)
            appPreferences.setPrivacyScreenEnabled(false)
        }
        financeViewModel = FinanceViewModel(repository)
        settingsViewModel = SettingsViewModel(appPreferences)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `full end to end user simulation across all 6 screens exports and reset`() {
        composeRule.setContent {
            OshanFinanceTheme {
                OshanFinanceApp(
                    viewModel = financeViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Dashboard starts clean with Finance Tracker and no dummy categories
        composeRule.onNodeWithTag("kpi_net_worth_card").assertIsDisplayed()
        composeRule.onNode(hasText("Finance Tracker")).assertIsDisplayed()

        // 2. Open Settings -> Load Sample Finance Entries
        composeRule.onNodeWithTag("top_bar_settings_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasTestTag("load_sample_workbook_button"))
        composeRule.onNodeWithTag("load_sample_workbook_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repository.observeAllTransactions().first().size == 10 }
        }

        // Verify sample data loaded and exports generate non-zero byte content
        runBlocking {
            val csv = financeViewModel.buildCsvExport()
            val xls = financeViewModel.buildExcelExport()
            val json = financeViewModel.buildJsonBackup()
            assertTrue(csv.toByteArray(Charsets.UTF_8).size > 100)
            assertTrue(xls.toByteArray(Charsets.UTF_8).size > 500)
            assertTrue(json.toByteArray(Charsets.UTF_8).size > 500)
            val restored = BackupExportManager.parseBackupJson("\uFEFF$json")
            assertEquals(10, restored.transactions.size)
            assertEquals(14, restored.categories.size)
        }

        // Open App Info dialog and verify branding & developer credit
        composeRule.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasTestTag("app_info_button"))
        composeRule.onNodeWithTag("app_info_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasText("App v1.2") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        composeRule.onNode(hasText("Developed by Oshan Dimantha") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        composeRule.onNode(hasText("Close") and hasAnyAncestor(isDialog())).performClick()
        composeRule.waitForIdle()

        // Return to Dashboard and simulate period filtering (2026 -> Aug -> All)
        composeRule.onNodeWithTag("settings_back_button").performClick()
        composeRule.waitUntil(5_000) {
            val s = financeViewModel.dashboardUiState.value as? DashboardUiState.Success
            s != null && s.summary.totalNetWorth > 0.0
        }

        composeRule.onNodeWithTag("period_year_selector").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("year_option_2026").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("period_month_selector").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("month_option_Aug").performClick()
        composeRule.waitUntil(5_000) {
            val s = financeViewModel.dashboardUiState.value as? DashboardUiState.Success
            s != null && kotlin.math.abs(s.summary.totalIncome - 176000.0) < 0.01
        }

        composeRule.onNodeWithTag("period_month_selector").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("month_option_all").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("period_year_selector").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("year_option_all").performClick()
        composeRule.waitForIdle()

        // 3. Transactions Ledger: add transaction, search, clear search
        composeRule.onNodeWithTag("nav_transactions").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_transaction_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tx_amount_input").performScrollTo().performTextInput("12500")
        composeRule.onNodeWithTag("tx_description_input").performScrollTo().performTextInput("Office Printer Ink")
        composeRule.onNodeWithTag("tx_save_button").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            runBlocking {
                repository.observeAllTransactions().first().any { it.description == "Office Printer Ink" }
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Office Printer Ink").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("tx_search_input").performTextInput("Printer")
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Office Printer Ink").onFirst().assertIsDisplayed()
        composeRule.onNodeWithTag("tx_search_input").performTextClearance()
        composeRule.waitForIdle()

        // 4. Budget Screen: add new Business Category & verify it shows in Budget
        composeRule.onNodeWithTag("nav_budget").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("budget_summary_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("budget_add_category_fab").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("New Category").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasText("Business")).performScrollTo().performClick()
        composeRule.onNodeWithTag("category_icon_storefront").performScrollTo().performClick()
        composeRule.onNodeWithTag("category_name_input").performScrollTo().performTextInput("Shop Supplies")
        composeRule.onNodeWithTag("category_save_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking {
                repository.observeActiveCategories().first().any { it.name == "Shop Supplies" }
            }
        }
        composeRule.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasText("Shop Supplies"))
        composeRule.onAllNodesWithText("Shop Supplies").onFirst().assertIsDisplayed()

        // 5. Analytics Screen: verify bar chart and line chart render cleanly
        composeRule.onNodeWithTag("nav_analytics").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("monthly_expense_bar_chart").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("monthly_expense_bar_chart").assertIsDisplayed()
        composeRule.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasTestTag("category_trend_line_chart"))
        composeRule.onNodeWithTag("category_trend_line_chart").assertIsDisplayed()

        // 6. Goals Screen: create a goal and verify it appears
        composeRule.onNodeWithTag("nav_goals").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_goal_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("goal_title_input").performTextInput("Store Expansion")
        composeRule.onNodeWithTag("goal_target_input").performTextInput("500000")
        composeRule.onNodeWithTag("goal_save_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking {
                repository.observeAllGoals().first().any { it.title == "Store Expansion" }
            }
        }
        composeRule.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasText("Store Expansion"))
        composeRule.onAllNodesWithText("Store Expansion").onFirst().assertIsDisplayed()

        // 7. Reset App Data from Settings & verify clean slate
        composeRule.onNodeWithTag("top_bar_settings_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasTestTag("add_category_button"))
        composeRule.onNodeWithTag("reset_app_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("confirm_reset_app_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("confirm_reset_app_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repository.observeAllTransactions().first().isEmpty() }
        }

        runBlocking {
            assertTrue(repository.observeAllTransactions().first().isEmpty())
            assertTrue(repository.observeAllCategories().first().isEmpty())
            assertTrue(repository.observeAllGoals().first().isEmpty())
        }
    }

    @Test
    fun `ledger v1_2 supports card edit card delete sheet delete and multi select bulk edit and delete`() {
        runBlocking {
            repository.loadSampleWorkbookData(2026, "Aug")
        }

        composeRule.setContent {
            OshanFinanceTheme {
                OshanFinanceApp(
                    viewModel = financeViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
        composeRule.waitForIdle()

        // Navigate to Ledger
        composeRule.onNodeWithTag("nav_transactions").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("tx_select_mode_button").fetchSemanticsNodes().isNotEmpty()
        }

        val initialTxs = runBlocking { repository.observeAllTransactions().first() }
        assertTrue(initialTxs.size >= 3)
        val firstTx = initialTxs[0]
        val secondTx = initialTxs[1]
        val thirdTx = initialTxs[2]

        // 1. Edit first transaction via dedicated Ledger card Edit icon button
        composeRule.onNodeWithTag("tx_edit_button_${firstTx.id}").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("tx_description_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("tx_description_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("tx_description_input").performTextInput("Updated Ledger Entry")
        composeRule.onNodeWithTag("tx_amount_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("tx_amount_input").performTextInput("9850")
        composeRule.onNodeWithTag("tx_save_button").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            runBlocking {
                repository.observeAllTransactions().first().any {
                    it.id == firstTx.id && it.description == "Updated Ledger Entry" && kotlin.math.abs(it.amount - 9850.0) < 0.01
                }
            }
        }

        // 2. Delete second transaction via Ledger card Delete icon button -> confirm -> Undo
        composeRule.onNodeWithTag("tx_delete_button_${secondTx.id}").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("confirm_delete_tx_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("confirm_delete_tx_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repository.observeAllTransactions().first().none { it.id == secondTx.id } }
        }
        composeRule.onNode(hasText("Undo")).performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repository.observeAllTransactions().first().any { it.id == secondTx.id } }
        }

        // 3. Delete third transaction from inside Edit Transaction sheet
        composeRule.onNodeWithTag("tx_edit_button_${thirdTx.id}").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("tx_sheet_delete_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("tx_sheet_delete_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("tx_sheet_confirm_delete_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("tx_sheet_confirm_delete_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repository.observeAllTransactions().first().none { it.id == thirdTx.id } }
        }

        // 4. Multi-select Bulk Edit Category + Bulk Delete
        val targetCategory = runBlocking {
            repository.observeActiveCategories().first().last()
        }
        composeRule.onNodeWithTag("tx_select_mode_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("tx_selection_bar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("tx_select_checkbox_${firstTx.id}").performClick()
        composeRule.onNodeWithTag("tx_select_checkbox_${secondTx.id}").performClick()
        composeRule.onNodeWithTag("tx_bulk_edit_category_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("tx_bulk_category_dialog").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("bulk_category_option_${targetCategory.id}").performScrollTo().performClick()
        composeRule.onNodeWithTag("bulk_category_apply_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking {
                val updated = repository.observeAllTransactions().first().filter { it.id == firstTx.id || it.id == secondTx.id }
                updated.size == 2 && updated.all { it.categoryId == targetCategory.id && it.type == targetCategory.defaultType }
            }
        }

        // Now bulk delete the same two transactions
        composeRule.onNodeWithTag("tx_select_mode_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("tx_selection_bar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("tx_select_checkbox_${firstTx.id}").performClick()
        composeRule.onNodeWithTag("tx_select_checkbox_${secondTx.id}").performClick()
        composeRule.onNodeWithTag("tx_bulk_delete_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("confirm_bulk_delete_tx_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("confirm_bulk_delete_tx_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking {
                repository.observeAllTransactions().first().none { it.id == firstTx.id || it.id == secondTx.id }
            }
        }
    }
}
