package com.nndiph.liftverse.domain.model

/** How sets of an exercise are recorded. Locked once the exercise has history (docs/DATA_MODEL.md §2.2). */
enum class TrackingType {
    /** Load in kg + reps. */
    WEIGHT_REPS,

    /** Reps with optional added load; 0 kg means bodyweight only. */
    BODYWEIGHT_REPS,

    /** Time only, e.g. plank. */
    DURATION,

    /** Distance + time, e.g. treadmill. */
    DISTANCE_DURATION
}

/** The muscle-group chips of the design: Dada, Punggung, Bahu, Kaki, Lengan, Core, Kardio. */
enum class MuscleGroup { CHEST, BACK, SHOULDERS, LEGS, ARMS, CORE, CARDIO }

/** The equipment chips of the design: Barbell, Dumbbell, Mesin, Kabel, Bodyweight, Lainnya. */
enum class Equipment { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, OTHER }

/**
 * A built-in (seed) or custom exercise. [key] follows docs/DATA_MODEL.md §1:
 * `seed:<slug>` for built-in exercises, `custom:<id>` for the user's own.
 */
data class Exercise(
    val key: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val trackingType: TrackingType,
    val aliases: List<String> = emptyList(),
    /** Key of the exercise this one is a variation of ("Variasi dari"). */
    val basedOn: String? = null,
    val notes: String? = null,
    val archived: Boolean = false,
    /** Load step of the stepper ("±2,5"). Null uses the default for [equipment] (`weightStepOf`). */
    val weightStepKg: Double? = null
) {
    val isCustom: Boolean get() = key.startsWith(ExerciseKeys.CUSTOM_PREFIX)
}

/** The fields of the custom-exercise form. */
data class ExerciseDraft(
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val trackingType: TrackingType,
    val basedOn: String? = null,
    val notes: String? = null
)

object ExerciseKeys {
    const val SEED_PREFIX = "seed:"
    const val CUSTOM_PREFIX = "custom:"

    fun seed(slug: String): String = SEED_PREFIX + slug

    fun custom(id: String): String = CUSTOM_PREFIX + id
}
