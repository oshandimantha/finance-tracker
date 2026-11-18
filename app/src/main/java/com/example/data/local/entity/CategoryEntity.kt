package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val hexColor: String,
    val defaultType: TransactionType,
    val notes: String? = null,
    val icon: String? = null,
    val isDeleted: Boolean = false
)
