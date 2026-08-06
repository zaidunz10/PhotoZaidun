package com.zaidun.photozaidun.presentation.theme.motion

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing

object MotionTokens {
    // Durasi
    const val DURATION_SCREEN_ENTER = 280
    const val DURATION_SCREEN_EXIT = 250
    const val DURATION_DIALOG = 200
    const val DURATION_CARD = 220
    const val DURATION_BUTTON_PRESS = 120
    const val DURATION_TAB_SWITCH = 200
    const val DURATION_LIST_ITEM = 220
    const val LIST_STAGGER_DELAY = 25 // 20-30ms

    // Easing
    val ScreenEasing = FastOutSlowInEasing
    val DialogEasing = FastOutSlowInEasing
    val ExitEasing = FastOutLinearInEasing

    // Slide offset
    const val SLIDE_FRACTION = 1 // 100% lebar layar untuk screen transition
    const val TAB_SLIDE_OFFSET_DP = 12
    const val LIST_SLIDE_OFFSET_DP = 12
}