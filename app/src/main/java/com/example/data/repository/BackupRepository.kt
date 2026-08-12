package com.example.data.repository

import android.content.Context
import com.example.MawaeednaApplication
import com.example.data.model.*
import com.example.di.AppContainer
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface BackupRepository {
    suspend fun createBackupJson(): String
    fun validateBackupJson(jsonString: String): Boolean
    suspend fun restoreBackup(jsonString: String): Boolean
}

class LocalBackupRepository(
    private val familyRepository: FamilyRepository = AppContainer.familyRepository,
    private val appointmentRepository: AppointmentRepository = AppContainer.appointmentRepository,
    private val healthRecordsRepository: HealthRecordsRepository = AppContainer.healthRecordsRepository,
    private val settingsRepository: SettingsRepository = AppContainer.settingsRepository
) : BackupRepository {

    override suspend fun createBackupJson(): String {
        val root = JSONObject()
        root.put("backupVersion", 1)
        root.put("createdAt", System.currentTimeMillis())
        root.put("appVersion", "1.0")

        // 1. Family
        val family = familyRepository.getFamily().first()
        val famObj = JSONObject().apply {
            put("id", family.id)
            put("ownerUserId", family.ownerUserId)
            put("familyName", family.familyName)
            put("createdAt", family.createdAt)
            put("updatedAt", family.updatedAt)
        }
        root.put("family", famObj)

        // 2. Children
        val children = familyRepository.getChildren().first()
        val childrenArr = JSONArray()
        for (child in children) {
            val childObj = JSONObject().apply {
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
            }
            childrenArr.put(childObj)
        }
        root.put("children", childrenArr)

        // 3. Appointments
        val appointments = appointmentRepository.getAllAppointments().first()
        val appArr = JSONArray()
        for (app in appointments) {
            val appObj = JSONObject().apply {
                put("id", app.id)
                put("childId", app.childId)
                put("familyId", app.familyId)
                put("type", app.type.name)
                put("title", app.title)
                put("doctorSpecialty", app.doctorSpecialty)
                put("doctorName", app.doctorName)
                put("clinicName", app.clinicName)
                put("testDefinitionId", app.testDefinitionId)
                put("dateTimestamp", app.dateTimestamp)
                put("dateText", app.dateText)
                put("timeText", app.timeText)
                put("status", app.status.name)
                put("notes", app.notes)
                put("linkedAppointmentId", app.linkedAppointmentId ?: "")
                put("linkedAppointmentTitle", app.linkedAppointmentTitle ?: "")
                put("createdAt", app.createdAt)
                put("updatedAt", app.updatedAt)

                val remArr = JSONArray()
                for (rem in app.reminders) {
                    val remObj = JSONObject().apply {
                        put("id", rem.id)
                        put("type", rem.type.name)
                        put("customDaysBefore", rem.customDaysBefore)
                        put("timeHour", rem.timeHour)
                        put("timeMinute", rem.timeMinute)
                        put("labelAr", rem.labelAr)
                    }
                    remArr.put(remObj)
                }
                put("reminders", remArr)
            }
            appArr.put(appObj)
        }
        root.put("appointments", appArr)

        // 4. Custom Tests from SharedPreferences
        val context = MawaeednaApplication.appContext
        val customTestsArr = JSONArray()
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences("mawaeedna_custom_tests_prefs", Context.MODE_PRIVATE)
                val customTestsStr = prefs.getString("custom_tests", null)
                if (customTestsStr != null) {
                    val customTestsJson = JSONArray(customTestsStr)
                    for (i in 0 until customTestsJson.length()) {
                        customTestsArr.put(customTestsJson.getJSONObject(i))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        root.put("customTests", customTestsArr)

        // 5. Test Results
        val testResults = healthRecordsRepository.getAllTestResults().first()
        val resultsArr = JSONArray()
        for (res in testResults) {
            val resObj = JSONObject().apply {
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
            }
            resultsArr.put(resObj)
        }
        root.put("testResults", resultsArr)

        // 6. Glucose Readings
        val glucoseReadings = healthRecordsRepository.getAllGlucoseReadings().first()
        val glucoseArr = JSONArray()
        for (g in glucoseReadings) {
            val gObj = JSONObject().apply {
                put("id", g.id)
                put("childId", g.childId)
                put("readingValue", g.readingValue)
                put("unit", g.unit)
                put("mealContext", g.mealContext.name)
                put("dateText", g.dateText)
                put("timeText", g.timeText)
                put("isTargetRange", g.isTargetRange)
                put("notes", g.notes)
                put("createdAt", g.createdAt)
                put("updatedAt", g.updatedAt)
            }
            glucoseArr.put(gObj)
        }
        root.put("glucoseReadings", glucoseArr)

        // 7. Settings
        val settings = settingsRepository.getAppSettings().first()
        val settingsObj = JSONObject().apply {
            put("language", settings.language)
            put("themeMode", settings.themeMode)
            put("syncEnabled", settings.syncEnabled)
            put("notificationsEnabled", settings.notificationsEnabled)
            put("backupEnabled", settings.backupEnabled)
        }
        root.put("settings", settingsObj)

        return root.toString(2)
    }

    override fun validateBackupJson(jsonString: String): Boolean {
        if (jsonString.isBlank()) return false
        return try {
            val root = JSONObject(jsonString)
            // Verify mandatory headers
            val version = root.optInt("backupVersion", -1)
            if (version < 1) return false

            // Verify basic entities exist
            if (!root.has("family") || !root.has("children")) return false

            val familyObj = root.getJSONObject("family")
            if (!familyObj.has("id") || !familyObj.has("familyName")) return false

            val childrenArr = root.getJSONArray("children")
            // Basic structure check passed
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun restoreBackup(jsonString: String): Boolean {
        if (!validateBackupJson(jsonString)) return false
        return try {
            val root = JSONObject(jsonString)

            // 1. Parse Family
            val familyObj = root.getJSONObject("family")
            val family = Family(
                id = familyObj.getString("id"),
                ownerUserId = familyObj.getString("ownerUserId"),
                familyName = familyObj.getString("familyName"),
                createdAt = familyObj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = familyObj.optLong("updatedAt", System.currentTimeMillis())
            )

            // 2. Parse Children
            val childrenArr = root.getJSONArray("children")
            val childrenList = mutableListOf<Child>()
            for (i in 0 until childrenArr.length()) {
                val cObj = childrenArr.getJSONObject(i)
                val genderStr = cObj.optString("gender", "BOY")
                val gender = try { Gender.valueOf(genderStr) } catch (e: Exception) { Gender.BOY }
                childrenList.add(
                    Child(
                        id = cObj.getString("id"),
                        familyId = cObj.optString("familyId", family.id),
                        name = cObj.getString("name"),
                        birthDate = cObj.optString("birthDate", ""),
                        ageText = cObj.optString("ageText", ""),
                        gender = gender,
                        avatarColorHex = cObj.optString("avatarColorHex", "#00A896"),
                        notes = cObj.optString("notes", ""),
                        createdAt = cObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = cObj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 3. Parse Appointments
            val appointmentsArr = root.getJSONArray("appointments")
            val appointmentsList = mutableListOf<Appointment>()
            for (i in 0 until appointmentsArr.length()) {
                val aObj = appointmentsArr.getJSONObject(i)
                val typeStr = aObj.optString("type", "DOCTOR_VISIT")
                val type = try { AppointmentType.valueOf(typeStr) } catch (e: Exception) { AppointmentType.DOCTOR_VISIT }
                val statusStr = aObj.optString("status", "UPCOMING")
                val status = try { AppointmentStatus.valueOf(statusStr) } catch (e: Exception) { AppointmentStatus.UPCOMING }

                val remArr = aObj.optJSONArray("reminders")
                val remindersList = mutableListOf<ReminderConfig>()
                if (remArr != null) {
                    for (j in 0 until remArr.length()) {
                        val rObj = remArr.getJSONObject(j)
                        val rTypeStr = rObj.optString("type", "SAME_DAY")
                        val rType = try { ReminderType.valueOf(rTypeStr) } catch (e: Exception) { ReminderType.SAME_DAY }
                        remindersList.add(
                            ReminderConfig(
                                id = rObj.optString("id", UUID.randomUUID().toString()),
                                type = rType,
                                customDaysBefore = rObj.optInt("customDaysBefore", 0),
                                timeHour = rObj.optInt("timeHour", 9),
                                timeMinute = rObj.optInt("timeMinute", 0),
                                labelAr = rObj.optString("labelAr", "تذكير")
                            )
                        )
                    }
                }

                appointmentsList.add(
                    Appointment(
                        id = aObj.getString("id"),
                        childId = aObj.getString("childId"),
                        familyId = aObj.optString("familyId", family.id),
                        type = type,
                        title = aObj.getString("title"),
                        doctorSpecialty = aObj.optString("doctorSpecialty", ""),
                        doctorName = aObj.optString("doctorName", ""),
                        clinicName = aObj.optString("clinicName", ""),
                        testDefinitionId = aObj.optString("testDefinitionId", ""),
                        dateTimestamp = aObj.optLong("dateTimestamp", System.currentTimeMillis()),
                        dateText = aObj.optString("dateText", ""),
                        timeText = aObj.optString("timeText", ""),
                        status = status,
                        notes = aObj.optString("notes", ""),
                        reminders = remindersList,
                        linkedAppointmentId = aObj.optString("linkedAppointmentId").ifBlank { null },
                        linkedAppointmentTitle = aObj.optString("linkedAppointmentTitle").ifBlank { null },
                        createdAt = aObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = aObj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 4. Parse Custom Tests
            val customTestsArr = root.optJSONArray("customTests")
            if (customTestsArr != null) {
                val context = MawaeednaApplication.appContext
                if (context != null) {
                    val prefs = context.getSharedPreferences("mawaeedna_custom_tests_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("custom_tests", customTestsArr.toString()).apply()
                }
            }

            // 5. Parse Test Results
            val resultsArr = root.getJSONArray("testResults")
            val resultsList = mutableListOf<TestResult>()
            for (i in 0 until resultsArr.length()) {
                val rObj = resultsArr.getJSONObject(i)
                resultsList.add(
                    TestResult(
                        id = rObj.getString("id"),
                        childId = rObj.getString("childId"),
                        testName = rObj.getString("testName"),
                        resultValue = rObj.getString("resultValue"),
                        unit = rObj.optString("unit", ""),
                        isNormal = rObj.optBoolean("isNormal", true),
                        normalRangeText = rObj.optString("normalRangeText", ""),
                        testDateText = rObj.getString("testDateText"),
                        doctorNotes = rObj.optString("doctorNotes", ""),
                        testDefinitionId = rObj.optString("testDefinitionId").ifBlank { null },
                        testAppointmentId = rObj.optString("testAppointmentId").ifBlank { null },
                        createdAt = rObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = rObj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 6. Parse Glucose Readings
            val glucoseArr = root.getJSONArray("glucoseReadings")
            val glucoseList = mutableListOf<GlucoseReading>()
            for (i in 0 until glucoseArr.length()) {
                val gObj = glucoseArr.getJSONObject(i)
                val mealStr = gObj.optString("mealContext", "RANDOM")
                val mealContext = try { MealContext.valueOf(mealStr) } catch (e: Exception) { MealContext.RANDOM }
                glucoseList.add(
                    GlucoseReading(
                        id = gObj.getString("id"),
                        childId = gObj.getString("childId"),
                        readingValue = gObj.getInt("readingValue"),
                        unit = gObj.optString("unit", "mg/dL"),
                        mealContext = mealContext,
                        dateText = gObj.getString("dateText"),
                        timeText = gObj.getString("timeText"),
                        isTargetRange = gObj.optBoolean("isTargetRange", true),
                        notes = gObj.optString("notes", ""),
                        createdAt = gObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = gObj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 7. Parse Settings
            val settingsObj = root.optJSONObject("settings")
            if (settingsObj != null) {
                val settings = AppSettings(
                    language = settingsObj.optString("language", "العربية"),
                    themeMode = settingsObj.optString("themeMode", "فاتح (طبي هادئ)"),
                    syncEnabled = settingsObj.optBoolean("syncEnabled", false),
                    notificationsEnabled = settingsObj.optBoolean("notificationsEnabled", true),
                    backupEnabled = settingsObj.optBoolean("backupEnabled", false)
                )
                settingsRepository.updateSettings(settings)
            }

            // Overwrite all repositories
            familyRepository.restoreFamilyAndChildren(family, childrenList)
            appointmentRepository.restoreAppointments(appointmentsList)
            healthRecordsRepository.restoreHealthRecords(resultsList, glucoseList)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
