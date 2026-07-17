package com.zaidun.photozaidun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zaidun.photozaidun.presentation.navigation.AppNavGraph
import com.zaidun.photozaidun.presentation.theme.PhotoZaidunTheme
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            PhotoZaidunTheme {

                AppNavGraph()

            }
        }
    }
}