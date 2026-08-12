package com.example.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Gender
import com.example.ui.theme.TealPrimary
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

@Composable
fun AddChildDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, birthDate: String, ageText: String, gender: Gender) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("15 مارس 2021") }
    var ageText by remember { mutableStateOf("3 سنوات") }
    var gender by remember { mutableStateOf(Gender.BOY) }

    val context = LocalContext.current
    val calendar = remember {
        Calendar.getInstance().apply {
            set(2021, Calendar.MARCH, 15)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة فرد جديد للأسرة",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم") },
                    placeholder = { Text("مثال: يوسف أحمد") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_child_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    birthDate = SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(calendar.time)
                                    
                                    // Calculate age automatically
                                    val today = Calendar.getInstance()
                                    var yearsDiff = today.get(Calendar.YEAR) - year
                                    var monthsDiff = today.get(Calendar.MONTH) - month
                                    if (today.get(Calendar.DAY_OF_MONTH) < dayOfMonth) {
                                        monthsDiff--
                                    }
                                    if (monthsDiff < 0) {
                                        yearsDiff--
                                        monthsDiff += 12
                                    }
                                    val ageStr = if (yearsDiff <= 0) {
                                        if (monthsDiff <= 0) "أقل من شهر" else "$monthsDiff أشهر"
                                    } else {
                                        if (yearsDiff == 1) "سنة واحدة"
                                        else if (yearsDiff == 2) "سنتان"
                                        else if (yearsDiff in 3..10) "$yearsDiff سنوات"
                                        else "$yearsDiff سنة"
                                    }
                                    ageText = ageStr
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("تاريخ الميلاد") },
                        placeholder = { Text("مثال: 12 مايو 2020") },
                        singleLine = true,
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
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it },
                    label = { Text("العمر") },
                    placeholder = { Text("مثال: 4 سنوات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "الجنس:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                )

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = (gender == Gender.BOY),
                        onClick = { gender = Gender.BOY },
                        label = { Text("ولد 👦") },
                        shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FilterChip(
                        selected = (gender == Gender.GIRL),
                        onClick = { gender = Gender.GIRL },
                        label = { Text("بنت 👧") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, birthDate, ageText, gender)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_add_child_button")
            ) {
                Text("حفظ والإضافة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF64748B))
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}
