package com.zaidun.photozaidun.presentation.component.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zaidun.photozaidun.presentation.theme.motion.MotionTokens

@Composable
fun AnimatedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(MotionTokens.DURATION_CARD, easing = MotionTokens.ScreenEasing)
        ) + expandVertically(
            animationSpec = tween(MotionTokens.DURATION_CARD, easing = MotionTokens.ScreenEasing),
            expandFrom = Alignment.Top,
            initialHeight = { (it * 0.9f).toInt() } // "sedikit" expand, bukan dari 0
        )
    ) {
        Card(modifier = modifier) {
            Column(content = content)
        }
    }
}