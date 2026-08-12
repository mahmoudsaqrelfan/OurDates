package com.example.data.model

data class NotificationReminder(
    val id: String,
    val targetId: String, // childId or appointmentId
    val title: String,
    val message: String,
    val scheduledTimeText: String,
    val isSent: Boolean = false
)
