package com.zaidun.photozaidun.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zaidun.photozaidun.presentation.screen.home.HomeScreen
import com.zaidun.photozaidun.presentation.screen.home.HomeViewModel
import com.zaidun.photozaidun.presentation.screen.settings.SettingsScreen
import com.zaidun.photozaidun.presentation.screen.watermarksettings.WatermarkSettingsScreen
import com.zaidun.photozaidun.presentation.shared.SharedFolderViewModel

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import com.zaidun.photozaidun.presentation.theme.motion.MotionTokens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaidun.photozaidun.presentation.screen.profile.ProfileScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.zaidun.photozaidun.presentation.screen.settings.SettingsViewModel

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
    val items = listOf(
        Screen.Home,
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
                    containerColor = androidx.compose.ui.graphics.Color(0xFF23143D)
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when(screen) {
                                        Screen.Home -> Icons.Default.Home
                                        Screen.Watermark -> Icons.Default.Brush
                                        Screen.Profile -> Icons.Default.Person
                                        else -> Icons.Default.Settings
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { 
                                Text(
                                    screen.route.replaceFirstChar { it.uppercase() },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.ui.graphics.Color(0xFF7B4FE0),
                                selectedIconColor = Color.White,
                                unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF9E8DBE),
                                selectedTextColor = Color.White,
                                unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF9E8DBE)
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
            composable(Screen.Watermark.route) {
                WatermarkSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Profile.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val settingsUiState by settingsViewModel.uiState.collectAsState()
                ProfileScreen(
                    uiState = settingsUiState,
                    onBack = { navController.popBackStack() },
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
