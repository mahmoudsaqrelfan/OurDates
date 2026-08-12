# مواعيدنا --- PROJECT MASTER DOCUMENT

## المصدر الأساسي للحقيقة (Single Source of Truth)

> هذه الوثيقة هي المرجع الرئيسي للمشروع. أي AI Agent يعمل على المشروع
> يجب أن يقرأها قبل تعديل الكود.
>
> **قاعدة أساسية:** لا تعتبر تقارير AI السابقة دليلًا كافيًا على أن الميزة
> تعمل. الحالة المعتمدة هي ما تؤكده الشيفرة والاختبارات والفحص العملي.

------------------------------------------------------------------------

## 1. هوية المشروع

-   اسم التطبيق: **مواعيدنا**
-   Repository: **OurDates**
-   Branch الأساسي: `main`
-   التطبيق Android مبني بـ Kotlin وJetpack Compose.
-   الوظائف الأساسية: الأسرة والأطفال، المواعيد، التنبيهات، الفحوصات،
    التحاليل، قياسات السكر، النسخ الاحتياطي والاستعادة، تقارير PDF،
    Firebase Authentication/Firestore.

------------------------------------------------------------------------

## 2. قواعد إلزامية للـAI Agent

1.  اقرأ هذه الوثيقة أولًا.
2.  لا تعِد بناء المشروع من الصفر.
3.  لا تحذف ميزات موجودة.
4.  لا تغيّر المعمارية إلا عند الضرورة.
5.  لا تنتقل إلى مرحلة جديدة قبل إغلاق الـcritical blockers للمرحلة
    الحالية.
6.  لا تعتمد على تقرير AI السابق دون التحقق من الكود/الاختبارات.
7.  عند تعارض التقرير مع الكود، افحص الكود والاختبارات وأبلغ عن التعارض.
8.  لا تضف خدمات مدفوعة أو Dependencies جديدة إلا عند الضرورة.
9.  حافظ على العربية وRTL.
10. حافظ على البيانات وعدم فقدانها.
11. نفّذ أصغر تغيير يحقق المطلوب.
12. بعد الإصلاح افحص الملفات المتأثرة وشغّل الاختبار/البناء المناسب إذا
    كانت البيئة تسمح.
13. لا تعمل Git commit/push إلا إذا طُلب ذلك.
14. لا تنفذ عمليات Git تدميرية.
15. لا تضع Firebase secrets أو API credentials السرية داخل Git.
16. لا تنفذ مرحلة مستقبلية لمجرد أن هناك ميزة تبدو مفيدة؛ اتبع خطة الـ14
    مرحلة.

------------------------------------------------------------------------

## 3. الحالة الحالية

### Phase 8 --- Backup / Restore

تم تنفيذها حسب التقرير السابق: - JSON backup محلي. - Restore. - Android
SAF. - CreateDocument/OpenDocument. - تأكيد قبل الاستبدال. - الأسرة
والأطفال والمواعيد والفحوصات والنتائج وقياسات السكر والإعدادات.

### Phase 9 --- PDF Medical Reports

تم تنفيذها حسب التقرير: - `PdfReportHelper.kt` -
`generateLabResultsPdf()` - `generateGlucosePdf()` - PDF عربي وRTL. -
جداول منظمة. - إحصائيات السكر. - FileProvider ومشاركة آمنة. -
`file_paths.xml`. - دمج التصدير في `LabResultsSectionScreen`
و`GlucoseSectionScreen`.

**ملاحظة:** يجب التحقق الفعلي قبل اعتبارها معتمدة نهائيًا.

### Phase 10 --- Notification Reliability

آخر commit معروف: `5042c35`
`Phase 9 & 10: Implement PDF reports and fix notification system`

تم الإعلان عن: - إصلاح ظهور اسم الطفل الحقيقي بدل النص العام. - إضافة
`getChildrenNamesMap()`. - تحديث `attachUser()`. - تحديث Firestore
listener. - تحديث `addAppointment()`. - تحديث `restoreAppointments()`. -
إلغاء alarms القديمة قبل جدولة الجديدة عند تعديل الموعد. - التحقق من
إعادة الجدولة بعد reboot. - تجاوز reminders التي أصبح وقتها في الماضي.

