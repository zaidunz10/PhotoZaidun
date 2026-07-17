package com.zaidun.photozaidun.presentation.screen.folderpicker.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun EmptyState() {

    Box(

        modifier = Modifier.fillMaxSize(),

        contentAlignment = Alignment.Center

    ) {

        Text(

            text = "Belum ada foto",

            style = MaterialTheme.typography.titleMedium

        )

    }

}