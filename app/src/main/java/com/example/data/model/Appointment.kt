package com.example.data.model

import java.util.UUID

enum class AppointmentType(val labelAr: String, val iconEmoji: String) {
    DOCTOR_VISIT("زيارة طبيب", "🩺"),
    LAB_TEST("فحص / تحليل", "🧪"),
    OTHER("موعد آخر", "📅")
}

enum class AppointmentStatus {
    UPCOMING, COMPLETED, CANCELLED
}

enum class ReminderType(val labelAr: String, val defaultDaysBefore: Int) {
    NONE("بدون تذكير", 0),
    SAME_DAY("يوم الموعد", 0),
    DAY_1_BEFORE("قبل الموعد بيوم", 1),
    DAYS_3_BEFORE("قبل الموعد بـ 3 أيام", 3),
    DAYS_7_BEFORE("قبل الموعد بـ 7 أيام", 7),
    CUSTOM("تذكير مخصص", 0)
}

data class ReminderConfig(
    val id: String = UUID.randomUUID().toString(),
    val type: ReminderType = ReminderType.SAME_DAY,
    val customDaysBefore: Int = 0,
    val timeHour: Int = 9,
    val timeMinute: Int = 0,
    val labelAr: String = "يوم الموعد"
)

data class Appointment(
    val id: String = "",
    val childId: String = "",
    val familyId: String = "",
    val type: AppointmentType = AppointmentType.DOCTOR_VISIT,
    val title: String = "",
    val doctorSpecialty: String = "",
    val doctorName: String = "",
    val clinicName: String = "",
    val testDefinitionId: String = "",
    val dateTimestamp: Long = System.currentTimeMillis(),
    val dateText: String = "",
    val timeText: String = "",
    val status: AppointmentStatus = AppointmentStatus.UPCOMING,
    val notes: String = "",
    val reminders: List<ReminderConfig> = emptyList(),
    val linkedAppointmentId: String? = null,
    val linkedAppointmentTitle: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
