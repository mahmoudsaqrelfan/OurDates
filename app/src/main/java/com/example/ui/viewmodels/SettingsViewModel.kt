package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.Family
import com.example.data.model.SyncStatus
import com.example.data.repository.FamilyRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.BackupRepository
import com.example.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val settingsRepository: SettingsRepository = AppContainer.settingsRepository,
    private val familyRepository: FamilyRepository = AppContainer.familyRepository,
    private val backupRepository: BackupRepository = AppContainer.backupRepository
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = familyRepository.syncStatus

    val settings: StateFlow<AppSettings> = settingsRepository.getAppSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val family: StateFlow<Family> = familyRepository.getFamily().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Family()
    )

    fun updateSettings(newSettings: AppSettings) {
        settingsRepository.updateSettings(newSettings)
    }

    fun updateFamilyName(newName: String) {
        familyRepository.updateFamilyName(newName)
    }

    suspend fun createBackupJson(): String {
        return backupRepository.createBackupJson()
    }

    fun validateBackupJson(json: String): Boolean {
        return backupRepository.validateBackupJson(json)
    }

    suspend fun restoreBackup(json: String): Boolean {
        return backupRepository.restoreBackup(json)
    }
}
