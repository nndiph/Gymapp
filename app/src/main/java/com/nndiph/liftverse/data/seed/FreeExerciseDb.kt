package com.nndiph.liftverse.data.seed

import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.ExerciseKeys
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType
import org.json.JSONArray
import org.json.JSONObject

// Built-in exercises come from free-exercise-db (github.com/yuhonas/free-exercise-db, Unlicense), file
// dist/exercises.json at commit f00c92c, bundled unchanged as assets/seed/exercises.json. The mapping to the
// design's filters was decided by the project owner on 2026-11-13.

/** The fields of one free-exercise-db entry that the app uses. Images and instructions are not imported. */
data class FreeExercise(
    val id: String,
    val name: String,
    val category: String,
    val equipment: String?,
    val force: String?,
    val primaryMuscles: List<String>
)

/** Categories left out of the library: the design has no stretching or strongman exercises. */
private val EXCLUDED_CATEGORIES = setOf("stretching", "strongman")

private const val CARDIO = "cardio"

private val MUSCLE_GROUPS: Map<String, MuscleGroup> = buildMap {
    put("chest", MuscleGroup.CHEST)
    listOf("lats", "middle back", "lower back", "traps").forEach { put(it, MuscleGroup.BACK) }
    listOf("shoulders", "neck").forEach { put(it, MuscleGroup.SHOULDERS) }
    listOf("quadriceps", "hamstrings", "glutes", "calves", "adductors", "abductors")
        .forEach { put(it, MuscleGroup.LEGS) }
    listOf("biceps", "triceps", "forearms").forEach { put(it, MuscleGroup.ARMS) }
    put("abdominals", MuscleGroup.CORE)
}

/** Equipment chip of the design; anything not listed is "Lainnya". A missing equipment means bodyweight. */
fun seedEquipment(raw: String?): Equipment = when (raw) {
    "barbell", "e-z curl bar" -> Equipment.BARBELL
    "dumbbell", "kettlebells" -> Equipment.DUMBBELL
    "machine" -> Equipment.MACHINE
    "cable", "bands" -> Equipment.CABLE
    "body only", null -> Equipment.BODYWEIGHT
    else -> Equipment.OTHER
}

/** Load step per source equipment: barbell 2,5 · dumbbell 2 · cable/machine 2,5 · kettlebell 4. */
fun seedWeightStepKg(raw: String?): Double? = when (raw) {
    "barbell", "e-z curl bar", "machine", "cable", "bands" -> 2.5
    "dumbbell" -> 2.0
    "kettlebells" -> 4.0
    else -> null
}

private val HOLD_NAME = Regex("""\b(plank|hold|wall sit)\b""", RegexOption.IGNORE_CASE)

/**
 * Plank, holds and wall sits are timed. Only static ones count: "Push Up to Side Plank" is a pushing movement
 * done for reps.
 */
private fun FreeExercise.isHold(): Boolean = HOLD_NAME.containsMatchIn(name) && force != "push" && force != "pull"

fun seedTrackingType(exercise: FreeExercise): TrackingType = when {
    exercise.isHold() -> TrackingType.DURATION
    exercise.category == CARDIO -> TrackingType.DISTANCE_DURATION
    seedEquipment(exercise.equipment) == Equipment.BODYWEIGHT -> TrackingType.BODYWEIGHT_REPS
    else -> TrackingType.WEIGHT_REPS
}

/** The library entry, or null when the exercise is left out (excluded category or unknown muscle). */
fun FreeExercise.toSeedExercise(): Exercise? {
    if (category in EXCLUDED_CATEGORIES) return null
    val muscleGroup = if (category == CARDIO) {
        MuscleGroup.CARDIO
    } else {
        primaryMuscles.firstNotNullOfOrNull { MUSCLE_GROUPS[it] } ?: return null
    }
    return Exercise(
        key = ExerciseKeys.seed(id),
        name = name,
        muscleGroup = muscleGroup,
        equipment = seedEquipment(equipment),
        trackingType = seedTrackingType(this),
        weightStepKg = seedWeightStepKg(equipment)
    )
}

/** Parses free-exercise-db `dist/exercises.json`. */
fun parseFreeExercises(json: String): List<FreeExercise> {
    val array = JSONArray(json)
    return List(array.length()) { index ->
        val item = array.getJSONObject(index)
        val muscles = item.getJSONArray("primaryMuscles")
        FreeExercise(
            id = item.getString("id"),
            name = item.getString("name"),
            category = item.getString("category"),
            equipment = item.optStringOrNull("equipment"),
            force = item.optStringOrNull("force"),
            primaryMuscles = List(muscles.length()) { muscles.getString(it) }
        )
    }
}

/** The library built from the bundled file: excluded categories dropped, the rest mapped. */
fun seedExercisesFrom(json: String): List<Exercise> = parseFreeExercises(json).mapNotNull { it.toSeedExercise() }

private fun JSONObject.optStringOrNull(name: String): String? = if (isNull(name)) null else getString(name)
