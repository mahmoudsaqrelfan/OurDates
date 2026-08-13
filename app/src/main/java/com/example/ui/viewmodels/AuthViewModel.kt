package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.repository.AppointmentRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.BackupRepository
import com.example.data.repository.FamilyRepository
import com.example.data.repository.GoogleDataLinkRepository
import com.example.data.repository.HealthRecordsRepository
import com.example.di.AppContainer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository,
    private val familyRepository: FamilyRepository = AppContainer.familyRepository,
    private val appointmentRepository: AppointmentRepository = AppContainer.appointmentRepository,
    private val healthRecordsRepository: HealthRecordsRepository = AppContainer.healthRecordsRepository,
    private val backupRepository: BackupRepository = AppContainer.backupRepository,
    private val googleDataLinkRepository: GoogleDataLinkRepository = AppContainer.googleDataLinkRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser

    fun checkSession(context: Context): UserProfile? {
        val user = authRepository.checkSession(context)
        if (user != null) attachRepositories(user.id)
        return user
    }

    fun signInWithGoogle(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(context).onSuccess { user ->
                attachRepositories(user.id)
                onSuccess()
            }
        }
    }

    fun continueAsLocalUser(context: Context, onSuccess: () -> Unit) {
        val user = authRepository.continueAsLocalUser(context)
        attachRepositories(user.id)
        onSuccess()
    }

    /**
     * Local-first linking flow:
     * 1) Snapshot the current on-device data.
     * 2) Authenticate with Google.
     * 3) Non-destructively merge the local snapshot into the Firebase UID.
     * 4) Attach live Firestore listeners to the same data set.
     *
     * Existing cloud documents with a newer updatedAt are preserved.
     */
    fun linkGoogleForSync(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val localBackup = try {
                backupRepository.createBackupJson()
            } catch (_: Exception) {
                onError("تعذر تجهيز البيانات المحلية للمزامنة.")
                return@launch
            }

            val authResult = authRepository.signInWithGoogle(context)
            authResult.onFailure {
                onError(it.message ?: "تعذر ربط حساب Google.")
                return@launch
            }

            val user = authResult.getOrNull() ?: run {
                onError("تعذر ربط حساب Google.")
                return@launch
            }

            val mergeResult = googleDataLinkRepository.mergeBackupIntoGoogle(localBackup, user.id)
            if (mergeResult.isSuccess) {
                attachRepositories(user.id)
                onSuccess()
            } else {
                // Keep the user's local data authoritative if cloud linking fails.
                authRepository.signOut(context)
                appointmentRepository.clearUser()
                healthRecordsRepository.clearUser()
                val localUser = authRepository.continueAsLocalUser(context)
                attachRepositories(localUser.id)
                backupRepository.restoreBackup(localBackup)
                onError("تم إلغاء الربط لأن مزامنة البيانات لم تكتمل. بياناتك المحلية محفوظة كما هي.")
            }
        }
    }

    /**
     * Stops Google synchronization without deleting either the device data or
     * the existing Firestore copy. The currently visible data is first captured,
     * then restored into the local profile after Firebase sign-out.
     */
    fun unlinkGoogleSync(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val currentBackup = try {
                backupRepository.createBackupJson()
            } catch (_: Exception) {
                onError("تعذر حفظ نسخة محلية قبل إيقاف المزامنة.")
                return@launch
            }

            authRepository.signOut(context)
            appointmentRepository.clearUser()
            healthRecordsRepository.clearUser()

            val localUser = authRepository.continueAsLocalUser(context)
            attachRepositories(localUser.id)
            val restored = backupRepository.restoreBackup(currentBackup)
            if (restored) {
                onSuccess()
            } else {
                onError("تم إيقاف مزامنة Google، لكن تعذر استعادة نسخة الجهاز تلقائياً.")
            }
        }
    }

    fun signOut(context: Context, onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(context)
            appointmentRepository.clearUser()
            healthRecordsRepository.clearUser()
            onSignedOut()
        }
    }

    private fun attachRepositories(userId: String) {
        familyRepository.ensureFamilyCreated(userId)
        appointmentRepository.attachUser(userId)
        healthRecordsRepository.attachUser(userId)
    }
}
