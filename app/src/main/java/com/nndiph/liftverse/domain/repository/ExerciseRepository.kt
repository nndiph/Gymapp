package com.nndiph.liftverse.domain.repository

import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.ExerciseDraft
import com.nndiph.liftverse.domain.model.MuscleGroup
import kotlinx.coroutines.flow.Flow

/** The exercise library: built-in (seed) and custom exercises in one list. Works fully offline. */
interface ExerciseRepository {
    /** Non-archived exercises whose name or alias contains [query], filtered by the picker chips. */
    fun observeExercises(
        query: String = "",
        muscleGroup: MuscleGroup? = null,
        equipment: Equipment? = null
    ): Flow<List<Exercise>>

    /** Any exercise including archived ones, so history keeps resolving. */
    suspend fun getExercise(key: String): Exercise?

    /** Returns the new `custom:` key. */
    suspend fun createCustomExercise(draft: ExerciseDraft): String

    /** Throws [IllegalStateException] when the tracking type changes after the exercise has history. */
    suspend fun updateCustomExercise(key: String, draft: ExerciseDraft)

    /** Soft delete: hidden from the picker, history keeps the exercise (docs/DATA_MODEL.md §1). */
    suspend fun archiveCustomExercise(key: String)

    /** True once a session uses the exercise; the form then disables the tracking type. */
    suspend fun isTrackingTypeLocked(key: String): Boolean

    /** For the seed loader: inserts or refreshes built-in exercises (`seed:` keys only). */
    suspend fun upsertSeedExercises(exercises: List<Exercise>)
}
