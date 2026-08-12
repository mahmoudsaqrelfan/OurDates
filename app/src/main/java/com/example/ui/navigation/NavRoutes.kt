package com.example.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val CHILD_PROFILE = "child_profile/{childId}"
    const val CHILD_APPOINTMENTS = "child_appointments/{childId}"
    const val CHILD_TESTS = "child_tests/{childId}"
    const val CHILD_LAB_RESULTS = "child_lab_results/{childId}"
    const val CHILD_GLUCOSE = "child_glucose/{childId}"
    const val SETTINGS = "settings"

    fun childProfile(childId: String) = "child_profile/$childId"
    fun childAppointments(childId: String) = "child_appointments/$childId"
    fun childTests(childId: String) = "child_tests/$childId"
    fun childLabResults(childId: String) = "child_lab_results/$childId"
    fun childGlucose(childId: String) = "child_glucose/$childId"
}
