package com.example.ui.screens.child.sections

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlucoseReading
import com.example.data.model.MealContext
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelGreenBorder
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodels.ChildDetailViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class GlucoseFilterOption(val labelAr: String) {
    LAST_7_DAYS("آخر 7 أيام"),
    LAST_30_DAYS("آخر 30 يومًا"),
    LAST_3_MONTHS("آخر 3 أشهر"),
    LAST_6_MONTHS("آخر 6 أشهر"),
    LAST_1_YEAR("آخر سنة"),
    ALL_TIME("كل الفترة"),
    CUSTOM("فترة مخصصة")
}

@Composable
fun GlucoseSectionScreen(
    viewModel: ChildDetailViewModel,
    onBackClick: () -> Unit
) {
    val child by viewModel.child.collectAsState()
    val glucoseReadings by viewModel.glucoseReadings.collectAsState()
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

    var selectedFilter by remember { mutableStateOf(GlucoseFilterOption.LAST_30_DAYS) }
    
    // For Custom Date Picker
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    // Filtered readings based on time filter
    val filteredReadings = remember(glucoseReadings, selectedFilter, customStartDate, customEndDate) {
        glucoseReadings.filter { reading ->
            when (selectedFilter) {
                GlucoseFilterOption.ALL_TIME -> true
                GlucoseFilterOption.LAST_7_DAYS -> reading.createdAt >= (now - 7 * oneDayMs)
                GlucoseFilterOption.LAST_30_DAYS -> reading.createdAt >= (now - 30 * oneDayMs)
                GlucoseFilterOption.LAST_3_MONTHS -> reading.createdAt >= (now - 90 * oneDayMs)
                GlucoseFilterOption.LAST_6_MONTHS -> reading.createdAt >= (now - 180 * oneDayMs)
                GlucoseFilterOption.LAST_1_YEAR -> reading.createdAt >= (now - 365 * oneDayMs)
                GlucoseFilterOption.CUSTOM -> {
                    val start = customStartDate ?: 0L
                    val end = customEndDate ?: Long.MAX_VALUE
                    reading.createdAt in start..end
                }
            }
        }.sortedByDescending { it.createdAt }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var readingToEdit by remember { mutableStateOf<GlucoseReading?>(null) }
    var readingToDelete by remember { mutableStateOf<GlucoseReading?>(null) }

    // Dialogs setup
    if (showAddDialog) {
        GlucoseReadingDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { value, mealContext, dateText, timeText, notes, customTime ->
                viewModel.addGlucoseReading(value, mealContext, dateText, timeText, notes, customTime)
                showAddDialog = false
            }
        )
    }

    if (readingToEdit != null) {
        GlucoseReadingDialog(
            initialReading = readingToEdit,
            onDismiss = { readingToEdit = null },
            onConfirm = { value, mealContext, dateText, timeText, notes, customTime ->
                viewModel.updateGlucoseReading(readingToEdit!!.id, value, mealContext, dateText, timeText, notes, customTime)
                readingToEdit = null
            }
        )
    }

    if (readingToDelete != null) {
        AlertDialog(
            onDismissRequest = { readingToDelete = null },
            title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold, color = TealDark) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذه القراءة؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGlucoseReading(readingToDelete!!.id)
                        readingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { readingToDelete = null }) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showPdfSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            title = { Text("تم حفظ التقرير بنجاح 🎉", fontWeight = FontWeight.Bold, color = TealDark) },
            text = { Text("تم حفظ ملف PDF الخاص بقياسات السكر بنجاح على جهازك. يمكنك الآن مشاركته مع الطبيب المختص.") },
            confirmButton = {
                Button(
                    onClick = {
                        showPdfSuccessDialog = false
                        pdfBytesToSave?.let { bytes ->
                            com.example.data.repository.PdfReportHelper.sharePdf(
                                context = context,
                                pdfBytes = bytes,
                                childName = child?.name ?: "Child",
                                reportType = "Glucose"
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
            text = { Text("لا توجد قراءات سكر مسجلة للطفل ضمن الفترة الزمنية المحددة لتصديرها.") },
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

    // Force Arabic RTL alignment
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("glucose_section_screen"),
            containerColor = Color(0xFFF7FBFB),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = TealPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("add_glucose_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة قياس سكر")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TealDark)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📊 قياسات السكر",
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
                            if (filteredReadings.isEmpty()) {
                                showEmptyDataDialog = true
                            } else {
                                try {
                                    val pdfBytes = com.example.data.repository.PdfReportHelper.generateGlucosePdf(
                                        context = context,
                                        child = child ?: return@Button,
                                        readings = filteredReadings,
                                        filterPeriod = selectedFilter.labelAr
                                    )
                                    pdfBytesToSave = pdfBytes
                                    val cleanChild = (child?.name ?: "child").replace(Regex("[^a-zA-Z0-9\\u0621-\\u064A_]"), "_")
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                    createPdfLauncher.launch("Mawaeedna_${cleanChild}_Glucose_$dateStr.pdf")
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

                // Period Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("filter_row")
                ) {
                    items(GlucoseFilterOption.values()) { option ->
                        FilterChip(
                            selected = (selectedFilter == option),
                            onClick = { selectedFilter = option },
                            label = { Text(option.labelAr) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("filter_chip_${option.name.lowercase()}")
                        )
                    }
                }

                // Custom Date Selectors
                if (selectedFilter == GlucoseFilterOption.CUSTOM) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
                        
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedCal = Calendar.getInstance()
                                        selectedCal.set(year, month, dayOfMonth, 0, 0, 0)
                                        selectedCal.set(Calendar.MILLISECOND, 0)
                                        customStartDate = selectedCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (customStartDate != null) "من: ${sdf.format(Date(customStartDate!!))}" else "من تاريخ",
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedCal = Calendar.getInstance()
                                        selectedCal.set(year, month, dayOfMonth, 23, 59, 59)
                                        selectedCal.set(Calendar.MILLISECOND, 999)
                                        customEndDate = selectedCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (customEndDate != null) "إلى: ${sdf.format(Date(customEndDate!!))}" else "إلى تاريخ",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Dashboard Section (Graph & Stats Card)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, tint = TealDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "متابعة قياسات السكر",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TealDark
                                )
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${filteredReadings.size} قراءة",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (filteredReadings.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد قراءات مسجلة خلال هذه الفترة.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8)),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            SimpleGlucoseLineChart(readings = filteredReadings)
                        }
                    }
                }

                // Readings list title
                Text(
                    text = "سجل القراءات",
                    fontWeight = FontWeight.Bold,
                    color = TealDark,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // List of filtered readings
                if (filteredReadings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "لا توجد قراءات مضافة في هذه الفترة.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8)),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("إضافة قراءة الآن")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(filteredReadings, key = { it.id }) { reading ->
                            GlucoseReadingCard(
                                reading = reading,
                                onEditClick = { readingToEdit = reading },
                                onDeleteClick = { readingToDelete = reading }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleGlucoseLineChart(
    readings: List<GlucoseReading>,
    modifier: Modifier = Modifier
) {
    // Sort oldest to newest for drawing left to right
    val sortedReadings = remember(readings) { readings.sortedBy { it.createdAt } }

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val width = size.width
        val height = size.height

        val paddingLeft = 45.dp.toPx()
        val paddingRight = 15.dp.toPx()
        val paddingTop = 25.dp.toPx()
        val paddingBottom = 30.dp.toPx()

        val graphWidth = width - paddingLeft - paddingRight
        val graphHeight = height - paddingTop - paddingBottom

        if (sortedReadings.isEmpty()) return@Canvas

        val maxVal = sortedReadings.maxOf { it.readingValue }
        val minVal = sortedReadings.minOf { it.readingValue }

        val minY = Math.max(0, Math.min(60, minVal - 10))
        val maxY = Math.max(200, maxVal + 20)
        val yRange = if (maxY != minY) (maxY - minY).toFloat() else 1f

        // Draw helper horizontal grid lines
        val helperLines = listOf(70, 110, 150, 190).filter { it in minY..maxY }
        helperLines.forEach { gridValue ->
            val ratio = (gridValue - minY).toFloat() / yRange
            val y = paddingTop + graphHeight - ratio * graphHeight

            drawLine(
                color = Color(0xFFE2E8F0),
                start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                end = androidx.compose.ui.geometry.Offset(width - paddingRight, y),
                strokeWidth = 1.dp.toPx()
            )

            // Y label text
            drawContext.canvas.nativeCanvas.drawText(
                "$gridValue",
                paddingLeft - 8.dp.toPx(),
                y + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        // Map coordinates for points
        val minX = sortedReadings.first().createdAt
        val maxX = sortedReadings.last().createdAt
        val xRange = if (maxX != minX) (maxX - minX).toFloat() else 1f

        val coordinates = sortedReadings.map { reading ->
            val x = if (maxX != minX) {
                paddingLeft + ((reading.createdAt - minX).toFloat() / xRange) * graphWidth
            } else {
                paddingLeft + graphWidth / 2f
            }
            val y = paddingTop + (1f - ((reading.readingValue - minY).toFloat() / yRange)) * graphHeight
            androidx.compose.ui.geometry.Offset(x, y)
        }

        // Draw curve lines and area gradient fill
        if (coordinates.isNotEmpty()) {
            if (coordinates.size > 1) {
                val path = androidx.compose.ui.graphics.Path()
                val fillPath = androidx.compose.ui.graphics.Path()

                path.moveTo(coordinates[0].x, coordinates[0].y)
                fillPath.moveTo(coordinates[0].x, coordinates[0].y)

                for (i in 1 until coordinates.size) {
                    path.lineTo(coordinates[i].x, coordinates[i].y)
                    fillPath.lineTo(coordinates[i].x, coordinates[i].y)
                }

                fillPath.lineTo(coordinates.last().x, paddingTop + graphHeight)
                fillPath.lineTo(coordinates.first().x, paddingTop + graphHeight)
                fillPath.close()

                // Draw Area Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            TealPrimary.copy(alpha = 0.22f),
                            TealPrimary.copy(alpha = 0.0f)
                        ),
                        startY = paddingTop,
                        endY = paddingTop + graphHeight
                    )
                )

                // Draw connector line
                drawPath(
                    path = path,
                    color = TealPrimary,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }

            // Draw circular dots and exact value markers
            coordinates.forEachIndexed { index, offset ->
                val reading = sortedReadings[index]
                
                drawCircle(
                    color = TealDark,
                    radius = 5.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = offset
                )

                // Exact Reading Value above point
                drawContext.canvas.nativeCanvas.drawText(
                    "${reading.readingValue}",
                    offset.x,
                    offset.y - 8.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#004D40")
                        textSize = 10.dp.toPx()
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }

        // Draw X-axis dates (first, mid, last points to avoid clutter)
        if (sortedReadings.isNotEmpty()) {
            val labelIndices = when {
                sortedReadings.size <= 1 -> listOf(0)
                sortedReadings.size == 2 -> listOf(0, 1)
                else -> listOf(0, sortedReadings.size / 2, sortedReadings.size - 1)
            }

            labelIndices.forEach { index ->
                val reading = sortedReadings[index]
                val offset = coordinates[index]
                val friendlyDate = try {
                    val sdf = SimpleDateFormat("d MMM", Locale("ar"))
                    sdf.format(Date(reading.createdAt))
                } catch (e: Exception) {
                    reading.dateText
                }

                drawContext.canvas.nativeCanvas.drawText(
                    friendlyDate,
                    offset.x,
                    paddingTop + graphHeight + 15.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#64748B")
                        textSize = 8.5.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
private fun GlucoseReadingCard(
    reading: GlucoseReading,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("glucose_reading_card_${reading.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PastelGreenCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, PastelGreenBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual circle for glucose value
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${reading.readingValue}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TealDark,
                                fontSize = 18.sp
                            ),
                            modifier = Modifier.testTag("glucose_reading_value")
                        )
                        Text(
                            text = "mg/dL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 8.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                val label = when (reading.mealContext) {
                    MealContext.FASTING -> "صائم"
                    MealContext.BEFORE_MEAL -> "قبل الوجبة"
                    MealContext.AFTER_MEAL -> "بعد الوجبة"
                    MealContext.BEDTIME -> "قبل النوم"
                    MealContext.RANDOM -> "أخرى"
                }
                
                Text(
                    text = "سياق القياس: $label",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                
                // Formatted display date & time
                val friendlyDate = try {
                    val sdfIn = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
                    val sdfOut = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
                    val parsed = sdfIn.parse(reading.dateText)
                    if (parsed != null) sdfOut.format(parsed) else reading.dateText
                } catch (e: Exception) {
                    reading.dateText
                }
                
                Text(
                    text = "⏰ $friendlyDate (${reading.timeText})",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
                )
                if (reading.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📝 ملاحظات: ${reading.notes}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                    )
                }
            }

            // Edit & Delete Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEditClick, modifier = Modifier.testTag("edit_glucose_button")) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = TealPrimary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.testTag("delete_glucose_button")) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun GlucoseReadingDialog(
    initialReading: GlucoseReading? = null,
    onDismiss: () -> Unit,
    onConfirm: (value: Int, mealContext: MealContext, dateText: String, timeText: String, notes: String, customTime: Long) -> Unit
) {
    val context = LocalContext.current
    
    var readingText by remember { mutableStateOf(initialReading?.readingValue?.toString() ?: "") }
    var mealContext by remember { mutableStateOf(initialReading?.mealContext ?: MealContext.FASTING) }
    var notes by remember { mutableStateOf(initialReading?.notes ?: "") }
    
    val calendar = remember {
        Calendar.getInstance().apply {
            if (initialReading != null) {
                timeInMillis = initialReading.createdAt
            }
        }
    }
    
    val sdfDateFriendly = remember { SimpleDateFormat("d MMMM yyyy", Locale("ar")) }
    val sdfDateDb = remember { SimpleDateFormat("yyyy/MM/dd", Locale("ar")) }
    val sdfTimeFriendly = remember { SimpleDateFormat("hh:mm a", Locale("ar")) }
    
    var dateDisplayState by remember { mutableStateOf(sdfDateFriendly.format(calendar.time)) }
    var timeDisplayState by remember { mutableStateOf(sdfTimeFriendly.format(calendar.time)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialReading == null) "تسجيل قياس سكر جديد" else "تعديل قياس السكر",
                fontWeight = FontWeight.Bold,
                color = TealDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Glucose Value text field
                OutlinedTextField(
                    value = readingText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            readingText = newValue
                        }
                    },
                    label = { Text("قيمة القياس (mg/dL)") },
                    placeholder = { Text("مثال: 115") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("glucose_value_input"),
                    shape = RoundedCornerShape(10.dp)
                )
                
                // Select Date Button
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                dateDisplayState = sdfDateFriendly.format(calendar.time)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_date_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("التاريخ: $dateDisplayState", fontWeight = FontWeight.Bold)
                }
                
                // Select Time Button
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                calendar.set(Calendar.MINUTE, minute)
                                timeDisplayState = sdfTimeFriendly.format(calendar.time)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_time_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("الوقت: $timeDisplayState", fontWeight = FontWeight.Bold)
                }
                
                // Meal Context selection Title
                Text(
                    text = "توقيت القياس:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                // Meal context chips layout (FlowRow)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MealContext.values().forEach { ctx ->
                        val label = when (ctx) {
                            MealContext.FASTING -> "صائم"
                            MealContext.BEFORE_MEAL -> "قبل الوجبة"
                            MealContext.AFTER_MEAL -> "بعد الوجبة"
                            MealContext.BEDTIME -> "قبل النوم"
                            MealContext.RANDOM -> "أخرى"
                        }
                        FilterChip(
                            selected = (mealContext == ctx),
                            onClick = { mealContext = ctx },
                            label = { Text(label) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("meal_chip_${ctx.name.lowercase()}")
                        )
                    }
                }
                
                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية (اختياري)") },
                    placeholder = { Text("مثال: بعد الوجبة بساعتين") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valInt = readingText.toIntOrNull()
                    if (valInt != null) {
                        onConfirm(
                            valInt,
                            mealContext,
                            sdfDateDb.format(calendar.time),
                            timeDisplayState,
                            notes,
                            calendar.timeInMillis
                        )
                    }
                },
                enabled = readingText.isNotBlank() && readingText.toIntOrNull() != null,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("dialog_save_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
