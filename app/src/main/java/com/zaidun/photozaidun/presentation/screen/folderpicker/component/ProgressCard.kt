package com.zaidun.photozaidun.presentation.screen.folderpicker.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressCard(

    scanned: Int,

    batch: Int,

    scanning: Boolean

) {

    Card(

        modifier = Modifier.fillMaxWidth()

    ) {

        Column(

            modifier = Modifier.padding(16.dp)

        ) {

            Text(

                text = if (scanning)
                    "Scanning..."
                else
                    "Selesai"

            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Batch : $batch"
            )

            Text(
                text = "Foto : $scanned"
            )

        }

    }

}