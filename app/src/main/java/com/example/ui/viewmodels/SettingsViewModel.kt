package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.Family
import com.example.data.model.SyncStatus
import com.example.data.repository.BackupRepository
import com.example.data.repository.FamilyRepository
import com.example.data.repository.GoogleDataLinkRepository
import com.example.data.repository.SettingsRepository
import com.example.di.AppContainer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val settingsRepository: SettingsRepository = AppContainer.settingsRepository,
    private val familyRepository: FamilyRepository = AppContainer.familyRepository,
    private val backupRepository: BackupRepository = AppContainer.backupRepository,
    private val googleDataLinkRepository: GoogleDataLinkRepository = AppContainer.googleDataLinkRepository
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

    suspend fun createBackupJson(): String = backupRepository.createBackupJson()

    fun validateBackupJson(json: String): Boolean = backupRepository.validateBackupJson(json)

    suspend fun restoreBackup(json: String): Boolean {
        val restoredLocally = backupRepository.restoreBackup(json)
        if (!restoredLocally) return false

        val uid = try { FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null }
        if (uid != null) {
            return googleDataLinkRepository.mergeBackupIntoGoogle(json, uid).isSuccess
        }
        return true
    }
}
