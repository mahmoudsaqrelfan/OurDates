package com.example.data.repository

import android.content.Context
import com.example.MawaeednaApplication
import com.example.data.model.Child
import com.example.data.model.Family
import com.example.data.model.Gender
import com.example.data.model.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class FirestoreFamilyRepository : FamilyRepository {

    private fun saveLocalFamilyToPrefs(family: Family) {
        try {
            val context = MawaeednaApplication.appContext ?: return
            val prefs = context.getSharedPreferences("mawaeedna_local_family_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("family_id", family.id)
                .putString("owner_id", family.ownerUserId)
                .putString("family_name", family.familyName)
                .putLong("created_at", family.createdAt)
                .putLong("updated_at", family.updatedAt)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLocalFamilyFromPrefs(userId: String): Family? {
        return try {
            val context = MawaeednaApplication.appContext ?: return null
            val prefs = context.getSharedPreferences("mawaeedna_local_family_prefs", Context.MODE_PRIVATE)
            val familyName = prefs.getString("family_name", null) ?: return null
            Family(
                id = prefs.getString("family_id", "fam_$userId") ?: "fam_$userId",
                ownerUserId = prefs.getString("owner_id", userId) ?: userId,
                familyName = familyName,
                createdAt = prefs.getLong("created_at", System.currentTimeMillis()),
                updatedAt = prefs.getLong("updated_at", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun saveLocalChildrenToPrefs(children: List<Child>) {
        try {
            val context = MawaeednaApplication.appContext ?: return
            val prefs = context.getSharedPreferences("mawaeedna_local_family_prefs", Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            for (child in children) {
                val obj = JSONObject()
                obj.put("id", child.id)
                obj.put("familyId", child.familyId)
                obj.put("name", child.name)
                obj.put("birthDate", child.birthDate)
                obj.put("ageText", child.ageText)
                obj.put("gender", child.gender.name)
                obj.put("avatarColorHex", child.avatarColorHex)
                obj.put("notes", child.notes)
                obj.put("createdAt", child.createdAt)
                obj.put("updatedAt", child.updatedAt)
                jsonArray.put(obj)
            }
            prefs.edit().putString("children_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLocalChildrenFromPrefs(userId: String): List<Child>? {
        return try {
            val context = MawaeednaApplication.appContext ?: return null
            val prefs = context.getSharedPreferences("mawaeedna_local_family_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("children_json", null) ?: return null
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Child>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val genderStr = obj.optString("gender", "BOY")
                val gender = try { Gender.valueOf(genderStr) } catch (e: Exception) { Gender.BOY }
                list.add(
                    Child(
                        id = obj.getString("id"),
                        familyId = obj.optString("familyId", "fam_$userId"),
                        name = obj.getString("name"),
                        birthDate = obj.optString("birthDate", ""),
                        ageText = obj.optString("ageText", ""),
                        gender = gender,
                        avatarColorHex = obj.optString("avatarColorHex", "#00A896"),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun getFirestoreInstance(): FirebaseFirestore? {
        return try {
            val instance = FirebaseFirestore.getInstance()
            try {
                instance.firestoreSettings = firestoreSettings {
                    setLocalCacheSettings(persistentCacheSettings { })
                }
            } catch (e: Exception) {
                // Settings already initialized or unsupported
            }
            instance
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.CONNECTED)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _familyState = MutableStateFlow(Family())
    private val _childrenState = MutableStateFlow<List<Child>>(emptyList())

    private fun getDefaultChildren(userId: String) = listOf(
        Child(
            id = "child_1",
            familyId = "fam_$userId",
            name = "ياسين أحمد",
            birthDate = "12 مايو 2020",
            ageText = "4 سنوات",
            gender = Gender.BOY,
            avatarColorHex = "#00A896"
        ),
        Child(
            id = "child_2",
            familyId = "fam_$userId",
            name = "مريم أحمد",
            birthDate = "28 سبتمبر 2022",
            ageText = "سنتان",
            gender = Gender.GIRL,
            avatarColorHex = "#EC407A"
        )
    )

    private fun seedInitialChildren(database: FirebaseFirestore, userId: String) {
        try {
            val childrenColl = database.collection("users").document(userId).collection("children")
            val now = System.currentTimeMillis()

            val child1Map = mapOf(
                "id" to "child_1",
                "familyId" to "fam_$userId",
                "name" to "ياسين أحمد",
                "birthDate" to "12 مايو 2020",
                "ageText" to "4 سنوات",
                "gender" to "BOY",
                "avatarColorHex" to "#00A896",
                "notes" to "",
                "createdAt" to now - 2000,
                "updatedAt" to now - 2000
            )

            val child2Map = mapOf(
                "id" to "child_2",
                "familyId" to "fam_$userId",
                "name" to "مريم أحمد",
                "birthDate" to "28 سبتمبر 2022",
                "ageText" to "سنتان",
                "gender" to "GIRL",
                "avatarColorHex" to "#EC407A",
                "notes" to "",
                "createdAt" to now - 1000,
                "updatedAt" to now - 1000
            )

            childrenColl.document("child_1").set(child1Map, SetOptions.merge())
            childrenColl.document("child_2").set(child2Map, SetOptions.merge())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var familyListener: ListenerRegistration? = null
    private var childrenListener: ListenerRegistration? = null
    private var activeUserId: String? = null

    override fun getFamily(): Flow<Family> = _familyState.asStateFlow()

    override fun getChildren(): Flow<List<Child>> = _childrenState.asStateFlow()

    override fun getChildById(id: String): Flow<Child?> = _childrenState.map { list ->
        list.find { it.id == id }
    }

    override fun ensureFamilyCreated(ownerUserId: String): Family {
        if (ownerUserId.isBlank()) return _familyState.value

        if (ownerUserId.startsWith("local_")) {
            activeUserId = ownerUserId
            val savedFamily = loadLocalFamilyFromPrefs(ownerUserId)
            val savedChildren = loadLocalChildrenFromPrefs(ownerUserId)

            val family = savedFamily ?: Family(
                id = "fam_$ownerUserId",
                ownerUserId = ownerUserId,
                familyName = "عائلتي"
            )
            val children = savedChildren ?: getDefaultChildren(ownerUserId)

            _familyState.value = family
            _childrenState.value = children

            if (savedFamily == null) {
                saveLocalFamilyToPrefs(family)
            }
            if (savedChildren == null) {
                saveLocalChildrenToPrefs(children)
            }

            _syncStatus.value = SyncStatus.CONNECTED
            return family
        }

        if (activeUserId != ownerUserId) {
            attachListenersForUser(ownerUserId)
        }

        val database = getFirestoreInstance() ?: return _familyState.value

        try {
            val famRef = database.collection("users").document(ownerUserId)
                .collection("family").document("profile")

            famRef.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    val newFamilyMap = mapOf(
                        "id" to "fam_$ownerUserId",
                        "ownerUserId" to ownerUserId,
                        "familyName" to "عائلتي",
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    famRef.set(newFamilyMap, SetOptions.merge())
                    seedInitialChildren(database, ownerUserId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return _familyState.value
    }

    @Synchronized
    private fun attachListenersForUser(userId: String) {
        clearListenersAndState()
        activeUserId = userId
        _childrenState.value = getDefaultChildren(userId)

        val database = getFirestoreInstance()
        if (database == null) {
            _syncStatus.value = SyncStatus.OFFLINE
            return
        }

        try {
            val userDoc = database.collection("users").document(userId)

            // 1. Listen to Family Profile document
            familyListener = userDoc.collection("family").document("profile")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _syncStatus.value = SyncStatus.OFFLINE
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val familyName = snapshot.getString("familyName") ?: "عائلتي"
                        val ownerId = snapshot.getString("ownerUserId") ?: userId
                        val createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                        val updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()

                        _familyState.value = Family(
                            id = "fam_$userId",
                            ownerUserId = ownerId,
                            familyName = familyName,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                    }
                    updateSyncStatusFromSnapshot(snapshot)
                }

            // 2. Listen to Children Subcollection in real-time
            childrenListener = userDoc.collection("children")
                .addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        _syncStatus.value = SyncStatus.OFFLINE
                        return@addSnapshotListener
                    }

                    if (querySnapshot != null) {
                        if (querySnapshot.documents.isEmpty()) {
                            seedInitialChildren(database, userId)
                            _childrenState.value = getDefaultChildren(userId)
                        } else {
                            val list = querySnapshot.documents.mapNotNull { doc ->
                                try {
                                    val id = doc.id
                                    val name = doc.getString("name") ?: ""
                                    val birthDate = doc.getString("birthDate") ?: ""
                                    val ageText = doc.getString("ageText") ?: ""
                                    val genderStr = doc.getString("gender") ?: "BOY"
                                    val gender = try {
                                        Gender.valueOf(genderStr)
                                    } catch (e: Exception) {
                                        Gender.BOY
                                    }
                                    val avatarColorHex = doc.getString("avatarColorHex") ?: "#00A896"
                                    val notes = doc.getString("notes") ?: ""
                                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                                    Child(
                                        id = id,
                                        familyId = "fam_$userId",
                                        name = name,
                                        birthDate = birthDate,
                                        ageText = ageText,
                                        gender = gender,
                                        avatarColorHex = avatarColorHex,
                                        notes = notes,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            // Conflict resolution: Sort by createdAt
                            _childrenState.value = list.sortedByDescending { it.createdAt }
                        }
                    }
                    updateSyncStatusFromSnapshot(querySnapshot)
                }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    private fun updateSyncStatusFromSnapshot(snapshot: DocumentSnapshot?) {
        if (snapshot == null) return
        if (snapshot.metadata.hasPendingWrites()) {
            _syncStatus.value = SyncStatus.SYNCING
        } else if (snapshot.metadata.isFromCache) {
            _syncStatus.value = SyncStatus.OFFLINE
        } else {
            _syncStatus.value = SyncStatus.CONNECTED
        }
    }

    private fun updateSyncStatusFromSnapshot(snapshot: QuerySnapshot?) {
        if (snapshot == null) return
        if (snapshot.metadata.hasPendingWrites()) {
            _syncStatus.value = SyncStatus.SYNCING
        } else if (snapshot.metadata.isFromCache) {
            _syncStatus.value = SyncStatus.OFFLINE
        } else {
            _syncStatus.value = SyncStatus.CONNECTED
        }
    }

    private fun getCurrentUserId(): String? {
        if (activeUserId != null) return activeUserId
        return try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }

    override fun updateFamilyName(newName: String) {
        val uid = getCurrentUserId() ?: return
        val now = System.currentTimeMillis()

        _familyState.value = _familyState.value.copy(familyName = newName, updatedAt = now)

        if (uid.startsWith("local_")) {
            saveLocalFamilyToPrefs(_familyState.value)
            _syncStatus.value = SyncStatus.CONNECTED
            return
        }

        val database = getFirestoreInstance() ?: return

        val familyMap = mapOf(
            "id" to "fam_$uid",
            "ownerUserId" to uid,
            "familyName" to newName,
            "updatedAt" to now
        )

        try {
            database.collection("users").document(uid)
                .collection("family").document("profile")
                .set(familyMap, SetOptions.merge())
                .addOnSuccessListener { _syncStatus.value = SyncStatus.CONNECTED }
                .addOnFailureListener { _syncStatus.value = SyncStatus.OFFLINE }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    override fun addChild(child: Child) {
        val uid = getCurrentUserId() ?: return
        val childId = if (child.id.isBlank() || child.id.startsWith("temp_")) {
            if (uid.startsWith("local_")) {
                "child_${System.currentTimeMillis()}"
            } else {
                try {
                    getFirestoreInstance()?.collection("users")?.document(uid)?.collection("children")?.document()?.id
                        ?: "child_${System.currentTimeMillis()}"
                } catch (e: Exception) {
                    "child_${System.currentTimeMillis()}"
                }
            }
        } else {
            child.id
        }

        val now = System.currentTimeMillis()
        val newChild = child.copy(
            id = childId,
            familyId = "fam_$uid",
            createdAt = if (child.createdAt == 0L) now else child.createdAt,
            updatedAt = now
        )

        // Optimistic UI update for immediate response
        _childrenState.value = (_childrenState.value.filterNot { it.id == childId } + newChild).sortedByDescending { it.createdAt }

        if (uid.startsWith("local_")) {
            saveLocalChildrenToPrefs(_childrenState.value)
            _syncStatus.value = SyncStatus.CONNECTED
            return
        }

        val database = getFirestoreInstance() ?: return

        val childMap = mapOf(
            "id" to childId,
            "familyId" to "fam_$uid",
            "name" to newChild.name,
            "birthDate" to newChild.birthDate,
            "ageText" to newChild.ageText,
            "gender" to newChild.gender.name,
            "avatarColorHex" to newChild.avatarColorHex,
            "notes" to newChild.notes,
            "createdAt" to newChild.createdAt,
            "updatedAt" to newChild.updatedAt
        )

        try {
            database.collection("users").document(uid)
                .collection("children").document(childId)
                .set(childMap)
                .addOnSuccessListener { _syncStatus.value = SyncStatus.CONNECTED }
                .addOnFailureListener { _syncStatus.value = SyncStatus.OFFLINE }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    override fun updateChild(child: Child) {
        val uid = getCurrentUserId() ?: return
        val now = System.currentTimeMillis()
        val updatedChild = child.copy(updatedAt = now)

        _childrenState.value = _childrenState.value.map {
            if (it.id == child.id) updatedChild else it
        }

        if (uid.startsWith("local_")) {
            saveLocalChildrenToPrefs(_childrenState.value)
            _syncStatus.value = SyncStatus.CONNECTED
            return
        }

        val database = getFirestoreInstance() ?: return

        val childMap = mapOf(
            "id" to child.id,
            "familyId" to "fam_$uid",
            "name" to child.name,
            "birthDate" to child.birthDate,
            "ageText" to child.ageText,
            "gender" to child.gender.name,
            "avatarColorHex" to child.avatarColorHex,
            "notes" to child.notes,
            "createdAt" to child.createdAt,
            "updatedAt" to now
        )

        try {
            database.collection("users").document(uid)
                .collection("children").document(child.id)
                .set(childMap, SetOptions.merge())
                .addOnSuccessListener { _syncStatus.value = SyncStatus.CONNECTED }
                .addOnFailureListener { _syncStatus.value = SyncStatus.OFFLINE }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    override fun deleteChild(id: String) {
        val uid = getCurrentUserId() ?: return
        _childrenState.value = _childrenState.value.filterNot { it.id == id }

        if (uid.startsWith("local_")) {
            saveLocalChildrenToPrefs(_childrenState.value)
            _syncStatus.value = SyncStatus.CONNECTED
            return
        }

        val database = getFirestoreInstance() ?: return

        try {
            database.collection("users").document(uid)
                .collection("children").document(id)
                .delete()
                .addOnSuccessListener { _syncStatus.value = SyncStatus.CONNECTED }
                .addOnFailureListener { _syncStatus.value = SyncStatus.OFFLINE }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    override fun clearListenersAndState() {
        familyListener?.remove()
        familyListener = null
        childrenListener?.remove()
        childrenListener = null
        activeUserId = null

        _familyState.value = Family()
        _childrenState.value = emptyList()
        _syncStatus.value = SyncStatus.CONNECTED
    }

    override fun restoreFamilyAndChildren(family: Family, children: List<Child>) {
        _familyState.value = family
        _childrenState.value = children
        saveLocalFamilyToPrefs(family)
        saveLocalChildrenToPrefs(children)
    }
}
