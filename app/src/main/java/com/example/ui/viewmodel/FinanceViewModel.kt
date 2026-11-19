package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.BackupExportManager
import com.example.data.repository.FinanceRepository
import com.example.domain.model.BudgetItemStatus
import com.example.domain.model.BudgetSummary
import com.example.domain.model.CategorySpending
import com.example.domain.model.DashboardSummary
import com.example.domain.model.FinanceFormatter
import com.example.domain.model.GoalCountdownStatus
import com.example.domain.model.GoalUiModel
import com.example.domain.model.MoMVariance
import com.example.domain.model.MonthlyTrendPoint
import com.example.domain.model.PeriodFilter
import com.example.domain.model.TransactionDateGroup
import com.example.domain.model.TransactionItemUi
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FinanceEvent {
    data class ShowMessage(val message: String) : FinanceEvent
    data class TransactionDeleted(val transaction: TransactionEntity) : FinanceEvent
    data class TransactionsBulkDeleted(val transactions: List<TransactionEntity>) : FinanceEvent
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FinanceViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _periodFilter = MutableStateFlow(PeriodFilter(year = null, month = null))
    val periodFilter: StateFlow<PeriodFilter> = _periodFilter

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTxType = MutableStateFlow<TransactionType?>(null)

    private val _budgetYear = MutableStateFlow(FinanceFormatter.defaultYear())
    private val _budgetMonth = MutableStateFlow(FinanceFormatter.currentMonthAbbrev())

    private val _analyticsYear = MutableStateFlow(FinanceFormatter.defaultYear())
    private val _analyticsCategoryId = MutableStateFlow<Long?>(null)

    private val _events = MutableSharedFlow<FinanceEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<FinanceEvent> = _events.asSharedFlow()

    private val allTransactionsFlow = repository.observeAllTransactions()
    private val allCategoriesFlow = repository.observeAllCategories()
    private val activeCategoriesFlow = repository.observeActiveCategories()
    private val allGoalsFlow = repository.observeAllGoals()

    val availableYears: StateFlow<List<Int>> = allTransactionsFlow
        .map { transactions ->
            val years = transactions.map { it.year }.toMutableSet()
            years.add(FinanceFormatter.defaultYear())
            years.sortedDescending()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(FinanceFormatter.defaultYear())
        )

    init {
        viewModelScope.launch {
            repository.ensureSeededCategories()
        }
    }

    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        _periodFilter,
        allTransactionsFlow,
        allCategoriesFlow
    ) { period, allTx, allCats ->
        val filteredTx = allTx.filter { tx ->
            (period.year == null || tx.year == period.year) &&
                (period.month == null || tx.month == period.month)
        }

        val totalIncome = filteredTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpenses = filteredTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpenses
        val savingsRate = if (totalIncome > 0.0) netSavings / totalIncome else 0.0
        val savingsTransferred = filteredTx.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amount }
        val totalInvestments = filteredTx.filter { it.type == TransactionType.INVESTMENT }.sumOf { it.amount }

        // Net Worth calculated dynamically for the selected period (All Time, Selected Year, or Selected Month)
        val totalNetWorth = (totalIncome - totalExpenses) + totalInvestments

        val summary = DashboardSummary(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings,
            savingsRate = savingsRate,
            savingsTransferred = savingsTransferred,
            totalInvestments = totalInvestments,
            totalNetWorth = totalNetWorth
        )

        val catMap = allCats.associateBy { it.id }
        val expenseTx = filteredTx.filter { it.type == TransactionType.EXPENSE }
        val categorySpending = expenseTx
            .groupBy { it.categoryId }
            .mapNotNull { (catId, txs) ->
                val cat = catMap[catId] ?: return@mapNotNull null
                val amt = txs.sumOf { it.amount }
                CategorySpending(
                    category = cat,
                    amount = amt,
                    percentage = if (totalExpenses > 0.0) amt / totalExpenses else 0.0
                )
            }
            .sortedByDescending { it.amount }

        // MoM comparison: use selected month/year, or most recent month in filtered period
        val refYear = period.year ?: FinanceFormatter.defaultYear()
        val refMonth = period.month ?: filteredTx.firstOrNull()?.month ?: FinanceFormatter.currentMonthAbbrev()
        val (prevMonth, prevYear) = FinanceFormatter.previousMonthYear(refMonth, refYear)

        val currMonthTx = allTx.filter { it.year == refYear && it.month == refMonth }
        val prevMonthTx = allTx.filter { it.year == prevYear && it.month == prevMonth }

        val currInc = currMonthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val currExp = currMonthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val prevInc = prevMonthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val prevExp = prevMonthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val mom = MoMVariance(
            currentLabel = "$refMonth $refYear",
            previousLabel = "$prevMonth $prevYear",
            currentIncome = currInc,
            previousIncome = prevInc,
            currentExpense = currExp,
            previousExpense = prevExp,
            currentSavings = currInc - currExp,
            previousSavings = prevInc - prevExp
        )

        DashboardUiState.Success(
            period = period,
            summary = summary,
            categorySpending = categorySpending,
            momVariance = mom,
            transactionCount = filteredTx.size
        ) as DashboardUiState
    }
        .catch { emit(DashboardUiState.Error(it.message ?: "Failed to load dashboard")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Loading)

    val transactionsUiState: StateFlow<TransactionsUiState> = combine(
        _periodFilter,
        _searchQuery,
        _selectedTxType,
        allTransactionsFlow,
        allCategoriesFlow
    ) { period, query, selectedType, allTx, allCats ->
        val catMap = allCats.associateBy { it.id }
        val activeCats = allCats.filter { !it.isDeleted }

        val filtered = allTx.filter { tx ->
            val matchesYear = period.year == null || tx.year == period.year
            val matchesMonth = period.month == null || tx.month == period.month
            val matchesType = selectedType == null || tx.type == selectedType
            val catName = catMap[tx.categoryId]?.name.orEmpty()
            val matchesQuery = query.isBlank() ||
                tx.description.contains(query, ignoreCase = true) ||
                catName.contains(query, ignoreCase = true) ||
                (tx.notes?.contains(query, ignoreCase = true) == true)
            matchesYear && matchesMonth && matchesType && matchesQuery
        }

        val groups = filtered
            .groupBy { FinanceFormatter.dayStartMillis(it.date) }
            .entries
            .sortedByDescending { it.key }
            .map { (dayTs, txs) ->
                val items = txs.map { tx -> TransactionItemUi(tx, catMap[tx.categoryId]) }
                val dailyNet = txs.sumOf { tx ->
                    when (tx.type) {
                        TransactionType.INCOME -> tx.amount
                        TransactionType.EXPENSE -> -tx.amount
                        TransactionType.SAVINGS, TransactionType.INVESTMENT -> 0.0
                    }
                }
                TransactionDateGroup(
                    dateLabel = FinanceFormatter.formatDateHeader(dayTs),
                    dayTimestamp = dayTs,
                    items = items,
                    dailyNet = dailyNet
                )
            }

        TransactionsUiState.Success(
            period = period,
            searchQuery = query,
            selectedType = selectedType,
            groups = groups,
            activeCategories = activeCats,
            totalCount = filtered.size
        ) as TransactionsUiState
    }
        .catch { emit(TransactionsUiState.Error(it.message ?: "Failed to load transactions")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState.Loading)

    private val budgetsForSelectedPeriodFlow = combine(_budgetYear, _budgetMonth) { y, m -> y to m }
        .flatMapLatest { (y, m) -> repository.observeBudgetsForPeriod(y, m) }

    val budgetUiState: StateFlow<BudgetUiState> = combine(
        _budgetYear,
        _budgetMonth,
        budgetsForSelectedPeriodFlow,
        allTransactionsFlow,
        activeCategoriesFlow
    ) { year, month, budgets, allTx, activeCats ->
        val expenseCategories = activeCats.filter { it.defaultType == TransactionType.EXPENSE }
        val budgetByCategory = budgets.associateBy { it.categoryId }
        val spentByCategory = allTx
            .filter { it.year == year && it.month == month && it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val items = expenseCategories.map { cat ->
            BudgetItemStatus(
                category = cat,
                monthlyLimit = budgetByCategory[cat.id]?.monthlyLimit ?: 0.0,
                actualSpent = spentByCategory[cat.id] ?: 0.0
            )
        }.sortedWith(
            compareByDescending<BudgetItemStatus> { it.monthlyLimit > 0.0 }
                .thenByDescending { it.actualSpent }
                .thenBy { it.category.name }
        )

        val totalPlanned = items.sumOf { it.monthlyLimit }
        val totalSpent = items.sumOf { it.actualSpent }

        BudgetUiState.Success(
            selectedYear = year,
            selectedMonth = month,
            summary = BudgetSummary(totalPlanned = totalPlanned, totalSpent = totalSpent),
            items = items
        ) as BudgetUiState
    }
        .catch { emit(BudgetUiState.Error(it.message ?: "Failed to load budgets")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetUiState.Loading)

    val analyticsUiState: StateFlow<AnalyticsUiState> = combine(
        _analyticsYear,
        _analyticsCategoryId,
        allTransactionsFlow,
        activeCategoriesFlow
    ) { year, selectedCatId, allTx, activeCats ->
        val yearExpenses = allTx.filter { it.year == year && it.type == TransactionType.EXPENSE }

        val monthlyTotals = FinanceFormatter.MONTHS.mapIndexed { index, monthName ->
            val amt = yearExpenses.filter { it.month == monthName }.sumOf { it.amount }
            MonthlyTrendPoint(month = monthName, monthIndex = index, amount = amt)
        }

        val categoryTrend = FinanceFormatter.MONTHS.mapIndexed { index, monthName ->
            val amt = yearExpenses
                .filter { it.month == monthName && (selectedCatId == null || it.categoryId == selectedCatId) }
                .sumOf { it.amount }
            MonthlyTrendPoint(month = monthName, monthIndex = index, amount = amt)
        }

        val annualTotal = monthlyTotals.sumOf { it.amount }
        val activeMonthsCount = monthlyTotals.count { it.amount > 0.0 }.coerceAtLeast(1)

        AnalyticsUiState.Success(
            selectedYear = year,
            monthlyExpenses = monthlyTotals,
            expenseCategories = activeCats.filter { it.defaultType == TransactionType.EXPENSE },
            selectedCategoryId = selectedCatId,
            categoryTrend = categoryTrend,
            annualTotalExpense = annualTotal,
            averageMonthlyExpense = annualTotal / activeMonthsCount
        ) as AnalyticsUiState
    }
        .catch { emit(AnalyticsUiState.Error(it.message ?: "Failed to load analytics")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState.Loading)

    val goalsUiState: StateFlow<GoalsUiState> = allGoalsFlow
        .combine(MutableStateFlow(Unit)) { goals, _ ->
            val now = System.currentTimeMillis()
            val mapped = goals.map { g ->
                val days = FinanceFormatter.daysUntil(g.targetDate, now)
                val status = when {
                    g.savedAmount >= g.targetAmount && g.targetAmount > 0.0 -> GoalCountdownStatus.COMPLETED
                    days < 0 -> GoalCountdownStatus.OVERDUE
                    days <= 30 -> GoalCountdownStatus.URGENT
                    else -> GoalCountdownStatus.HEALTHY
                }
                GoalUiModel(goal = g, daysLeft = days, status = status)
            }
            GoalsUiState.Success(
                goals = mapped,
                totalTarget = goals.sumOf { it.targetAmount },
                totalSaved = goals.sumOf { it.savedAmount }
            ) as GoalsUiState
        }
        .catch { emit(GoalsUiState.Error(it.message ?: "Failed to load goals")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalsUiState.Loading)

    val settingsUiState: StateFlow<SettingsUiState> = combine(
        allCategoriesFlow,
        allTransactionsFlow,
        repository.observeBudgetsForPeriod(FinanceFormatter.defaultYear(), FinanceFormatter.currentMonthAbbrev()),
        allGoalsFlow
    ) { cats, txs, budgets, goals ->
        SettingsUiState.Success(
            activeCategories = cats.filter { !it.isDeleted },
            archivedCategories = cats.filter { it.isDeleted },
            totalTransactions = txs.size,
            totalBudgets = budgets.size,
            totalGoals = goals.size
        ) as SettingsUiState
    }
        .catch { emit(SettingsUiState.Error(it.message ?: "Failed to load settings")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState.Loading)

    // Period & Filter Actions
    fun setYearFilter(year: Int?) {
        _periodFilter.value = _periodFilter.value.copy(year = year)
    }

    fun setMonthFilter(month: String?) {
        _periodFilter.value = _periodFilter.value.copy(month = month)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTransactionType(type: TransactionType?) {
        _selectedTxType.value = type
    }

    fun setBudgetPeriod(year: Int, month: String) {
        _budgetYear.value = year
        _budgetMonth.value = month
    }

    fun setAnalyticsYear(year: Int) {
        _analyticsYear.value = year
    }

    fun setAnalyticsCategory(categoryId: Long?) {
        _analyticsCategoryId.value = categoryId
    }

    // Transaction CRUD
    fun saveTransaction(
        existingId: Long?,
        dateMillis: Long,
        description: String,
        categoryId: Long,
        type: TransactionType,
        amount: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            val (month, year) = FinanceFormatter.extractMonthAndYear(dateMillis)
            val entity = TransactionEntity(
                id = existingId ?: 0L,
                date = dateMillis,
                description = description.trim(),
                categoryId = categoryId,
                type = type,
                amount = amount,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                month = month,
                year = year
            )
            if (existingId == null || existingId == 0L) {
                repository.insertTransaction(entity)
                _events.emit(FinanceEvent.ShowMessage("Transaction added"))
            } else {
                repository.updateTransaction(entity)
                _events.emit(FinanceEvent.ShowMessage("Transaction updated"))
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _events.emit(FinanceEvent.TransactionDeleted(transaction))
        }
    }

    fun undoDeleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransactions(transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) return
        viewModelScope.launch {
            repository.deleteTransactions(transactions)
            _events.emit(FinanceEvent.TransactionsBulkDeleted(transactions))
        }
    }

    fun undoDeleteTransactions(transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) return
        viewModelScope.launch {
            repository.insertTransactions(transactions)
            _events.emit(FinanceEvent.ShowMessage("Restored ${transactions.size} transactions"))
        }
    }

    fun bulkUpdateTransactionsCategory(
        transactionIds: List<Long>,
        categoryId: Long,
        type: TransactionType
    ) {
        if (transactionIds.isEmpty()) return
        viewModelScope.launch {
            repository.bulkUpdateTransactionsCategory(transactionIds, categoryId, type)
            _events.emit(
                FinanceEvent.ShowMessage(
                    "Updated ${transactionIds.size} ${if (transactionIds.size == 1) "entry" else "entries"}"
                )
            )
        }
    }

    // Budget CRUD
    fun updateCategoryBudget(categoryId: Long, monthlyLimit: Double) {
        viewModelScope.launch {
            repository.upsertBudget(
                categoryId = categoryId,
                year = _budgetYear.value,
                month = _budgetMonth.value,
                monthlyLimit = monthlyLimit
            )
            _events.emit(FinanceEvent.ShowMessage("Monthly budget saved"))
        }
    }

    fun copyPreviousMonthBudgets() {
        viewModelScope.launch {
            val copied = repository.copyPreviousMonthBudgets(_budgetYear.value, _budgetMonth.value)
            if (copied > 0) {
                _events.emit(FinanceEvent.ShowMessage("Copied $copied budgets from previous month"))
            } else {
                _events.emit(FinanceEvent.ShowMessage("No new budgets found in previous month"))
            }
        }
    }

    // Goals CRUD
    fun saveGoal(
        existingId: Long?,
        title: String,
        targetAmount: Double,
        savedAmount: Double,
        targetDate: Long
    ) {
        viewModelScope.launch {
            val entity = SavingsGoalEntity(
                id = existingId ?: 0L,
                title = title.trim(),
                targetAmount = targetAmount,
                savedAmount = savedAmount,
                targetDate = targetDate
            )
            if (existingId == null || existingId == 0L) {
                repository.insertGoal(entity)
                _events.emit(FinanceEvent.ShowMessage("Savings goal created"))
            } else {
                repository.updateGoal(entity)
                _events.emit(FinanceEvent.ShowMessage("Savings goal updated"))
            }
        }
    }

    fun depositToGoal(goalId: Long, amount: Double, alsoLogTransaction: Boolean) {
        viewModelScope.launch {
            repository.depositToGoal(goalId, amount, alsoLogTransaction)
            _events.emit(FinanceEvent.ShowMessage("Added ${FinanceFormatter.formatLkr(amount)} to goal"))
        }
    }

    fun deleteGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            _events.emit(FinanceEvent.ShowMessage("Goal removed"))
        }
    }

    // Category CRUD
    fun saveCategory(
        existingId: Long?,
        name: String,
        hexColor: String,
        defaultType: TransactionType,
        icon: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val entity = CategoryEntity(
                id = existingId ?: 0L,
                name = name.trim(),
                hexColor = hexColor,
                defaultType = defaultType,
                icon = icon,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                isDeleted = false
            )
            if (existingId == null || existingId == 0L) {
                repository.insertCategory(entity)
                _events.emit(FinanceEvent.ShowMessage("Category added"))
            } else {
                repository.updateCategory(entity)
                _events.emit(FinanceEvent.ShowMessage("Category updated"))
            }
        }
    }

    fun softDeleteCategory(categoryId: Long) {
        viewModelScope.launch {
            repository.softDeleteCategory(categoryId)
            _events.emit(FinanceEvent.ShowMessage("Category archived"))
        }
    }

    fun restoreCategory(categoryId: Long) {
        viewModelScope.launch {
            repository.restoreCategory(categoryId)
            _events.emit(FinanceEvent.ShowMessage("Category restored"))
        }
    }

    suspend fun countTransactionsForCategory(categoryId: Long): Int {
        return repository.countTransactionsForCategory(categoryId)
    }

    fun deleteCategoryPermanently(categoryId: Long, deleteAssociatedTransactions: Boolean) {
        viewModelScope.launch {
            try {
                repository.deleteCategoryPermanently(categoryId, deleteAssociatedTransactions)
                _events.emit(FinanceEvent.ShowMessage("Category permanently deleted"))
            } catch (e: Exception) {
                _events.emit(FinanceEvent.ShowMessage("Could not delete category: ${e.message ?: "In use"}"))
            }
        }
    }

    // Export / Backup / Restore
    suspend fun buildCsvExport(): String {
        val snapshot = repository.getExportSnapshot()
        return BackupExportManager.generateCsv(snapshot)
    }

    suspend fun buildExcelExport(): String {
        val snapshot = repository.getExportSnapshot()
        return BackupExportManager.generateExcelSpreadsheetXml(snapshot)
    }

    suspend fun buildJsonBackup(): String {
        val snapshot = repository.getExportSnapshot()
        return BackupExportManager.generateBackupJson(snapshot)
    }

    fun restoreFromBackupJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val snapshot = BackupExportManager.parseBackupJson(jsonString)
                repository.restoreFromSnapshot(snapshot)
                _events.emit(
                    FinanceEvent.ShowMessage(
                        "Restored ${snapshot.transactions.size} transactions & ${snapshot.goals.size} goals"
                    )
                )
            } catch (e: Exception) {
                _events.emit(FinanceEvent.ShowMessage("Restore failed: ${e.message ?: "Invalid backup file"}"))
            }
        }
    }

    fun loadSampleWorkbookData() {
        viewModelScope.launch {
            repository.loadSampleWorkbookData(_budgetYear.value, _budgetMonth.value)
            _events.emit(FinanceEvent.ShowMessage("Loaded sample finance entries"))
        }
    }

    fun resetAllAppData() {
        viewModelScope.launch {
            repository.resetAllAppData()
            _periodFilter.value = PeriodFilter(year = null, month = null)
            _searchQuery.value = ""
            _selectedTxType.value = null
            _analyticsCategoryId.value = null
            _events.emit(FinanceEvent.ShowMessage("App data has been reset to defaults"))
        }
    }

    fun notifyMessage(message: String) {
        viewModelScope.launch {
            _events.emit(FinanceEvent.ShowMessage(message))
        }
    }

    class Factory(
        private val repository: FinanceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                return FinanceViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
