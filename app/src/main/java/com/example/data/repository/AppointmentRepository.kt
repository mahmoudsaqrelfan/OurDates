package com.example.data.repository

import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.NotificationReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

interface AppointmentRepository {
    fun getUpcomingAppointments(): Flow<List<Appointment>>
    fun getAppointmentsForChild(childId: String): Flow<List<Appointment>>
    fun getAllAppointments(): Flow<List<Appointment>>
    fun addAppointment(appointment: Appointment)
    fun updateAppointment(appointment: Appointment)
    fun deleteAppointment(appointmentId: String)
    fun getFollowUpReminders(): Flow<List<NotificationReminder>>
    fun attachUser(userId: String)
    fun clearUser()
    fun restoreAppointments(appointments: List<Appointment>)
}

class InMemoryAppointmentRepository : AppointmentRepository {
    private val appointmentsState = MutableStateFlow<List<Appointment>>(emptyList())
    private val remindersState = MutableStateFlow<List<NotificationReminder>>(emptyList())

    override fun getUpcomingAppointments(): Flow<List<Appointment>> = appointmentsState.map { list ->
        list.filter { it.status == AppointmentStatus.UPCOMING }.sortedBy { it.dateTimestamp }
    }

    override fun getAppointmentsForChild(childId: String): Flow<List<Appointment>> = appointmentsState.map { list ->
        list.filter { it.childId == childId }.sortedBy { it.dateTimestamp }
    }

    override fun getAllAppointments(): Flow<List<Appointment>> = appointmentsState.asStateFlow()

    override fun restoreAppointments(appointments: List<Appointment>) {
        appointmentsState.value = appointments
    }

    override fun addAppointment(appointment: Appointment) {
        appointmentsState.value = appointmentsState.value + appointment
    }

    override fun updateAppointment(appointment: Appointment) {
        appointmentsState.value = appointmentsState.value.map {
            if (it.id == appointment.id) appointment else it
        }
    }

    override fun deleteAppointment(appointmentId: String) {
        appointmentsState.value = appointmentsState.value.filter { it.id != appointmentId }
    }

    override fun getFollowUpReminders(): Flow<List<NotificationReminder>> = remindersState.asStateFlow()

    override fun attachUser(userId: String) {}
    override fun clearUser() {
        appointmentsState.value = emptyList()
    }
}
