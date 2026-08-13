package com.example.data.model

enum class SyncStatus(val labelAr: String, val iconEmoji: String) {
    LOCAL("محلي", "📱"),
    CONNECTED("متزامن", "🟢"),
    SYNCING("جارٍ المزامنة", "🟡"),
    OFFLINE("بانتظار الاتصال", "⚠️"),
    ERROR("خطأ في المزامنة", "🔴")
}
