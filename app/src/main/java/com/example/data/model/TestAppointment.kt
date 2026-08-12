package com.example.data.model

enum class TestStatus {
    SCHEDULED, DONE, PENDING_RESULT
}

data class TestAppointment(
    val id: String,
    val childId: String,
    val testName: String,
    val labName: String,
    val dateText: String,
    val timeText: String,
    val status: TestStatus = TestStatus.SCHEDULED,
    val instructions: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
