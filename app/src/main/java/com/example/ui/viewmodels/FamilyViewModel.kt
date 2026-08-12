package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.Child
import com.example.data.model.Family
import com.example.data.model.Gender
import com.example.data.model.NotificationReminder
import com.example.data.model.SyncStatus
import com.example.data.repository.AppointmentRepository
import com.example.data.repository.FamilyRepository
import com.example.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

class FamilyViewModel(
    private val familyRepository: FamilyRepository = AppContainer.familyRepository,
    private val appointmentRepository: AppointmentRepository = AppContainer.appointmentRepository
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = familyRepository.syncStatus

    val family: StateFlow<Family> = familyRepository.getFamily().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Family()
    )

    val children: StateFlow<List<Child>> = familyRepository.getChildren().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val upcomingAppointments: StateFlow<List<Appointment>> = appointmentRepository.getUpcomingAppointments().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val followUpReminders: StateFlow<List<NotificationReminder>> = appointmentRepository.getFollowUpReminders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addChild(name: String, birthDate: String, ageText: String, gender: Gender) {
        val newChild = Child(
            id = "child_${UUID.randomUUID().toString().take(6)}",
            name = name,
            birthDate = birthDate,
            ageText = ageText,
            gender = gender,
            avatarColorHex = if (gender == Gender.BOY) "#00A896" else "#EC407A"
        )
        familyRepository.addChild(newChild)
    }

    fun addUpcomingAppointment(
        childId: String,
        title: String,
        doctorName: String,
        clinicName: String,
        dateText: String,
        timeText: String
    ) {
        val newApp = Appointment(
            id = "app_${UUID.randomUUID().toString().take(6)}",
            childId = childId,
            title = title,
            doctorName = doctorName,
            clinicName = clinicName,
            dateText = dateText,
            timeText = timeText,
            status = AppointmentStatus.UPCOMING
        )
        appointmentRepository.addAppointment(newApp)
    }
}
