package com.example.data.model

data class AppSettings(
    val language: String = "العربية",
    val themeMode: String = "فاتح (طبي هادئ)",
    val syncEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val backupEnabled: Boolean = false
)
