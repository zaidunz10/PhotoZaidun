package com.zaidun.photozaidun.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zaidun.photozaidun.presentation.screen.folderpicker.FolderPickerScreen
import com.zaidun.photozaidun.presentation.screen.history.HistoryScreen
import com.zaidun.photozaidun.presentation.screen.home.HomeViewModel
import com.zaidun.photozaidun.presentation.screen.splash.SplashRoute
import com.zaidun.photozaidun.presentation.shared.SharedFolderViewModel

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val sharedFolderViewModel: SharedFolderViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashRoute(navController)
        }

        // Host Utama V2: MainScreen dengan Bottom Navbar
        composable("main") {

            MainScreen(
                homeViewModel = homeViewModel,
                sharedFolderViewModel = sharedFolderViewModel,
                onNavigateToFolderPicker = {
                    navController.navigate(Screen.FolderPicker.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }

        composable(Screen.FolderPicker.route) {
            FolderPickerScreen(
                viewModel = hiltViewModel(),
                sharedFolderViewModel = sharedFolderViewModel,
                onFolderSelected = { uri, total ->
                    homeViewModel.updateSelectedFolder(uri.toString(), total)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
