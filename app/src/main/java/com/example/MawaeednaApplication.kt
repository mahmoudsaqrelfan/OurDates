package com.example

import android.app.Application
import com.google.firebase.FirebaseApp

class MawaeednaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        ensureFirebaseInitialized(this)
    }

    companion object {
        lateinit var instance: MawaeednaApplication
            private set

        val appContext: android.content.Context?
            get() = if (::instance.isInitialized) instance.applicationContext else null

        fun ensureFirebaseInitialized(appContext: android.content.Context) {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
        }
    }
}