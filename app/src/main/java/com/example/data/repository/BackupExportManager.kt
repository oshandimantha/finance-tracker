package com.example.data.repository

import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.FinanceFormatter
import com.example.domain.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject

object BackupExportManager {

    fun generateCsv(snapshot: BackupSnapshot): String {
        val catMap = snapshot.categories.associateBy { it.id }
        val sb = StringBuilder()
        sb.appendLine("Date,Description,Category,Type,Amount (LKR),Month,Year,Notes")
        snapshot.transactions.forEach { tx ->
            val catName = catMap[tx.categoryId]?.name ?: "Uncategorized"
            val row = listOf(
                FinanceFormatter.formatIsoDate(tx.date),
                escapeCsv(tx.description),
                escapeCsv(catName),
                tx.type.label,
                String.format(java.util.Locale.US, "%.2f", tx.amount),
                tx.month,
                tx.year.toString(),
                escapeCsv(tx.notes.orEmpty())
            )
            sb.appendLine(row.joinToString(","))
        }
        return sb.toString()
    }

    fun generateExcelSpreadsheetXml(snapshot: BackupSnapshot): String {
        val catMap = snapshot.categories.associateBy { it.id }
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<?mso-application progid="Excel.Sheet"?>""")
        sb.appendLine(
            """<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
                xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">"""
        )

        // Sheet 1: Transactions
        sb.appendLine("""<Worksheet ss:Name="Transactions"><Table>""")
        sb.appendLine(xmlRow(listOf("Date", "Description", "Category", "Type", "Amount (LKR)", "Month", "Year", "Notes")))
        snapshot.transactions.forEach { tx ->
            sb.appendLine(
                xmlRow(
                    listOf(
                        FinanceFormatter.formatIsoDate(tx.date),
                        tx.description,
                        catMap[tx.categoryId]?.name ?: "Other",
                        tx.type.label,
                        tx.amount,
                        tx.month,
                        tx.year,
                        tx.notes.orEmpty()
                    )
                )
            )
        }
        sb.appendLine("""</Table></Worksheet>""")

        // Sheet 2: Budget
        sb.appendLine("""<Worksheet ss:Name="Budget"><Table>""")
        sb.appendLine(xmlRow(listOf("Year", "Month", "Category", "Monthly Budget (LKR)")))
        snapshot.budgets.forEach { b ->
            sb.appendLine(
                xmlRow(
                    listOf(
                        b.year,
                        b.month,
                        catMap[b.categoryId]?.name ?: "Category #${b.categoryId}",
                        b.monthlyLimit
                    )
                )
            )
        }
        sb.appendLine("""</Table></Worksheet>""")

        // Sheet 3: Savings Goals
        sb.appendLine("""<Worksheet ss:Name="Savings Goals"><Table>""")
        sb.appendLine(xmlRow(listOf("Goal", "Target Amount (LKR)", "Saved So Far (LKR)", "Remaining (LKR)", "Target Date")))
        snapshot.goals.forEach { g ->
            sb.appendLine(
                xmlRow(
                    listOf(
                        g.title,
                        g.targetAmount,
                        g.savedAmount,
                        (g.targetAmount - g.savedAmount).coerceAtLeast(0.0),
                        FinanceFormatter.formatIsoDate(g.targetDate)
                    )
                )
            )
        }
        sb.appendLine("""</Table></Worksheet>""")

