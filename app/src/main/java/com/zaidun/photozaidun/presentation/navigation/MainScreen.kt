package com.zaidun.photozaidun.presentation.navigation

import android.app.Activity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zaidun.photozaidun.presentation.screen.dashboard.DashboardScreen
import com.zaidun.photozaidun.presentation.screen.dashboard.DashboardViewModel
import com.zaidun.photozaidun.presentation.screen.home.HomeScreen
import com.zaidun.photozaidun.presentation.screen.home.HomeViewModel
import com.zaidun.photozaidun.presentation.screen.profile.ProfileScreen
import com.zaidun.photozaidun.presentation.screen.settings.SettingsScreen
import com.zaidun.photozaidun.presentation.screen.settings.SettingsViewModel
import com.zaidun.photozaidun.presentation.screen.watermarksettings.WatermarkSettingsScreen
import com.zaidun.photozaidun.presentation.shared.SharedFolderViewModel
import com.zaidun.photozaidun.presentation.theme.motion.MotionTokens

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    sharedFolderViewModel: SharedFolderViewModel,
    onNavigateToFolderPicker: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToExportLoading: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val items = listOf(
        Screen.Home,
        Screen.History,
        Screen.Watermark,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color(0xFF23143D),
                shadowElevation = 8.dp
            ) {
                NavigationBar(
                    containerColor = Color(0xFF23143D)
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        val labelText = when (screen) {
                            Screen.Home -> "Export"
                            Screen.History -> "Dashboard"
                            Screen.Watermark -> "Watermark"
                            Screen.Profile -> "Profil"
                            else -> "Settings"
                        }

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when(screen) {
                                        Screen.Home -> Icons.Default.FileUpload
                                        Screen.History -> Icons.Default.Dashboard
                                        Screen.Watermark -> Icons.Default.Brush
                                        Screen.Profile -> Icons.Default.Person
                                        else -> Icons.Default.Settings
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { 
                                Text(
                                    labelText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color(0xFF7B4FE0),
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color(0xFF9E8DBE),
                                selectedTextColor = Color.White,
                                unselectedTextColor = Color(0xFF9E8DBE)
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(tween(MotionTokens.DURATION_TAB_SWITCH)) +
                slideInVertically(
                    animationSpec = tween(MotionTokens.DURATION_TAB_SWITCH),
                    initialOffsetY = { it / 20 }
                )
            },
            exitTransition = { fadeOut(tween(MotionTokens.DURATION_TAB_SWITCH)) },
            popEnterTransition = {
                fadeIn(tween(MotionTokens.DURATION_TAB_SWITCH)) +
                slideInVertically(
                    animationSpec = tween(MotionTokens.DURATION_TAB_SWITCH),
                    initialOffsetY = { it / 20 }
                )
            },
            popExitTransition = { fadeOut(tween(MotionTokens.DURATION_TAB_SWITCH)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToExportLoading = onNavigateToExportLoading,
                    viewModel = homeViewModel,
                    sharedFolderViewModel = sharedFolderViewModel,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToFolderPicker = onNavigateToFolderPicker,
                    onNavigateToWatermark = { navController.navigate(Screen.Watermark.route) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@composable
                )
            }
            composable(Screen.History.route) {
                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(viewModel = dashboardViewModel)
            }
            composable(Screen.Watermark.route) {
                WatermarkSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Profile.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val settingsUiState by settingsViewModel.uiState.collectAsState()
                ProfileScreen(
                    uiState = settingsUiState,
                    onBack = { navController.popBackStack() },
                    onLoginGoogle = {
                        (context as? Activity)?.let { act ->
                            settingsViewModel.loginGoogle(act)
                        }
                    },
                    onLogoutGoogle = {
                        (context as? Activity)?.let { act ->
                            settingsViewModel.logoutGoogle(act)
                        }
                    },
                    onSaveSuccess = { wa, ig, bank, accNum, holder, priceSingle, minQty, discountPrice ->
                        settingsViewModel.savePhotographerProfileAndBank(wa, ig, bank, accNum, holder, priceSingle, minQty, discountPrice)
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
