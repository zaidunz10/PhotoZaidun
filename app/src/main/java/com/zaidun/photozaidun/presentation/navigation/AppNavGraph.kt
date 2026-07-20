package com.zaidun.photozaidun.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zaidun.photozaidun.data.processor.export.ExportLoadingScreen
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
        startDestination = Screen.Splash.route,

        enterTransition = {
            fadeIn(
                animationSpec = tween(250)
            ) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(250)
            )
        },

        exitTransition = {
            fadeOut(
                animationSpec = tween(200)
            )
        },

        popEnterTransition = {
            fadeIn(
                animationSpec = tween(200)
            )
        },

        popExitTransition = {
            fadeOut(
                animationSpec = tween(150)
            )
        }
    ) {
        composable(Screen.Splash.route) {
            SplashRoute(navController)
        }

        // Host Utama V2: MainScreen dengan Bottom Navbar
        composable("main") {

            MainScreen(
                onNavigateToExportLoading = {
                    navController.navigate(Screen.ExportLoading.route)
                },
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
        composable(Screen.ExportLoading.route) {
            ExportLoadingScreen(
                viewModel = homeViewModel,
                navController = navController
            )
        }
    }
}