        sb.appendLine("""</Workbook>""")
        return sb.toString()
    }

    fun generateBackupJson(snapshot: BackupSnapshot): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val catsArray = JSONArray()
        snapshot.categories.forEach { c ->
            catsArray.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("hexColor", c.hexColor)
                    put("defaultType", c.defaultType.name)
                    put("notes", c.notes)
                    put("icon", c.icon)
                    put("isDeleted", c.isDeleted)
                }
            )
        }
        root.put("categories", catsArray)

        val txArray = JSONArray()
        snapshot.transactions.forEach { t ->
            txArray.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("date", t.date)
                    put("description", t.description)
                    put("categoryId", t.categoryId)
                    put("type", t.type.name)
                    put("amount", t.amount)
                    put("notes", t.notes)
                    put("month", t.month)
                    put("year", t.year)
                }
            )
        }
        root.put("transactions", txArray)

        val budgetArray = JSONArray()
        snapshot.budgets.forEach { b ->
            budgetArray.put(
                JSONObject().apply {
                    put("id", b.id)
                    put("categoryId", b.categoryId)
                    put("monthlyLimit", b.monthlyLimit)
                    put("year", b.year)
                    put("month", b.month)
                }
            )
        }
        root.put("budgets", budgetArray)

        val goalArray = JSONArray()
        snapshot.goals.forEach { g ->
            goalArray.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("title", g.title)
                    put("targetAmount", g.targetAmount)
                    put("savedAmount", g.savedAmount)
                    put("targetDate", g.targetDate)
                }
            )
        }
        root.put("goals", goalArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): BackupSnapshot {
        val cleaned = jsonString.trimStart('\uFEFF').trim()
        require(cleaned.isNotEmpty()) { "Backup file is empty (0 bytes)" }
        val root = JSONObject(cleaned)
        require(root.has("categories") && root.has("transactions")) {
            "Not a valid Finance Tracker backup file"
        }

        val catsJson = root.getJSONArray("categories")
        val categories = buildList {
            for (i in 0 until catsJson.length()) {
                val obj = catsJson.getJSONObject(i)
                add(
                    CategoryEntity(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        hexColor = obj.getString("hexColor"),
                        defaultType = TransactionType.fromString(obj.getString("defaultType")),
                        notes = obj.optString("notes").takeIf { it.isNotEmpty() && it != "null" },
                        icon = obj.optString("icon").takeIf { it.isNotEmpty() && it != "null" },
                        isDeleted = obj.optBoolean("isDeleted", false)
                    )
                )
            }
        }

        val txJson = root.getJSONArray("transactions")
        val transactions = buildList {
            for (i in 0 until txJson.length()) {
                val obj = txJson.getJSONObject(i)
                add(
                    TransactionEntity(
                        id = obj.getLong("id"),
                        date = obj.getLong("date"),
                        description = obj.getString("description"),
                        categoryId = obj.getLong("categoryId"),
                        type = TransactionType.fromString(obj.getString("type")),
                        amount = obj.getDouble("amount"),
                        notes = obj.optString("notes").takeIf { it.isNotEmpty() && it != "null" },
                        month = obj.getString("month"),
                        year = obj.getInt("year")
                    )
                )
            }
        }

        val budgetJson = root.optJSONArray("budgets") ?: JSONArray()
        val budgets = buildList {
            for (i in 0 until budgetJson.length()) {
                val obj = budgetJson.getJSONObject(i)
                add(
                    BudgetEntity(
                        id = obj.getLong("id"),
                        categoryId = obj.getLong("categoryId"),
                        monthlyLimit = obj.getDouble("monthlyLimit"),
                        year = obj.getInt("year"),
                        month = obj.getString("month")
                    )
                )
            }
        }

        val goalJson = root.optJSONArray("goals") ?: JSONArray()
        val goals = buildList {
            for (i in 0 until goalJson.length()) {
                val obj = goalJson.getJSONObject(i)
                add(
                    SavingsGoalEntity(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        targetAmount = obj.getDouble("targetAmount"),
                        savedAmount = obj.getDouble("savedAmount"),
                        targetDate = obj.getLong("targetDate")
                    )
                )
            }
        }

        return BackupSnapshot(
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            goals = goals
        )
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun xmlRow(values: List<Any>): String {
        val cells = values.joinToString("") { value ->
            when (value) {
                is Number -> """<Cell><Data ss:Type="Number">$value</Data></Cell>"""
                else -> """<Cell><Data ss:Type="String">${escapeXml(value.toString())}</Data></Cell>"""
            }
        }
        return "<Row>$cells</Row>"
    }

    private fun escapeXml(input: String): String {
        val sanitized = buildString(input.length) {
            for (ch in input) {
                if (ch == '\t' || ch == '\n' || ch == '\r' || ch >= ' ') {
                    append(ch)
                }
            }
        }
        return sanitized
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
