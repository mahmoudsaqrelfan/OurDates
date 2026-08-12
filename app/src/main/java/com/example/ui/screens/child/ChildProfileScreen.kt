package com.example.ui.screens.child

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Face2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Gender
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelCyanCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPinkCard
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodels.ChildDetailViewModel

@Composable
fun ChildProfileScreen(
    childId: String,
    viewModel: ChildDetailViewModel,
    onBackClick: () -> Unit,
    onOpenAppointments: () -> Unit,
    onOpenTests: () -> Unit,
    onOpenLabResults: () -> Unit,
    onOpenGlucose: () -> Unit
) {
    val child by viewModel.child.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val testResults by viewModel.testResults.collectAsState()
    val glucoseReadings by viewModel.glucoseReadings.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("child_profile_screen"),
        color = Color(0xFFF7FBFB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("child_profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع للرئيسية",
                            tint = TealDark
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الملف الصحي للطفل",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TealDark
                        )
                    )
                }

                if (child != null) {
                    Row {
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.testTag("edit_child_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل الطفل",
                                tint = TealPrimary
                            )
                        }
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("delete_child_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف الطفل",
                                tint = Color(0xFFE53935)
                            )
                        }
                    }
                }
            }

            if (child == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("جاري تحميل بيانات الطفل...")
                }
                return@Surface
            }

            val currentChild = child!!
            val avatarBg = if (currentChild.gender == Gender.BOY) TealPrimary else Color(0xFFEC407A)

            if (showEditDialog) {
                EditChildDialog(
                    child = currentChild,
                    onDismiss = { showEditDialog = false },
                    onConfirm = { name, birthDate, ageText, gender, avatarColorHex ->
                        viewModel.updateChild(name, birthDate, ageText, gender, avatarColorHex)
                        showEditDialog = false
                    }
                )
            }

            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = {
                        Text(
                            text = "حذف الطفل",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    },
                    text = {
                        Text(
                            text = "هل تريد حذف هذا الطفل؟",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF475569))
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = false
                                viewModel.deleteChild(onDeleted = onBackClick)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("confirm_delete_child_button")
                        ) {
                            Text("حذف", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("إلغاء", color = Color(0xFF64748B))
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Color.White
                )
            }

            // Child Profile Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = avatarBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (currentChild.gender == Gender.BOY) Icons.Default.Face else Icons.Default.Face2,
                                contentDescription = currentChild.name,
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentChild.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color(0xFF1E293B)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🎂 تاريخ الميلاد: ${currentChild.birthDate}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "👶 العمر: ${currentChild.ageText}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4 MAIN CATEGORY SECTIONS (Pastel Grid Cards)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "الأقسام الرئيسية 📁",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MainSectionCard(
                        modifier = Modifier.weight(1f),
                        title = "📅 المواعيد",
                        subtitle = "${appointments.size} مواعيد مسجلة",
                        backgroundColor = PastelCyanCard,
                        icon = Icons.Default.CalendarMonth,
                        iconColor = TealPrimary,
                        onClick = onOpenAppointments,
                        testTag = "section_appointments"
                    )

                    MainSectionCard(
                        modifier = Modifier.weight(1f),
                        title = "🧪 الفحوصات والتحاليل",
                        subtitle = "مواعيد التحاليل القادمة",
                        backgroundColor = PastelBlueCard,
                        icon = Icons.Default.Science,
                        iconColor = Color(0xFF0288D1),
                        onClick = onOpenTests,
                        testTag = "section_tests"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MainSectionCard(
                        modifier = Modifier.weight(1f),
                        title = "📋 نتائج التحاليل",
                        subtitle = "${testResults.size} نتائج معملية",
                        backgroundColor = PastelPinkCard,
                        icon = Icons.Default.Medication,
                        iconColor = Color(0xFFD81B60),
                        onClick = onOpenLabResults,
                        testTag = "section_lab_results"
                    )

                    MainSectionCard(
                        modifier = Modifier.weight(1f),
                        title = "📊 قياسات السكر",
                        subtitle = "${glucoseReadings.size} قراءات مسجلة",
                        backgroundColor = PastelGreenCard,
                        icon = Icons.Default.Bloodtype,
                        iconColor = Color(0xFF2E7D32),
                        onClick = onOpenGlucose,
                        testTag = "section_glucose"
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Overview Section
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "🏠 نظرة عامة ملخصة",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📅 الموعد القادم:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TealDark
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val nextApp = appointments.firstOrNull()
                        if (nextApp != null) {
                            Text(
                                text = "${nextApp.title} (${nextApp.doctorName})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            )
                            Text(
                                text = "${nextApp.dateText} الساعة ${nextApp.timeText}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                            )
                        } else {
                            Text(
                                text = "لا يوجد موعد قريب مسجل",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainSectionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "عرض التفاصيل",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
