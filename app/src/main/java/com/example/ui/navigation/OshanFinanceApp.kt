package com.example.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.TransactionEditorSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.viewmodel.FinanceEvent
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.TransactionsUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OshanFinanceApp(
    viewModel: FinanceViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = AppDestination.fromRoute(backStackEntry?.destination?.route)

    val dashboardState by viewModel.dashboardUiState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsUiState.collectAsStateWithLifecycle()
    val budgetState by viewModel.budgetUiState.collectAsStateWithLifecycle()
    val analyticsState by viewModel.analyticsUiState.collectAsStateWithLifecycle()
    val goalsState by viewModel.goalsUiState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val businessName by settingsViewModel.appName.collectAsStateWithLifecycle()
    val businessSubtitle by settingsViewModel.appSubtitle.collectAsStateWithLifecycle()
    val appLockEnabled by settingsViewModel.appLockEnabled.collectAsStateWithLifecycle()
    val privacyScreenEnabled by settingsViewModel.privacyScreenEnabled.collectAsStateWithLifecycle()
    val availableYears by viewModel.availableYears.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(Unit) {
        var snackbarJob: Job? = null
        viewModel.events.collect { event ->
            snackbarJob?.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarJob = launch {
                when (event) {
                    is FinanceEvent.ShowMessage -> {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                    is FinanceEvent.TransactionDeleted -> {
                        val result = snackbarHostState.showSnackbar(
                            message = "Deleted \"${event.transaction.description}\"",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDeleteTransaction(event.transaction)
                        }
                    }
                    is FinanceEvent.TransactionsBulkDeleted -> {
                        val count = event.transactions.size
                        val result = snackbarHostState.showSnackbar(
                            message = "Deleted $count ${if (count == 1) "transaction" else "transactions"}",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDeleteTransactions(event.transactions)
                        }
                    }
                }
            }
        }
    }

    val activeCategories = (transactionsState as? TransactionsUiState.Success)?.activeCategories.orEmpty()

    if (showTransactionSheet) {
        TransactionEditorSheet(
            existingTransaction = editingTransaction,
            categories = activeCategories,
            onDismiss = {
                showTransactionSheet = false
                editingTransaction = null
            },
            onDelete = if (editingTransaction != null) {
                { tx ->
                    viewModel.deleteTransaction(tx)
                    showTransactionSheet = false
                    editingTransaction = null
                }
            } else {
                null
            },
            onSave = { id, date, desc, catId, type, amount, notes ->
                viewModel.saveTransaction(id, date, desc, catId, type, amount, notes)
                showTransactionSheet = false
                editingTransaction = null
            }
        )
    }

    fun navigateTo(destination: AppDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        val snackbarBottomPadding = when (currentDestination) {
            AppDestination.TRANSACTIONS,
            AppDestination.BUDGET,
            AppDestination.GOALS -> 76.dp
            else -> 0.dp
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = snackbarBottomPadding)
                )
            },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.modern_finance_logo_1794928820205),
                                contentDescription = businessName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Column {
                                Text(
                                    text = businessName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (currentDestination == AppDestination.DASHBOARD) {
                                        businessSubtitle
                                    } else {
                                        currentDestination.title
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (!useNavigationRail && currentDestination == AppDestination.SETTINGS) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.testTag("settings_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back)
                                )
                            }
                        }
                    },
                    actions = {
                        if (!useNavigationRail && currentDestination != AppDestination.SETTINGS) {
                            IconButton(
                                onClick = { navigateTo(AppDestination.SETTINGS) },
                                modifier = Modifier.testTag("top_bar_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.cd_settings),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail && currentDestination != AppDestination.SETTINGS) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        AppDestination.bottomBarDestinations.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.DASHBOARD.route
                    ) {
                        composable(AppDestination.DASHBOARD.route) {
                            DashboardScreen(
                                uiState = dashboardState,
                                availableYears = availableYears,
                                onYearSelected = viewModel::setYearFilter,
                                onMonthSelected = viewModel::setMonthFilter,
                                onAddTransactionClick = {
                                    editingTransaction = null
                                    showTransactionSheet = true
                                },
                                onNavigateToTransactions = {
                                    navigateTo(AppDestination.TRANSACTIONS)
                                }
                            )
                        }
                        composable(AppDestination.TRANSACTIONS.route) {
                            TransactionsScreen(
                                uiState = transactionsState,
                                availableYears = availableYears,
                                onYearSelected = viewModel::setYearFilter,
                                onMonthSelected = viewModel::setMonthFilter,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onTypeSelected = viewModel::setSelectedTransactionType,
                                onAddClick = {
                                    editingTransaction = null
                                    showTransactionSheet = true
                                },
                                onEditClick = { tx ->
                                    editingTransaction = tx
                                    showTransactionSheet = true
                                },
                                onDeleteTransaction = viewModel::deleteTransaction,
                                onBulkDeleteTransactions = viewModel::deleteTransactions,
                                onBulkUpdateCategory = viewModel::bulkUpdateTransactionsCategory
                            )
                        }
                        composable(AppDestination.BUDGET.route) {
                            BudgetScreen(
                                uiState = budgetState,
                                availableYears = availableYears,
                                onPeriodSelected = viewModel::setBudgetPeriod,
                                onUpdateCategoryBudget = viewModel::updateCategoryBudget,
                                onCopyPreviousMonth = viewModel::copyPreviousMonthBudgets,
                                onSaveCategory = viewModel::saveCategory,
                                onCountCategoryTransactions = viewModel::countTransactionsForCategory,
                                onDeleteCategoryPermanently = viewModel::deleteCategoryPermanently
                            )
                        }
                        composable(AppDestination.ANALYTICS.route) {
                            AnalyticsScreen(
                                uiState = analyticsState,
                                availableYears = availableYears,
                                onYearSelected = viewModel::setAnalyticsYear,
                                onCategorySelected = viewModel::setAnalyticsCategory
                            )
                        }
                        composable(AppDestination.GOALS.route) {
                            GoalsScreen(
                                uiState = goalsState,
                                onSaveGoal = viewModel::saveGoal,
                                onDepositToGoal = viewModel::depositToGoal,
                                onDeleteGoal = viewModel::deleteGoal
                            )
                        }
                        composable(AppDestination.SETTINGS.route) {
                            SettingsScreen(
                                uiState = settingsState,
                                businessName = businessName,
                                businessSubtitle = businessSubtitle,
                                appLockEnabled = appLockEnabled,
                                privacyScreenEnabled = privacyScreenEnabled,
                                onToggleAppLock = settingsViewModel::setAppLockEnabled,
                                onTogglePrivacyScreen = settingsViewModel::setPrivacyScreenEnabled,
                                onUpdateBusinessBranding = { name, subtitle ->
                                    settingsViewModel.updateAppBranding(name, subtitle) { savedName ->
                                        viewModel.notifyMessage("App name updated to $savedName")
                                    }
                                },
                                onSaveCategory = viewModel::saveCategory,
                                onSoftDeleteCategory = viewModel::softDeleteCategory,
                                onRestoreCategory = viewModel::restoreCategory,
                                onCountCategoryTransactions = viewModel::countTransactionsForCategory,
                                onDeleteCategoryPermanently = viewModel::deleteCategoryPermanently,
                                onBuildCsv = viewModel::buildCsvExport,
                                onBuildExcelXml = viewModel::buildExcelExport,
                                onBuildJsonBackup = viewModel::buildJsonBackup,
                                onRestoreBackupJson = viewModel::restoreFromBackupJson,
                                onLoadSampleWorkbook = viewModel::loadSampleWorkbookData,
                                onResetAppData = {
                                    viewModel.resetAllAppData()
                                    settingsViewModel.resetAppBranding()
                                },
                                onNotifyMessage = viewModel::notifyMessage
                            )
                        }
                    }
                }
            }
        }
    }
}
