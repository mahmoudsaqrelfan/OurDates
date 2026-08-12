package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.Appointment
import com.example.data.model.AppointmentType
import com.example.data.model.AppointmentStatus
import com.example.data.model.ReminderConfig
import com.example.data.model.ReminderType
import org.json.JSONArray
import org.json.JSONObject

class AppointmentNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAlarms(context)
            return
        }

        val childName = intent.getStringExtra(EXTRA_CHILD_NAME) ?: "الطفل"
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "موعد قادم"
        val reminderLabel = intent.getStringExtra(EXTRA_REMINDER_LABEL) ?: "تذكير"
        val appointmentId = intent.getStringExtra(EXTRA_APPOINTMENT_ID) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Create Channel if Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكيرات المواعيد",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات تذكير لمواعيد الأطفال والأطباء"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Check permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Tap intent to launch MainActivity
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("appointmentId", appointmentId)
        }
        val pendingTapIntent = PendingIntent.getActivity(
            context,
            appointmentId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = "📅 موعد قادم لـ $childName"
        val notificationText = "$title ($reminderLabel)"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingTapIntent)
            .build()

        val notificationId = (appointmentId.hashCode() * 31 + reminderLabel.hashCode()).let { Math.abs(it) }
        notificationManager.notify(notificationId, notification)
    }

    private fun rescheduleAlarms(context: Context) {
        try {
            val childrenMap = mutableMapOf<String, String>()
            val familyPrefs = context.getSharedPreferences("mawaeedna_local_family_prefs", Context.MODE_PRIVATE)
            val childrenJson = familyPrefs.getString("children_json", null)
            if (childrenJson != null) {
                val array = JSONArray(childrenJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id")
                    val name = obj.optString("name")
                    if (id.isNotBlank() && name.isNotBlank()) {
                        childrenMap[id] = name
                    }
                }
            }

            val appPrefs = context.getSharedPreferences("mawaeedna_local_app_prefs", Context.MODE_PRIVATE)
            val appJson = appPrefs.getString("appointments_json", null)
            if (appJson != null) {
                val array = JSONArray(appJson)
                val appointmentsList = mutableListOf<Appointment>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id")
                    val childId = obj.optString("childId", "")
                    val familyId = obj.optString("familyId", "")
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

                    appointmentsList.add(
                        Appointment(
                            id = id,
                            childId = childId,
                            familyId = familyId,
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

                AppointmentNotificationManager.syncAllLocalNotifications(context, childrenMap, appointmentsList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val CHANNEL_ID = "appointment_reminders_channel"
        const val EXTRA_APPOINTMENT_ID = "extra_appointment_id"
        const val EXTRA_CHILD_NAME = "extra_child_name"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_REMINDER_LABEL = "extra_reminder_label"
    }
}
