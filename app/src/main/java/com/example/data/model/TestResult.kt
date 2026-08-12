package com.example.data.model

data class TestResult(
    val id: String,
    val childId: String,
    val testName: String,
    val resultValue: String,
    val unit: String,
    val isNormal: Boolean = true,
    val normalRangeText: String,
    val testDateText: String,
    val doctorNotes: String = "",
    val testDefinitionId: String? = null,
    val testAppointmentId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
