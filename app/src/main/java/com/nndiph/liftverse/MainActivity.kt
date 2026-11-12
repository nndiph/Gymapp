package com.nndiph.liftverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nndiph.liftverse.core.theme.LiftverseTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single activity; every screen lives in Compose under [LiftverseApp]. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate(): swaps the launch theme (Theme.Liftverse.Starting) for the app theme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiftverseTheme {
                LiftverseApp()
            }
        }
    }
}
