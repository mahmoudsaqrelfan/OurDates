package com.example.di

import com.example.data.repository.AppointmentRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.FamilyRepository
import com.example.data.repository.FirebaseAuthRepository
import com.example.data.repository.FirestoreAppointmentRepository
import com.example.data.repository.FirestoreFamilyRepository
import com.example.data.repository.HealthRecordsRepository
import com.example.data.repository.InMemoryHealthRecordsRepository
import com.example.data.repository.InMemorySettingsRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.BackupRepository
import com.example.data.repository.LocalBackupRepository

object AppContainer {
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    val familyRepository: FamilyRepository by lazy { FirestoreFamilyRepository() }
    val appointmentRepository: AppointmentRepository by lazy { FirestoreAppointmentRepository() }
    val healthRecordsRepository: HealthRecordsRepository by lazy { InMemoryHealthRecordsRepository() }
    val settingsRepository: SettingsRepository by lazy { InMemorySettingsRepository() }
    val backupRepository: BackupRepository by lazy { LocalBackupRepository() }
}
