package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MawaeednaApplication
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.AppointmentType
import com.example.data.model.Child
import com.example.data.model.Gender
import com.example.data.model.GlucoseReading
import com.example.data.model.MealContext
import com.example.data.model.TestDefinition
import com.example.data.model.TestResult
import com.example.data.repository.AppointmentRepository
import com.example.data.repository.FamilyRepository
import com.example.data.repository.HealthRecordsRepository
import com.example.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

class ChildDetailViewModel(
    private val childId: String,
    private val familyRepository: FamilyRepository = AppContainer.familyRepository,
    private val appointmentRepository: AppointmentRepository = AppContainer.appointmentRepository,
    private val healthRecordsRepository: HealthRecordsRepository = AppContainer.healthRecordsRepository
) : ViewModel() {

    private val _customTests = MutableStateFlow<List<TestDefinition>>(emptyList())
    val customTests: StateFlow<List<TestDefinition>> = _customTests.asStateFlow()

    init {
        loadCustomTests()
    }

    val child: StateFlow<Child?> = familyRepository.getChildById(childId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val appointments: StateFlow<List<Appointment>> = appointmentRepository.getAppointmentsForChild(childId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val testAppointments: StateFlow<List<Appointment>> = appointmentRepository.getAppointmentsForChild(childId)
        .map { list -> list.filter { it.type == AppointmentType.LAB_TEST } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val testResults: StateFlow<List<TestResult>> = healthRecordsRepository.getTestResultsForChild(childId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val glucoseReadings: StateFlow<List<GlucoseReading>> = healthRecordsRepository.getGlucoseReadingsForChild(childId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateChild(name: String, birthDate: String, ageText: String, gender: Gender, avatarColorHex: String) {
        val currentChild = child.value ?: return
        val updated = currentChild.copy(
            name = name,
            birthDate = birthDate,
            ageText = ageText,
            gender = gender,
            avatarColorHex = avatarColorHex,
            updatedAt = System.currentTimeMillis()
        )
        familyRepository.updateChild(updated)
    }

    fun deleteChild(onDeleted: () -> Unit) {
        familyRepository.deleteChild(childId)
        onDeleted()
    }

    fun saveAppointment(appointment: Appointment) {
        if (appointment.id.isBlank() || appointment.id.startsWith("new_")) {
            val newApp = appointment.copy(
                id = "app_${UUID.randomUUID().toString().take(8)}",
                childId = childId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            appointmentRepository.addAppointment(newApp)
        } else {
            appointmentRepository.updateAppointment(appointment)
        }
    }

    fun deleteAppointment(appointmentId: String) {
        appointmentRepository.deleteAppointment(appointmentId)
    }

    fun addAppointment(title: String, doctor: String, clinic: String, date: String, time: String) {
        val newApp = Appointment(
            id = "app_${UUID.randomUUID().toString().take(8)}",
            childId = childId,
            title = title,
            doctorName = doctor,
            clinicName = clinic,
            dateText = date,
            timeText = time,
            status = AppointmentStatus.UPCOMING
        )
        appointmentRepository.addAppointment(newApp)
    }

    fun addTestAppointment(testName: String, labName: String, date: String, time: String, instructions: String) {
        val newApp = Appointment(
            id = "app_${UUID.randomUUID().toString().take(8)}",
            childId = childId,
            type = AppointmentType.LAB_TEST,
            title = testName,
            clinicName = labName,
            dateText = date,
            timeText = time,
            status = AppointmentStatus.UPCOMING,
            notes = instructions,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        appointmentRepository.addAppointment(newApp)
    }

    private var customTestsListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun loadCustomTests() {
        val currentUser = AppContainer.authRepository.currentUser.value
        val userId = currentUser?.id ?: "local_user_mode"

        if (userId.startsWith("local_")) {
            loadCustomTestsFromPrefs()
        } else {
            // Google Mode: Use real-time listener from Firestore for cross-device sync
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                customTestsListener?.remove()
                customTestsListener = db.collection("users").document(userId).collection("custom_tests")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            loadCustomTestsFromPrefs()
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val list = snapshot.documents.mapNotNull { doc ->
                                val id = doc.id
                                val name = doc.getString("name") ?: return@mapNotNull null
                                val category = doc.getString("category") ?: "تحاليل السكر الشائعة للأطفال"
                                val normalRangeText = doc.getString("normalRangeText") ?: ""
                                val unit = doc.getString("unit") ?: ""
                                val description = doc.getString("description") ?: ""
                                TestDefinition(id, name, category, normalRangeText, unit, description)
                            }
                            _customTests.value = list
                        }
                    }
            } catch (e: Exception) {
                loadCustomTestsFromPrefs()
            }
        }
    }

    private fun loadCustomTestsFromPrefs() {
        val context = MawaeednaApplication.appContext ?: return
        val prefs = context.getSharedPreferences("mawaeedna_custom_tests_prefs", android.content.Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("custom_tests", null)
        if (jsonStr != null) {
            try {
                val jsonArray = org.json.JSONArray(jsonStr)
                val list = mutableListOf<TestDefinition>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        TestDefinition(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            category = obj.getString("category"),
                            normalRangeText = obj.optString("normalRangeText", ""),
                            unit = obj.optString("unit", ""),
                            description = obj.optString("description", "")
                        )
                    )
                }
                _customTests.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveCustomTestsToPrefs(list: List<TestDefinition>) {
        val context = MawaeednaApplication.appContext ?: return
        val prefs = context.getSharedPreferences("mawaeedna_custom_tests_prefs", android.content.Context.MODE_PRIVATE)
        try {
            val jsonArray = org.json.JSONArray()
            for (test in list) {
                val obj = org.json.JSONObject()
                obj.put("id", test.id)
                obj.put("name", test.name)
                obj.put("category", test.category)
                obj.put("normalRangeText", test.normalRangeText)
                obj.put("unit", test.unit)
                obj.put("description", test.description)
                jsonArray.put(obj)
            }
            prefs.edit().putString("custom_tests", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addCustomTest(name: String, category: String, description: String = "") {
        val newTest = TestDefinition(
            id = "custom_${UUID.randomUUID().toString().take(6)}",
            name = name,
            category = category,
            description = description
        )

        val currentUser = AppContainer.authRepository.currentUser.value
        val userId = currentUser?.id ?: "local_user_mode"

        if (userId.startsWith("local_")) {
            // Local Mode: Keep completely local and never send to Firestore
            val updatedList = _customTests.value + newTest
            _customTests.value = updatedList
            saveCustomTestsToPrefs(updatedList)
        } else {
            // Google Mode: Store in Firestore and let real-time listener update UI
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val testMap = mapOf(
                    "id" to newTest.id,
                    "name" to newTest.name,
                    "category" to newTest.category,
                    "normalRangeText" to newTest.normalRangeText,
                    "unit" to newTest.unit,
                    "description" to newTest.description
                )
                db.collection("users").document(userId).collection("custom_tests").document(newTest.id)
                    .set(testMap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        customTestsListener?.remove()
        customTestsListener = null
    }

    fun addTestResult(
        testName: String,
        resultValue: String,
        unit: String,
        normalRange: String,
        testDate: String,
        notes: String,
        testDefinitionId: String? = null,
        testAppointmentId: String? = null
    ) {
        val newResult = TestResult(
            id = "res_${UUID.randomUUID().toString().take(8)}",
            childId = childId,
            testName = testName,
            resultValue = resultValue,
            unit = unit,
            isNormal = true,
            normalRangeText = normalRange,
            testDateText = testDate,
            doctorNotes = notes,
            testDefinitionId = testDefinitionId,
            testAppointmentId = testAppointmentId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        healthRecordsRepository.addTestResult(newResult)

        // Mark the linked appointment as COMPLETED if it isn't already
        if (testAppointmentId != null) {
            val app = appointments.value.find { it.id == testAppointmentId }
            if (app != null && app.status != AppointmentStatus.COMPLETED) {
                appointmentRepository.updateAppointment(app.copy(status = AppointmentStatus.COMPLETED))
            }
        }
    }

    fun updateTestResult(
        id: String,
        testName: String,
        resultValue: String,
        unit: String,
        normalRange: String,
        testDate: String,
        notes: String,
        testDefinitionId: String? = null,
        testAppointmentId: String? = null
    ) {
        val existing = testResults.value.find { it.id == id } ?: return
        val updated = existing.copy(
            testName = testName,
            resultValue = resultValue,
            unit = unit,
            normalRangeText = normalRange,
            testDateText = testDate,
            doctorNotes = notes,
            testDefinitionId = testDefinitionId,
            testAppointmentId = testAppointmentId,
            updatedAt = System.currentTimeMillis()
        )
        healthRecordsRepository.updateTestResult(updated)
    }

    fun deleteTestResult(resultId: String) {
        healthRecordsRepository.deleteTestResult(resultId)
    }

    fun addGlucoseReading(value: Int, mealContext: MealContext, date: String, time: String, notes: String, customTime: Long = System.currentTimeMillis()) {
        val newGlucose = GlucoseReading(
            id = "g_${UUID.randomUUID().toString().take(8)}",
            childId = childId,
            readingValue = value,
            unit = "mg/dL",
            mealContext = mealContext,
            dateText = date,
            timeText = time,
            isTargetRange = value in 70..180,
            notes = notes,
            createdAt = customTime,
            updatedAt = customTime
        )
        healthRecordsRepository.addGlucoseReading(newGlucose)
    }

    fun updateGlucoseReading(id: String, value: Int, mealContext: MealContext, date: String, time: String, notes: String, customTime: Long) {
        val existing = glucoseReadings.value.find { it.id == id } ?: return
        val updated = existing.copy(
            readingValue = value,
            mealContext = mealContext,
            dateText = date,
            timeText = time,
            notes = notes,
            createdAt = customTime,
            updatedAt = System.currentTimeMillis()
        )
        healthRecordsRepository.updateGlucoseReading(updated)
    }

    fun deleteGlucoseReading(id: String) {
        healthRecordsRepository.deleteGlucoseReading(id)
    }
}
