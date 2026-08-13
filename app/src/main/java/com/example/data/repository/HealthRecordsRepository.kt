package com.example.data.repository

import android.content.Context
import com.example.MawaeednaApplication
import com.example.data.model.GlucoseReading
import com.example.data.model.MealContext
import com.example.data.model.TestAppointment
import com.example.data.model.TestResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface HealthRecordsRepository {
    fun getTestAppointmentsForChild(childId: String): Flow<List<TestAppointment>>
    fun addTestAppointment(testAppointment: TestAppointment)
    fun getTestResultsForChild(childId: String): Flow<List<TestResult>>
    fun addTestResult(testResult: TestResult)
    fun updateTestResult(testResult: TestResult)
    fun deleteTestResult(id: String)
    fun getGlucoseReadingsForChild(childId: String): Flow<List<GlucoseReading>>
    fun addGlucoseReading(glucoseReading: GlucoseReading)
    fun updateGlucoseReading(glucoseReading: GlucoseReading)
    fun deleteGlucoseReading(id: String)
    fun attachUser(userId: String)
    fun clearUser()
    fun getAllTestResults(): Flow<List<TestResult>>
    fun getAllGlucoseReadings(): Flow<List<GlucoseReading>>
    fun restoreHealthRecords(results: List<TestResult>, readings: List<GlucoseReading>)
}

class InMemoryHealthRecordsRepository : HealthRecordsRepository {
    private val _testResultsState = MutableStateFlow<List<TestResult>>(emptyList())
    private val _glucoseReadingsState = MutableStateFlow<List<GlucoseReading>>(emptyList())

    private var resultsListener: ListenerRegistration? = null
    private var glucoseListener: ListenerRegistration? = null
    private var activeUserId: String? = null

    override fun getTestAppointmentsForChild(childId: String): Flow<List<TestAppointment>> =
        MutableStateFlow<List<TestAppointment>>(emptyList()).asStateFlow()

    override fun addTestAppointment(testAppointment: TestAppointment) {
        // Handled via main AppointmentRepository.
    }

    override fun getTestResultsForChild(childId: String): Flow<List<TestResult>> =
        _testResultsState.map { list -> list.filter { it.childId == childId }.sortedByDescending { it.createdAt } }

    override fun getGlucoseReadingsForChild(childId: String): Flow<List<GlucoseReading>> =
        _glucoseReadingsState.map { list -> list.filter { it.childId == childId }.sortedByDescending { it.createdAt } }

    override fun getAllTestResults(): Flow<List<TestResult>> = _testResultsState.asStateFlow()
    override fun getAllGlucoseReadings(): Flow<List<GlucoseReading>> = _glucoseReadingsState.asStateFlow()

    override fun restoreHealthRecords(results: List<TestResult>, readings: List<GlucoseReading>) {
        val userId = activeUserId ?: "local_user_mode"
        _testResultsState.value = results
        _glucoseReadingsState.value = readings
        saveResultsToPrefs(userId, results)
        saveGlucoseToPrefs(userId, readings)
    }

