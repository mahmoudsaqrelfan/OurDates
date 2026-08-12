package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Face2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Appointment
import com.example.data.model.AppointmentType
import com.example.data.model.Child
import com.example.data.model.Gender
import com.example.data.model.NotificationReminder
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelCyanCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.PastelPinkCard
import com.example.ui.theme.PastelYellowCard
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodels.FamilyViewModel

@Composable
fun FamilyHomeScreen(
    viewModel: FamilyViewModel = viewModel(),
    onChildClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val family by viewModel.family.collectAsState()
    val children by viewModel.children.collectAsState()
    val upcomingAppointments by viewModel.upcomingAppointments.collectAsState()
    val reminders by viewModel.followUpReminders.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var showAddChildDialog by remember { mutableStateOf(false) }

    if (showAddChildDialog) {
        AddChildDialog(
            onDismiss = { showAddChildDialog = false },
            onConfirm = { name, birthDate, ageText, gender ->
                viewModel.addChild(name, birthDate, ageText, gender)
                showAddChildDialog = false
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("family_home_screen"),
        color = Color(0xFFF7FBFB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مرحباً بك، ${family.familyName} 👋",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TealDark
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SyncStatusBadge(syncStatus = syncStatus)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "منظم مواعيدك وفحوصاتك اليومية",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .background(Color.White, CircleShape)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "الإعدادات",
                        tint = TealPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SECTION 1: Family Members (أفراد الأسرة)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "أفراد الأسرة 👨‍👩‍👧‍👦",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E293B)
                        )
                    )

                    OutlinedButton(
                        onClick = { showAddChildDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("add_child_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة طفل",
                            tint = TealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "إضافة طفل",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(children) { child ->
                        ChildMemberCard(
                            child = child,
                            onClick = { onChildClick(child.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // SECTION 2: Upcoming Appointments Placeholder (المواعيد القادمة)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المواعيد القادمة 📅",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E293B)
                        )
                    )
                    Text(
                        text = "${upcomingAppointments.size} مواعيد",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TealPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (upcomingAppointments.isEmpty()) {
                    EmptySectionCard(
                        title = "لا توجد مواعيد قادمة",
                        subtitle = "يمكنك إضافة موعد طبي من صفحة الطفل"
                    )
                } else {
                    upcomingAppointments.forEach { app ->
                        val child = children.find { it.id == app.childId }
                        UpcomingAppointmentCard(
                            appointment = app,
                            childName = child?.name ?: "طفل",
                            onClick = {
                                if (child != null) {
                                    onChildClick(child.id)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: Follow-up Reminders Placeholder (يحتاج متابعة)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "يحتاج متابعة ⚠️",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                reminders.forEach { reminder ->
                    FollowUpCard(reminder = reminder)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun ChildMemberCard(
    child: Child,
    onClick: () -> Unit
) {
    val cardBg = if (child.gender == Gender.BOY) PastelCyanCard else PastelPinkCard
    val avatarBg = if (child.gender == Gender.BOY) TealPrimary else Color(0xFFEC407A)

    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
            .testTag("child_card_${child.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = avatarBg,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (child.gender == Gender.BOY) Icons.Default.Face else Icons.Default.Face2,
                        contentDescription = child.name,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = child.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = child.ageText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "فتح الملف",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TealDark
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = TealDark,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun UpcomingAppointmentCard(
    appointment: Appointment,
    childName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PastelBlueCard)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = appointment.type.iconEmoji,
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = appointment.title.ifBlank { appointment.doctorSpecialty },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = childName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0288D1)
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = Color(0xFF475569)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = "⏰ ${appointment.dateText} ${if (appointment.timeText.isNotBlank()) "(${appointment.timeText})" else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0288D1)
                    )
                )
            }
        }
    }
}

@Composable
private fun FollowUpCard(
    reminder: NotificationReminder
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PastelYellowCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Color(0xFFF57F17),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = reminder.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF5D4037)
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptySectionCard(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8)
                )
            )
        }
    }
}
