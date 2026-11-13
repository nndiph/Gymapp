package com.nndiph.liftverse.data.seed

import android.content.Context
import androidx.core.content.edit
import com.nndiph.liftverse.domain.repository.ExerciseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Imports the bundled exercise library into Room on first launch, and again when [SEED_VERSION] goes up with a
 * new bundled file. Runs on the device only, so the library works offline from the first start.
 */
@Singleton
class ExerciseSeedImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val exercises: ExerciseRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    /** Starts the import in the background once per process; later calls do nothing. */
    fun start() {
        if (started.compareAndSet(false, true)) scope.launch { importIfNeeded() }
    }

    suspend fun importIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_VERSION, 0) >= SEED_VERSION) return
        val json = context.assets.open(SEED_ASSET).use { it.readBytes().decodeToString() }
        exercises.upsertSeedExercises(seedExercisesFrom(json))
        prefs.edit { putInt(KEY_VERSION, SEED_VERSION) }
    }

    companion object {
        /** Raise when assets/seed/exercises.json or the mapping changes, so existing installs import again. */
        const val SEED_VERSION = 1
        const val SEED_ASSET = "seed/exercises.json"
        private const val PREFS_NAME = "seed"
        private const val KEY_VERSION = "exercises_version"
    }
}
