package com.example.data.local.converter

import androidx.room.TypeConverter
import com.example.domain.model.TransactionType

class FinanceTypeConverters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.fromString(value)
}
