package com.zaidun.photozaidun.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import com.zaidun.photozaidun.presentation.screen.contact.ContactScreen
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    sharedFolderViewModel: SharedFolderViewModel,
    onNavigateToFolderPicker: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Watermark,
        Screen.Settings,
        Screen.Contact
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when(screen) {
                                    Screen.Home -> Icons.Default.Home
                                    Screen.Watermark -> Icons.Default.Brush
                                    Screen.Contact -> Icons.Default.Person
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = null
                            )
                        },
                        label = { Text(screen.route.replaceFirstChar { it.uppercase() }) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    sharedFolderViewModel = sharedFolderViewModel,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToFolderPicker = onNavigateToFolderPicker,
                    onNavigateToWatermark = { navController.navigate(Screen.Watermark.route) }
                )
            }
            composable(Screen.Watermark.route) {
                WatermarkSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
                    composable(Screen.Contact.route) {
                        ContactScreen()
                    }
        }
    }
}
