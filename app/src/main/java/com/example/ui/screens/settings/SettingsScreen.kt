package com.example.ui.screens.settings

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Family
import com.example.data.model.UserProfile
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.PastelCyanCard
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodels.SettingsViewModel

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentUser: UserProfile?,
    viewModel: SettingsViewModel = viewModel(),
    onBackClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val family by viewModel.family.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var backupJsonToSave by remember { mutableStateOf("") }
    var lastBackupUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showBackupSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf("") }
    var showRestoreSuccessDialog by remember { mutableStateOf(false) }

    fun shareBackupFile(uri: android.net.Uri) {
        try {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة النسخة الاحتياطية"))
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "فشل مشاركة ملف النسخة الاحتياطية: ${e.localizedMessage}"
            showErrorDialog = true
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(backupJsonToSave.toByteArray())
                    }
                    lastBackupUri = uri
                    showBackupSuccessDialog = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    errorMessage = "فشل حفظ ملف النسخة الاحتياطية: ${e.localizedMessage}"
                    showErrorDialog = true
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    } ?: ""

                    if (viewModel.validateBackupJson(jsonString)) {
                        pendingRestoreJson = jsonString
                        showConfirmRestoreDialog = true
                    } else {
                        errorMessage = "ملف النسخة الاحتياطية غير صالح أو غير متوافق."
                        showErrorDialog = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    errorMessage = "فشل قراءة ملف النسخة الاحتياطية: ${e.localizedMessage}"
                    showErrorDialog = true
                }
            }
        }
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            viewModel.updateSettings(settings.copy(notificationsEnabled = true))
        }
    }

    var showEditFamilyNameDialog by remember { mutableStateOf(false) }
    var familyNameInput by remember { mutableStateOf(family.familyName) }

    if (showEditFamilyNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditFamilyNameDialog = false },
            title = {
                Text(
                    text = "تعديل اسم الأسرة",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            },
            text = {
                OutlinedTextField(
                    value = familyNameInput,
                    onValueChange = { familyNameInput = it },
                    label = { Text("اسم الأسرة") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_family_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (familyNameInput.isNotBlank()) {
                            viewModel.updateFamilyName(familyNameInput)
                            showEditFamilyNameDialog = false
                        }
                    },
                    enabled = familyNameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_family_name_button")
                ) {
                    Text("حفظ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditFamilyNameDialog = false }) {
                    Text("إلغاء", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("settings_screen"),
        color = Color(0xFFF7FBFB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = TealDark
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "الإعدادات العامة",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = TealDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Profile Section
            Text(
                text = "الحساب المسجل 👤",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        color = PastelCyanCard
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = currentUser?.displayName ?: "المستخدم",
                                tint = TealPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: "أحمد علي",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E293B)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentUser?.email ?: "ahmed@example.com",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        )
                    }

                    SyncStatusBadge(syncStatus = syncStatus)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Family Profile Settings Section
            Text(
                text = "إدارة الأسرة 👨‍👩‍👧‍👦",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingItemCard(
                icon = Icons.Default.FamilyRestroom,
                title = "اسم الأسرة",
                subtitle = family.familyName,
                trailing = {
                    IconButton(
                        onClick = {
                            familyNameInput = family.familyName
                            showEditFamilyNameDialog = true
                        },
                        modifier = Modifier.testTag("edit_family_name_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل اسم الأسرة",
                            tint = TealPrimary
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // App Preferences
            Text(
                text = "التفضيلات والمظهر 🎨",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingItemCard(
                icon = Icons.Default.Notifications,
                title = "الإشعارات والتنبيهات المحليّة",
                subtitle = if (hasNotificationPermission) "إشعارات تذكير المواعيد والتطعيمات مفعلة" else "الإذن غير مفعّل - انقر للتفعيل",
                trailing = {
                    Switch(
                        checked = settings.notificationsEnabled && hasNotificationPermission,
                        onCheckedChange = { checked ->
                            if (checked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.updateSettings(settings.copy(notificationsEnabled = checked))
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary)
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingItemCard(
                icon = Icons.Default.Language,
                title = "اللغة (Language)",
                subtitle = "العربية",
                trailing = {
                    Text(
                        text = "افتراضي",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TealDark)
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingItemCard(
                icon = Icons.Default.Palette,
                title = "المظهر (Theme)",
                subtitle = "طبي هادئ",
                trailing = {
                    Text(
                        text = "Pastel",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0288D1))
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "💾 النسخ الاحتياطي والاستعادة",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backup_restore_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "النسخ الاحتياطي المحلي يدوي بالكامل ويعمل دون الحاجة لاتصال بالإنترنت. يحفظ ملف البيانات بأمان على جهازك ويمكنك استعادته لاحقاً.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    backupJsonToSave = viewModel.createBackupJson()
                                    val timeStr = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US).format(java.util.Date())
                                    val fileName = "Mawaeedna_Backup_$timeStr.mwbackup"
                                    createDocumentLauncher.launch(fileName)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("create_backup_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("نسخ احتياطي", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("restore_backup_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("استعادة البيانات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (lastBackupUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                lastBackupUri?.let { shareBackupFile(it) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("share_backup_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0288D1)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0288D1)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("مشاركة ملف النسخة الأخيرة 📤", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Dialogs for Backup & Restore
            if (showBackupSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { showBackupSuccessDialog = false },
                    title = {
                        Text(
                            text = "تم إنشاء النسخة الاحتياطية بنجاح 💾",
                            fontWeight = FontWeight.Bold,
                            color = TealDark
                        )
                    },
                    text = {
                        Text("تم حفظ ملف النسخة الاحتياطية بأمان على جهازك. يمكنك الآن مشاركتها أو نقلها لاستعادتها لاحقاً.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showBackupSuccessDialog = false
                                lastBackupUri?.let { shareBackupFile(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("مشاركة النسخة الاحتياطية 📤", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBackupSuccessDialog = false }) {
                            Text("إغلاق", color = Color(0xFF64748B))
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Color.White
                )
            }

            if (showConfirmRestoreDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmRestoreDialog = false },
                    title = {
                        Text(
                            text = "استعادة النسخة الاحتياطية ⚠️",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    },
                    text = {
                        Text("سيتم استبدال البيانات الحالية بالكامل ببيانات النسخة الاحتياطية. هل أنت متأكد من الاستمرار؟")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmRestoreDialog = false
                                scope.launch {
                                    val success = viewModel.restoreBackup(pendingRestoreJson)
                                    if (success) {
                                        showRestoreSuccessDialog = true
                                    } else {
                                        errorMessage = "حدث خطأ أثناء استعادة النسخة الاحتياطية."
                                        showErrorDialog = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("استعادة", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmRestoreDialog = false }) {
                            Text("إلغاء", color = Color(0xFF64748B))
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Color.White
                )
            }

            if (showRestoreSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { showRestoreSuccessDialog = false },
                    title = {
                        Text(
                            text = "تمت الاستعادة بنجاح 🎉",
                            fontWeight = FontWeight.Bold,
                            color = TealDark
                        )
                    },
                    text = {
                        Text("تمت استعادة كافة البيانات والإعدادات بنجاح إلى التطبيق.")
                    },
                    confirmButton = {
                        Button(
                            onClick = { showRestoreSuccessDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("حسناً", fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Color.White
                )
            }

            if (showErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showErrorDialog = false },
                    title = {
                        Text(
                            text = "تنبيه ❌",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    },
                    text = {
                        Text(errorMessage)
                    },
                    confirmButton = {
                        Button(
                            onClick = { showErrorDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("حسناً", fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Logout Button
            Button(
                onClick = onSignOutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFEBEE),
                    contentColor = Color(0xFFC62828)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "تسجيل الخروج",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل الخروج",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Version Note Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PastelCyanCard)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TealPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "مواعيدنا - الإصدار 2.0",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TealDark
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "منظم صحة ومواعيد العائلة - إدارة الحساب، أفراد الأسرة، وGoogle Sign-In.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = TealPrimary
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B)
                        )
                    )
                }
            }
            trailing()
        }
    }
}
