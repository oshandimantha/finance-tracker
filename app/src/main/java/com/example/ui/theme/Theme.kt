package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.domain.model.TransactionType

@Immutable
data class FinanceColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val savings: Color,
    val savingsContainer: Color,
    val investment: Color,
    val investmentContainer: Color,
    val warning: Color,
    val warningContainer: Color
) {
    fun colorFor(type: TransactionType): Color = when (type) {
        TransactionType.INCOME -> income
        TransactionType.EXPENSE -> expense
        TransactionType.SAVINGS -> savings
        TransactionType.INVESTMENT -> investment
    }

    fun containerFor(type: TransactionType): Color = when (type) {
        TransactionType.INCOME -> incomeContainer
        TransactionType.EXPENSE -> expenseContainer
        TransactionType.SAVINGS -> savingsContainer
        TransactionType.INVESTMENT -> investmentContainer
    }
}

val LocalFinanceColors = staticCompositionLocalOf {
    FinanceColors(
        income = IncomeGreenLight,
        incomeContainer = IncomeGreenContainerLight,
        expense = ExpenseRedLight,
        expenseContainer = ExpenseRedContainerLight,
        savings = SavingsBlueLight,
        savingsContainer = SavingsBlueContainerLight,
        investment = InvestmentPurpleLight,
        investmentContainer = InvestmentPurpleContainerLight,
        warning = WarningAmberLight,
        warningContainer = WarningAmberContainerLight
    )
}

private val LightFinanceColors = FinanceColors(
    income = IncomeGreenLight,
    incomeContainer = IncomeGreenContainerLight,
    expense = ExpenseRedLight,
    expenseContainer = ExpenseRedContainerLight,
    savings = SavingsBlueLight,
    savingsContainer = SavingsBlueContainerLight,
    investment = InvestmentPurpleLight,
    investmentContainer = InvestmentPurpleContainerLight,
    warning = WarningAmberLight,
    warningContainer = WarningAmberContainerLight
)

private val DarkFinanceColors = FinanceColors(
    income = IncomeGreenDark,
    incomeContainer = IncomeGreenContainerDark,
    expense = ExpenseRedDark,
    expenseContainer = ExpenseRedContainerDark,
    savings = SavingsBlueDark,
    savingsContainer = SavingsBlueContainerDark,
    investment = InvestmentPurpleDark,
    investmentContainer = InvestmentPurpleContainerDark,
    warning = WarningAmberDark,
    warningContainer = WarningAmberContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = SlateOnSecondaryContainer,
    tertiary = GoldTertiary,
    onTertiary = GoldOnTertiary,
    tertiaryContainer = GoldTertiaryContainer,
    onTertiaryContainer = GoldOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ExpenseRedLight,
    errorContainer = ExpenseRedContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkEmeraldOnPrimary,
    primaryContainer = DarkEmeraldPrimaryContainer,
    onPrimaryContainer = DarkEmeraldOnPrimaryContainer,
    secondary = DarkSlateSecondary,
    onSecondary = DarkSlateOnSecondary,
    secondaryContainer = DarkSlateSecondaryContainer,
    onSecondaryContainer = DarkSlateOnSecondaryContainer,
    tertiary = DarkGoldTertiary,
    onTertiary = DarkGoldOnTertiary,
    tertiaryContainer = DarkGoldTertiaryContainer,
    onTertiaryContainer = DarkGoldOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ExpenseRedDark,
    errorContainer = ExpenseRedContainerDark
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OshanFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val financeColors = if (darkTheme) DarkFinanceColors else LightFinanceColors

    CompositionLocalProvider(LocalFinanceColors provides financeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
