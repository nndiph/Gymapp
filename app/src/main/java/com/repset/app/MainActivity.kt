package com.repset.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.repset.app.core.theme.RepsetTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single activity; every screen lives in Compose under [RepsetApp]. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RepsetTheme {
                RepsetApp()
            }
        }
    }
}
