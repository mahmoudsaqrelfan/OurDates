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
    NavHost(navController = navController, startDestination = NavRoutes.SPLASH) {
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                authViewModel = authViewModel,
                onNavigateNext = { isLoggedIn ->
                    navController.navigate(if (isLoggedIn) NavRoutes.HOME else NavRoutes.WELCOME) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.WELCOME) {
            val context = LocalContext.current
            WelcomeScreen(
                onStartClick = {
                    authViewModel.continueAsLocalUser(context) {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.WELCOME) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            FamilyHomeScreen(
                viewModel = familyViewModel,
                onChildClick = { childId -> navController.navigate(NavRoutes.childProfile(childId)) },
                onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) }
            )
        }

        composable(
            route = NavRoutes.CHILD_PROFILE,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val vm: ChildDetailViewModel = viewModel(key = childId, factory = childDetailFactory(childId))
            ChildProfileScreen(
                childId = childId,
                viewModel = vm,
                onBackClick = { navController.popBackStack() },
                onOpenAppointments = { navController.navigate(NavRoutes.childAppointments(childId)) },
                onOpenTests = { navController.navigate(NavRoutes.childTests(childId)) },
                onOpenLabResults = { navController.navigate(NavRoutes.childLabResults(childId)) },
                onOpenGlucose = { navController.navigate(NavRoutes.childGlucose(childId)) }
            )
        }

        composable(
            route = NavRoutes.CHILD_APPOINTMENTS,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val vm: ChildDetailViewModel = viewModel(key = childId, factory = childDetailFactory(childId))
            AppointmentsSectionScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
        }

        composable(
            route = NavRoutes.CHILD_TESTS,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val vm: ChildDetailViewModel = viewModel(key = childId, factory = childDetailFactory(childId))
            TestsSectionScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
        }

        composable(
            route = NavRoutes.CHILD_LAB_RESULTS,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val vm: ChildDetailViewModel = viewModel(key = childId, factory = childDetailFactory(childId))
            LabResultsSectionScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
        }

        composable(
            route = NavRoutes.CHILD_GLUCOSE,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val vm: ChildDetailViewModel = viewModel(key = childId, factory = childDetailFactory(childId))
            GlucoseSectionScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
        }

        composable(NavRoutes.SETTINGS) {
            val currentUser by authViewModel.currentUser.collectAsState()
            val authState by authViewModel.authState.collectAsState()
            val context = LocalContext.current
            val isLocal = currentUser?.id?.startsWith("local_") != false
            val busy = authState is AuthState.Authenticating
            val authError = (authState as? AuthState.Error)?.message

            SettingsScreen(
                currentUser = currentUser,
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() },
                onAccountActionClick = {
                    if (isLocal) {
                        authViewModel.linkGoogleForSync(
                            context = context,
                            onSuccess = { },
                            onError = { }
                        )
                    } else {
                        authViewModel.unlinkGoogleSync(
                            context = context,
                            onSuccess = { },
                            onError = { }
                        )
                    }
                },
                isAccountBusy = busy,
                accountErrorMessage = authError
            )
        }
    }
}

private fun childDetailFactory(childId: String) = object : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ChildDetailViewModel(childId = childId) as T
    }
}
