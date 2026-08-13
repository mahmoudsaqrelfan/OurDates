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

    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _familyState = MutableStateFlow(Family())
    private val _childrenState = MutableStateFlow<List<Child>>(emptyList())

    private var familyListener: ListenerRegistration? = null
    private var childrenListener: ListenerRegistration? = null
    private var activeUserId: String? = null

    private fun localPrefs() = MawaeednaApplication.appContext
        ?.getSharedPreferences("mawaeedna_local_family_prefs", Context.MODE_PRIVATE)

    private fun saveLocalFamilyToPrefs(family: Family) {
        localPrefs()?.edit()
            ?.putString("family_id", family.id)
            ?.putString("owner_id", family.ownerUserId)
            ?.putString("family_name", family.familyName)
            ?.putLong("created_at", family.createdAt)
            ?.putLong("updated_at", family.updatedAt)
            ?.apply()
    }

    private fun loadLocalFamilyFromPrefs(userId: String): Family? {
        val prefs = localPrefs() ?: return null
        val familyName = prefs.getString("family_name", null) ?: return null
        return Family(
            id = prefs.getString("family_id", "fam_$userId") ?: "fam_$userId",
            ownerUserId = prefs.getString("owner_id", userId) ?: userId,
            familyName = familyName,
            createdAt = prefs.getLong("created_at", System.currentTimeMillis()),
            updatedAt = prefs.getLong("updated_at", System.currentTimeMillis())
        )
    }

    private fun saveLocalChildrenToPrefs(children: List<Child>) {
        val array = JSONArray()
        children.forEach { child ->
            array.put(JSONObject().apply {
                put("id", child.id)
                put("familyId", child.familyId)
                put("name", child.name)
                put("birthDate", child.birthDate)
                put("ageText", child.ageText)
                put("gender", child.gender.name)
                put("avatarColorHex", child.avatarColorHex)
                put("notes", child.notes)
                put("createdAt", child.createdAt)
                put("updatedAt", child.updatedAt)
            })
        }
        localPrefs()?.edit()?.putString("children_json", array.toString())?.apply()
    }

    private fun loadLocalChildrenFromPrefs(userId: String): List<Child>? {
        val json = localPrefs()?.getString("children_json", null) ?: return null
        return try {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val gender = try {
                        Gender.valueOf(obj.optString("gender", "BOY"))
                    } catch (_: Exception) {
                        Gender.BOY
                    }
                    add(
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
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun firestore(): FirebaseFirestore? = try {
        FirebaseFirestore.getInstance().also { db ->
            try {
                db.firestoreSettings = firestoreSettings {
                    setLocalCacheSettings(persistentCacheSettings { })
                }
            } catch (_: Exception) {
                // Firestore settings can only be changed before first use.
            }
        }
    } catch (_: Exception) {
        null
    }

    override fun getFamily(): Flow<Family> = _familyState.asStateFlow()
    override fun getChildren(): Flow<List<Child>> = _childrenState.asStateFlow()
    override fun getChildById(id: String): Flow<Child?> = _childrenState.map { children -> children.find { it.id == id } }

    override fun ensureFamilyCreated(ownerUserId: String): Family {
        if (ownerUserId.isBlank()) return _familyState.value

        if (ownerUserId.startsWith("local_")) {
            if (activeUserId != ownerUserId) clearListenersAndState()
            activeUserId = ownerUserId

            val family = loadLocalFamilyFromPrefs(ownerUserId) ?: Family(
                id = "fam_$ownerUserId",
                ownerUserId = ownerUserId,
                familyName = "عائلتي"
            ).also(::saveLocalFamilyToPrefs)

            // An explicitly saved empty JSON array is a valid state. Never reseed deleted children.
            val children = loadLocalChildrenFromPrefs(ownerUserId) ?: emptyList<Child>().also(::saveLocalChildrenToPrefs)
            _familyState.value = family
            _childrenState.value = children
            _syncStatus.value = SyncStatus.LOCAL
            return family
        }

        if (activeUserId != ownerUserId) attachListenersForUser(ownerUserId)
        val db = firestore() ?: run {
            _syncStatus.value = SyncStatus.OFFLINE
            return _familyState.value
        }

        val familyRef = db.collection("users").document(ownerUserId).collection("family").document("profile")
        familyRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    val now = System.currentTimeMillis()
                    familyRef.set(
                        mapOf(
                            "id" to "fam_$ownerUserId",
                            "ownerUserId" to ownerUserId,
                            "familyName" to "عائلتي",
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                }
            }
            .addOnFailureListener { _syncStatus.value = SyncStatus.OFFLINE }

        return _familyState.value
    }

    @Synchronized
    private fun attachListenersForUser(userId: String) {
        clearListenersAndState()
        activeUserId = userId
        _syncStatus.value = SyncStatus.SYNCING

        val db = firestore() ?: run {
            _syncStatus.value = SyncStatus.OFFLINE
            return
        }
        val userDoc = db.collection("users").document(userId)

        familyListener = userDoc.collection("family").document("profile")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _syncStatus.value = SyncStatus.OFFLINE
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _familyState.value = Family(
                        id = snapshot.getString("id") ?: "fam_$userId",
                        ownerUserId = snapshot.getString("ownerUserId") ?: userId,
                        familyName = snapshot.getString("familyName") ?: "عائلتي",
                        createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                }
                updateSyncStatus(snapshot)
            }

        childrenListener = userDoc.collection("children")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _syncStatus.value = SyncStatus.OFFLINE
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Empty is a real synchronized state. Do not recreate demo children.
                    _childrenState.value = snapshot.documents.mapNotNull { doc ->
                        try {
                            val gender = try {
                                Gender.valueOf(doc.getString("gender") ?: "BOY")
                            } catch (_: Exception) {
                                Gender.BOY
                            }
                            Child(
                                id = doc.id,
                                familyId = doc.getString("familyId") ?: "fam_$userId",
                                name = doc.getString("name") ?: "",
                                birthDate = doc.getString("birthDate") ?: "",
                                ageText = doc.getString("ageText") ?: "",
                                gender = gender,
                                avatarColorHex = doc.getString("avatarColorHex") ?: "#00A896",
                                notes = doc.getString("notes") ?: "",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }.sortedByDescending { it.createdAt }
                }
                updateSyncStatus(snapshot)
            }
    }

    private fun updateSyncStatus(snapshot: DocumentSnapshot?) {
        if (snapshot == null) return
        _syncStatus.value = when {
            snapshot.metadata.hasPendingWrites() -> SyncStatus.SYNCING
            snapshot.metadata.isFromCache -> SyncStatus.OFFLINE
            else -> SyncStatus.CONNECTED
        }
    }

    private fun updateSyncStatus(snapshot: QuerySnapshot?) {
        if (snapshot == null) return
        _syncStatus.value = when {
            snapshot.metadata.hasPendingWrites() -> SyncStatus.SYNCING
            snapshot.metadata.isFromCache -> SyncStatus.OFFLINE
            else -> SyncStatus.CONNECTED
        }
    }

    private fun currentUserId(): String? = activeUserId ?: try {
        FirebaseAuth.getInstance().currentUser?.uid
    } catch (_: Exception) {
        null
    }

    override fun updateFamilyName(newName: String) {
        val uid = currentUserId() ?: return
        val now = System.currentTimeMillis()
        _familyState.value = _familyState.value.copy(familyName = newName, updatedAt = now)

        if (uid.startsWith("local_")) {
            saveLocalFamilyToPrefs(_familyState.value)
            _syncStatus.value = SyncStatus.LOCAL
            return
        }

        val db = firestore() ?: run { _syncStatus.value = SyncStatus.OFFLINE; return }
        _syncStatus.value = SyncStatus.SYNCING
        db.collection("users").document(uid).collection("family").document("profile")
            .set(
                mapOf(
                    "id" to "fam_$uid",
                    "ownerUserId" to uid,
                    "familyName" to newName,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { _syncStatus.value = SyncStatus.OFFLINE }
    }

    override fun addChild(child: Child) {
        val uid = currentUserId() ?: return
        val db = if (uid.startsWith("local_")) null else firestore()
        val childId = when {
            child.id.isNotBlank() && !child.id.startsWith("temp_") -> child.id
            db != null -> db.collection("users").document(uid).collection("children").document().id
            else -> "child_${System.currentTimeMillis()}"
        }
        val now = System.currentTimeMillis()
        val newChild = child.copy(
            id = childId,
            familyId = "fam_$uid",
            createdAt = if (child.createdAt == 0L) now else child.createdAt,
            updatedAt = now
        )
        _childrenState.value = (_childrenState.value.filterNot { it.id == childId } + newChild)
            .sortedByDescending { it.createdAt }

        if (uid.startsWith("local_")) {
            saveLocalChildrenToPrefs(_childrenState.value)
            _syncStatus.value = SyncStatus.LOCAL
            return
        }
        if (db == null) { _syncStatus.value = SyncStatus.OFFLINE; return }

        _syncStatus.value = SyncStatus.SYNCING
        db.collection("users").document(uid).collection("children").document(childId)
            .set(childMap(newChild))
            .addOnFailureListener { _syncStatus.value = SyncStatus.OFFLINE }
    }

    override fun updateChild(child: Child) {
        val uid = currentUserId() ?: return
        val updated = child.copy(familyId = "fam_$uid", updatedAt = System.currentTimeMillis())
        _childrenState.value = _childrenState.value.map { if (it.id == child.id) updated else it }

        if (uid.startsWith("local_")) {
            saveLocalChildrenToPrefs(_childrenState.value)
            _syncStatus.value = SyncStatus.LOCAL
            return
        }
        val db = firestore() ?: run { _syncStatus.value = SyncStatus.OFFLINE; return }
        _syncStatus.value = SyncStatus.SYNCING
        db.collection("users").document(uid).collection("children").document(child.id)
            .set(childMap(updated), SetOptions.merge())
            .addOnFailureListener { _syncStatus.value = SyncStatus.OFFLINE }
    }

    override fun deleteChild(id: String) {
        val uid = currentUserId() ?: return
        _childrenState.value = _childrenState.value.filterNot { it.id == id }

        if (uid.startsWith("local_")) {
            saveLocalChildrenToPrefs(_childrenState.value)
            _syncStatus.value = SyncStatus.LOCAL
            return
        }
        val db = firestore() ?: run { _syncStatus.value = SyncStatus.OFFLINE; return }
        _syncStatus.value = SyncStatus.SYNCING
        db.collection("users").document(uid).collection("children").document(id)
            .delete()
            .addOnFailureListener { _syncStatus.value = SyncStatus.OFFLINE }
    }

    private fun childMap(child: Child) = mapOf(
        "id" to child.id,
        "familyId" to child.familyId,
        "name" to child.name,
        "birthDate" to child.birthDate,
        "ageText" to child.ageText,
        "gender" to child.gender.name,
        "avatarColorHex" to child.avatarColorHex,
        "notes" to child.notes,
        "createdAt" to child.createdAt,
        "updatedAt" to child.updatedAt
    )

    override fun clearListenersAndState() {
        familyListener?.remove()
        childrenListener?.remove()
        familyListener = null
        childrenListener = null
        activeUserId = null
        _familyState.value = Family()
        _childrenState.value = emptyList()
        _syncStatus.value = SyncStatus.OFFLINE
    }

    override fun restoreFamilyAndChildren(family: Family, children: List<Child>) {
        _familyState.value = family
        _childrenState.value = children
        saveLocalFamilyToPrefs(family)
        saveLocalChildrenToPrefs(children)
        _syncStatus.value = if (family.ownerUserId.startsWith("local_")) SyncStatus.LOCAL else SyncStatus.OFFLINE
    }
}
