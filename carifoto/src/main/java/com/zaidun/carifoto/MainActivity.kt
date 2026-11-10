package com.zaidun.carifoto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zaidun.carifoto.presentation.navigation.CariFotoMainApp
import com.zaidun.carifoto.ui.theme.PhotoZaidunTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }

        enableEdgeToEdge()
        setContent {
            PhotoZaidunTheme {
                CariFotoMainApp()
            }
        }
    }
}