النواقص التي ذكرها التقرير: - Custom Reminder Time UI لم يُنفذ. -
Offline/Online duplicate prevention لم يُنفذ. - Unit tests الإضافية لم
تُنفذ بسبب البيئة.

**لا تفترض أن هذه البنود كلها مطلوبة في Phase 10؛ ارجع لمتطلبات المرحلة
قبل تنفيذها.**

------------------------------------------------------------------------

# 4. Critical Blocker --- Google Sign-In + Firebase

**هذه المشكلة لم تُعتمد على أنها محلولة.**

الفحوص السابقة أظهرت: - عدم وجود `google-services.json` في النسخة
المفحوصة. - وجود Firebase fallback داخل `MawaeednaApplication.kt`. -
ظهرت قيم تجريبية/Placeholder، منها: - Project ID: `aistudio-mawaeedna` -
Android App ID: `1:100000000000:android:aistudio123456` - API key dummy
حسب التقرير السابق.

لذلك لا تعتبر Google Sign-In أو Firebase Authentication مكتملين حتى يثبت
العكس عمليًا.

يجب التحقق من: 1. Firebase initialization. 2. Firebase Authentication.
3. Google provider. 4. Android OAuth configuration. 5.
application/package ID. 6. SHA-1 وSHA-256. 7. `google-services.json`. 8.
Firebase project الحقيقي. 9. Firestore. 10. ربط المستخدم بالعائلة
والأطفال. 11. Local/Offline/Online sync.

**لا تستخدم placeholder أو dummy configuration.**

------------------------------------------------------------------------

# 5. Firestore / Synchronization

المكونات المعروفة: - `AppointmentRepository.kt` -
`FirestoreAppointmentRepository.kt` - `FamilyRepository.kt` -
`FirestoreFamilyRepository.kt` - `HealthRecordsRepository.kt` -
`BackupRepository.kt` - `SettingsRepository.kt` - `SyncStatus.kt` -
`SyncStatusBadge.kt`

يجب إثبات المسار:

`Google Login → Firebase Auth → User → Family → Children → Firestore → Local data → Sync`

### Online

يجب أن تعمل البيانات مع Firestore.

### Offline

يجب أن يعمل التطبيق بالبيانات المحلية دون فقدان البيانات.

### Reconnect

عند عودة الاتصال: - تتم المزامنة. - لا تتكرر البيانات. - لا تتكرر
التنبيهات. - لا يتم استبدال البيانات الأحدث ببيانات أقدم بصورة خاطئة.

------------------------------------------------------------------------

# 6. الملفات الرئيسية المعروفة

### Application

`app/src/main/java/com/example/MawaeednaApplication.kt`

### Authentication

`app/src/main/java/com/example/data/repository/AuthRepository.kt`
`app/src/main/java/com/example/ui/viewmodels/AuthViewModel.kt`

### DI

`app/src/main/java/com/example/di/AppContainer.kt`

### Appointments / Notifications

`Appointment.kt` `NotificationReminder.kt` `AppointmentRepository.kt`
`FirestoreAppointmentRepository.kt` `AppointmentNotificationManager.kt`
`AppointmentNotificationReceiver.kt` `AppointmentsSectionScreen.kt`

### Family / Children

`FamilyRepository.kt` `FirestoreFamilyRepository.kt`
`FamilyViewModel.kt` `ChildDetailViewModel.kt` `AddChildDialog.kt`
`EditChildDialog.kt`

### Health

`HealthRecordsRepository.kt` `GlucoseReading.kt` `TestDefinition.kt`
`TestAppointment.kt` `TestResult.kt` `LabResultsSectionScreen.kt`
`GlucoseSectionScreen.kt` `TestsSectionScreen.kt`

### Backup

`BackupRepository.kt`

### PDF

`PdfReportHelper.kt` `file_paths.xml`

