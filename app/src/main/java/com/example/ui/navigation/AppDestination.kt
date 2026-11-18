package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val label: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD(
        route = "dashboard",
        label = "Dashboard",
        title = "Finance Tracker",
        selectedIcon = Icons.Rounded.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    TRANSACTIONS(
        route = "transactions",
        label = "Ledger",
        title = "Transactions Ledger",
        selectedIcon = Icons.Rounded.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong
    ),
    BUDGET(
        route = "budget",
        label = "Budget",
        title = "Budget Tracker",
        selectedIcon = Icons.Rounded.PieChart,
        unselectedIcon = Icons.Outlined.PieChartOutline
    ),
    ANALYTICS(
        route = "analytics",
        label = "Analytics",
        title = "Analytics & Trends",
        selectedIcon = Icons.Rounded.Analytics,
        unselectedIcon = Icons.Outlined.Analytics
    ),
    GOALS(
        route = "goals",
        label = "Goals",
        title = "Savings Goals",
        selectedIcon = Icons.Rounded.Flag,
        unselectedIcon = Icons.Outlined.Flag
    ),
    SETTINGS(
        route = "settings",
        label = "Settings",
        title = "Categories & Settings",
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings
    );

    companion object {
        val bottomBarDestinations = listOf(DASHBOARD, TRANSACTIONS, BUDGET, ANALYTICS, GOALS)

        fun fromRoute(route: String?): AppDestination {
            return entries.firstOrNull { it.route == route } ?: DASHBOARD
        }
    }
}
