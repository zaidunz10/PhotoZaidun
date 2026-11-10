package com.zaidun.carifoto.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zaidun.carifoto.presentation.auth.CariFotoAuthViewModel
import com.zaidun.carifoto.presentation.auth.CariFotoLoginScreen
import com.zaidun.carifoto.presentation.home.CariFotoHomeScreen
import com.zaidun.carifoto.presentation.home.CariFotoHomeViewModel
import com.zaidun.carifoto.presentation.orders.CariFotoOrdersScreen
import com.zaidun.carifoto.presentation.profile.CariFotoProfileScreen
import com.zaidun.carifoto.presentation.splash.CariFotoSplashScreen

enum class CariFotoNavRoute(val route: String) {
    SPLASH("splash"),
    LOGIN("login"),
    MAIN("main")
}

enum class CariFotoTabRoute(val route: String, val title: String) {
    HOME("home", "Cari Foto"),
    ORDERS("orders", "Pesanan"),
    PROFILE("profile", "Profil")
}

@Composable
fun CariFotoMainApp(
    homeViewModel: CariFotoHomeViewModel = viewModel(),
    authViewModel: CariFotoAuthViewModel = viewModel()
) {
    val rootNavController = rememberNavController()
    val authUiState by authViewModel.uiState.collectAsState()
    LaunchedEffect(authUiState.googleUser?.id) {
        homeViewModel.setRiderId(
            authUiState.googleUser?.id ?: ""
        )
    }

    NavHost(
        navController = rootNavController,
        startDestination = CariFotoNavRoute.SPLASH.route
    ) {
        composable(CariFotoNavRoute.SPLASH.route) {
            CariFotoSplashScreen(
                onSplashFinished = {
                    val isLoggedIn = authUiState.googleUser != null
                    if (isLoggedIn) {
                        rootNavController.navigate(CariFotoNavRoute.MAIN.route) {
                            popUpTo(CariFotoNavRoute.SPLASH.route) { inclusive = true }
                        }
                    } else {
                        rootNavController.navigate(CariFotoNavRoute.LOGIN.route) {
                            popUpTo(CariFotoNavRoute.SPLASH.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(CariFotoNavRoute.LOGIN.route) {
            CariFotoLoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    rootNavController.navigate(CariFotoNavRoute.MAIN.route) {
                        popUpTo(CariFotoNavRoute.LOGIN.route) { inclusive = true }
                    }
                }
            )
        }

        composable(CariFotoNavRoute.MAIN.route) {
            CariFotoMainContent(
                homeViewModel = homeViewModel,
                authViewModel = authViewModel,
                onLogoutFinished = {
                    rootNavController.navigate(CariFotoNavRoute.LOGIN.route) {
                        popUpTo(CariFotoNavRoute.MAIN.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun CariFotoMainContent(
    homeViewModel: CariFotoHomeViewModel,
    authViewModel: CariFotoAuthViewModel,
    onLogoutFinished: () -> Unit
) {
    val navController = rememberNavController()
    val screens = listOf(
        CariFotoTabRoute.HOME,
        CariFotoTabRoute.ORDERS,
        CariFotoTabRoute.PROFILE
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

                    screens.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when(screen) {
                                        CariFotoTabRoute.HOME -> Icons.Default.Home
                                        CariFotoTabRoute.ORDERS -> Icons.Default.ShoppingCart
                                        CariFotoTabRoute.PROFILE -> Icons.Default.Person
                                    },
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
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
            startDestination = CariFotoTabRoute.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(CariFotoTabRoute.HOME.route) {
                CariFotoHomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToOrders = { navController.navigate(CariFotoTabRoute.ORDERS.route) }
                )
            }

            composable(CariFotoTabRoute.ORDERS.route) {
                CariFotoOrdersScreen(
                    viewModel = homeViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(CariFotoTabRoute.PROFILE.route) {
                CariFotoProfileScreen(
                    authViewModel = authViewModel,
                    homeViewModel = homeViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToOrders = { navController.navigate(CariFotoTabRoute.ORDERS.route) },
                    onLogoutFinished = onLogoutFinished
                )
            }
        }
    }
}