### Settings

`SettingsRepository.kt` `SettingsViewModel.kt` `SettingsScreen.kt`

------------------------------------------------------------------------

# 7. خطة المشروع --- 14 مرحلة

## Phase 1 --- Foundation

Android/Kotlin/Compose، Navigation، Theme، Models، Repositories، Local
foundation.

## Phase 2 --- Authentication / Family Identity

Google Sign-In، Firebase Auth، User identity، Family، Children،
Login/logout.

**الحالة:** تحتاج تحقق/إصلاح بسبب Google Sign-In/Firebase.

## Phase 3 --- Firestore / Synchronization

Firestore، User/Family data، Children، Offline/Online، Sync، Data
consistency.

**الحالة:** تحتاج تحقق واعتماد.

## Phase 4 --- Appointments / Notifications

إنشاء وتعديل المواعيد، reminders، notifications، cancel/reschedule،
reboot recovery.

**الحالة:** تم إصلاح أجزاء مهمة، وتحتاج regression verification.

## Phase 5 --- Tests / Medical Appointments

Test definitions، test appointments، DatePicker-only dates.

**الحالة:** تم تطويرها ضمن المراحل السابقة حسب التقارير.

## Phase 6 --- Lab Results

Results، filtering، date ranges، notes، graph/report support.

**الحالة:** تم تطويرها حسب التقارير.

## Phase 7 --- Glucose

Measurements، DatePicker، TimePicker، filtering، statistics، graphs.

**الحالة:** تم تطويرها حسب التقارير.

## Phase 8 --- Backup / Restore

JSON، SAF، Restore، confirmation، integrity.

**الحالة:** منفذة حسب التقرير.

## Phase 9 --- PDF Reports

Lab PDF، Glucose PDF، Arabic/RTL، tables، statistics، sharing.

**الحالة:** منفذة حسب التقرير، مع ضرورة التحقق النهائي.

## Phase 10 --- Notification Reliability

Child names، update/cancel، reboot rescheduling، past reminders.

**الحالة:** تم تنفيذ إصلاحات وتحتاج regression verification.

## Phase 11 --- Data / Sync Reliability

Local vs Firestore consistency، duplicate prevention، offline/online
transitions، conflict handling، data integrity، sync recovery.

**الحالة:** لم تبدأ رسميًا.

## Phase 12 --- UI/UX / Accessibility / Polish

RTL، consistency، accessibility، empty/loading/error states، dialogs،
visual polish.

**الحالة:** لم تبدأ رسميًا.

## Phase 13 --- Full QA / Regression

Auth، family، children، appointments، notifications، tests، lab،
glucose، backup، PDF، offline/online، reconnect، reboot، multi-device.

**الحالة:** لم تبدأ رسميًا.

## Phase 14 --- Release Readiness

Final build، release config، versioning، icon/name، permissions،
security/privacy review، final regression، APK/AAB.

**الحالة:** لم تبدأ رسميًا.

------------------------------------------------------------------------

# 8. الترتيب الصحيح من الآن

**لا تبدأ Phase 11 مباشرة.**

الترتيب:

### Step A --- Checkpoint

فحص فعلي للمراحل 1--10.

### Step B --- Firebase/Auth

إغلاق Google Sign-In + Firebase blocker.

### Step C --- Firestore/Sync

إثبات Auth → User → Family → Firestore → Local → Sync.

### Step D --- Notification Regression

اختبار إصلاحات Phase 10.

### Step E --- PDF Regression

التحقق من Phase 9.

### Step F --- اعتماد Phases 1--10

بعد إغلاق الـcritical blockers.

### Step G --- Phase 11

بدء Data/Sync Reliability.

------------------------------------------------------------------------

# 9. Firebase Rules

لا تفترض وجود Firebase project صحيح.

يجب تحديد المشروع الحقيقي ثم التحقق من: - Project ID. - Android App. -
Package name. - SHA-1. - SHA-256. - Google provider. -
`google-services.json`. - Firestore configuration.

