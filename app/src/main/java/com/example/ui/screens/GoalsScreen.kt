package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.SavingsGoalEntity
import com.example.domain.model.FinanceFormatter
import com.example.domain.model.GoalCountdownStatus
import com.example.domain.model.GoalUiModel
import com.example.ui.components.GoalDepositDialog
import com.example.ui.components.GoalEditorDialog
import com.example.ui.theme.CurrencyTextStyleLarge
import com.example.ui.theme.CurrencyTextStyleMedium
import com.example.ui.theme.CurrencyTextStyleSmall
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.GoalsUiState

@Composable
fun GoalsScreen(
    uiState: GoalsUiState,
    onSaveGoal: (existingId: Long?, title: String, targetAmount: Double, savedAmount: Double, targetDate: Long) -> Unit,
    onDepositToGoal: (goalId: Long, amount: Double, alsoLogTx: Boolean) -> Unit,
    onDeleteGoal: (SavingsGoalEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var depositingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    if (showCreateDialog || editingGoal != null) {
        GoalEditorDialog(
            existingGoal = editingGoal,
            onDismiss = {
                showCreateDialog = false
                editingGoal = null
            },
            onSave = { id, title, target, saved, date ->
                onSaveGoal(id, title, target, saved, date)
                showCreateDialog = false
                editingGoal = null
            }
        )
    }

    depositingGoal?.let { goal ->
        GoalDepositDialog(
            goal = goal,
            onDismiss = { depositingGoal = null },
            onDeposit = { amount, alsoLogTx ->
                onDepositToGoal(goal.id, amount, alsoLogTx)
                depositingGoal = null
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = "New savings goal") },
                text = { Text("New Goal") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_goal_fab")
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is GoalsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is GoalsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is GoalsUiState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            GoalsSummaryCard(
                                totalSaved = uiState.totalSaved,
                                totalTarget = uiState.totalTarget,
                                goalCount = uiState.goals.size
                            )
                        }

                        if (uiState.goals.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Flag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Text(
                                            text = "No savings goals yet",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Create targets like Emergency Fund, Vacation, or New Laptop and track countdown milestones.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = uiState.goals,
                                key = { it.goal.id }
                            ) { goalUi ->
                                GoalProgressCard(
                                    item = goalUi,
                                    onDepositClick = { depositingGoal = goalUi.goal },
                                    onEditClick = { editingGoal = goalUi.goal },
                                    onDeleteClick = { onDeleteGoal(goalUi.goal) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsSummaryCard(
    totalSaved: Double,
    totalTarget: Double,
    goalCount: Int
) {
    val financeColors = LocalFinanceColors.current
    val overallProgress = if (totalTarget > 0.0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Savings,
                        contentDescription = "Savings Goals Overview",
                        tint = financeColors.savings,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Savings Goals Overview",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = financeColors.savingsContainer
                ) {
                    Text(
                        text = "$goalCount Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = financeColors.savings,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FinanceFormatter.formatLkr(totalSaved),
                        style = CurrencyTextStyleLarge,
                        color = financeColors.savings
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Combined Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FinanceFormatter.formatLkr(totalTarget),
                        style = CurrencyTextStyleLarge
                    )
                }
            }

            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = financeColors.savings,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun GoalProgressCard(
    item: GoalUiModel,
    onDepositClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val goal = item.goal

    val (badgeText, badgeIcon, badgeColor, badgeBg) = when (item.status) {
        GoalCountdownStatus.COMPLETED -> GoalBadgeStyle(
            "Completed!",
            Icons.Rounded.Celebration,
            financeColors.income,
            financeColors.incomeContainer
        )
        GoalCountdownStatus.HEALTHY -> GoalBadgeStyle(
            "${item.daysLeft} days left",
            Icons.Rounded.Schedule,
            financeColors.income,
            financeColors.incomeContainer
        )
        GoalCountdownStatus.URGENT -> GoalBadgeStyle(
            if (item.daysLeft == 0L) "Due today" else "${item.daysLeft} days left",
            Icons.Rounded.WarningAmber,
            financeColors.warning,
            financeColors.warningContainer
        )
        GoalCountdownStatus.OVERDUE -> GoalBadgeStyle(
            "Overdue (${-item.daysLeft}d)",
            Icons.Rounded.WarningAmber,
            financeColors.expense,
            financeColors.expenseContainer
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Target date: ${FinanceFormatter.formatShortDate(goal.targetDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBg,
                    modifier = Modifier.testTag("goal_countdown_badge_${goal.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = badgeText,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Saved: ${FinanceFormatter.formatLkr(goal.savedAmount)}",
                        style = CurrencyTextStyleMedium,
                        color = financeColors.savings
                    )
                    Text(
                        text = "Target: ${FinanceFormatter.formatLkr(goal.targetAmount)} • Remaining: ${FinanceFormatter.formatLkr(item.remainingAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = FinanceFormatter.formatPercent(item.rawPercent),
                    style = CurrencyTextStyleSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }

            LinearProgressIndicator(
                progress = { item.progress.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp),
                color = if (item.status == GoalCountdownStatus.COMPLETED) financeColors.income else financeColors.savings,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDepositClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("goal_deposit_button_${goal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Deposit")
                }

                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.testTag("goal_edit_button_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit goal",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("goal_delete_button_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete goal",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private data class GoalBadgeStyle(
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val bg: Color
)
