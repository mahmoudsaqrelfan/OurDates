package com.example

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

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
                try {
                    FirebaseApp.initializeApp(appContext)
                } catch (e: Exception) {
                    try {
                        val options = FirebaseOptions.Builder()
                            .setApplicationId("1:100000000000:android:aistudio123456")
                            .setProjectId("aistudio-mawaeedna")
                            .setApiKey("AIzaSyAISTUDIO_DUMMY_KEY_FOR_LOCAL")
                            .build()
                        FirebaseApp.initializeApp(appContext, options)
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                }
            }
        }
    }
}
