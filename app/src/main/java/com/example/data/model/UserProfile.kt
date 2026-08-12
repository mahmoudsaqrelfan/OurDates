package com.example.data.model

data class UserProfile(
    val id: String = "user_001",
    val googleUserId: String = "google_user_001",
    val displayName: String = "أحمد علي",
    val email: String = "ahmed@example.com",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