لا تستخدم: - placeholder IDs. - dummy keys. - fake App IDs. - إعدادات
تجريبية من Google AI Studio.

لا تضع credentials سرية في Git.

------------------------------------------------------------------------

# 10. Build Environment

المشروع نُقل من Google AI Studio إلى GitHub/Codespaces.

Repository: `OurDates`

Commit استعادة الملفات: `fc9fd8e`

Commit لاحق: `5042c35`

في Windows كانت البيئة ناقصة في البداية: - Java غير متاحة. - Gradle
wrapper مفقود. - Gradle command مفقود.

تم تثبيت Eclipse Temurin JDK 17.0.20 على Windows.

العمل الحالي في Codespaces.

**تحقق من بيئة البناء عند الحاجة ولا تفترض أنها جاهزة.**

------------------------------------------------------------------------

# 11. Git Rules

Repository: `OurDates`

Branch: `main`

قبل التعديل:

``` bash
git status
```

بعد التعديل:

``` bash
git diff
```

لا تستخدم:

``` bash
git reset --hard
```

أو أي عملية تدميرية دون موافقة.

لا تعمل commit/push إلا عند الطلب.

------------------------------------------------------------------------

# 12. Definition of Done

المرحلة لا تعتبر مكتملة لمجرد أن: - الكود موجود. - AI قال complete. -
syntax صحيح.

يجب أن: 1. تكون الوظيفة موجودة ومتصلة بالواجهة. 2. لا توجد placeholders
حرجة. 3. لا توجد blockers معروفة. 4. ينجح build عندما تكون البيئة متاحة.
5. تنجح الاختبارات المناسبة. 6. يتم التحقق العملي متى أمكن. 7. لا يحدث
فقد للبيانات. 8. لا تعتمد المرحلة التالية على وظيفة غير مثبتة.

------------------------------------------------------------------------

# 13. أسلوب عمل Cline / AI Agent

قبل أي مهمة: 1. اقرأ `PROJECT_MASTER_DOCUMENT.md`. 2. حدد المرحلة
الحالية. 3. اقرأ الملفات المرتبطة بالمهمة فقط. 4. لا تعِد فحص المشروع
بالكامل بلا سبب. 5. نفذ أصغر تغيير ممكن. 6. اختبر. 7. اذكر الملفات
والتغييرات ونتيجة الاختبار والمشاكل المتبقية. 8. توقف عند نهاية المهمة
وانتظر التعليمات.

------------------------------------------------------------------------

# 14. Current Mission

**المهمة الحالية ليست Phase 11.**

المطلوب الآن:

> إغلاق Checkpoint للمراحل 1--10، ثم إصلاح Google Sign-In/Firebase، ثم
> التحقق من Firestore/Sync، ثم regression للمرحلة 10، ثم اعتماد الانتقال
> إلى Phase 11.

------------------------------------------------------------------------

# 15. Known Open Issues

## Critical

-   Google Sign-In/Firebase غير معتمد.
-   Firebase project/configuration الحقيقية تحتاج تحقق.
-   Firestore synchronization تحتاج تحقق عملي.

## Important

-   Offline/Online duplicate prevention يحتاج تقييم.
-   Notification regression يحتاج تحقق.

## Deferred

-   Custom Reminder Time UI: لا ينفذ إلا إذا كان ضمن المتطلبات الرسمية.
-   Unit tests إضافية: تنفذ بعد تحديد المطلوب وعندما تسمح البيئة.

------------------------------------------------------------------------

# 16. Updating This Document

بعد كل مرحلة: - لا تمسح التاريخ. - حدّث حالة المرحلة. - أضف المشاكل
المفتوحة. - أضف commit/reference عند توفره. - لا تعلن Complete إلا وفق
Definition of Done.

إذا اكتشف AI Agent اختلافًا بين هذه الوثيقة والحالة الفعلية للكود، يجب أن
يبلغ عن الاختلاف قبل تنفيذ تغييرات واسعة.

------------------------------------------------------------------------

# END OF PROJECT MASTER DOCUMENT
