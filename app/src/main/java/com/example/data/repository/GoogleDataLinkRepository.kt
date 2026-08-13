package com.example.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

interface GoogleDataLinkRepository {
    suspend fun mergeBackupIntoGoogle(jsonString: String, userId: String): Result<Unit>
}

class FirestoreGoogleDataLinkRepository : GoogleDataLinkRepository {
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    override suspend fun mergeBackupIntoGoogle(jsonString: String, userId: String): Result<Unit> = runCatching {
        val root = JSONObject(jsonString)
        val userRef = db.collection("users").document(userId)

        root.optJSONObject("family")?.let { familyObj ->
            val familyRef = userRef.collection("family").document("profile")
            val remote = familyRef.get().await()
            val localName = familyObj.optString("familyName", "عائلتي")
            val localUpdatedAt = familyObj.optLong("updatedAt", 0L)
            val remoteUpdatedAt = remote.getLong("updatedAt") ?: 0L
            val shouldWrite = !remote.exists() || (localName != "عائلتي" && localUpdatedAt >= remoteUpdatedAt)
            if (shouldWrite) {
                val localMap = jsonObjectToMap(familyObj).toMutableMap().apply {
                    this["id"] = "fam_$userId"
                    this["ownerUserId"] = userId
                }
                familyRef.set(localMap, SetOptions.merge()).await()
            }
        }

        mergeArrayCollection(root.optJSONArray("children"), userId, "children") { map ->
            map.toMutableMap().apply { this["familyId"] = "fam_$userId" }
        }
        mergeArrayCollection(root.optJSONArray("appointments"), userId, "appointments") { map ->
            map.toMutableMap().apply { this["familyId"] = "fam_$userId" }
        }
        mergeArrayCollection(root.optJSONArray("testResults"), userId, "test_results") { it }
        mergeArrayCollection(root.optJSONArray("glucoseReadings"), userId, "glucose_readings") { it }

        val extras = mutableMapOf<String, Any?>()
        root.optJSONObject("settings")?.let { extras["settings"] = jsonObjectToMap(it) }
        root.optJSONArray("customTests")?.let { extras["customTests"] = jsonArrayToList(it) }
        extras["updatedAt"] = System.currentTimeMillis()
        userRef.collection("app_state").document("local_extras")
            .set(extras, SetOptions.merge()).await()
    }

    private suspend fun mergeArrayCollection(
        array: JSONArray?,
        userId: String,
        collectionName: String,
        transform: (Map<String, Any?>) -> Map<String, Any?>
    ) {
        if (array == null) return
        val collection = db.collection("users").document(userId).collection(collectionName)
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val id = obj.optString("id")
            if (id.isBlank()) continue
            val map = transform(jsonObjectToMap(obj))
            val remote = collection.document(id).get().await()
            val localUpdatedAt = obj.optLong("updatedAt", 0L)
            val remoteUpdatedAt = remote.getLong("updatedAt") ?: 0L
            if (!remote.exists() || localUpdatedAt >= remoteUpdatedAt) {
                collection.document(id).set(map, SetOptions.merge()).await()
            }
        }
    }

    private fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> {
        val result = linkedMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = jsonValue(obj.opt(key))
        }
        return result
    }

    private fun jsonArrayToList(array: JSONArray): List<Any?> = buildList {
        for (i in 0 until array.length()) add(jsonValue(array.opt(i)))
    }

    private fun jsonValue(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> jsonObjectToMap(value)
        is JSONArray -> jsonArrayToList(value)
        is Number, is Boolean, is String -> value
        else -> value.toString()
    }
}
