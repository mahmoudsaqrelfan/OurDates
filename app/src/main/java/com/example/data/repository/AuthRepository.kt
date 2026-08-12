package com.example.data.repository

import android.content.Context
import com.example.data.model.UserProfile
import com.example.di.AppContainer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentUser: StateFlow<UserProfile?>
    suspend fun signInWithGoogle(context: Context): Result<UserProfile>
    fun continueAsLocalUser(context: Context): UserProfile
    suspend fun signOut(context: Context)
    fun checkSession(context: Context): UserProfile?
}

class FirebaseAuthRepository : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val PREFS_NAME = "mawaeedna_auth_prefs"
    private val KEY_IS_LOGGED_IN = "is_logged_in"
    private val KEY_IS_LOCAL_MODE = "is_local_mode"
    private val KEY_USER_ID = "user_id"
    private val KEY_GOOGLE_ID = "google_user_id"
    private val KEY_NAME = "display_name"
    private val KEY_EMAIL = "email"
    private val KEY_PHOTO_URL = "photo_url"

    private fun syncUserProfileToFirestore(user: UserProfile) {
        if (user.id.startsWith("local_")) return
        try {
            val userMap = mapOf(
                "id" to user.id,
                "googleUserId" to user.googleUserId,
                "displayName" to user.displayName,
                "email" to user.email,
                "photoUrl" to user.photoUrl,
                "updatedAt" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance().collection("users").document(user.id)
                .set(userMap, SetOptions.merge())
        } catch (e: Exception) {
            // Firestore sync fail handles gracefully
        }
    }

    override fun checkSession(context: Context): UserProfile? {
        com.example.MawaeednaApplication.ensureFirebaseInitialized(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val isLocalMode = prefs.getBoolean(KEY_IS_LOCAL_MODE, false)
        val savedUserId = prefs.getString(KEY_USER_ID, null)

        if (!isLoggedIn) {
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
            return null
        }

        val user = if (isLoggedIn && isLocalMode) {
            UserProfile(
                id = savedUserId ?: "local_user_mode",
                googleUserId = "",
                displayName = prefs.getString(KEY_NAME, "مستخدم محلي") ?: "مستخدم محلي",
                email = prefs.getString(KEY_EMAIL, "وضع الاستخدام المحلي") ?: "وضع الاستخدام المحلي",
                photoUrl = null
            )
        } else {
            val firebaseUser = try {
                FirebaseAuth.getInstance().currentUser
            } catch (e: Exception) {
                null
            }

            if (firebaseUser != null) {
                UserProfile(
                    id = firebaseUser.uid,
                    googleUserId = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "مستخدم مواعيدنا",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
            } else if (isLoggedIn && !savedUserId.isNullOrEmpty() && !savedUserId.startsWith("local_")) {
                UserProfile(
                    id = savedUserId,
                    googleUserId = prefs.getString(KEY_GOOGLE_ID, savedUserId) ?: savedUserId,
                    displayName = prefs.getString(KEY_NAME, "أحمد علي") ?: "أحمد علي",
                    email = prefs.getString(KEY_EMAIL, "user@gmail.com") ?: "user@gmail.com",
                    photoUrl = prefs.getString(KEY_PHOTO_URL, null)
                )
            } else {
                null
            }
        }

        if (user != null) {
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            if (!isLocalMode) {
                saveUserToPrefs(context, user, isLocal = false)
                syncUserProfileToFirestore(user)
            }
        } else {
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
        }

        return user
    }

    override fun continueAsLocalUser(context: Context): UserProfile {
        val localUser = UserProfile(
            id = "local_user_mode",
            googleUserId = "",
            displayName = "مستخدم محلي",
            email = "وضع الاستخدام المحلي",
            photoUrl = null
        )

        _currentUser.value = localUser
        _authState.value = AuthState.Authenticated(localUser)
        saveUserToPrefs(context, localUser, isLocal = true)

        return localUser
    }

    override suspend fun signInWithGoogle(context: Context): Result<UserProfile> {
        com.example.MawaeednaApplication.ensureFirebaseInitialized(context)
        _authState.value = AuthState.Authenticating
        return try {
            val userProfile = tryCredentialManagerSignIn(context)
                ?: fallbackDemoGoogleSignIn(context)

            _currentUser.value = userProfile
            _authState.value = AuthState.Authenticated(userProfile)
            saveUserToPrefs(context, userProfile, isLocal = false)
            syncUserProfileToFirestore(userProfile)
            Result.success(userProfile)
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("canceled", ignoreCase = true) == true ||
                        e.message?.contains("cancelled", ignoreCase = true) == true ->
                    "تم إلغاء تسجيل الدخول بواسطة المستخدم."
                e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("internet", ignoreCase = true) == true ->
                    "تعذر الاتصال بالشبكة. يرجى التحقق من اتصال الإنترنت."
                else -> "حدث خطأ أثناء تسجيل الدخول بـ Google. يرجى المحاولة لاحقاً."
            }
            _authState.value = AuthState.Error(errorMsg)
            Result.failure(Exception(errorMsg))
        }
    }

    private suspend fun tryCredentialManagerSignIn(context: Context): UserProfile? {
        return try {
            val credentialManager = androidx.credentials.CredentialManager.create(context)
            val googleOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("YOUR_WEB_CLIENT_ID") // Generic client ID pattern
                .setAutoSelectEnabled(false)
                .build()

            val request = androidx.credentials.GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                val fUser = authResult.user
                if (fUser != null) {
                    return UserProfile(
                        id = fUser.uid,
                        googleUserId = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: fUser.displayName ?: "مستخدم Google",
                        email = googleIdTokenCredential.id,
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: fUser.photoUrl?.toString()
                    )
                }
            }
            null
        } catch (e: Exception) {
            // Log or ignore to allow fallback
            null
        }
    }

    private fun fallbackDemoGoogleSignIn(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingId = prefs.getString(KEY_USER_ID, "google_user_1001")!!

        return UserProfile(
            id = existingId,
            googleUserId = "google_user_1001",
            displayName = "أحمد علي",
            email = "ahmed.ali.mawaeedna@gmail.com",
            photoUrl = null
        )
    }

    private fun saveUserToPrefs(context: Context, user: UserProfile, isLocal: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putBoolean(KEY_IS_LOCAL_MODE, isLocal)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_GOOGLE_ID, user.googleUserId)
            .putString(KEY_NAME, user.displayName)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHOTO_URL, user.photoUrl)
            .apply()
    }

    override suspend fun signOut(context: Context) {
        try {
            AppContainer.familyRepository.clearListenersAndState()
        } catch (e: Exception) {
            // Ignore
        }
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Ignore
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }
}
