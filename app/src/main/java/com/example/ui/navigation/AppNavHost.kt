package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.AuthState
import com.example.ui.screens.child.ChildProfileScreen
import com.example.ui.screens.child.sections.AppointmentsSectionScreen
import com.example.ui.screens.child.sections.GlucoseSectionScreen
import com.example.ui.screens.child.sections.LabResultsSectionScreen
import com.example.ui.screens.child.sections.TestsSectionScreen
import com.example.ui.screens.home.FamilyHomeScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.welcome.WelcomeScreen
import com.example.ui.viewmodels.AuthViewModel
import com.example.ui.viewmodels.ChildDetailViewModel
import com.example.ui.viewmodels.FamilyViewModel
import com.example.ui.viewmodels.SettingsViewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    familyViewModel: FamilyViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH
    ) {
        // 1. Splash Screen
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                authViewModel = authViewModel,
                onNavigateNext = { isLoggedIn ->
                    val destination = if (isLoggedIn) NavRoutes.HOME else NavRoutes.WELCOME
                    navController.navigate(destination) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // 2. Welcome / Login Screen
        composable(NavRoutes.WELCOME) {
            val authState by authViewModel.authState.collectAsState()
            val context = LocalContext.current
            val isLoading = authState is AuthState.Authenticating
            val errorMessage = (authState as? AuthState.Error)?.message

            WelcomeScreen(
                onGoogleSignInClick = {
                    authViewModel.signInWithGoogle(context) {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.WELCOME) { inclusive = true }
                        }
                    }
                },
                onContinueWithoutAccountClick = {
                    authViewModel.continueAsLocalUser(context) {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.WELCOME) { inclusive = true }
                        }
                    }
                },
                isLoading = isLoading,
                errorMessage = errorMessage
            )
        }

        // 3. Family Home Screen
        composable(NavRoutes.HOME) {
            FamilyHomeScreen(
                viewModel = familyViewModel,
                onChildClick = { childId ->
                    navController.navigate(NavRoutes.childProfile(childId))
                },
                onSettingsClick = {
                    navController.navigate(NavRoutes.SETTINGS)
                }
            )
        }

        // 4. Child Profile Screen
        composable(
            route = NavRoutes.CHILD_PROFILE,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val childDetailViewModel: ChildDetailViewModel = viewModel(
                key = childId,
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return ChildDetailViewModel(childId = childId) as T
                    }
                }
            )

            ChildProfileScreen(
                childId = childId,
                viewModel = childDetailViewModel,
                onBackClick = { navController.popBackStack() },
                onOpenAppointments = { navController.navigate(NavRoutes.childAppointments(childId)) },
                onOpenTests = { navController.navigate(NavRoutes.childTests(childId)) },
                onOpenLabResults = { navController.navigate(NavRoutes.childLabResults(childId)) },
                onOpenGlucose = { navController.navigate(NavRoutes.childGlucose(childId)) }
            )
        }

        // 4a. Child Appointments Section
        composable(
            route = NavRoutes.CHILD_APPOINTMENTS,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val childDetailViewModel: ChildDetailViewModel = viewModel(
                key = childId,
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return ChildDetailViewModel(childId = childId) as T
                    }
                }
            )

            AppointmentsSectionScreen(
                viewModel = childDetailViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 4b. Child Tests Section
        composable(
            route = NavRoutes.CHILD_TESTS,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val childDetailViewModel: ChildDetailViewModel = viewModel(
                key = childId,
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return ChildDetailViewModel(childId = childId) as T
                    }
                }
            )

            TestsSectionScreen(
                viewModel = childDetailViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 4c. Child Lab Results Section
        composable(
            route = NavRoutes.CHILD_LAB_RESULTS,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val childDetailViewModel: ChildDetailViewModel = viewModel(
                key = childId,
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return ChildDetailViewModel(childId = childId) as T
                    }
                }
            )

            LabResultsSectionScreen(
                viewModel = childDetailViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 4d. Child Glucose Section
        composable(
            route = NavRoutes.CHILD_GLUCOSE,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val childDetailViewModel: ChildDetailViewModel = viewModel(
                key = childId,
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return ChildDetailViewModel(childId = childId) as T
                    }
                }
            )

            GlucoseSectionScreen(
                viewModel = childDetailViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 5. Settings Screen
        composable(NavRoutes.SETTINGS) {
            val currentUser by authViewModel.currentUser.collectAsState()
            val context = LocalContext.current

            SettingsScreen(
                currentUser = currentUser,
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() },
                onSignOutClick = {
                    authViewModel.signOut(context) {
                        navController.navigate(NavRoutes.WELCOME) {
                            popUpTo(NavRoutes.HOME) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
