package com.nndiph.liftverse.data.local

import com.nndiph.liftverse.data.local.entity.DayStatsRow
import com.nndiph.liftverse.data.local.entity.ExerciseEntity
import com.nndiph.liftverse.data.local.entity.RoutineExerciseEntity
import com.nndiph.liftverse.data.local.entity.RoutineWithExercises
import com.nndiph.liftverse.data.local.entity.ScheduleMode
import com.nndiph.liftverse.data.local.entity.SessionExerciseWithSets
import com.nndiph.liftverse.data.local.entity.SessionSummaryRow
import com.nndiph.liftverse.data.local.entity.SessionWithExercises
import com.nndiph.liftverse.data.local.entity.SetEntity
import com.nndiph.liftverse.data.local.entity.StreakScheduleEntity
import com.nndiph.liftverse.domain.model.DayStats
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.PlannedSet
import com.nndiph.liftverse.domain.model.RestTimer
import com.nndiph.liftverse.domain.model.Routine
import com.nndiph.liftverse.domain.model.RoutineExercise
import com.nndiph.liftverse.domain.model.Session
import com.nndiph.liftverse.domain.model.SessionExercise
import com.nndiph.liftverse.domain.model.SessionSummary
import com.nndiph.liftverse.domain.model.SetEntry
import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.StreakSchedule
import java.time.Instant

internal fun ExerciseEntity.toDomain() = Exercise(
    key = exerciseKey,
    name = name,
    muscleGroup = muscleGroup,
    equipment = equipment,
    trackingType = trackingType,
    aliases = aliases,
    basedOn = basedOn,
    notes = notes,
    archived = archived
)

internal fun RoutineWithExercises.toDomain() = Routine(
    id = routine.id,
    name = routine.name,
    weekdays = routine.weekdays,
    reminderEnabled = routine.reminderEnabled,
    reminderTime = routine.reminderTime,
    exercises = exercises.sortedBy { it.position }.map {
        RoutineExercise(it.exerciseKey, it.nameSnapshot, it.targetSets, it.targetReps)
    }
)

internal fun List<RoutineExercise>.toEntities(routineId: String) = mapIndexed { position, exercise ->
    RoutineExerciseEntity(
        routineId = routineId,
        position = position,
        exerciseKey = exercise.exerciseKey,
        nameSnapshot = exercise.nameSnapshot,
        targetSets = exercise.targetSets,
        targetReps = exercise.targetReps
    )
}

internal fun SessionWithExercises.toDomain(): Session {
    val restSetId = session.restSetId
    val restStartedAt = session.restStartedAt
    val restTargetSec = session.restTargetSec
    return Session(
        id = session.id,
        name = session.name,
        routineId = session.routineId,
        startedAt = session.startedAt,
        endedAt = session.endedAt,
        day = session.day,
        routineSnapshot = session.routineSnapshot,
        rest = if (restSetId != null && restStartedAt != null && restTargetSec != null) {
            RestTimer(restSetId, restStartedAt, restTargetSec)
        } else {
            null
        },
        exercises = exercises.sortedBy { it.exercise.position }.map { it.toDomain() }
    )
}

internal fun SessionExerciseWithSets.toDomain() = SessionExercise(
    id = exercise.id,
    exerciseKey = exercise.exerciseKey,
    nameSnapshot = exercise.nameSnapshot,
    trackingType = exercise.trackingType,
    sets = sets.sortedBy { it.position }.map { it.toDomain() }
)

internal fun SetEntity.toDomain() = SetEntry(
    id = id,
    values = SetValues(weightKg, reps, durationSec, distanceM),
    isWarmup = isWarmup,
    completedAt = completedAt,
    restAfterSec = restAfterSec,
    isPr = isPr
)

internal fun SetEntity.toPlannedSet() = PlannedSet(SetValues(weightKg, reps, durationSec, distanceM), isWarmup)

internal fun SetEntry.toEntity(sessionExerciseId: String, position: Int) = SetEntity(
    id = id,
    sessionExerciseId = sessionExerciseId,
    position = position,
    weightKg = values.weightKg,
    reps = values.reps,
    durationSec = values.durationSec,
    distanceM = values.distanceM,
    isWarmup = isWarmup,
    completedAt = completedAt,
    restAfterSec = restAfterSec,
    isPr = isPr
)

internal fun SessionSummaryRow.toDomain() = SessionSummary(
    id = id,
    name = name,
    day = day,
    startedAt = startedAt,
    endedAt = endedAt,
    exerciseCount = exerciseCount,
    setCount = setCount,
    volumeKg = volumeKg,
    restSec = restSec,
    prCount = prCount
)

internal fun DayStatsRow.toDomain() = DayStats(day, sessionCount, setCount, volumeKg, durationSec)

internal fun StreakScheduleEntity.toDomain(): StreakSchedule = when (mode) {
    ScheduleMode.FIXED_DAYS -> StreakSchedule.FixedDays(days, allowSwap, preset)
    ScheduleMode.WEEKLY_TARGET -> StreakSchedule.WeeklyTarget(weeklyTarget.coerceIn(1, 7))
}

internal fun StreakSchedule.toEntity(now: Instant) = when (this) {
    is StreakSchedule.FixedDays -> StreakScheduleEntity(
        mode = ScheduleMode.FIXED_DAYS,
        days = days,
        allowSwap = allowSwap,
        weeklyTarget = days.size.coerceIn(1, 7),
        preset = preset,
        updatedAt = now
    )
    is StreakSchedule.WeeklyTarget -> StreakScheduleEntity(
        mode = ScheduleMode.WEEKLY_TARGET,
        days = emptySet(),
        allowSwap = true,
        weeklyTarget = target,
        preset = null,
        updatedAt = now
    )
}
