package com.example.data.repository

import com.example.data.model.Child
import com.example.data.model.Family
import com.example.data.model.Gender
import com.example.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

interface FamilyRepository {
    val syncStatus: StateFlow<SyncStatus>
    fun getFamily(): Flow<Family>
    fun ensureFamilyCreated(ownerUserId: String): Family
    fun updateFamilyName(newName: String)
    fun getChildren(): Flow<List<Child>>
    fun getChildById(id: String): Flow<Child?>
    fun addChild(child: Child)
    fun updateChild(child: Child)
    fun deleteChild(id: String)
    fun clearListenersAndState()
    fun restoreFamilyAndChildren(family: Family, children: List<Child>)
}

class InMemoryFamilyRepository : FamilyRepository {
    private val _syncStatus = MutableStateFlow(SyncStatus.CONNECTED)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val familyState = MutableStateFlow(
        Family(
            id = "fam_001",
            ownerUserId = "user_001",
            familyName = "عائلتي"
        )
    )

    private val childrenState = MutableStateFlow(
        listOf(
            Child(
                id = "child_1",
                familyId = "fam_001",
                name = "ياسين أحمد",
                birthDate = "12 مايو 2020",
                ageText = "4 سنوات",
                gender = Gender.BOY,
                avatarColorHex = "#00A896"
            ),
            Child(
                id = "child_2",
                familyId = "fam_001",
                name = "مريم أحمد",
                birthDate = "28 سبتمبر 2022",
                ageText = "سنتان",
                gender = Gender.GIRL,
                avatarColorHex = "#EC407A"
            )
        )
    )

    override fun restoreFamilyAndChildren(family: Family, children: List<Child>) {
        familyState.value = family
        childrenState.value = children
    }

    override fun getFamily(): Flow<Family> = familyState.asStateFlow()

    override fun ensureFamilyCreated(ownerUserId: String): Family {
        val current = familyState.value
        if (current.ownerUserId != ownerUserId) {
            val newFamily = Family(
                id = "fam_$ownerUserId",
                ownerUserId = ownerUserId,
                familyName = "عائلتي"
            )
            familyState.value = newFamily
            return newFamily
        }
        return current
    }

    override fun updateFamilyName(newName: String) {
        familyState.value = familyState.value.copy(
            familyName = newName,
            updatedAt = System.currentTimeMillis()
        )
    }

    override fun getChildren(): Flow<List<Child>> = childrenState.asStateFlow()

    override fun getChildById(id: String): Flow<Child?> = childrenState.map { list ->
        list.find { it.id == id }
    }

    override fun addChild(child: Child) {
        val currentFam = familyState.value
        val childWithFamId = child.copy(familyId = currentFam.id)
        val newList = childrenState.value + childWithFamId
        childrenState.value = newList
    }

    override fun updateChild(child: Child) {
        val newList = childrenState.value.map {
            if (it.id == child.id) child else it
        }
        childrenState.value = newList
    }

    override fun deleteChild(id: String) {
        val newList = childrenState.value.filterNot { it.id == id }
        childrenState.value = newList
    }

    override fun clearListenersAndState() {
        familyState.value = Family()
        childrenState.value = emptyList()
        _syncStatus.value = SyncStatus.CONNECTED
    }
}
