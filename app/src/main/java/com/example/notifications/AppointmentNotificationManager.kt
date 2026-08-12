package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.ReminderConfig
import com.example.data.model.ReminderType
import java.util.Calendar
import kotlin.math.abs

object AppointmentNotificationManager {

    private fun getRequestCode(appointmentId: String, reminderId: String): Int {
        return abs((appointmentId.hashCode() * 31) + reminderId.hashCode())
    }

    fun scheduleLocalNotificationsForAppointment(
        context: Context,
        childName: String,
        appointment: Appointment
    ) {
        if (appointment.status != AppointmentStatus.UPCOMING) {
            cancelLocalNotificationsForAppointment(context, appointment.id, appointment.reminders)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for (reminder in appointment.reminders) {
            if (reminder.type == ReminderType.NONE) continue

            val triggerMillis = calculateTriggerMillis(appointment.dateTimestamp, reminder)
            if (triggerMillis <= System.currentTimeMillis()) {
                // Time has passed
                continue
            }

            val requestCode = getRequestCode(appointment.id, reminder.id)
            val intent = Intent(context, AppointmentNotificationReceiver::class.java).apply {
                putExtra(AppointmentNotificationReceiver.EXTRA_APPOINTMENT_ID, appointment.id)
                putExtra(AppointmentNotificationReceiver.EXTRA_CHILD_NAME, childName)
                putExtra(AppointmentNotificationReceiver.EXTRA_TITLE, appointment.title.ifBlank { appointment.doctorSpecialty })
                putExtra(AppointmentNotificationReceiver.EXTRA_REMINDER_LABEL, reminder.labelAr)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } catch (e: Exception) {
                try {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
    }

    fun cancelLocalNotificationsForAppointment(
        context: Context,
        appointmentId: String,
        reminders: List<ReminderConfig>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for (reminder in reminders) {
            val requestCode = getRequestCode(appointmentId, reminder.id)
            val intent = Intent(context, AppointmentNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun syncAllLocalNotifications(
        context: Context,
        childrenNamesMap: Map<String, String>,
        appointments: List<Appointment>
    ) {
        for (appointment in appointments) {
            val childName = childrenNamesMap[appointment.childId] ?: "الطفل"
            scheduleLocalNotificationsForAppointment(context, childName, appointment)
        }
    }

    private fun calculateTriggerMillis(appointmentDateTimestamp: Long, reminder: ReminderConfig): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = appointmentDateTimestamp

        val daysToSubtract = when (reminder.type) {
            ReminderType.NONE -> return -1L
            ReminderType.SAME_DAY -> 0
            ReminderType.DAY_1_BEFORE -> 1
            ReminderType.DAYS_3_BEFORE -> 3
            ReminderType.DAYS_7_BEFORE -> 7
            ReminderType.CUSTOM -> reminder.customDaysBefore
        }

        calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        calendar.set(Calendar.HOUR_OF_DAY, reminder.timeHour)
        calendar.set(Calendar.MINUTE, reminder.timeMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }
}
