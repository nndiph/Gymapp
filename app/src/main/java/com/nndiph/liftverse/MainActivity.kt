package com.nndiph.liftverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nndiph.liftverse.core.theme.LiftverseTheme
import com.nndiph.liftverse.data.seed.ExerciseSeedImporter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Single activity; every screen lives in Compose under [LiftverseApp]. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var exerciseSeedImporter: ExerciseSeedImporter

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate(): swaps the launch theme (Theme.Liftverse.Starting) for the app theme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // First launch: fill the exercise library from the bundled file (no network needed).
        exerciseSeedImporter.start()
        enableEdgeToEdge()
        setContent {
            LiftverseTheme {
                LiftverseApp()
            }
        }
    }
}
