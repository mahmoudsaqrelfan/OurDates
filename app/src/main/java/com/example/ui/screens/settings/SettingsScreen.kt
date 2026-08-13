package com.example.ui.screens.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserProfile
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.PastelCyanCard
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentUser: UserProfile?,
    viewModel: SettingsViewModel = viewModel(),
    onBackClick: () -> Unit,
    onAccountActionClick: () -> Unit,
    isAccountBusy: Boolean = false,
    accountErrorMessage: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val family by viewModel.family.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isLocal = currentUser?.id?.startsWith("local_") != false

    var backupJsonToSave by remember { mutableStateOf("") }
    var lastBackupUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showBackupSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf("") }
    var showRestoreSuccessDialog by remember { mutableStateOf(false) }
    var showEditFamilyNameDialog by remember { mutableStateOf(false) }
    var familyNameInput by remember { mutableStateOf(family.familyName) }

    fun shareBackupFile(uri: android.net.Uri) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "مشاركة النسخة الاحتياطية"))
        } catch (e: Exception) {
            errorMessage = "فشل مشاركة ملف النسخة الاحتياطية: ${e.localizedMessage}"
            showErrorDialog = true
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(backupJsonToSave.toByteArray()) }
                    lastBackupUri = uri
                    showBackupSuccessDialog = true
                } catch (e: Exception) {
                    errorMessage = "فشل حفظ ملف النسخة الاحتياطية: ${e.localizedMessage}"
                    showErrorDialog = true
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().use { it.readText() }
                    }.orEmpty()
                    if (viewModel.validateBackupJson(json)) {
                        pendingRestoreJson = json
                        showConfirmRestoreDialog = true
                    } else {
                        errorMessage = "ملف النسخة الاحتياطية غير صالح أو غير متوافق."
                        showErrorDialog = true
                    }
                } catch (e: Exception) {
                    errorMessage = "فشل قراءة ملف النسخة الاحتياطية: ${e.localizedMessage}"
                    showErrorDialog = true
                }
            }
        }
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotificationPermission = granted
        if (granted) viewModel.updateSettings(settings.copy(notificationsEnabled = true))
    }

    if (showEditFamilyNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditFamilyNameDialog = false },
            title = { Text("تعديل اسم الأسرة", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = familyNameInput,
                    onValueChange = { familyNameInput = it },
                    label = { Text("اسم الأسرة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_family_name_input")
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (familyNameInput.isNotBlank()) {
                        viewModel.updateFamilyName(familyNameInput)
                        showEditFamilyNameDialog = false
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showEditFamilyNameDialog = false }) { Text("إلغاء") } }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).testTag("settings_screen"),
        color = Color(0xFFF7FBFB)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TealDark)
                }
                Spacer(Modifier.width(8.dp))
                Text("الإعدادات العامة", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TealDark))
            }

            Spacer(Modifier.height(12.dp))
            SectionTitle("الحفظ والمزامنة ☁️")
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(54.dp), shape = CircleShape, color = PastelCyanCard) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(36.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isLocal) "بياناتك على هذا الجهاز" else currentUser?.displayName ?: "حساب Google",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                if (isLocal) "Google غير مربوط — يمكنك المزامنة في أي وقت" else currentUser?.email.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        SyncStatusBadge(syncStatus)
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (isLocal)
                            "البيانات الحالية هي الأصل على جهازك. عند ربط Google سيتم دمجها بأمان مع بياناتك السحابية ولن تُعامل كحساب منفصل."
                        else
                            "Google مرتبط للمزامنة. ستظهر نفس البيانات على أجهزتك المرتبطة بهذا الحساب. إيقاف المزامنة لا يحذف بيانات الجهاز ولا النسخة الموجودة على Google.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )

                    if (!accountErrorMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(accountErrorMessage, color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onAccountActionClick,
                        enabled = !isAccountBusy,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("google_sync_action_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLocal) TealPrimary else Color(0xFFFFF3E0),
                            contentColor = if (isLocal) Color.White else Color(0xFFE65100)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isAccountBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (isLocal) "ربط بحساب Google للمزامنة" else "إيقاف مزامنة Google",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("إدارة الأسرة 👨‍👩‍👧‍👦")
            Spacer(Modifier.height(8.dp))
            SettingItemCard(
                icon = Icons.Default.FamilyRestroom,
                title = "اسم الأسرة",
                subtitle = family.familyName,
                trailing = {
                    IconButton(onClick = {
                        familyNameInput = family.familyName
                        showEditFamilyNameDialog = true
                    }) { Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = TealPrimary) }
                }
            )

            Spacer(Modifier.height(20.dp))
            SectionTitle("التفضيلات والمظهر 🎨")
            Spacer(Modifier.height(8.dp))
            SettingItemCard(
                icon = Icons.Default.Notifications,
                title = "الإشعارات والتنبيهات المحليّة",
                subtitle = if (hasNotificationPermission) "إشعارات التذكير مفعلة" else "انقر للتفعيل",
                trailing = {
                    Switch(
                        checked = settings.notificationsEnabled && hasNotificationPermission,
                        onCheckedChange = { checked ->
                            if (checked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else viewModel.updateSettings(settings.copy(notificationsEnabled = checked))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary)
                    )
                }
            )
            Spacer(Modifier.height(8.dp))
            SettingItemCard(Icons.Default.Language, "اللغة (Language)", "العربية") { Text("افتراضي", color = TealDark) }
            Spacer(Modifier.height(8.dp))
            SettingItemCard(Icons.Default.Palette, "المظهر (Theme)", "طبي هادئ") { Text("Pastel", color = Color(0xFF0288D1)) }

            Spacer(Modifier.height(20.dp))
            SectionTitle("💾 النسخ الاحتياطي والاستعادة")
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().testTag("backup_restore_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (isLocal)
                            "النسخة الاحتياطية اليدوية تُحفظ على جهازك."
                        else
                            "النسخة الاحتياطية اليدوية تعمل بجانب مزامنة Google. عند الاستعادة سيتم دمج البيانات المستعادة مع حساب Google المرتبط.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    backupJsonToSave = viewModel.createBackupJson()
                                    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US).format(java.util.Date())
                                    createDocumentLauncher.launch("Mawaeedna_Backup_$stamp.mwbackup")
                                }
                            },
                            modifier = Modifier.weight(1f).height(46.dp).testTag("create_backup_button")
                        ) { Text("نسخ احتياطي", fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f).height(46.dp).testTag("restore_backup_button"),
                            border = BorderStroke(1.dp, TealPrimary)
                        ) { Text("استعادة البيانات", fontWeight = FontWeight.Bold) }
                    }
                    if (lastBackupUri != null) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { lastBackupUri?.let(::shareBackupFile) }, modifier = Modifier.fillMaxWidth()) {
                            Text("مشاركة ملف النسخة الأخيرة 📤")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PastelCyanCard)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TealPrimary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("مواعيدنا - الإصدار 2.0", fontWeight = FontWeight.Bold, color = TealDark)
                        Text("بيانات محلية أولاً، مع مزامنة Google اختيارية.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showBackupSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showBackupSuccessDialog = false },
            title = { Text("تم إنشاء النسخة الاحتياطية بنجاح 💾", fontWeight = FontWeight.Bold) },
            text = { Text("تم حفظ ملف النسخة الاحتياطية بأمان على جهازك.") },
            confirmButton = {
                Button(onClick = {
                    showBackupSuccessDialog = false
                    lastBackupUri?.let(::shareBackupFile)
                }) { Text("مشاركة النسخة 📤") }
            },
            dismissButton = { TextButton(onClick = { showBackupSuccessDialog = false }) { Text("إغلاق") } }
        )
    }

    if (showConfirmRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmRestoreDialog = false },
            title = { Text("استعادة النسخة الاحتياطية ⚠️", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isLocal) "سيتم استبدال بيانات الجهاز الحالية ببيانات النسخة الاحتياطية."
                    else "سيتم استعادة بيانات النسخة على الجهاز ثم دمجها بأمان مع بيانات Google المرتبطة."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showConfirmRestoreDialog = false
                    scope.launch {
                        if (viewModel.restoreBackup(pendingRestoreJson)) showRestoreSuccessDialog = true
                        else {
                            errorMessage = "حدث خطأ أثناء استعادة أو مزامنة النسخة الاحتياطية."
                            showErrorDialog = true
                        }
                    }
                }) { Text("استعادة") }
            },
            dismissButton = { TextButton(onClick = { showConfirmRestoreDialog = false }) { Text("إلغاء") } }
        )
    }

    if (showRestoreSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreSuccessDialog = false },
            title = { Text("تمت الاستعادة بنجاح 🎉", fontWeight = FontWeight.Bold) },
            text = { Text(if (isLocal) "تمت استعادة البيانات على الجهاز." else "تمت استعادة البيانات ودمجها مع Google.") },
            confirmButton = { Button(onClick = { showRestoreSuccessDialog = false }) { Text("حسناً") } }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("تنبيه ❌", fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage) },
            confirmButton = { Button(onClick = { showErrorDialog = false }) { Text("حسناً") } }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)))
}

@Composable
private fun SettingItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF1F5F9)) {
                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = TealPrimary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
            }
            trailing()
        }
    }
}
