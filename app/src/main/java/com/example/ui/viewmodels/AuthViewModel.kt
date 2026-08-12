package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.repository.AppointmentRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.FamilyRepository
import com.example.di.AppContainer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository,
    private val familyRepository: FamilyRepository = AppContainer.familyRepository,
    private val appointmentRepository: AppointmentRepository = AppContainer.appointmentRepository,
    private val healthRecordsRepository: com.example.data.repository.HealthRecordsRepository = AppContainer.healthRecordsRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser

    fun checkSession(context: Context): UserProfile? {
        val user = authRepository.checkSession(context)
        if (user != null) {
            familyRepository.ensureFamilyCreated(user.id)
            appointmentRepository.attachUser(user.id)
            healthRecordsRepository.attachUser(user.id)
        }
        return user
    }

    fun signInWithGoogle(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(context)
            result.onSuccess { user ->
                familyRepository.ensureFamilyCreated(user.id)
                appointmentRepository.attachUser(user.id)
                healthRecordsRepository.attachUser(user.id)
                onSuccess()
            }
        }
    }

    fun continueAsLocalUser(context: Context, onSuccess: () -> Unit) {
        val user = authRepository.continueAsLocalUser(context)
        familyRepository.ensureFamilyCreated(user.id)
        appointmentRepository.attachUser(user.id)
        healthRecordsRepository.attachUser(user.id)
        onSuccess()
    }

    fun signOut(context: Context, onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(context)
            appointmentRepository.clearUser()
            healthRecordsRepository.clearUser()
            onSignedOut()
        }
    }
}
