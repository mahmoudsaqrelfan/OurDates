package com.example.data.repository

import android.content.Context
import com.example.MawaeednaApplication
import com.example.data.model.GlucoseReading
import com.example.data.model.MealContext
import com.example.data.model.TestAppointment
import com.example.data.model.TestResult
import com.example.data.model.TestStatus
import com.google.firebase.auth.FirebaseAuth
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

    init {
        // Initialize with local prefs fallback
        val listRes = loadLocalResultsFromPrefs()
        _testResultsState.value = listRes
        val listGluc = loadLocalGlucoseFromPrefs()
        _glucoseReadingsState.value = listGluc
    }

    override fun getTestAppointmentsForChild(childId: String): Flow<List<TestAppointment>> {
        return MutableStateFlow<List<TestAppointment>>(emptyList()).asStateFlow()
    }

    override fun addTestAppointment(testAppointment: TestAppointment) {
        // Handled via main AppointmentRepository
    }

    override fun getTestResultsForChild(childId: String): Flow<List<TestResult>> = _testResultsState.map { list ->
        list.filter { it.childId == childId }.sortedByDescending { it.createdAt }
    }

    override fun getGlucoseReadingsForChild(childId: String): Flow<List<GlucoseReading>> = _glucoseReadingsState.map { list ->
        list.filter { it.childId == childId }.sortedByDescending { it.createdAt }
    }

    override fun getAllTestResults(): Flow<List<TestResult>> = _testResultsState.asStateFlow()

    override fun getAllGlucoseReadings(): Flow<List<GlucoseReading>> = _glucoseReadingsState.asStateFlow()

    override fun restoreHealthRecords(results: List<TestResult>, readings: List<GlucoseReading>) {
        _testResultsState.value = results
        _glucoseReadingsState.value = readings
        saveLocalResultsToPrefs(results)
        saveLocalGlucoseToPrefs(readings)
    }

    override fun attachUser(userId: String) {
        if (activeUserId == userId && (userId.startsWith("local_") || resultsListener != null)) return
        clearUser()
        activeUserId = userId

        if (userId.startsWith("local_")) {
            _testResultsState.value = loadLocalResultsFromPrefs()
            _glucoseReadingsState.value = loadLocalGlucoseFromPrefs()
            return
        }

        // Google Mode: Sync results and glucose readings from Firestore in real-time
        try {
            val db = FirebaseFirestore.getInstance()
            
            // 1. Test Results snapshot listener
            resultsListener = db.collection("users").document(userId).collection("test_results")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _testResultsState.value = loadLocalResultsFromPrefs()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val childId = doc.getString("childId") ?: ""
                                val testName = doc.getString("testName") ?: ""
                                val resultValue = doc.getString("resultValue") ?: ""
                                val unit = doc.getString("unit") ?: ""
                                val isNormal = doc.getBoolean("isNormal") ?: true
                                val normalRangeText = doc.getString("normalRangeText") ?: ""
                                val testDateText = doc.getString("testDateText") ?: ""
                                val doctorNotes = doc.getString("doctorNotes") ?: ""
                                val testDefinitionId = doc.getString("testDefinitionId")
                                val testAppointmentId = doc.getString("testAppointmentId")
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                                TestResult(
                                    id = id,
                                    childId = childId,
                                    testName = testName,
                                    resultValue = resultValue,
                                    unit = unit,
                                    isNormal = isNormal,
                                    normalRangeText = normalRangeText,
                                    testDateText = testDateText,
                                    doctorNotes = doctorNotes,
                                    testDefinitionId = testDefinitionId,
                                    testAppointmentId = testAppointmentId,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _testResultsState.value = list
                        // Keep a backup local cache
                        saveLocalResultsToPrefs(list)
                    }
                }

            // 2. Glucose Readings snapshot listener
            glucoseListener = db.collection("users").document(userId).collection("glucose_readings")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _glucoseReadingsState.value = loadLocalGlucoseFromPrefs()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val childId = doc.getString("childId") ?: ""
                                val readingValue = doc.getLong("readingValue")?.toInt() ?: 100
                                val unit = doc.getString("unit") ?: "mg/dL"
                                val mealContextStr = doc.getString("mealContext") ?: "RANDOM"
                                val mealContext = try { MealContext.valueOf(mealContextStr) } catch (e: Exception) { MealContext.RANDOM }
                                val dateText = doc.getString("dateText") ?: ""
                                val timeText = doc.getString("timeText") ?: ""
                                val isTargetRange = doc.getBoolean("isTargetRange") ?: true
                                val notes = doc.getString("notes") ?: ""
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                                GlucoseReading(
                                    id = id,
                                    childId = childId,
                                    readingValue = readingValue,
                                    unit = unit,
                                    mealContext = mealContext,
                                    dateText = dateText,
                                    timeText = timeText,
                                    isTargetRange = isTargetRange,
                                    notes = notes,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _glucoseReadingsState.value = list
                        saveLocalGlucoseToPrefs(list)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun clearUser() {
        resultsListener?.remove()
        resultsListener = null
        glucoseListener?.remove()
        glucoseListener = null
        activeUserId = null
        _testResultsState.value = emptyList()
        _glucoseReadingsState.value = emptyList()
    }

    override fun addTestResult(testResult: TestResult) {
        val currentUserId = activeUserId ?: "local_user_mode"
        val toSave = if (testResult.id.isBlank()) testResult.copy(id = "res_${UUID.randomUUID().toString().take(8)}") else testResult

        if (currentUserId.startsWith("local_")) {
            val newList = _testResultsState.value + toSave
            _testResultsState.value = newList
            saveLocalResultsToPrefs(newList)
            return
        }

        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(currentUserId).collection("test_results").document(toSave.id)
                .set(mapResultToMap(toSave), SetOptions.merge())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun updateTestResult(testResult: TestResult) {
        val currentUserId = activeUserId ?: "local_user_mode"
        val updated = testResult.copy(updatedAt = System.currentTimeMillis())

        if (currentUserId.startsWith("local_")) {
            val newList = _testResultsState.value.map { if (it.id == updated.id) updated else it }
            _testResultsState.value = newList
            saveLocalResultsToPrefs(newList)
            return
        }

        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(currentUserId).collection("test_results").document(updated.id)
                .set(mapResultToMap(updated), SetOptions.merge())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deleteTestResult(id: String) {
        val currentUserId = activeUserId ?: "local_user_mode"

        if (currentUserId.startsWith("local_")) {
            val newList = _testResultsState.value.filterNot { it.id == id }
            _testResultsState.value = newList
            saveLocalResultsToPrefs(newList)
            return
        }

        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(currentUserId).collection("test_results").document(id)
                .delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun addGlucoseReading(glucoseReading: GlucoseReading) {
        val currentUserId = activeUserId ?: "local_user_mode"
        val toSave = if (glucoseReading.id.isBlank()) glucoseReading.copy(id = "gluc_${UUID.randomUUID().toString().take(8)}") else glucoseReading

        if (currentUserId.startsWith("local_")) {
            val newList = _glucoseReadingsState.value + toSave
            _glucoseReadingsState.value = newList
            saveLocalGlucoseToPrefs(newList)
            return
        }

        try {
            val db = FirebaseFirestore.getInstance()
            val gMap = mapOf(
                "id" to toSave.id,
                "childId" to toSave.childId,
                "readingValue" to toSave.readingValue,
                "unit" to toSave.unit,
                "mealContext" to toSave.mealContext.name,
                "dateText" to toSave.dateText,
                "timeText" to toSave.timeText,
                "isTargetRange" to toSave.isTargetRange,
                "notes" to toSave.notes,
                "createdAt" to toSave.createdAt,
                "updatedAt" to toSave.updatedAt
            )
            db.collection("users").document(currentUserId).collection("glucose_readings").document(toSave.id)
                .set(gMap, SetOptions.merge())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun updateGlucoseReading(glucoseReading: GlucoseReading) {
        val currentUserId = activeUserId ?: "local_user_mode"
        val updated = glucoseReading.copy(updatedAt = System.currentTimeMillis())

        if (currentUserId.startsWith("local_")) {
            val newList = _glucoseReadingsState.value.map { if (it.id == updated.id) updated else it }
            _glucoseReadingsState.value = newList
            saveLocalGlucoseToPrefs(newList)
            return
        }

        try {
            val db = FirebaseFirestore.getInstance()
            val gMap = mapOf(
                "id" to updated.id,
                "childId" to updated.childId,
                "readingValue" to updated.readingValue,
                "unit" to updated.unit,
                "mealContext" to updated.mealContext.name,
                "dateText" to updated.dateText,
                "timeText" to updated.timeText,
                "isTargetRange" to updated.isTargetRange,
                "notes" to updated.notes,
                "createdAt" to updated.createdAt,
                "updatedAt" to updated.updatedAt
            )
            db.collection("users").document(currentUserId).collection("glucose_readings").document(updated.id)
                .set(gMap, SetOptions.merge())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deleteGlucoseReading(id: String) {
        val currentUserId = activeUserId ?: "local_user_mode"

        if (currentUserId.startsWith("local_")) {
            val newList = _glucoseReadingsState.value.filterNot { it.id == id }
            _glucoseReadingsState.value = newList
            saveLocalGlucoseToPrefs(newList)
            return
        }

        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(currentUserId).collection("glucose_readings").document(id)
                .delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mapResultToMap(res: TestResult): Map<String, Any?> {
        return mapOf(
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
    }

    // --- Local JSON Serialization for Preferences (Local Mode and cache fallback) ---

    private fun saveLocalResultsToPrefs(results: List<TestResult>) {
        try {
            val context = MawaeednaApplication.appContext ?: return
            val prefs = context.getSharedPreferences("mawaeedna_local_results_prefs", Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            for (res in results) {
                val obj = JSONObject()
                obj.put("id", res.id)
                obj.put("childId", res.childId)
                obj.put("testName", res.testName)
                obj.put("resultValue", res.resultValue)
                obj.put("unit", res.unit)
                obj.put("isNormal", res.isNormal)
                obj.put("normalRangeText", res.normalRangeText)
                obj.put("testDateText", res.testDateText)
                obj.put("doctorNotes", res.doctorNotes)
                obj.put("testDefinitionId", res.testDefinitionId ?: "")
                obj.put("testAppointmentId", res.testAppointmentId ?: "")
                obj.put("createdAt", res.createdAt)
                obj.put("updatedAt", res.updatedAt)
                jsonArray.put(obj)
            }
            prefs.edit().putString("results_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLocalResultsFromPrefs(): List<TestResult> {
        return try {
            val context = MawaeednaApplication.appContext ?: return emptyList()
            val prefs = context.getSharedPreferences("mawaeedna_local_results_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("results_json", null) ?: return emptyList()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<TestResult>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TestResult(
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
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLocalGlucoseToPrefs(readings: List<GlucoseReading>) {
        try {
            val context = MawaeednaApplication.appContext ?: return
            val prefs = context.getSharedPreferences("mawaeedna_local_glucose_prefs", Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            for (g in readings) {
                val obj = JSONObject()
                obj.put("id", g.id)
                obj.put("childId", g.childId)
                obj.put("readingValue", g.readingValue)
                obj.put("unit", g.unit)
                obj.put("mealContext", g.mealContext.name)
                obj.put("dateText", g.dateText)
                obj.put("timeText", g.timeText)
                obj.put("isTargetRange", g.isTargetRange)
                obj.put("notes", g.notes)
                obj.put("createdAt", g.createdAt)
                obj.put("updatedAt", g.updatedAt)
                jsonArray.put(obj)
            }
            prefs.edit().putString("glucose_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLocalGlucoseFromPrefs(): List<GlucoseReading> {
        return try {
            val context = MawaeednaApplication.appContext ?: return emptyList()
            val prefs = context.getSharedPreferences("mawaeedna_local_glucose_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("glucose_json", null) ?: return emptyList()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<GlucoseReading>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val mealStr = obj.optString("mealContext", "RANDOM")
                val mealContext = try { MealContext.valueOf(mealStr) } catch (e: Exception) { MealContext.RANDOM }
                list.add(
                    GlucoseReading(
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
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
