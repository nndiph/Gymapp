package com.nndiph.liftverse.domain.repository

import com.nndiph.liftverse.domain.model.Routine
import com.nndiph.liftverse.domain.model.RoutineDraft
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    /** Routines that are not deleted, oldest first. */
    fun observeRoutines(): Flow<List<Routine>>

    /** Null when the routine does not exist or was deleted. */
    suspend fun getRoutine(id: String): Routine?

    /** Returns the new routine id. */
    suspend fun createRoutine(draft: RoutineDraft): String

    /** Replaces the routine's fields and exercises. */
    suspend fun updateRoutine(id: String, draft: RoutineDraft)

    /** "Hapus routine": a soft delete, so sessions started from it keep their link (docs/DATA_MODEL.md §5). */
    suspend fun deleteRoutine(id: String)
}
