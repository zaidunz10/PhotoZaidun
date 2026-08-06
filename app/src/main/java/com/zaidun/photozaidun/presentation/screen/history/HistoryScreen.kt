package com.zaidun.photozaidun.presentation.screen.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import com.zaidun.photozaidun.presentation.theme.motion.MotionTokens
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Batch") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        StaggeredHistoryList(
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}

@Composable
fun StaggeredHistoryList(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    // Track apakah initial load sudah selesai, agar animasi tidak re-trigger tiap scroll
    var hasAnimatedIn by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current

    // Contoh data dummy untuk demonstrasi stagger
    val items = remember { List(10) { it } }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items, key = { _, item -> item }) { index, item ->
            var itemVisible by remember { mutableStateOf(hasAnimatedIn) }

            LaunchedEffect(Unit) {
                if (!hasAnimatedIn) {
                    // Stagger dibatasi max ~10 item pertama agar list panjang tidak "lama"
                    val cappedIndex = index.coerceAtMost(10)
                    delay(cappedIndex * MotionTokens.LIST_STAGGER_DELAY.toLong())
                }
                itemVisible = true
            }

            AnimatedVisibility(
                visible = itemVisible,
                enter = fadeIn(tween(MotionTokens.DURATION_LIST_ITEM)) +
                        slideInVertically(
                            animationSpec = tween(
                                MotionTokens.DURATION_LIST_ITEM,
                                easing = MotionTokens.ScreenEasing
                            ),
                            initialOffsetY = { with(density) { MotionTokens.LIST_SLIDE_OFFSET_DP.dp.roundToPx() } }
                        )
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Batch #$item") },
                        supportingContent = { Text("150 Foto • Sukses") },
                        trailingContent = { Text("12/05/24", style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }
        }
    }

    LaunchedEffect(items) {
        if (items.isNotEmpty() && !hasAnimatedIn) {
            delay((10 * MotionTokens.LIST_STAGGER_DELAY + MotionTokens.DURATION_LIST_ITEM).toLong())
            hasAnimatedIn = true
        }
    }
}
