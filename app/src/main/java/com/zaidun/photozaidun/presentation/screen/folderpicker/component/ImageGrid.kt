package com.zaidun.photozaidun.presentation.screen.folderpicker.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaidun.photozaidun.domain.model.PhotoItem

@Composable
fun ImageGrid(

    photos: List<PhotoItem>

) {

    LazyVerticalGrid(

        columns = GridCells.Adaptive(120.dp),

        modifier = Modifier.fillMaxSize()

    ) {

        items(

            items = photos,

            key = {

                it.uri.toString()

            }

        ) {

            ImageCard(it)

        }

    }

}