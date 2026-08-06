package com.zaidun.photozaidun.presentation.component.motion

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import com.zaidun.photozaidun.presentation.theme.motion.MotionTokens
import kotlinx.coroutines.delay

@Composable
fun AnimatedDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    if (visible) {
        Dialog(onDismissRequest = onDismiss) {
            val transition = updateTransition(visible, label = "dialog_transition")

            val scale by transition.animateFloat(
                transitionSpec = {
                    tween(MotionTokens.DURATION_DIALOG, easing = MotionTokens.DialogEasing)
                },
                label = "scale"
            ) { if (it) 1f else 0.9f }

            val alpha by transition.animateFloat(
                transitionSpec = { tween(MotionTokens.DURATION_DIALOG) },
                label = "alpha"
            ) { if (it) 1f else 0f }

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                content()
            }
        }
    }
}

@Composable
fun rememberAnimatedDialogState(): AnimatedDialogState {
    return remember { AnimatedDialogState() }
}

class AnimatedDialogState {
    var isVisible by mutableStateOf(false)
        private set
    var isDismissing by mutableStateOf(false)
        private set

    fun show() {
        isVisible = true
        isDismissing = false
    }

    suspend fun dismiss(onFinished: () -> Unit) {
        isDismissing = true
        delay(MotionTokens.DURATION_DIALOG.toLong())
        isVisible = false
        isDismissing = false
        onFinished()
    }
}