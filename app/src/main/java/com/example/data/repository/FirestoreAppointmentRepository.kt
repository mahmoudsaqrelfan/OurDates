package com.example.data.repository

import android.content.Context
import com.example.MawaeednaApplication
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.AppointmentType
import com.example.data.model.NotificationReminder
import com.example.data.model.ReminderConfig
import com.example.data.model.ReminderType
import com.example.notifications.AppointmentNotificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class FirestoreAppointmentRepository : AppointmentRepository {

    private fun saveLocalAppointmentsToPrefs(appointments: List<Appointment>) {
        try {
            val context = MawaeednaApplication.appContext ?: return
            val prefs = context.getSharedPreferences("mawaeedna_local_app_prefs", Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            for (app in appointments) {
                val obj = JSONObject()
                obj.put("id", app.id)
                obj.put("childId", app.childId)
                obj.put("familyId", app.familyId)
                obj.put("type", app.type.name)
                obj.put("title", app.title)
                obj.put("doctorSpecialty", app.doctorSpecialty)
                obj.put("doctorName", app.doctorName)
                obj.put("clinicName", app.clinicName)
                obj.put("testDefinitionId", app.testDefinitionId)
                obj.put("dateTimestamp", app.dateTimestamp)
                obj.put("dateText", app.dateText)
                obj.put("timeText", app.timeText)
                obj.put("status", app.status.name)
                obj.put("notes", app.notes)
                obj.put("linkedAppointmentId", app.linkedAppointmentId ?: "")
                obj.put("linkedAppointmentTitle", app.linkedAppointmentTitle ?: "")
                obj.put("createdAt", app.createdAt)
                obj.put("updatedAt", app.updatedAt)

                val remArray = JSONArray()
                for (rem in app.reminders) {
                    val remObj = JSONObject()
                    remObj.put("id", rem.id)
                    remObj.put("type", rem.type.name)
                    remObj.put("customDaysBefore", rem.customDaysBefore)
                    remObj.put("timeHour", rem.timeHour)
                    remObj.put("timeMinute", rem.timeMinute)
                    remObj.put("labelAr", rem.labelAr)
                    remArray.put(remObj)
                }
                obj.put("reminders", remArray)
                jsonArray.put(obj)
            }
            prefs.edit().putString("appointments_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLocalAppointmentsFromPrefs(): List<Appointment> {
        return try {
            val context = MawaeednaApplication.appContext ?: return emptyList()
            val prefs = context.getSharedPreferences("mawaeedna_local_app_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("appointments_json", null) ?: return emptyList()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Appointment>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeStr = obj.optString("type", "DOCTOR_VISIT")
                val type = try { AppointmentType.valueOf(typeStr) } catch (e: Exception) { AppointmentType.DOCTOR_VISIT }
                val statusStr = obj.optString("status", "UPCOMING")
                val status = try { AppointmentStatus.valueOf(statusStr) } catch (e: Exception) { AppointmentStatus.UPCOMING }

                val remArray = obj.optJSONArray("reminders")
                val reminders = mutableListOf<ReminderConfig>()
                if (remArray != null) {
                    for (j in 0 until remArray.length()) {
                        val remObj = remArray.getJSONObject(j)
                        val remTypeStr = remObj.optString("type", "SAME_DAY")
                        val remType = try { ReminderType.valueOf(remTypeStr) } catch (e: Exception) { ReminderType.SAME_DAY }
                        reminders.add(
                            ReminderConfig(
                                id = remObj.optString("id", java.util.UUID.randomUUID().toString()),
                                type = remType,
                                customDaysBefore = remObj.optInt("customDaysBefore", 0),
                                timeHour = remObj.optInt("timeHour", 9),
                                timeMinute = remObj.optInt("timeMinute", 0),
                                labelAr = remObj.optString("labelAr", "تذكير")
                            )
                        )
                    }
                }

                list.add(
                    Appointment(
                        id = obj.getString("id"),
                        childId = obj.optString("childId", ""),
                        familyId = obj.optString("familyId", ""),
                        type = type,
                        title = obj.optString("title", ""),
                        doctorSpecialty = obj.optString("doctorSpecialty", ""),
                        doctorName = obj.optString("doctorName", ""),
                        clinicName = obj.optString("clinicName", ""),
                        testDefinitionId = obj.optString("testDefinitionId", ""),
                        dateTimestamp = obj.optLong("dateTimestamp", System.currentTimeMillis()),
                        dateText = obj.optString("dateText", ""),
                        timeText = obj.optString("timeText", ""),
                        status = status,
                        notes = obj.optString("notes", ""),
                        reminders = reminders,
                        linkedAppointmentId = obj.optString("linkedAppointmentId").ifBlank { null },
                        linkedAppointmentTitle = obj.optString("linkedAppointmentTitle").ifBlank { null },
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

    private val _appointmentsState = MutableStateFlow<List<Appointment>>(emptyList())
    private val _remindersState = MutableStateFlow<List<NotificationReminder>>(emptyList())

    private var appointmentsListener: ListenerRegistration? = null
    private var activeUserId: String? = null

    override fun getUpcomingAppointments(): Flow<List<Appointment>> = _appointmentsState.map { list ->
        list.filter { it.status == AppointmentStatus.UPCOMING }.sortedBy { it.dateTimestamp }
    }

    override fun getAppointmentsForChild(childId: String): Flow<List<Appointment>> = _appointmentsState.map { list ->
        list.filter { it.childId == childId }.sortedBy { it.dateTimestamp }
    }

    override fun getAllAppointments(): Flow<List<Appointment>> = _appointmentsState.asStateFlow()

    override fun getFollowUpReminders(): Flow<List<NotificationReminder>> = _remindersState.asStateFlow()

    override fun attachUser(userId: String) {
        if (activeUserId == userId && (userId.startsWith("local_") || appointmentsListener != null)) return
        clearUser()
        activeUserId = userId

        if (userId.startsWith("local_")) {
            val list = loadLocalAppointmentsFromPrefs()
            _appointmentsState.value = list
            try {
                MawaeednaApplication.appContext?.let { ctx ->
                    AppointmentNotificationManager.syncAllLocalNotifications(ctx, emptyMap(), list)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        val database = getFirestoreInstance() ?: return
        val appColl = database.collection("users").document(userId).collection("appointments")

        appointmentsListener = appColl.addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val childId = doc.getString("childId") ?: ""
                        val familyId = doc.getString("familyId") ?: ""
                        val typeStr = doc.getString("type") ?: "DOCTOR_VISIT"
                        val type = try { AppointmentType.valueOf(typeStr) } catch (e: Exception) { AppointmentType.DOCTOR_VISIT }
                        val title = doc.getString("title") ?: ""
                        val doctorSpecialty = doc.getString("doctorSpecialty") ?: ""
                        val doctorName = doc.getString("doctorName") ?: ""
                        val clinicName = doc.getString("clinicName") ?: ""
                        val testDefinitionId = doc.getString("testDefinitionId") ?: ""
                        val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                        val dateText = doc.getString("dateText") ?: ""
                        val timeText = doc.getString("timeText") ?: ""
                        val statusStr = doc.getString("status") ?: "UPCOMING"
                        val status = try { AppointmentStatus.valueOf(statusStr) } catch (e: Exception) { AppointmentStatus.UPCOMING }
                        val notes = doc.getString("notes") ?: ""
                        val linkedAppointmentId = doc.getString("linkedAppointmentId")
                        val linkedAppointmentTitle = doc.getString("linkedAppointmentTitle")
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                        val remindersListRaw = doc.get("reminders") as? List<Map<String, Any>> ?: emptyList()
                        val reminders = remindersListRaw.mapNotNull { remMap ->
                            try {
                                val remId = remMap["id"] as? String ?: java.util.UUID.randomUUID().toString()
                                val remTypeStr = remMap["type"] as? String ?: "SAME_DAY"
                                val remType = try { ReminderType.valueOf(remTypeStr) } catch (e: Exception) { ReminderType.SAME_DAY }
                                val customDaysBefore = (remMap["customDaysBefore"] as? Long)?.toInt() ?: 0
                                val timeHour = (remMap["timeHour"] as? Long)?.toInt() ?: 9
                                val timeMinute = (remMap["timeMinute"] as? Long)?.toInt() ?: 0
                                val labelAr = remMap["labelAr"] as? String ?: "تذكير"

                                ReminderConfig(
                                    id = remId,
                                    type = remType,
                                    customDaysBefore = customDaysBefore,
                                    timeHour = timeHour,
                                    timeMinute = timeMinute,
                                    labelAr = labelAr
                                )
                            } catch (e: Exception) { null }
                        }

                        Appointment(
                            id = id,
                            childId = childId,
                            familyId = familyId,
                            type = type,
                            title = title,
                            doctorSpecialty = doctorSpecialty,
                            doctorName = doctorName,
                            clinicName = clinicName,
                            testDefinitionId = testDefinitionId,
                            dateTimestamp = dateTimestamp,
                            dateText = dateText,
                            timeText = timeText,
                            status = status,
                            notes = notes,
                            reminders = reminders,
                            linkedAppointmentId = linkedAppointmentId,
                            linkedAppointmentTitle = linkedAppointmentTitle,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                _appointmentsState.value = list

                // Sync local notifications on this device
                try {
                    MawaeednaApplication.appContext?.let { ctx ->
                        AppointmentNotificationManager.syncAllLocalNotifications(ctx, emptyMap(), list)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun clearUser() {
        appointmentsListener?.remove()
        appointmentsListener = null
        activeUserId = null
        _appointmentsState.value = emptyList()
    }

    override fun addAppointment(appointment: Appointment) {
        val currentUserId = activeUserId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return

        val now = System.currentTimeMillis()
        val appToSave = if (appointment.id.isBlank()) {
            appointment.copy(id = "app_${java.util.UUID.randomUUID().toString().take(8)}", createdAt = now, updatedAt = now)
        } else {
            appointment.copy(updatedAt = now)
        }

        if (currentUserId.startsWith("local_")) {
            val newList = (_appointmentsState.value.filterNot { it.id == appToSave.id } + appToSave).sortedBy { it.dateTimestamp }
            _appointmentsState.value = newList
            saveLocalAppointmentsToPrefs(newList)
            try {
                MawaeednaApplication.appContext?.let { context ->
                    AppointmentNotificationManager.scheduleLocalNotificationsForAppointment(
                        context,
                        "الطفل",
                        appToSave
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        val database = getFirestoreInstance() ?: return

        val docRef = database.collection("users")
            .document(currentUserId)
            .collection("appointments")
            .document(appToSave.id)

        docRef.set(mapAppointmentToDoc(appToSave), SetOptions.merge())

        // Schedule local notification on this device immediately
        try {
            MawaeednaApplication.appContext?.let { context ->
                AppointmentNotificationManager.scheduleLocalNotificationsForAppointment(
                    context,
                    "الطفل",
                    appToSave
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun updateAppointment(appointment: Appointment) {
        val currentUserId = activeUserId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        val updated = appointment.copy(updatedAt = System.currentTimeMillis())

        if (currentUserId.startsWith("local_")) {
            val newList = _appointmentsState.value.map {
                if (it.id == updated.id) updated else it
            }.sortedBy { it.dateTimestamp }
            _appointmentsState.value = newList
            saveLocalAppointmentsToPrefs(newList)
            try {
                MawaeednaApplication.appContext?.let { context ->
                    AppointmentNotificationManager.scheduleLocalNotificationsForAppointment(
                        context,
                        "الطفل",
                        updated
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        val database = getFirestoreInstance() ?: return

        val docRef = database.collection("users")
            .document(currentUserId)
            .collection("appointments")
            .document(updated.id)

        docRef.set(mapAppointmentToDoc(updated), SetOptions.merge())

        // Reschedule local notification
        try {
            MawaeednaApplication.appContext?.let { context ->
                AppointmentNotificationManager.scheduleLocalNotificationsForAppointment(
                    context,
                    "الطفل",
                    updated
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deleteAppointment(appointmentId: String) {
        val currentUserId = activeUserId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return

        val existing = _appointmentsState.value.find { it.id == appointmentId }
        if (existing != null) {
            try {
                MawaeednaApplication.appContext?.let { context ->
                    AppointmentNotificationManager.cancelLocalNotificationsForAppointment(
                        context,
                        appointmentId,
                        existing.reminders
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (currentUserId.startsWith("local_")) {
            val newList = _appointmentsState.value.filterNot { it.id == appointmentId }
            _appointmentsState.value = newList
            saveLocalAppointmentsToPrefs(newList)
            return
        }

        val database = getFirestoreInstance() ?: return

        database.collection("users")
            .document(currentUserId)
            .collection("appointments")
            .document(appointmentId)
            .delete()
    }

    private fun mapAppointmentToDoc(app: Appointment): Map<String, Any?> {
        val remindersMapList = app.reminders.map { rem ->
            mapOf(
                "id" to rem.id,
                "type" to rem.type.name,
                "customDaysBefore" to rem.customDaysBefore,
                "timeHour" to rem.timeHour,
                "timeMinute" to rem.timeMinute,
                "labelAr" to rem.labelAr
            )
        }

        return mapOf(
            "id" to app.id,
            "childId" to app.childId,
            "familyId" to app.familyId,
            "type" to app.type.name,
            "title" to app.title,
            "doctorSpecialty" to app.doctorSpecialty,
            "doctorName" to app.doctorName,
            "clinicName" to app.clinicName,
            "testDefinitionId" to app.testDefinitionId,
            "dateTimestamp" to app.dateTimestamp,
            "dateText" to app.dateText,
            "timeText" to app.timeText,
            "status" to app.status.name,
            "notes" to app.notes,
            "reminders" to remindersMapList,
            "linkedAppointmentId" to app.linkedAppointmentId,
            "linkedAppointmentTitle" to app.linkedAppointmentTitle,
            "createdAt" to app.createdAt,
            "updatedAt" to app.updatedAt
        )
    }

    override fun restoreAppointments(appointments: List<Appointment>) {
        _appointmentsState.value = appointments
        saveLocalAppointmentsToPrefs(appointments)
        try {
            MawaeednaApplication.appContext?.let { ctx ->
                AppointmentNotificationManager.syncAllLocalNotifications(ctx, emptyMap(), appointments)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
