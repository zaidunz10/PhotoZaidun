package com.zaidun.photozaidun.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zaidun.photozaidun.presentation.screen.folderpicker.FolderPickerScreen
import com.zaidun.photozaidun.presentation.screen.history.HistoryScreen
import com.zaidun.photozaidun.presentation.screen.home.HomeScreen
import com.zaidun.photozaidun.presentation.screen.home.HomeViewModel
import com.zaidun.photozaidun.presentation.screen.settings.SettingsScreen
import com.zaidun.photozaidun.presentation.screen.splash.SplashRoute
import com.zaidun.photozaidun.presentation.screen.watermarksettings.WatermarkSettingsScreen
import com.zaidun.photozaidun.presentation.shared.SharedFolderViewModel

@Composable
fun AppNavGraph() {

    val navController = rememberNavController()

    val sharedFolderViewModel: SharedFolderViewModel = hiltViewModel()

    val homeViewModel: HomeViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        composable(Screen.Splash.route) {

            SplashRoute(navController)

        }

        composable(Screen.Home.route) {

            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToFolderPicker = {
                    navController.navigate(Screen.FolderPicker.route)
                },
                onNavigateToWatermark = {
                    navController.navigate(Screen.Watermark.route)
                }
            )

        }

        composable(Screen.FolderPicker.route) {

            FolderPickerScreen(
                viewModel = hiltViewModel(),
                sharedFolderViewModel = sharedFolderViewModel,
                onFolderSelected = { uri, total ->

                    homeViewModel.updateSelectedFolder(
                        uri.toString(),
                        total
                    )

                },
                onBack = {
                    navController.popBackStack()
                }
            )

        }

        composable(Screen.History.route) {

            HistoryScreen(

                onBack = {

                    navController.popBackStack()

                }

            )

        }

        composable(Screen.Watermark.route) {

            WatermarkSettingsScreen(

                onBack = {

                    navController.popBackStack()

                }

            )

        }

        composable(Screen.Settings.route) {

            val homeState = homeViewModel.uiState.collectAsState()

            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )

        }

    }

}