    override fun attachUser(userId: String) {
        if (activeUserId == userId && (userId.startsWith("local_") || resultsListener != null)) return

        removeListeners()
        activeUserId = userId

        // Always restore only this account's cache. Local and Google data must never share storage.
        _testResultsState.value = loadResultsFromPrefs(userId)
        _glucoseReadingsState.value = loadGlucoseFromPrefs(userId)

        if (userId.startsWith("local_")) return

        try {
            val db = FirebaseFirestore.getInstance()

            resultsListener = db.collection("users").document(userId).collection("test_results")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                TestResult(
                                    id = doc.id,
                                    childId = doc.getString("childId") ?: "",
                                    testName = doc.getString("testName") ?: "",
                                    resultValue = doc.getString("resultValue") ?: "",
                                    unit = doc.getString("unit") ?: "",
                                    isNormal = doc.getBoolean("isNormal") ?: true,
                                    normalRangeText = doc.getString("normalRangeText") ?: "",
                                    testDateText = doc.getString("testDateText") ?: "",
                                    doctorNotes = doc.getString("doctorNotes") ?: "",
                                    testDefinitionId = doc.getString("testDefinitionId"),
                                    testAppointmentId = doc.getString("testAppointmentId"),
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                        _testResultsState.value = list
                        saveResultsToPrefs(userId, list)
                    }
                }

            glucoseListener = db.collection("users").document(userId).collection("glucose_readings")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val mealContext = try {
                                    MealContext.valueOf(doc.getString("mealContext") ?: "RANDOM")
                                } catch (_: Exception) {
                                    MealContext.RANDOM
                                }
                                GlucoseReading(
                                    id = doc.id,
                                    childId = doc.getString("childId") ?: "",
                                    readingValue = doc.getLong("readingValue")?.toInt() ?: 100,
                                    unit = doc.getString("unit") ?: "mg/dL",
                                    mealContext = mealContext,
                                    dateText = doc.getString("dateText") ?: "",
                                    timeText = doc.getString("timeText") ?: "",
                                    isTargetRange = doc.getBoolean("isTargetRange") ?: true,
                                    notes = doc.getString("notes") ?: "",
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                        _glucoseReadingsState.value = list
                        saveGlucoseToPrefs(userId, list)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeListeners() {
        resultsListener?.remove()
        resultsListener = null
        glucoseListener?.remove()
        glucoseListener = null
    }

    override fun clearUser() {
        removeListeners()
        activeUserId = null
        _testResultsState.value = emptyList()
        _glucoseReadingsState.value = emptyList()
    }

    override fun addTestResult(testResult: TestResult) {
        val userId = activeUserId ?: "local_user_mode"
        val toSave = if (testResult.id.isBlank()) {
            testResult.copy(id = "res_${UUID.randomUUID().toString().take(8)}")
        } else testResult

        val newList = _testResultsState.value.filterNot { it.id == toSave.id } + toSave
        _testResultsState.value = newList
        saveResultsToPrefs(userId, newList)

        if (userId.startsWith("local_")) return
        try {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("test_results").document(toSave.id)
                .set(mapResultToMap(toSave), SetOptions.merge())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun updateTestResult(testResult: TestResult) {
        val userId = activeUserId ?: "local_user_mode"
        val updated = testResult.copy(updatedAt = System.currentTimeMillis())
        val newList = _testResultsState.value.map { if (it.id == updated.id) updated else it }
        _testResultsState.value = newList
        saveResultsToPrefs(userId, newList)

        if (userId.startsWith("local_")) return
        try {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("test_results").document(updated.id)
                .set(mapResultToMap(updated), SetOptions.merge())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deleteTestResult(id: String) {
        val userId = activeUserId ?: "local_user_mode"
        val newList = _testResultsState.value.filterNot { it.id == id }
        _testResultsState.value = newList
        saveResultsToPrefs(userId, newList)

        if (userId.startsWith("local_")) return
        try {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("test_results").document(id).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun addGlucoseReading(glucoseReading: GlucoseReading) {
        val userId = activeUserId ?: "local_user_mode"
        val toSave = if (glucoseReading.id.isBlank()) {
            glucoseReading.copy(id = "gluc_${UUID.randomUUID().toString().take(8)}")
        } else glucoseReading

        val newList = _glucoseReadingsState.value.filterNot { it.id == toSave.id } + toSave
        _glucoseReadingsState.value = newList
        saveGlucoseToPrefs(userId, newList)

        if (userId.startsWith("local_")) return
        writeGlucoseToFirestore(userId, toSave)
    }

    override fun updateGlucoseReading(glucoseReading: GlucoseReading) {
        val userId = activeUserId ?: "local_user_mode"
        val updated = glucoseReading.copy(updatedAt = System.currentTimeMillis())
        val newList = _glucoseReadingsState.value.map { if (it.id == updated.id) updated else it }
        _glucoseReadingsState.value = newList
        saveGlucoseToPrefs(userId, newList)

        if (userId.startsWith("local_")) return
        writeGlucoseToFirestore(userId, updated)
    }

    override fun deleteGlucoseReading(id: String) {
        val userId = activeUserId ?: "local_user_mode"
        val newList = _glucoseReadingsState.value.filterNot { it.id == id }
        _glucoseReadingsState.value = newList
        saveGlucoseToPrefs(userId, newList)

        if (userId.startsWith("local_")) return
        try {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("glucose_readings").document(id).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeGlucoseToFirestore(userId: String, reading: GlucoseReading) {
        try {
            val map = mapOf(
                "id" to reading.id,
                "childId" to reading.childId,
                "readingValue" to reading.readingValue,
                "unit" to reading.unit,
                "mealContext" to reading.mealContext.name,
                "dateText" to reading.dateText,
                "timeText" to reading.timeText,
                "isTargetRange" to reading.isTargetRange,
                "notes" to reading.notes,
                "createdAt" to reading.createdAt,
                "updatedAt" to reading.updatedAt
            )
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("glucose_readings").document(reading.id)
                .set(map, SetOptions.merge())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mapResultToMap(res: TestResult): Map<String, Any?> = mapOf(
        "id" to res.id,
        "childId" to res.childId,
        "testName" to res.testName,
        "resultValue" to res.resultValue,
        "unit" to res.unit,
        "isNormal" to res.isNormal,
        "normalRangeText" to res.normalRangeText,
        "testDateText" to res.testDateText,
        "doctorNotes" to res.doctorNotes,
        "testDefinitionId" to res.testDefinitionId,
        "testAppointmentId" to res.testAppointmentId,
        "createdAt" to res.createdAt,
        "updatedAt" to res.updatedAt
    )

    private fun resultsPrefsName(userId: String): String =
        if (userId.startsWith("local_")) "mawaeedna_local_results_prefs"
        else "mawaeedna_results_cache_${safeUserKey(userId)}"

    private fun glucosePrefsName(userId: String): String =
        if (userId.startsWith("local_")) "mawaeedna_local_glucose_prefs"
        else "mawaeedna_glucose_cache_${safeUserKey(userId)}"

    private fun safeUserKey(userId: String): String = userId.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private fun saveResultsToPrefs(userId: String, results: List<TestResult>) {
        try {
            val context = MawaeednaApplication.appContext ?: return
            val prefs = context.getSharedPreferences(resultsPrefsName(userId), Context.MODE_PRIVATE)
            val array = JSONArray()
            results.forEach { res ->
                array.put(JSONObject().apply {
                    put("id", res.id)
                    put("childId", res.childId)
                    put("testName", res.testName)
                    put("resultValue", res.resultValue)
                    put("unit", res.unit)
                    put("isNormal", res.isNormal)
                    put("normalRangeText", res.normalRangeText)
                    put("testDateText", res.testDateText)
                    put("doctorNotes", res.doctorNotes)
                    put("testDefinitionId", res.testDefinitionId ?: "")
                    put("testAppointmentId", res.testAppointmentId ?: "")
                    put("createdAt", res.createdAt)
                    put("updatedAt", res.updatedAt)
                })
            }
            prefs.edit().putString("results_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadResultsFromPrefs(userId: String): List<TestResult> {
        return try {
            val context = MawaeednaApplication.appContext ?: return emptyList()
            val prefs = context.getSharedPreferences(resultsPrefsName(userId), Context.MODE_PRIVATE)
            val json = prefs.getString("results_json", null) ?: return emptyList()
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(TestResult(
                        id = obj.getString("id"),
                        childId = obj.getString("childId"),
                        testName = obj.getString("testName"),
                        resultValue = obj.getString("resultValue"),
                        unit = obj.optString("unit", ""),
                        isNormal = obj.optBoolean("isNormal", true),
                        normalRangeText = obj.optString("normalRangeText", ""),
                        testDateText = obj.getString("testDateText"),
                        doctorNotes = obj.optString("doctorNotes", ""),
                        testDefinitionId = obj.optString("testDefinitionId").ifBlank { null },
                        testAppointmentId = obj.optString("testAppointmentId").ifBlank { null },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveGlucoseToPrefs(userId: String, readings: List<GlucoseReading>) {
        try {
            val context = MawaeednaApplication.appContext ?: return
            val prefs = context.getSharedPreferences(glucosePrefsName(userId), Context.MODE_PRIVATE)
            val array = JSONArray()
            readings.forEach { reading ->
                array.put(JSONObject().apply {
                    put("id", reading.id)
                    put("childId", reading.childId)
                    put("readingValue", reading.readingValue)
                    put("unit", reading.unit)
                    put("mealContext", reading.mealContext.name)
                    put("dateText", reading.dateText)
                    put("timeText", reading.timeText)
                    put("isTargetRange", reading.isTargetRange)
                    put("notes", reading.notes)
                    put("createdAt", reading.createdAt)
                    put("updatedAt", reading.updatedAt)
                })
            }
            prefs.edit().putString("glucose_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadGlucoseFromPrefs(userId: String): List<GlucoseReading> {
        return try {
            val context = MawaeednaApplication.appContext ?: return emptyList()
            val prefs = context.getSharedPreferences(glucosePrefsName(userId), Context.MODE_PRIVATE)
            val json = prefs.getString("glucose_json", null) ?: return emptyList()
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val mealContext = try {
                        MealContext.valueOf(obj.optString("mealContext", "RANDOM"))
                    } catch (_: Exception) {
                        MealContext.RANDOM
                    }
                    add(GlucoseReading(
                        id = obj.getString("id"),
                        childId = obj.getString("childId"),
                        readingValue = obj.getInt("readingValue"),
                        unit = obj.optString("unit", "mg/dL"),
                        mealContext = mealContext,
                        dateText = obj.getString("dateText"),
                        timeText = obj.getString("timeText"),
                        isTargetRange = obj.optBoolean("isTargetRange", true),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
