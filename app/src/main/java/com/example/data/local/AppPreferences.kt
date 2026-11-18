package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "oshan_finance_datastore"
)

class AppPreferences(private val context: Context) {

    val appNameFlow: Flow<String> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_APP_NAME]?.takeIf { it.isNotBlank() } ?: DEFAULT_APP_NAME
    }

    val appSubtitleFlow: Flow<String> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_APP_SUBTITLE]?.takeIf { it.isNotBlank() } ?: DEFAULT_APP_SUBTITLE
    }

    val appLockEnabledFlow: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_ENABLED] ?: false
    }

    val privacyScreenEnabledFlow: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_PRIVACY_SCREEN_ENABLED] ?: false
    }

    suspend fun setAppBranding(name: String, subtitle: String) {
        val cleanName = name.trim().ifBlank { DEFAULT_APP_NAME }
        val cleanSubtitle = subtitle.trim().ifBlank { DEFAULT_APP_SUBTITLE }
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_APP_NAME] = cleanName
            prefs[KEY_APP_SUBTITLE] = cleanSubtitle
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setPrivacyScreenEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_PRIVACY_SCREEN_ENABLED] = enabled
        }
    }

    companion object {
        private val KEY_APP_NAME = stringPreferencesKey("custom_app_name")
        private val KEY_APP_SUBTITLE = stringPreferencesKey("custom_app_subtitle")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_PRIVACY_SCREEN_ENABLED = booleanPreferencesKey("privacy_screen_enabled")
        const val DEFAULT_APP_NAME = "Finance Tracker"
        const val DEFAULT_APP_SUBTITLE = "LKR Personal & Business Ledger"
    }
}
