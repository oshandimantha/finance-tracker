package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.RealEstateAgent
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val isBusiness: Boolean
)

object CategoryVisuals {

    val iconOptions: List<CategoryIconOption> by lazy {
        listOf(
            // Business, Commerce & Finance
            CategoryIconOption("storefront", "Shop / Retail", Icons.Rounded.Storefront, isBusiness = true),
            CategoryIconOption("point_of_sale", "Sales / POS", Icons.Rounded.PointOfSale, isBusiness = true),
            CategoryIconOption("business", "Company / Office", Icons.Rounded.Business, isBusiness = true),
            CategoryIconOption("business_center", "Business Ops", Icons.Rounded.BusinessCenter, isBusiness = true),
            CategoryIconOption("receipt_long", "Invoices / Bills", Icons.AutoMirrored.Rounded.ReceiptLong, isBusiness = true),
            CategoryIconOption("request_quote", "Quotes / Tax", Icons.Rounded.RequestQuote, isBusiness = true),
            CategoryIconOption("account_balance", "Bank / Tax", Icons.Rounded.AccountBalance, isBusiness = true),
            CategoryIconOption("payments", "Cash / Revenue", Icons.Rounded.Payments, isBusiness = true),
            CategoryIconOption("credit_card", "Card / Fees", Icons.Rounded.CreditCard, isBusiness = true),
            CategoryIconOption("currency_exchange", "FX / Exchange", Icons.Rounded.CurrencyExchange, isBusiness = true),
            CategoryIconOption("sell", "Sales / Pricing", Icons.Rounded.Sell, isBusiness = true),
            CategoryIconOption("inventory_2", "Inventory / Stock", Icons.Rounded.Inventory2, isBusiness = true),
            CategoryIconOption("warehouse", "Warehouse / Storage", Icons.Rounded.Warehouse, isBusiness = true),
            CategoryIconOption("local_shipping", "Delivery / Freight", Icons.Rounded.LocalShipping, isBusiness = true),
            CategoryIconOption("groups", "Staff / Team", Icons.Rounded.Groups, isBusiness = true),
            CategoryIconOption("badge", "Payroll / HR", Icons.Rounded.Badge, isBusiness = true),
            CategoryIconOption("campaign", "Marketing / Ads", Icons.Rounded.Campaign, isBusiness = true),
            CategoryIconOption("analytics", "Consulting / Audit", Icons.Rounded.Analytics, isBusiness = true),
            CategoryIconOption("handshake", "Suppliers / Deals", Icons.Rounded.Handshake, isBusiness = true),
            CategoryIconOption("support_agent", "Client Support", Icons.Rounded.SupportAgent, isBusiness = true),
            CategoryIconOption("engineering", "Contractors", Icons.Rounded.Engineering, isBusiness = true),
            CategoryIconOption("construction", "Equipment", Icons.Rounded.Construction, isBusiness = true),
            CategoryIconOption("build", "Repairs / Tools", Icons.Rounded.Build, isBusiness = true),
            CategoryIconOption("precision_manufacturing", "Manufacturing", Icons.Rounded.PrecisionManufacturing, isBusiness = true),
            CategoryIconOption("real_estate_agent", "Property / Lease", Icons.Rounded.RealEstateAgent, isBusiness = true),
            CategoryIconOption("print", "Office Supplies", Icons.Rounded.Print, isBusiness = true),
            CategoryIconOption("cloud", "Software / Cloud", Icons.Rounded.Cloud, isBusiness = true),
            CategoryIconOption("wifi", "Internet / Hosting", Icons.Rounded.Wifi, isBusiness = true),
            CategoryIconOption("gavel", "Legal / Licenses", Icons.Rounded.Gavel, isBusiness = true),
            CategoryIconOption("work", "Freelance / Work", Icons.Rounded.Work, isBusiness = true),
            CategoryIconOption("trending_up", "Investments", Icons.AutoMirrored.Rounded.TrendingUp, isBusiness = true),

            // Personal & Everyday
            CategoryIconOption("wallet", "Wallet", Icons.Rounded.AccountBalanceWallet, isBusiness = false),
            CategoryIconOption("savings", "Savings", Icons.Rounded.Savings, isBusiness = false),
            CategoryIconOption("home", "Home / Rent", Icons.Rounded.Home, isBusiness = false),
            CategoryIconOption("shopping_cart", "Groceries", Icons.Rounded.ShoppingCart, isBusiness = false),
            CategoryIconOption("shopping_bag", "Shopping", Icons.Rounded.ShoppingBag, isBusiness = false),
            CategoryIconOption("restaurant", "Dining / Food", Icons.Rounded.Restaurant, isBusiness = false),
            CategoryIconOption("directions_car", "Transport", Icons.Rounded.DirectionsCar, isBusiness = false),
            CategoryIconOption("local_gas_station", "Fuel", Icons.Rounded.LocalGasStation, isBusiness = false),
            CategoryIconOption("flight", "Travel / Flights", Icons.Rounded.Flight, isBusiness = false),
            CategoryIconOption("hotel", "Hotel / Stay", Icons.Rounded.Hotel, isBusiness = false),
            CategoryIconOption("bolt", "Electricity / Utilities", Icons.Rounded.Bolt, isBusiness = false),
            CategoryIconOption("phone_iphone", "Mobile / Telecom", Icons.Rounded.PhoneIphone, isBusiness = false),
            CategoryIconOption("devices", "Electronics", Icons.Rounded.Devices, isBusiness = false),
            CategoryIconOption("medical_services", "Healthcare", Icons.Rounded.MedicalServices, isBusiness = false),
            CategoryIconOption("school", "Education", Icons.Rounded.School, isBusiness = false),
            CategoryIconOption("movie", "Entertainment", Icons.Rounded.Movie, isBusiness = false),
            CategoryIconOption("category", "Other", Icons.Rounded.Category, isBusiness = false)
        )
    }

    val availableIcons: List<Pair<String, ImageVector>> by lazy { iconOptions.map { it.key to it.icon } }

    val paletteHexOptions: List<String> = listOf(
        "#15803D", "#0D9488", "#0284C7", "#2563EB",
        "#6366F1", "#7C3AED", "#EC4899", "#E11D48",
        "#DC2626", "#EA580C", "#D97706", "#64748B"
    )

    fun iconFor(key: String?): ImageVector {
        return iconOptions.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.Category
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
