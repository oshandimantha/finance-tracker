package com.example.domain.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object FinanceFormatter {

    val MONTHS = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    val YEARS = (2020..2050).toList()

    private val currencyFormat = ThreadLocal.withInitial {
        DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    }
    private val wholeFormat = ThreadLocal.withInitial {
        DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    }
    private val dateHeaderFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
    }
    private val shortDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("dd MMM yyyy", Locale.US)
    }
    private val isoDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    fun formatLkr(amount: Double): String {
        val fmt = currencyFormat.get()!!
        return if (amount < 0.0) {
            "-Rs ${fmt.format(abs(amount))}"
        } else {
            "Rs ${fmt.format(amount)}"
        }
    }

    fun formatSignedLkr(amount: Double): String {
        val fmt = currencyFormat.get()!!
        return when {
            amount > 0.0 -> "+Rs ${fmt.format(amount)}"
            amount < 0.0 -> "-Rs ${fmt.format(abs(amount))}"
            else -> "Rs 0.00"
        }
    }

    fun formatCompactLkr(amount: Double): String {
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 1_000_000.0 -> "${sign}Rs ${String.format(Locale.US, "%.1fM", absVal / 1_000_000.0)}"
            absVal >= 1_000.0 -> "${sign}Rs ${String.format(Locale.US, "%.1fk", absVal / 1_000.0)}"
            else -> "${sign}Rs ${wholeFormat.get()!!.format(absVal)}"
        }
    }

    fun formatPercent(ratio: Double): String {
        return String.format(Locale.US, "%.1f%%", ratio * 100.0)
    }

    fun formatDateHeader(timestamp: Long): String {
        return dateHeaderFormat.get()!!.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.get()!!.format(Date(timestamp))
    }

    fun formatIsoDate(timestamp: Long): String {
        return isoDateFormat.get()!!.format(Date(timestamp))
    }

    fun extractMonthAndYear(timestamp: Long): Pair<String, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val year = cal.get(Calendar.YEAR)
        return MONTHS[monthIdx] to year
    }

    fun dayStartMillis(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun daysUntil(targetTimestamp: Long, nowTimestamp: Long = System.currentTimeMillis()): Long {
        val startNow = dayStartMillis(nowTimestamp)
        val startTarget = dayStartMillis(targetTimestamp)
        return TimeUnit.MILLISECONDS.toDays(startTarget - startNow)
    }

    fun currentMonthAbbrev(): String {
        val cal = Calendar.getInstance()
        return MONTHS[cal.get(Calendar.MONTH).coerceIn(0, 11)]
    }

    fun defaultYear(): Int {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return if (year in 2020..2050) year else 2026
    }

    fun previousMonthYear(month: String, year: Int): Pair<String, Int> {
        val idx = MONTHS.indexOf(month)
        return if (idx <= 0) {
            MONTHS.last() to (year - 1)
        } else {
            MONTHS[idx - 1] to year
        }
    }

    fun dateToMillis(year: Int, monthOneBased: Int, day: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (monthOneBased - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
