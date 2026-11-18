package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.repository.FinanceRepository
import com.example.data.repository.RoomFinanceRepository

class OshanFinanceApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: FinanceRepository by lazy { RoomFinanceRepository(database) }
    val appPreferences: AppPreferences by lazy { AppPreferences(this) }
}
