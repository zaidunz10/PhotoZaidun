package com.zaidun.photozaidun.presentation.screen.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.zaidun.photozaidun.presentation.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashRoute(
    navController: NavController
) {

    LaunchedEffect(Unit) {

        delay(1800)

        navController.navigate("main") {

            popUpTo(Screen.Splash.route) {
                inclusive = true
            }
        }
    }

    SplashScreen()
}

@Composable
fun SplashScreen() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "Photo Zaidun",
            style = MaterialTheme.typography.headlineLarge
        )
    }
}