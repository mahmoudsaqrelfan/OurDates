package com.example.data.model

enum class Gender {
    BOY, GIRL
}

data class Child(
    val id: String,
    val familyId: String = "fam_001",
    val name: String,
    val birthDate: String, // e.g. "12 مايو 2020"
    val ageText: String, // e.g. "4 سنوات"
    val gender: Gender = Gender.BOY,
    val avatarColorHex: String = "#00A896",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
