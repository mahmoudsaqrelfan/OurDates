package com.example.ui.screens.child.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.material.icons.filled.CalendarToday
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.AppointmentType
import com.example.data.model.ReminderConfig
import com.example.data.model.ReminderType
import com.example.ui.theme.PastelCyanCard
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodels.ChildDetailViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsSectionScreen(
    viewModel: ChildDetailViewModel,
    onBackClick: () -> Unit
) {
    val child by viewModel.child.collectAsState()
    val appointments by viewModel.appointments.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Upcoming, 1: Past
    var showAddDialog by remember { mutableStateOf(false) }
    var appointmentToEdit by remember { mutableStateOf<Appointment?>(null) }
    var appointmentToDelete by remember { mutableStateOf<Appointment?>(null) }

    val upcomingList = remember(appointments) {
        appointments.filter { it.status == AppointmentStatus.UPCOMING }.sortedBy { it.dateTimestamp }
    }
    val pastList = remember(appointments) {
        appointments.filter { it.status != AppointmentStatus.UPCOMING }.sortedByDescending { it.dateTimestamp }
    }

    if (showAddDialog || appointmentToEdit != null) {
        AddOrEditAppointmentDialog(
            initialAppointment = appointmentToEdit,
            existingAppointments = appointments,
            onDismiss = {
                showAddDialog = false
                appointmentToEdit = null
            },
            onSave = { app ->
                viewModel.saveAppointment(app)
                showAddDialog = false
                appointmentToEdit = null
            }
        )
    }

    if (appointmentToDelete != null) {
        AlertDialog(
            onDismissRequest = { appointmentToDelete = null },
            title = { Text("حذف الموعد", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد حذف هذا الموعد؟ سيتم إلغاء التذكيرات المرتبطة به أيضاً.") },
            confirmButton = {
                Button(
                    onClick = {
                        appointmentToDelete?.id?.let { id ->
                            viewModel.deleteAppointment(id)
                        }
                        appointmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { appointmentToDelete = null }) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("appointments_section_screen"),
        containerColor = Color(0xFFF7FBFB),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    appointmentToEdit = null
                    showAddDialog = true
                },
                containerColor = TealPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_appointment_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة موعد")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TealDark)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "📅 المواعيد والتذكيرات",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TealDark
                        )
                    )
                    Text(
                        text = child?.name ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                    )
                }
            }

            // Tabs for Upcoming / Past
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = TealPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = TealPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "المواعيد القادمة (${upcomingList.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "المواعيد السابقة (${pastList.size})",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentList = if (selectedTabIndex == 0) upcomingList else pastList

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTabIndex == 0)
                            "لا توجد مواعيد قادمة مسجلة.\nانقر على زر (+) لإضافة موعد جديد."
                        else
                            "لا توجد مواعيد سابقة مسجلة.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8)),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentList, key = { it.id }) { app ->
                        AppointmentItemCard(
                            appointment = app,
                            onToggleStatus = {
                                val newStatus = if (app.status == AppointmentStatus.UPCOMING)
                                    AppointmentStatus.COMPLETED
                                else
                                    AppointmentStatus.UPCOMING
                                viewModel.saveAppointment(app.copy(status = newStatus))
                            },
                            onEdit = { appointmentToEdit = app },
                            onDelete = { appointmentToDelete = app }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppointmentItemCard(
    appointment: Appointment,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = appointment.status == AppointmentStatus.COMPLETED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFF1F5F9) else PastelCyanCard
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Icon Badge
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = appointment.type.iconEmoji, fontSize = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.title.ifBlank { appointment.doctorSpecialty },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Color(0xFF64748B) else Color(0xFF1E293B)
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val subText = buildString {
                        if (appointment.doctorSpecialty.isNotBlank() && appointment.type == AppointmentType.DOCTOR_VISIT) {
                            append(appointment.doctorSpecialty)
                        }
                        if (appointment.doctorName.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append("د/ ${appointment.doctorName}")
                        }
                        if (appointment.clinicName.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(appointment.clinicName)
                        }
                    }

                    if (subText.isNotBlank()) {
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
                        )
                    }
                }

                // Complete toggle
                IconButton(onClick = onToggleStatus) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "تغيير حالة الموعد",
                        tint = if (isCompleted) Color(0xFF4CAF50) else TealPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date and Time Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${appointment.dateText} ${if (appointment.timeText.isNotBlank()) "(${appointment.timeText})" else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TealDark
                    )
                )
            }

            // Reminders Badges
            if (appointment.reminders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    appointment.reminders.forEach { reminder ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = reminder.labelAr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TealDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Linked Appointment Badge
            if (!appointment.linkedAppointmentTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE0F2FE)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "مرتبط بـ: ${appointment.linkedAppointmentTitle}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF0369A1),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Notes
            if (appointment.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📝 ملاحظات: ${appointment.notes}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = TealPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private val DEFAULT_SPECIALTIES = listOf(
    "طبيب أطفال",
    "باطنة",
    "أسنان",
    "عيون",
    "أنف وأذن",
    "جلدية",
    "قلب",
    "عظام",
    "نساء وتوليد",
    "مخ وأعصاب",
    "طبيب آخر"
)

private val DEFAULT_TESTS = listOf(
    "صورة دم كاملة CBC",
    "تحليل بول",
    "سكر صائم",
    "وظائف كبد",
    "وظائف كلى",
    "تحليل آخر"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddOrEditAppointmentDialog(
    initialAppointment: Appointment?,
    existingAppointments: List<Appointment>,
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialAppointment?.type ?: AppointmentType.DOCTOR_VISIT) }

    // Specialty or Test selections
    var doctorSpecialty by remember { mutableStateOf(initialAppointment?.doctorSpecialty ?: "طبيب أطفال") }
    var customSpecialtyInput by remember { mutableStateOf("") }
    var showCustomSpecialtyField by remember { mutableStateOf(false) }

    var testName by remember { mutableStateOf(initialAppointment?.title ?: "صورة دم كاملة CBC") }
    var customTestInput by remember { mutableStateOf("") }
    var showCustomTestField by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf(initialAppointment?.title ?: "") }
    var doctorName by remember { mutableStateOf(initialAppointment?.doctorName ?: "") }
    var clinicName by remember { mutableStateOf(initialAppointment?.clinicName ?: "") }
    var dateText by remember {
        mutableStateOf(
            initialAppointment?.dateText?.ifBlank { null } ?: SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(Date())
        )
    }
    var timeText by remember { mutableStateOf(initialAppointment?.timeText ?: "10:00 صباحاً") }
    var notes by remember { mutableStateOf(initialAppointment?.notes ?: "") }

    val context = LocalContext.current
    val calendar = remember {
        Calendar.getInstance().apply {
            try {
                val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
                val parsed = sdf.parse(dateText)
                if (parsed != null) time = parsed
            } catch (e: Exception) {}
        }
    }

    // Reminders selection
    var remSameDay by remember {
        mutableStateOf(initialAppointment?.reminders?.any { it.type == ReminderType.SAME_DAY } ?: true)
    }
    var rem1DayBefore by remember {
        mutableStateOf(initialAppointment?.reminders?.any { it.type == ReminderType.DAY_1_BEFORE } ?: false)
    }
    var rem3DaysBefore by remember {
        mutableStateOf(initialAppointment?.reminders?.any { it.type == ReminderType.DAYS_3_BEFORE } ?: false)
    }
    var rem7DaysBefore by remember {
        mutableStateOf(initialAppointment?.reminders?.any { it.type == ReminderType.DAYS_7_BEFORE } ?: false)
    }
    var remCustom by remember {
        mutableStateOf(initialAppointment?.reminders?.any { it.type == ReminderType.CUSTOM } ?: false)
    }
    var customDaysInput by remember {
        mutableStateOf(
            initialAppointment?.reminders?.find { it.type == ReminderType.CUSTOM }?.customDaysBefore?.toString() ?: "5"
        )
    }

    // Linked Appointment
    var linkedAppId by remember { mutableStateOf(initialAppointment?.linkedAppointmentId) }
    var linkedAppTitle by remember { mutableStateOf(initialAppointment?.linkedAppointmentTitle) }
    var expandedLinkedDropdown by remember { mutableStateOf(false) }

    val otherAppointments = remember(existingAppointments, initialAppointment) {
        existingAppointments.filter { it.id != initialAppointment?.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAppointment == null) "إضافة موعد جديد" else "تعديل الموعد",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Type selector
                Text(text = "نوع الموعد:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppointmentType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text("${type.iconEmoji} ${type.labelAr}", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content based on type
                when (selectedType) {
                    AppointmentType.DOCTOR_VISIT -> {
                        Text(text = "التخصص / نوع الطبيب:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DEFAULT_SPECIALTIES.forEach { specialty ->
                                FilterChip(
                                    selected = doctorSpecialty == specialty && !showCustomSpecialtyField,
                                    onClick = {
                                        doctorSpecialty = specialty
                                        showCustomSpecialtyField = false
                                    },
                                    label = { Text(specialty, fontSize = 11.sp) }
                                )
                            }
                            FilterChip(
                                selected = showCustomSpecialtyField,
                                onClick = { showCustomSpecialtyField = true },
                                label = { Text("➕ إضافة تخصص", fontSize = 11.sp) }
                            )
                        }

                        if (showCustomSpecialtyField) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customSpecialtyInput,
                                onValueChange = {
                                    customSpecialtyInput = it
                                    doctorSpecialty = it
                                },
                                label = { Text("اكتب التخصص الجديد") },
                                placeholder = { Text("مثال: علاج طبيعي") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = doctorName,
                            onValueChange = { doctorName = it },
                            label = { Text("اسم الطبيب (اختياري)") },
                            placeholder = { Text("مثال: د. محمد طاهر") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = clinicName,
                            onValueChange = { clinicName = it },
                            label = { Text("اسم العيادة / المستشفى (اختياري)") },
                            placeholder = { Text("مثال: مستشفى الأطفال") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    AppointmentType.LAB_TEST -> {
                        Text(text = "نوع الفحص / التحليل:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DEFAULT_TESTS.forEach { test ->
                                FilterChip(
                                    selected = testName == test && !showCustomTestField,
                                    onClick = {
                                        testName = test
                                        showCustomTestField = false
                                    },
                                    label = { Text(test, fontSize = 11.sp) }
                                )
                            }
                            FilterChip(
                                selected = showCustomTestField,
                                onClick = { showCustomTestField = true },
                                label = { Text("➕ إضافة تحليل", fontSize = 11.sp) }
                            )
                        }

                        if (showCustomTestField) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customTestInput,
                                onValueChange = {
                                    customTestInput = it
                                    testName = it
                                },
                                label = { Text("اكتب اسم التحليل الجديد") },
                                placeholder = { Text("مثال: تحليل فيتامين د") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = clinicName,
                            onValueChange = { clinicName = it },
                            label = { Text("اسم المعمل / المستشفى (اختياري)") },
                            placeholder = { Text("مثال: معمل المختبر") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    AppointmentType.OTHER -> {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("عنوان الموعد (مطلوب)") },
                            placeholder = { Text("مثال: قياس النظارة أو الجلسة") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Common Date and Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        dateText = SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(calendar.time)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                    ) {
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("التاريخ") },
                            trailingIcon = {
                                Icon(Icons.Default.CalendarToday, contentDescription = "اختر التاريخ")
                            },
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val tCalendar = Calendar.getInstance()
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        val amPm = if (hour < 12) "صباحاً" else "مساءً"
                                        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                                        timeText = String.format(Locale("ar"), "%d:%02d %s", displayHour, minute, amPm)
                                    },
                                    tCalendar.get(Calendar.HOUR_OF_DAY),
                                    tCalendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            }
                    ) {
                        OutlinedTextField(
                            value = timeText,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("الوقت (اختياري)") },
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    placeholder = { Text("مثال: صيام 8 ساعات أو إحضار الأشعة") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Reminders Section
                Text(text = "🔔 التذكيرات والإشعارات:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = remSameDay,
                            onCheckedChange = { remSameDay = it },
                            colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                        )
                        Text("يوم الموعد (09:00 صباحاً)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rem1DayBefore,
                            onCheckedChange = { rem1DayBefore = it },
                            colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                        )
                        Text("قبل الموعد بيوم")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rem3DaysBefore,
                            onCheckedChange = { rem3DaysBefore = it },
                            colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                        )
                        Text("قبل الموعد بـ 3 أيام")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rem7DaysBefore,
                            onCheckedChange = { rem7DaysBefore = it },
                            colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                        )
                        Text("قبل الموعد بـ 7 أيام")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = remCustom,
                            onCheckedChange = { remCustom = it },
                            colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                        )
                        Text("تذكير مخصص")
                    }

                    if (remCustom) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("قبل الموعد بـ ")
                            OutlinedTextField(
                                value = customDaysInput,
                                onValueChange = { customDaysInput = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.width(70.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Text(" أيام")
                        }
                    }
                }

                // Linked Appointment Section
                if (otherAppointments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "🔗 ربط بموعد سابق (اختياري):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedLinkedDropdown,
                        onExpandedChange = { expandedLinkedDropdown = !expandedLinkedDropdown }
                    ) {
                        OutlinedTextField(
                            value = linkedAppTitle ?: "بدون ربط",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLinkedDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expandedLinkedDropdown,
                            onDismissRequest = { expandedLinkedDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون ربط") },
                                onClick = {
                                    linkedAppId = null
                                    linkedAppTitle = null
                                    expandedLinkedDropdown = false
                                }
                            )
                            otherAppointments.forEach { other ->
                                val titleStr = other.title.ifBlank { other.doctorSpecialty }
                                DropdownMenuItem(
                                    text = { Text("$titleStr (${other.dateText})") },
                                    onClick = {
                                        linkedAppId = other.id
                                        linkedAppTitle = "$titleStr (${other.dateText})"
                                        expandedLinkedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = when (selectedType) {
                        AppointmentType.DOCTOR_VISIT -> if (title.isNotBlank()) title else "زيارة $doctorSpecialty"
                        AppointmentType.LAB_TEST -> if (title.isNotBlank()) title else testName
                        AppointmentType.OTHER -> if (title.isNotBlank()) title else "موعد جديد"
                    }

                    val remindersList = mutableListOf<ReminderConfig>()
                    if (remSameDay) remindersList.add(ReminderConfig(type = ReminderType.SAME_DAY, labelAr = "يوم الموعد"))
                    if (rem1DayBefore) remindersList.add(ReminderConfig(type = ReminderType.DAY_1_BEFORE, labelAr = "قبل الموعد بيوم"))
                    if (rem3DaysBefore) remindersList.add(ReminderConfig(type = ReminderType.DAYS_3_BEFORE, labelAr = "قبل الموعد بـ 3 أيام"))
                    if (rem7DaysBefore) remindersList.add(ReminderConfig(type = ReminderType.DAYS_7_BEFORE, labelAr = "قبل الموعد بـ 7 أيام"))
                    if (remCustom) {
                        val days = customDaysInput.toIntOrNull() ?: 5
                        remindersList.add(ReminderConfig(type = ReminderType.CUSTOM, customDaysBefore = days, labelAr = "قبل الموعد بـ $days أيام"))
                    }

                    val dateTs = try {
                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
                        sdf.parse(dateText)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    val app = Appointment(
                        id = initialAppointment?.id ?: "",
                        childId = initialAppointment?.childId ?: "",
                        familyId = initialAppointment?.familyId ?: "",
                        type = selectedType,
                        title = finalTitle,
                        doctorSpecialty = if (selectedType == AppointmentType.DOCTOR_VISIT) doctorSpecialty else "",
                        doctorName = doctorName,
                        clinicName = clinicName,
                        dateTimestamp = dateTs,
                        dateText = dateText,
                        timeText = timeText,
                        status = initialAppointment?.status ?: AppointmentStatus.UPCOMING,
                        notes = notes,
                        reminders = remindersList,
                        linkedAppointmentId = linkedAppId,
                        linkedAppointmentTitle = linkedAppTitle,
                        createdAt = initialAppointment?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    onSave(app)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("حفظ الموعد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
