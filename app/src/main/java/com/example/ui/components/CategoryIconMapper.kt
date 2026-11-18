package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryVisuals {

    val availableIcons: List<Pair<String, ImageVector>> = listOf(
        "payments" to Icons.Rounded.Payments,
        "work" to Icons.Rounded.Work,
        "home" to Icons.Rounded.Home,
        "shopping_cart" to Icons.Rounded.ShoppingCart,
        "restaurant" to Icons.Rounded.Restaurant,
        "directions_car" to Icons.Rounded.DirectionsCar,
        "local_gas_station" to Icons.Rounded.LocalGasStation,
        "bolt" to Icons.Rounded.Bolt,
        "movie" to Icons.Rounded.Movie,
        "devices" to Icons.Rounded.Devices,
        "medical_services" to Icons.Rounded.MedicalServices,
        "savings" to Icons.Rounded.Savings,
        "trending_up" to Icons.Rounded.TrendingUp,
        "school" to Icons.Rounded.School,
        "shopping_bag" to Icons.Rounded.ShoppingBag,
        "wallet" to Icons.Rounded.AccountBalanceWallet,
        "category" to Icons.Rounded.Category
    )

    val paletteHexOptions: List<String> = listOf(
        "#15803D", "#0D9488", "#0284C7", "#2563EB",
        "#6366F1", "#7C3AED", "#EC4899", "#E11D48",
        "#DC2626", "#EA580C", "#D97706", "#64748B"
    )

    fun iconFor(key: String?): ImageVector {
        return availableIcons.firstOrNull { it.first == key }?.second ?: Icons.Rounded.Category
    }

    fun parseColor(hex: String?, fallback: Color): Color {
        if (hex.isNullOrBlank()) return fallback
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: IllegalArgumentException) {
            fallback
        }
    }
}
