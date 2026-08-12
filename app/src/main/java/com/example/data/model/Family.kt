package com.example.data.model

data class Family(
    val id: String = "fam_001",
    val ownerUserId: String = "user_001",
    val familyName: String = "عائلتي",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
