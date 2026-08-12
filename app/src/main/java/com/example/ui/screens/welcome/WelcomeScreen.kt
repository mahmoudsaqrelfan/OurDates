package com.example.ui.screens.welcome

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PastelBlueCard
import com.example.ui.theme.PastelCyanCard
import com.example.ui.theme.PastelGreenCard
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary

@Composable
fun WelcomeScreen(
    onGoogleSignInClick: () -> Unit,
    onContinueWithoutAccountClick: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("welcome_screen"),
        color = Color(0xFFF7FBFB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                // Header Hero Graphic
                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = CircleShape,
                    color = PastelCyanCard,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FamilyRestroom,
                            contentDescription = "عائلة مواعيدنا",
                            tint = TealPrimary,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "مواعيدنا",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = TealDark
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "نظّم مواعيد وفحوصات ومتابعة أسرتك بسهولة.",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color(0xFF475569)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Feature Highlights
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FeatureItemCard(
                    icon = Icons.Default.CalendarMonth,
                    title = "مواعيد الأطباء والعيادات",
                    description = "تسجيل ومتابعة المواعيد القادمة والتذكيرات المهمة.",
                    backgroundColor = PastelCyanCard,
                    iconColor = TealPrimary
                )

                FeatureItemCard(
                    icon = Icons.Default.MedicalServices,
                    title = "الفحوصات والتحاليل الطبية",
                    description = "متابعة فحوصات المختبر ومواعيد الفحص الدوري.",
                    backgroundColor = PastelBlueCard,
                    iconColor = Color(0xFF0288D1)
                )

                FeatureItemCard(
                    icon = Icons.Default.Bloodtype,
                    title = "سجل قياسات السكر",
                    description = "تسجيل قراءات السكر اليومية وملاحظات الوجبات.",
                    backgroundColor = PastelGreenCard,
                    iconColor = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!errorMessage.isNullOrEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 1. Google Sign In Button
                Button(
                    onClick = onGoogleSignInClick,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = Color.White
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF4285F4)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "المتابعة باستخدام Google",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }

                Text(
                    text = "مزامنة البيانات واسترجاعها من أي جهاز عبر السحابة",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                )

                // 2. Continue Without Account Button (Local Mode)
                androidx.compose.material3.OutlinedButton(
                    onClick = onContinueWithoutAccountClick,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_without_account_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TealPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TealPrimary
                    )
                ) {
                    Text(
                        text = "المتابعة بدون حساب",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                Text(
                    text = "حفظ البيانات محلياً على هذا الجهاز فقط مع الاحتفاظ بها عند إغلاق التطبيق",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    backgroundColor: Color,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
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
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                )
            }
        }
    }
}
