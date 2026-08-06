package com.zaidun.photozaidun.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.NavBackStackEntry
import com.zaidun.photozaidun.presentation.theme.motion.MotionTokens

fun screenEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        animationSpec = tween(
            durationMillis = MotionTokens.DURATION_SCREEN_ENTER,
            easing = MotionTokens.ScreenEasing
        ),
        initialOffsetX = { fullWidth -> fullWidth / 3 } // slide dari kanan, tidak full agar terasa ringan
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = MotionTokens.DURATION_SCREEN_ENTER,
            easing = MotionTokens.ScreenEasing
        )
    )
}

fun screenExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        animationSpec = tween(
            durationMillis = MotionTokens.DURATION_SCREEN_EXIT,
            easing = MotionTokens.ExitEasing
        ),
        targetOffsetX = { fullWidth -> -fullWidth / 4 }
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = MotionTokens.DURATION_SCREEN_EXIT
        )
    )
}

fun screenPopEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        animationSpec = tween(
            durationMillis = MotionTokens.DURATION_SCREEN_ENTER,
            easing = MotionTokens.ScreenEasing
        ),
        initialOffsetX = { fullWidth -> -fullWidth / 4 }
    ) + fadeIn(
        animationSpec = tween(durationMillis = MotionTokens.DURATION_SCREEN_ENTER)
    )
}

fun screenPopExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        animationSpec = tween(
            durationMillis = MotionTokens.DURATION_SCREEN_EXIT,
            easing = MotionTokens.ExitEasing
        ),
        targetOffsetX = { fullWidth -> fullWidth / 3 } // slide ke kanan saat back
    ) + fadeOut(
        animationSpec = tween(durationMillis = MotionTokens.DURATION_SCREEN_EXIT)
    )
}