package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val appName: StateFlow<String> = appPreferences.appNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppPreferences.DEFAULT_APP_NAME
        )

    val appSubtitle: StateFlow<String> = appPreferences.appSubtitleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppPreferences.DEFAULT_APP_SUBTITLE
        )

    val appLockEnabled: StateFlow<Boolean> = appPreferences.appLockEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val privacyScreenEnabled: StateFlow<Boolean> = appPreferences.privacyScreenEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updateAppBranding(name: String, subtitle: String, onSaved: (String) -> Unit = {}) {
        viewModelScope.launch {
            appPreferences.setAppBranding(name, subtitle)
            val savedName = name.trim().ifBlank { AppPreferences.DEFAULT_APP_NAME }
            onSaved(savedName)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setAppLockEnabled(enabled)
        }
    }

    fun setPrivacyScreenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setPrivacyScreenEnabled(enabled)
        }
    }

    fun resetAppBranding() {
        viewModelScope.launch {
            appPreferences.setAppBranding(
                AppPreferences.DEFAULT_APP_NAME,
                AppPreferences.DEFAULT_APP_SUBTITLE
            )
            appPreferences.setAppLockEnabled(false)
            appPreferences.setPrivacyScreenEnabled(false)
        }
    }

    class Factory(
        private val appPreferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(appPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
