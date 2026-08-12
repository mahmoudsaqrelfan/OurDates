package com.example.data.repository

import com.example.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    fun getAppSettings(): Flow<AppSettings>
    fun updateSettings(settings: AppSettings)
}

class InMemorySettingsRepository : SettingsRepository {
    private val settingsState = MutableStateFlow(AppSettings())

    override fun getAppSettings(): Flow<AppSettings> = settingsState.asStateFlow()

    override fun updateSettings(settings: AppSettings) {
        settingsState.value = settings
    }
}
