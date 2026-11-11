package com.nndiph.liftverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nndiph.liftverse.core.theme.LiftverseTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single activity; every screen lives in Compose under [LiftverseApp]. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiftverseTheme {
                LiftverseApp()
            }
        }
    }
}
