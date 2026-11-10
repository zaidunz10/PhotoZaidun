package com.zaidun.carifoto.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CariFotoSplashScreen(
    onSplashFinished: () -> Unit
) {
    var logoVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logoVisible = true
        delay(2200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF23143D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn(tween(600)) + scaleIn(initialScale = 0.8f, animationSpec = tween(600))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color(0xFF7B4FE0),
                        shape = CircleShape,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "CariFoto",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Temukan & Beli Foto Jalananmu Instan",
                        fontSize = 13.sp,
                        color = Color(0xFFB89EFF),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color(0xFF7B4FE0),
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
