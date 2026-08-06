package com.zaidun.photozaidun.presentation.screen.preview

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.zaidun.photozaidun.presentation.theme.motion.MotionTokens

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoPreviewScreen(
    photoUri: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        AsyncImage(
            model = Uri.parse(photoUri),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .sharedElement(
                    rememberSharedContentState(key = "photo-$photoUri"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        tween(durationMillis = 350, easing = MotionTokens.ScreenEasing)
                    }
                ),
            contentScale = ContentScale.Fit
        )
    }
}