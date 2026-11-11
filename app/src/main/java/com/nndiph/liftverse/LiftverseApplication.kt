package com.nndiph.liftverse

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Hilt generates the dependency container from here.
 * Firebase initialises itself through the google-services plugin; nothing to do manually.
 */
@HiltAndroidApp
class LiftverseApplication : Application()
