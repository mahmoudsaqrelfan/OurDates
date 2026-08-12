package com.example.ui.screens.child.sections

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.AppointmentType
import com.example.data.model.TestResult
import com.example.ui.theme.PastelPinkCard
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodels.ChildDetailViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

enum class DateFilterOption(val labelAr: String) {
    ALL("كل الفترة"),
    LAST_7_DAYS("آخر 7 أيام"),
    LAST_30_DAYS("آخر 30 يومًا"),
    LAST_3_MONTHS("آخر 3 أشهر"),
    LAST_6_MONTHS("آخر 6 أشهر"),
    LAST_1_YEAR("آخر سنة"),
    CUSTOM("فترة مخصصة")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabResultsSectionScreen(
    viewModel: ChildDetailViewModel,
    onBackClick: () -> Unit
) {
    val child by viewModel.child.collectAsState()
    val testResults by viewModel.testResults.collectAsState()
    val appointments by viewModel.appointments.collectAsState()

    // Filter states
    var selectedDateFilter by remember { mutableStateOf(DateFilterOption.ALL) }
    var selectedTestNameFilter by remember { mutableStateOf("كل التحاليل") }

    // Custom date range states
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    // Dialog trigger states
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<TestResult?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<TestResult?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pdfBytesToSave by remember { mutableStateOf<ByteArray?>(null) }
    var showPdfSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showEmptyDataDialog by remember { mutableStateOf(false) }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && pdfBytesToSave != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(pdfBytesToSave!!)
                }
                showPdfSuccessDialog = true
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "فشل حفظ الملف: ${e.localizedMessage}"
                showErrorDialog = true
            }
        }
    }
    // Combined filtering logic helper
    fun isTimestampInFilter(timestamp: Long, option: DateFilterOption, start: Long?, end: Long?): Boolean {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        return when (option) {
            DateFilterOption.ALL -> true
            DateFilterOption.LAST_7_DAYS -> timestamp >= (now - 7 * oneDayMs)
            DateFilterOption.LAST_30_DAYS -> timestamp >= (now - 30 * oneDayMs)
            DateFilterOption.LAST_3_MONTHS -> timestamp >= (now - 90 * oneDayMs)
            DateFilterOption.LAST_6_MONTHS -> timestamp >= (now - 180 * oneDayMs)
            DateFilterOption.LAST_1_YEAR -> timestamp >= (now - 365 * oneDayMs)
            DateFilterOption.CUSTOM -> {
                val startTs = start ?: 0L
                val endTs = end ?: Long.MAX_VALUE
                // Adjust endTs to include the entire day
                val adjustedEnd = endTs + oneDayMs - 1
                timestamp in startTs..adjustedEnd
            }
        }
    }

    // List of test results filtered by combined parameters
    val filteredResults = remember(testResults, selectedDateFilter, selectedTestNameFilter, customStartDate, customEndDate) {
        testResults.filter { res ->
            val matchesDate = isTimestampInFilter(res.createdAt, selectedDateFilter, customStartDate, customEndDate)
            val matchesTest = selectedTestNameFilter == "كل التحاليل" || res.testName == selectedTestNameFilter
            matchesDate && matchesTest
        }
    }

    // List of unique test names with results to populate the Test Filter Dropdown
    val uniqueTestNames = remember(testResults) {
        listOf("كل التحاليل") + testResults.map { it.testName }.distinct()
    }

    // Available LAB_TEST appointments that are past (due) or marked completed
    val availableAppointments = remember(appointments) {
        appointments.filter { app ->
            app.type == AppointmentType.LAB_TEST && (
                app.status == AppointmentStatus.COMPLETED || app.dateTimestamp <= System.currentTimeMillis()
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("lab_results_section_screen"),
        containerColor = Color(0xFFF7FBFB),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_lab_result_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة نتيجة تحليل")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Header Section
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📋 نتائج التحاليل",
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
                Button(
                    onClick = {
                        if (filteredResults.isEmpty()) {
                            showEmptyDataDialog = true
                        } else {
                            try {
                                val pdfBytes = com.example.data.repository.PdfReportHelper.generateLabResultsPdf(
                                    context = context,
                                    child = child ?: return@Button,
                                    results = filteredResults,
                                    filterPeriod = selectedDateFilter.labelAr,
                                    testFilter = selectedTestNameFilter
                                )
                                pdfBytesToSave = pdfBytes
                                val cleanChild = (child?.name ?: "child").replace(Regex("[^a-zA-Z0-9\\u0621-\\u064A_]"), "_")
                                val cleanTest = selectedTestNameFilter.replace(Regex("[^a-zA-Z0-9\\u0621-\\u064A_]"), "_")
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                createPdfLauncher.launch("Mawaeedna_${cleanChild}_${cleanTest}_$dateStr.pdf")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                errorMessage = "حدث خطأ أثناء إعداد التقرير: ${e.localizedMessage}"
                                showErrorDialog = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("export_pdf_button")
                ) {
                    Text("تصدير PDF 📄", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Notice / Medical Safety
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TealDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "النتائج المعروضة هي بيانات مسجلة بواسطة المستخدم وليست تفسيرًا طبيًا.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TealDark
                                )
                            )
                        }
                    }
                }

                // Dashboard summary & Filters Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "تصفية ومتابعة النتائج",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TealDark
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Date filter options row
                            Text(
                                text = "الفترة الزمنية:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(DateFilterOption.values()) { option ->
                                    FilterChip(
                                        selected = selectedDateFilter == option,
                                        onClick = { selectedDateFilter = option },
                                        label = { Text(option.labelAr) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TealLight,
                                            selectedLabelColor = TealDark
                                        )
                                    )
                                }
                            }

                            // Custom date range inputs
                            if (selectedDateFilter == DateFilterOption.CUSTOM) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            showDatePicker(context) { date ->
                                                customStartDate = date.time
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (customStartDate != null) {
                                                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(customStartDate!!))
                                            } else "من تاريخ",
                                            fontSize = 12.sp
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            showDatePicker(context) { date ->
                                                customEndDate = date.time
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (customEndDate != null) {
                                                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(customEndDate!!))
                                            } else "إلى تاريخ",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Test Type Dropdown filter
                            Text(
                                text = "نوع التحليل:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            var expandedDropdown by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TealDark)
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = selectedTestNameFilter, fontWeight = FontWeight.Bold)
                                }

                                DropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    uniqueTestNames.forEach { name ->
                                        DropdownMenuItem(
                                            text = { Text(name, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                            onClick = {
                                                selectedTestNameFilter = name
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Line Chart Card
                item {
                    val chartPoints = remember(filteredResults, selectedTestNameFilter) {
                        filteredResults
                            .filter { selectedTestNameFilter == "كل التحاليل" || it.testName == selectedTestNameFilter }
                            .mapNotNull { res ->
                                val numeric = res.resultValue.toDoubleOrNull()
                                if (numeric != null) {
                                    Pair(res.createdAt, numeric)
                                } else null
                            }
                            .sortedBy { it.first }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📈 مخطط تطور النتائج",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TealDark
                                    )
                                )
                                if (selectedTestNameFilter != "كل التحاليل") {
                                    Surface(
                                        color = TealLight,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = selectedTestNameFilter,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TealDark)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            if (selectedTestNameFilter == "كل التحاليل") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "اختر تحليلاً معيناً من القائمة أعلاه لعرض الرسم البياني لتطور نتائجه عبر الزمن.",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B)),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else if (chartPoints.size < 2) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "لا توجد نقاط رقمية كافية لعرض الرسم البياني لتطور $selectedTestNameFilter.\n(يتطلب تسجيل نتيجتين رقميتين على الأقل)",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                SimpleNativeLineChart(points = chartPoints)
                            }
                        }
                    }
                }

                // Results list
                item {
                    Text(
                        text = "📋 سجل النتائج (${filteredResults.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TealDark
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (filteredResults.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "لا توجد نتائج مطابقة لخيارات التصفية الحالية.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8)),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تسجيل نتيجة تحليل")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredResults) { res ->
                        LabResultItemCard(
                            result = res,
                            onEdit = { showEditDialog = res },
                            onDelete = { showDeleteConfirmDialog = res }
                        )
                    }
                }
            }
        }
    }

    // Add Result Dialog
    if (showAddDialog) {
        AddResultFlowDialog(
            availableAppointments = availableAppointments,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, value, unit, normalRange, testDate, notes, defId, appIdx ->
                viewModel.addTestResult(name, value, unit, normalRange, testDate, notes, defId, appIdx)
                showAddDialog = false
            }
        )
    }

    // Edit Result Dialog
    if (showEditDialog != null) {
        val editingResult = showEditDialog!!
        EditResultDialog(
            result = editingResult,
            onDismiss = { showEditDialog = null },
            onConfirm = { name, value, unit, normalRange, testDate, notes ->
                viewModel.updateTestResult(
                    id = editingResult.id,
                    testName = name,
                    resultValue = value,
                    unit = unit,
                    normalRange = normalRange,
                    testDate = testDate,
                    notes = notes,
                    testDefinitionId = editingResult.testDefinitionId,
                    testAppointmentId = editingResult.testAppointmentId
                )
                showEditDialog = null
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog != null) {
        val deletingResult = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("حذف نتيجة التحليل", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = { Text("هل تريد حذف نتيجة التحليل ${deletingResult.testName}؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTestResult(deletingResult.id)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showPdfSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            title = { Text("تم حفظ التقرير بنجاح 🎉", fontWeight = FontWeight.Bold, color = TealDark) },
            text = { Text("تم حفظ ملف PDF الخاص بنتائج التحاليل بنجاح على جهازك. يمكنك الآن مشاركته مع الطبيب أو الآخرين.") },
            confirmButton = {
                Button(
                    onClick = {
                        showPdfSuccessDialog = false
                        pdfBytesToSave?.let { bytes ->
                            com.example.data.repository.PdfReportHelper.sharePdf(
                                context = context,
                                pdfBytes = bytes,
                                childName = child?.name ?: "Child",
                                reportType = "LabResults"
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("مشاركة التقرير 📤", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfSuccessDialog = false }) {
                    Text("إغلاق", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    if (showEmptyDataDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDataDialog = false },
            title = { Text("تنبيه ⚠️", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
            text = { Text("لا توجد بيانات نتائج تحاليل مسجلة للطفل ضمن الفترة الزمنية المحددة لتصديرها.") },
            confirmButton = {
                Button(
                    onClick = { showEmptyDataDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("حسناً", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("حدث خطأ ❌", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("حسناً", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun LabResultItemCard(
    result: TestResult,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = TealLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = TealDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.testName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TealDark
                        )
                    )
                    Text(
                        text = result.testDateText,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = Color(0xFF64748B))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تعديل")
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("حذف", color = Color.Red)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Result value and optional elements
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "قيمة النتيجة:",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF475569))
                        )
                        Text(
                            text = "${result.resultValue} ${result.unit}".trim(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TealDark
                            )
                        )
                    }

                    if (result.doctorNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📝 ملاحظات: ${result.doctorNotes}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569)
                            )
                        )
                    }
                }
            }
        }
    }
}

// Draw a beautiful native Line Chart using Compose Canvas
@Composable
fun SimpleNativeLineChart(
    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val width = size.width
        val height = size.height

        val paddingLeft = 50.dp.toPx()
        val paddingRight = 20.dp.toPx()
        val paddingTop = 20.dp.toPx()
        val paddingBottom = 30.dp.toPx()

        val graphWidth = width - paddingLeft - paddingRight
        val graphHeight = height - paddingTop - paddingBottom

        val minX = points.minOf { it.first }
        val maxX = points.maxOf { it.first }
        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }

        val xRange = if (maxX != minX) (maxX - minX).toFloat() else 1f
        val yRange = if (maxY != minY) (maxY - minY).toFloat() else 1f

        // Pad Y axis slightly
        val yPadding = yRange * 0.2f
        val adjustedMinY = minY - yPadding
        val adjustedMaxY = maxY + yPadding
        val adjustedYRange = if (adjustedMaxY != adjustedMinY) (adjustedMaxY - adjustedMinY).toFloat() else 1f

        // 1. Draw horizontal grid lines and Y-axis labels
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val ratio = i.toFloat() / gridSteps
            val y = paddingTop + ratio * graphHeight
            val gridValue = adjustedMaxY - ratio * adjustedYRange

            drawLine(
                color = Color(0xFFE2E8F0),
                start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                end = androidx.compose.ui.geometry.Offset(width - paddingRight, y),
                strokeWidth = 1.dp.toPx()
            )

            // Y label text
            drawContext.canvas.nativeCanvas.drawText(
                String.format(java.util.Locale.US, "%.1f", gridValue),
                paddingLeft - 10.dp.toPx(),
                y + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#64748B")
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        // 2. Plot lines and gradient fills
        val path = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()

        val coordinates = points.map { (time, value) ->
            val x = paddingLeft + ((time - minX).toFloat() / xRange) * graphWidth
            val y = paddingTop + (1f - ((value - adjustedMinY).toFloat() / adjustedYRange)) * graphHeight
            androidx.compose.ui.geometry.Offset(x, y)
        }

        if (coordinates.isNotEmpty()) {
            path.moveTo(coordinates[0].x, coordinates[0].y)
            fillPath.moveTo(coordinates[0].x, coordinates[0].y)

            for (i in 1 until coordinates.size) {
                path.lineTo(coordinates[i].x, coordinates[i].y)
                fillPath.lineTo(coordinates[i].x, coordinates[i].y)
            }

            fillPath.lineTo(coordinates.last().x, paddingTop + graphHeight)
            fillPath.lineTo(coordinates.first().x, paddingTop + graphHeight)
            fillPath.close()

            // Draw area gradient fill under curves
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00ACC1).copy(alpha = 0.25f),
                        Color(0xFF00ACC1).copy(alpha = 0.0f)
                    ),
                    startY = paddingTop,
                    endY = paddingTop + graphHeight
                )
            )

            // Draw connector stroke
            drawPath(
                path = path,
                color = Color(0xFF00ACC1),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Draw coordinates node dots & value markers
            coordinates.forEachIndexed { index, offset ->
                drawCircle(
                    color = Color(0xFF00ACC1),
                    radius = 6.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = offset
                )

                // Value above dot
                val valueString = String.format(java.util.Locale.US, "%.1f", points[index].second)
                drawContext.canvas.nativeCanvas.drawText(
                    valueString,
                    offset.x,
                    offset.y - 10.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#006064")
                        textSize = 10.dp.toPx()
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }

        // 3. Draw X-axis dates (first, middle, last indices)
        if (points.size >= 2) {
            val labelIndices = if (points.size == 2) listOf(0, 1) else listOf(0, points.size / 2, points.size - 1)
            labelIndices.forEach { index ->
                val (time, _) = points[index]
                val offset = coordinates[index]
                val friendlyDate = try {
                    val sdf = SimpleDateFormat("d MMM", Locale("ar"))
                    sdf.format(Date(time))
                } catch (e: Exception) { "" }

                drawContext.canvas.nativeCanvas.drawText(
                    friendlyDate,
                    offset.x,
                    paddingTop + graphHeight + 18.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#64748B")
                        textSize = 9.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

// Elegant Flow-based Dialog that lists due/completed appointments for results input
@Composable
fun AddResultFlowDialog(
    availableAppointments: List<Appointment>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, value: String, unit: String, normalRange: String, testDate: String, notes: String, defId: String?, appIdx: String?) -> Unit
) {
    val context = LocalContext.current
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
    var isManualEntry by remember { mutableStateOf(false) }

    // If neither appointment nor manual entry is selected, show list
    if (selectedAppointment == null && !isManualEntry) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("اختر التحليل لتسجيل النتيجة", fontWeight = FontWeight.Bold, color = TealDark) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (availableAppointments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "لا توجد تحاليل جاهزة لإضافة نتائج.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF64748B)),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "بعد انتهاء موعد التحليل يمكنك تسجيل نتيجته هنا.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "المواعيد الحالية التي انتهت أو اكتملت جاهزة لإضافة نتائجها:",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableAppointments) { app ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAppointment = app },
                                    colors = CardDefaults.cardColors(containerColor = TealLight),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Medication, contentDescription = null, tint = TealDark)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(app.title, fontWeight = FontWeight.Bold, color = TealDark)
                                            Text("التاريخ: ${app.dateText} | المعمل: ${app.clinicName}", fontSize = 11.sp, color = Color(0xFF475569))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Manual Fallback Entry option
                    TextButton(
                        onClick = { isManualEntry = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل نتيجة لتحليل يدوي آخر", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("إلغاء") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    } else {
        // Form Dialog
        val initialName = selectedAppointment?.title ?: ""
        val defId = selectedAppointment?.testDefinitionId
        val appIdx = selectedAppointment?.id

        var testName by remember { mutableStateOf(initialName) }
        var resultValue by remember { mutableStateOf("") }
        var unit by remember { mutableStateOf("") }
        var testDateText by remember { mutableStateOf("") }
        var doctorNotes by remember { mutableStateOf("") }

        val sdfFriendly = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        if (testDateText.isBlank()) {
            testDateText = sdfFriendly.format(Date())
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "تسجيل النتيجة لـ: " + (if (isManualEntry) "تحليل يدوي" else testName),
                    fontWeight = FontWeight.Bold,
                    color = TealDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isManualEntry) {
                        OutlinedTextField(
                            value = testName,
                            onValueChange = { testName = it },
                            label = { Text("اسم التحليل") },
                            placeholder = { Text("مثال: CBC أو فيتامين د") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else {
                        // Linked case: display as filled non-editable text field
                        OutlinedTextField(
                            value = testName,
                            onValueChange = {},
                            label = { Text("اسم التحليل") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = TealDark,
                                disabledBorderColor = TealPrimary,
                                disabledLabelColor = TealDark
                            )
                        )
                    }

                    OutlinedTextField(
                        value = resultValue,
                        onValueChange = { resultValue = it },
                        label = { Text("قيمة النتيجة") },
                        placeholder = { Text("أدخل رقمًا أو نصًا (مثل: 6.5 أو سلبي)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("وحدة القياس (اختياري)") },
                        placeholder = { Text("مثال: % أو ng/mL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Pick Date trigger button
                    val calendar = Calendar.getInstance()
                    OutlinedButton(
                        onClick = {
                            val dialog = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, dayOfMonth)
                                    testDateText = sdfFriendly.format(cal.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            dialog.show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تاريخ ظهور النتيجة: $testDateText", fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = doctorNotes,
                        onValueChange = { doctorNotes = it },
                        label = { Text("ملاحظات إضافية (اختياري)") },
                        placeholder = { Text("مثال: الطبيب راضٍ عن النتيجة") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm(testName, resultValue, unit, "", testDateText, doctorNotes, defId, appIdx)
                    },
                    enabled = testName.isNotBlank() && resultValue.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (selectedAppointment != null || isManualEntry) {
                        // Go back to selection step
                        selectedAppointment = null
                        isManualEntry = false
                    } else {
                        onDismiss()
                    }
                }) {
                    Text("رجوع")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// Edit Result Dialog
@Composable
fun EditResultDialog(
    result: TestResult,
    onDismiss: () -> Unit,
    onConfirm: (name: String, value: String, unit: String, normalRange: String, testDate: String, notes: String) -> Unit
) {
    var testName by remember { mutableStateOf(result.testName) }
    var resultValue by remember { mutableStateOf(result.resultValue) }
    var unit by remember { mutableStateOf(result.unit) }
    var testDateText by remember { mutableStateOf(result.testDateText) }
    var doctorNotes by remember { mutableStateOf(result.doctorNotes) }

    val context = LocalContext.current
    val sdfFriendly = SimpleDateFormat("d MMMM yyyy", Locale("ar"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل نتيجة تحليل", fontWeight = FontWeight.Bold, color = TealDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = testName,
                    onValueChange = { testName = it },
                    label = { Text("اسم التحليل") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = resultValue,
                    onValueChange = { resultValue = it },
                    label = { Text("قيمة النتيجة") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("وحدة القياس (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                val calendar = Calendar.getInstance()
                OutlinedButton(
                    onClick = {
                        val dialog = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val cal = Calendar.getInstance()
                                cal.set(year, month, dayOfMonth)
                                testDateText = sdfFriendly.format(cal.time)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        dialog.show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تاريخ ظهور النتيجة: $testDateText", fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = doctorNotes,
                    onValueChange = { doctorNotes = it },
                    label = { Text("ملاحظات إضافية (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(testName, resultValue, unit, result.normalRangeText, testDateText, doctorNotes) },
                enabled = testName.isNotBlank() && resultValue.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// Utility to show date picker
private fun showDatePicker(context: Context, onDateSelected: (Date) -> Unit) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth, 0, 0, 0)
            onDateSelected(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
