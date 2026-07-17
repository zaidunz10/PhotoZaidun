package com.zaidun.photozaidun.presentation.screen.folderpicker.component

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.zaidun.photozaidun.domain.model.PhotoItem

@Composable
fun ImageCard(

    photo: PhotoItem

) {

    Card {

        AsyncImage(

            model = photo.uri,

            contentDescription = photo.name,

            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Crop

        )

    }

}