package com.example.data.model

enum class SyncStatus(val labelAr: String, val iconEmoji: String) {
    CONNECTED("متزامن", "🟢"),
    SYNCING("جارٍ المزامنة", "🟡"),
    OFFLINE("بانتظار الاتصال", "⚠️"),
    ERROR("خطأ في المزامنة", "🔴")
}
