package com.example.data.model

enum class MealContext(val labelAr: String) {
    FASTING("صائم"),
    BEFORE_MEAL("قبل الوجبة"),
    AFTER_MEAL("بعد الوجبة (ساعتين)"),
    BEDTIME("قبل النوم"),
    RANDOM("عشوائي")
}

data class GlucoseReading(
    val id: String,
    val childId: String,
    val readingValue: Int, // e.g. 110 mg/dL
    val unit: String = "mg/dL",
    val mealContext: MealContext,
    val dateText: String,
    val timeText: String,
    val isTargetRange: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
