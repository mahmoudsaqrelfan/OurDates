package com.example.data.model

data class TestDefinition(
    val id: String,
    val name: String,
    val category: String,
    val normalRangeText: String = "",
    val unit: String = "",
    val description: String = ""
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            "تحاليل السكر الشائعة للأطفال",
            "تحاليل الغدة الدرقية",
            "تحاليل الفيتامينات والمعادن",
            "تحاليل الفحوصات العامة"
        )

        val DEFAULT_DEFINITIONS = listOf(
            // Sugar Tests
            TestDefinition("sugar_hba1c", "تحليل السكر التراكمي (HbA1c)", "تحاليل السكر الشائعة للأطفال", "", "%", "متابعة معدل السكر في الدم على مدى الـ 2-3 أشهر الماضية."),
            TestDefinition("sugar_rbs", "مستوى السكر العشوائي (RBS)", "تحاليل السكر الشائعة للأطفال", "", "mg/dL", "قياس عشوائي لمستوى السكر في أي وقت من اليوم."),
            TestDefinition("sugar_fbs", "سكر صائم (FBS)", "تحاليل السكر الشائعة للأطفال", "", "mg/dL", "قياس مستوى السكر بعد صيام 8 ساعات على الأقل."),
            TestDefinition("sugar_ogtt", "اختبار تحمل الجلوكوز (OGTT)", "تحاليل السكر الشائعة للأطفال", "", "mg/dL", "مراقبة قدرة الجسم على معالجة السكر بعد شرب محلول جلوكوز مخصص."),
            
            // Thyroid Tests
            TestDefinition("thyroid_tsh", "تحليل هرمون الغدة الدرقية (TSH)", "تحاليل الغدة الدرقية", "", "uIU/mL", "الفحص الأساسي لتقييم نشاط الغدة الدرقية."),
            TestDefinition("thyroid_ft4", "تحليل الثايروكسين الحر (Free T4)", "تحاليل الغدة الدرقية", "", "ng/dL", "قياس مستوى هرمون T4 النشط في الدم."),
            TestDefinition("thyroid_ft3", "تحليل ثلاثي اليودوثيرونين الحر (Free T3)", "تحاليل الغدة الدرقية", "", "pg/mL", "متابعة مستوى هرمون T3 النشط لتأكيد التشخيص."),

            // Vitamins & Minerals
            TestDefinition("vitamin_d", "تحليل فيتامين د (Vitamin D)", "تحاليل الفيتامينات والمعادن", "", "ng/mL", "فحص أساسي للتأكد من سلامة العظام والنمو السليم."),
            TestDefinition("iron", "تحليل الحديد في الدم (Iron)", "تحاليل الفيتامينات والمعادن", "", "ug/dL", "قياس كمية الحديد المتوفرة في مجرى الدم."),
            TestDefinition("ferritin", "تحليل مخزون الحديد (Ferritin)", "تحاليل الفيتامينات والمعادن", "", "ng/mL", "تقييم احتياطي الحديد المخزن في الجسم."),
            TestDefinition("calcium", "تحليل الكالسيوم (Calcium)", "تحاليل الفيتامينات والمعادن", "", "mg/dL", "فحص هام لصحة العظام والأسنان ووظائف الأعصاب."),

            // General Tests
            TestDefinition("general_cbc", "صورة الدم الكاملة (CBC)", "تحاليل الفحوصات العامة", "", "", "فحص شامل لخلايا الدم الحمراء والبيضاء والصفائح للكشف عن فقر الدم أو الالتهابات."),
            TestDefinition("general_kidney", "وظائف الكلى (Kidney Functions)", "تحاليل الفحوصات العامة", "", "", "يشمل اليوريا والكرياتينين لتقييم سلامة وأداء الكليتين."),
            TestDefinition("general_liver", "وظائف الكبد (Liver Functions)", "تحاليل الفحوصات العامة", "", "", "مجموعة اختبارات لقياس إنزيمات الكبد والبروتينات لتقييم سلامته.")
        )
    }
}
