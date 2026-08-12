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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.ui.text.style.TextAlign
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
import com.example.data.model.TestDefinition
import com.example.ui.theme.PastelBlueCard
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
fun TestsSectionScreen(
    viewModel: ChildDetailViewModel,
    onBackClick: () -> Unit
) {
    val child by viewModel.child.collectAsState()
    val testAppointments by viewModel.testAppointments.collectAsState()
    val customTests by viewModel.customTests.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Scheduled, 1: Catalog
    var showAddDialog by remember { mutableStateOf(false) }
    var showCustomTestDialog by remember { mutableStateOf(false) }
    var appointmentToEdit by remember { mutableStateOf<Appointment?>(null) }
    var appointmentToDelete by remember { mutableStateOf<Appointment?>(null) }
    var prefilledTestName by remember { mutableStateOf("") }

    // Combine default and custom definitions
    val allDefinitions = remember(customTests) {
        TestDefinition.DEFAULT_DEFINITIONS + customTests
    }

    val currentTime = System.currentTimeMillis()
    // Upcoming: Upcoming status AND date in future or today
    val upcomingTests = remember(testAppointments, currentTime) {
        testAppointments.filter { 
            it.status == AppointmentStatus.UPCOMING && it.dateTimestamp >= currentTime - 24 * 60 * 60 * 1000 
        }.sortedBy { it.dateTimestamp }
    }

    // Past/Completed: Completed status OR date in past (preparation for result entry later)
    val pastTests = remember(testAppointments, currentTime) {
        testAppointments.filter { 
            it.status == AppointmentStatus.COMPLETED || it.dateTimestamp < currentTime - 24 * 60 * 60 * 1000 
        }.sortedByDescending { it.dateTimestamp }
    }

    if (showAddDialog || appointmentToEdit != null) {
        AddOrEditTestAppointmentDialog(
            initialAppointment = appointmentToEdit,
            prefilledTestName = prefilledTestName,
            testDefinitions = allDefinitions,
            onDismiss = {
                showAddDialog = false
                appointmentToEdit = null
                prefilledTestName = ""
            },
            onSave = { app ->
                viewModel.saveAppointment(app)
                showAddDialog = false
                appointmentToEdit = null
                prefilledTestName = ""
            }
        )
    }

    if (showCustomTestDialog) {
        AddCustomTestDialog(
            onDismiss = { showCustomTestDialog = false },
            onSave = { name, category, description ->
                viewModel.addCustomTest(name, category, description)
                showCustomTestDialog = false
            }
        )
    }

    if (appointmentToDelete != null) {
        AlertDialog(
            onDismissRequest = { appointmentToDelete = null },
            title = { Text("حذف موعد الفحص", fontWeight = FontWeight.Bold, color = TealDark) },
            text = { Text("هل أنت متأكد من رغبتك في حذف موعد هذا الفحص؟ سيتم إلغاء جميع التذكيرات المرتبطة به أيضاً.") },
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
                    Text("إلغاء", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("tests_section_screen"),
        containerColor = Color(0xFFF7FBFB),
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { 
                        appointmentToEdit = null
                        prefilledTestName = ""
                        showAddDialog = true 
                    },
                    containerColor = Color(0xFF0288D1),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("add_test_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة موعد فحص")
                }
            } else {
                FloatingActionButton(
                    onClick = { showCustomTestDialog = true },
                    containerColor = TealPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("add_custom_test_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة تحليل مخصص")
                }
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
                        text = "🧪 الفحوصات والتحاليل المعملية",
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

            // Central Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF0288D1),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = if (selectedTabIndex == 0) Color(0xFF0288D1) else TealPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "📅 مواعيد الفحوصات",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "📚 دليل الفحوصات",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTabIndex) {
                0 -> {
                    // Scheduled Lab Tests Screen
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section 1: Upcoming Test Appointments
                        item {
                            Text(
                                text = "📅 الفحوصات القادمة (${upcomingTests.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TealDark
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (upcomingTests.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "لا توجد فحوصات قادمة مجدولة.",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                                        )
                                    }
                                }
                            }
                        } else {
                            items(upcomingTests) { testApp ->
                                TestAppointmentCard(
                                    appointment = testApp,
                                    isPast = false,
                                    onEdit = { appointmentToEdit = testApp },
                                    onDelete = { appointmentToDelete = testApp },
                                    onToggleStatus = {
                                        viewModel.saveAppointment(testApp.copy(status = AppointmentStatus.COMPLETED))
                                    }
                                )
                            }
                        }

                        // Section 2: Completed / Past Tests
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "✅ أرشيف الفحوصات السابقة (${pastTests.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "الفحوصات السابقة جاهزة لتسجيل النتائج الطبية لاحقاً فور توفر الميزة.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        if (pastTests.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "لا توجد فحوصات سابقة بالأرشيف.",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                                        )
                                    }
                                }
                            }
                        } else {
                            items(pastTests) { testApp ->
                                TestAppointmentCard(
                                    appointment = testApp,
                                    isPast = true,
                                    onEdit = { appointmentToEdit = testApp },
                                    onDelete = { appointmentToDelete = testApp },
                                    onToggleStatus = {
                                        val newStatus = if (testApp.status == AppointmentStatus.COMPLETED) AppointmentStatus.UPCOMING else AppointmentStatus.COMPLETED
                                        viewModel.saveAppointment(testApp.copy(status = newStatus))
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }

                1 -> {
                    // Catalog Tab
                    val groupedDefinitions = remember(allDefinitions) {
                        allDefinitions.groupBy { it.category }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "تصفح دليل الفحوصات الشائعة والمخصصة لحجز موعد فحص سريع لطفلك:",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF475569)),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        TestDefinition.DEFAULT_CATEGORIES.forEach { category ->
                            val list = groupedDefinitions[category] ?: emptyList()
                            if (list.isNotEmpty()) {
                                item {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TealDark
                                        ),
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }

                                items(list) { definition ->
                                    TestDefinitionCard(
                                        definition = definition,
                                        onScheduleClick = {
                                            appointmentToEdit = null
                                            prefilledTestName = definition.name
                                            showAddDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        // Custom / Other category
                        val otherCategory = "تحاليل مخصصة"
                        val customList = customTests.filter { it.category == otherCategory || it.category !in TestDefinition.DEFAULT_CATEGORIES }
                        if (customList.isNotEmpty()) {
                            item {
                                Text(
                                    text = "🧪 الفحوصات والتحاليل المخصصة مسبقاً",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0288D1)
                                    ),
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }

                            items(customList) { definition ->
                                TestDefinitionCard(
                                    definition = definition,
                                    onScheduleClick = {
                                        appointmentToEdit = null
                                        prefilledTestName = definition.name
                                        showAddDialog = true
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(50.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestAppointmentCard(
    appointment: Appointment,
    isPast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast) Color(0xFFF1F5F9) else PastelBlueCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🧪", fontSize = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isPast) Color(0xFF475569) else Color(0xFF0F172A)
                        )
                    )

                    if (appointment.clinicName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "المختبر: ${appointment.clinicName}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
                        )
                    }
                }

                // Checkbox status toggle
                IconButton(onClick = onToggleStatus) {
                    Icon(
                        imageVector = if (appointment.status == AppointmentStatus.COMPLETED) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "تغيير حالة الفحص",
                        tint = if (appointment.status == AppointmentStatus.COMPLETED) Color(0xFF4CAF50) else Color(0xFF0288D1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date and Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFF0288D1),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${appointment.dateText} ${if (appointment.timeText.isNotBlank()) "(${appointment.timeText})" else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0288D1)
                    )
                )

                if (isPast) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE2E8F0)
                    ) {
                        Text(
                            text = "منتهي",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Reminders
            if (appointment.reminders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFF0288D1),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "منبه نشط: " + appointment.reminders.joinToString(", ") { it.labelAr },
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0288D1)),
                        fontSize = 11.sp
                    )
                }
            }

            // Notes
            if (appointment.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 التعليمات: ${appointment.notes}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD84315))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Edit / Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF0288D1), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun TestDefinitionCard(
    definition: TestDefinition,
    onScheduleClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🧪", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = definition.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    )

                    if (definition.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = definition.description,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                            lineHeight = 16.sp
                        )
                    }

                    if (definition.unit.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الوحدة المقررة: ${definition.unit}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onScheduleClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جدولة هذا الفحص", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddOrEditTestAppointmentDialog(
    initialAppointment: Appointment?,
    prefilledTestName: String,
    testDefinitions: List<TestDefinition>,
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit
) {
    var testName by remember { 
        mutableStateOf(
            initialAppointment?.title ?: prefilledTestName.ifBlank { testDefinitions.firstOrNull()?.name ?: "" }
        )
    }
    var labName by remember { mutableStateOf(initialAppointment?.clinicName ?: "") }
    var dateText by remember {
        mutableStateOf(
            initialAppointment?.dateText?.ifBlank { null } ?: SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(Date())
        )
    }
    var timeText by remember { mutableStateOf(initialAppointment?.timeText ?: "09:00 صباحاً") }
    var instructions by remember { mutableStateOf(initialAppointment?.notes ?: "صيام 8 ساعات") }

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

    var expandedTestsDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAppointment == null) "جدولة موعد فحص جديد" else "تعديل موعد الفحص",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = TealDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "اسم الفحص / التحليل:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedTestsDropdown,
                    onExpandedChange = { expandedTestsDropdown = !expandedTestsDropdown }
                ) {
                    OutlinedTextField(
                        value = testName,
                        onValueChange = { testName = it },
                        readOnly = false, // Allow writing a custom name directly as well
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTestsDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedTestsDropdown,
                        onDismissRequest = { expandedTestsDropdown = false }
                    ) {
                        testDefinitions.forEach { def ->
                            DropdownMenuItem(
                                text = { Text(def.name) },
                                onClick = {
                                    testName = def.name
                                    if (instructions.isBlank() || instructions == "صيام 8 ساعات") {
                                        instructions = if (def.name.contains("صائم") || def.name.contains("HbA1c") || def.name.contains("Glucose") || def.name.contains("تحمل")) {
                                            "يتطلب صيام 8 ساعات قبل الفحص"
                                        } else {
                                            "إجراء فحص روتيني طبيعي"
                                        }
                                    }
                                    expandedTestsDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = labName,
                    onValueChange = { labName = it },
                    label = { Text("اسم المعمل / المختبر (اختياري)") },
                    placeholder = { Text("مثال: معمل البرج الطبي") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                            placeholder = { Text("YYYY/MM/DD") },
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("التعليمات / التحضيرات (اختياري)") },
                    placeholder = { Text("مثال: صيام 8 ساعات، إحضار عينة") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "🔔 التذكيرات والإشعارات:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = remSameDay,
                            onCheckedChange = { remSameDay = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0288D1))
                        )
                        Text("يوم الفحص (09:00 صباحاً)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rem1DayBefore,
                            onCheckedChange = { rem1DayBefore = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0288D1))
                        )
                        Text("قبل الفحص بيوم")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rem3DaysBefore,
                            onCheckedChange = { rem3DaysBefore = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0288D1))
                        )
                        Text("قبل الفحص بـ 3 أيام")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rem7DaysBefore,
                            onCheckedChange = { rem7DaysBefore = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0288D1))
                        )
                        Text("قبل الفحص بـ 7 أيام")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val remindersList = mutableListOf<ReminderConfig>()
                    if (remSameDay) remindersList.add(ReminderConfig(type = ReminderType.SAME_DAY, labelAr = "يوم الفحص"))
                    if (rem1DayBefore) remindersList.add(ReminderConfig(type = ReminderType.DAY_1_BEFORE, labelAr = "قبل الفحص بيوم"))
                    if (rem3DaysBefore) remindersList.add(ReminderConfig(type = ReminderType.DAYS_3_BEFORE, labelAr = "قبل الفحص بـ 3 أيام"))
                    if (rem7DaysBefore) remindersList.add(ReminderConfig(type = ReminderType.DAYS_7_BEFORE, labelAr = "قبل الفحص بـ 7 أيام"))

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
                        type = AppointmentType.LAB_TEST,
                        title = testName,
                        clinicName = labName,
                        dateTimestamp = dateTs,
                        dateText = dateText,
                        timeText = timeText,
                        status = initialAppointment?.status ?: AppointmentStatus.UPCOMING,
                        notes = instructions,
                        reminders = remindersList,
                        createdAt = initialAppointment?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    onSave(app)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                enabled = testName.isNotBlank()
            ) {
                Text("حفظ الفحص", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color(0xFF64748B)) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomTestDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TestDefinition.DEFAULT_CATEGORIES.first()) }
    var description by remember { mutableStateOf("") }

    var expandedCategoryDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة تحليل مخصص للدليل", fontWeight = FontWeight.Bold, color = TealDark) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم التحليل المخصص") },
                    placeholder = { Text("مثال: تحليل حساسية الحليب") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "التصنيف الرئيسي:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedCategoryDropdown,
                    onExpandedChange = { expandedCategoryDropdown = !expandedCategoryDropdown }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCategoryDropdown,
                        onDismissRequest = { expandedCategoryDropdown = false }
                    ) {
                        TestDefinition.DEFAULT_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expandedCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف مختصر للتحليل (اختياري)") },
                    placeholder = { Text("مثال: فحص دم لمعرفة مدى تحسس الطفل للبروتين") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, category, description) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("إضافة للدليل", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color(0xFF64748B)) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
