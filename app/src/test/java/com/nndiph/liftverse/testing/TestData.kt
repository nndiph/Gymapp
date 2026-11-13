package com.nndiph.liftverse.testing

import androidx.room.Room
import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.domain.model.Equipment
import com.nndiph.liftverse.domain.model.Exercise
import com.nndiph.liftverse.domain.model.MuscleGroup
import com.nndiph.liftverse.domain.model.TrackingType
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.robolectric.RuntimeEnvironment

/** Needs a Robolectric test runner. */
fun inMemoryDatabase(): LiftverseDatabase =
    Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LiftverseDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** A clock the test moves by hand. Starts on Monday 9 November 2026, 18:00 in Jakarta. */
class MutableClock(
    private var now: Instant = Instant.parse("2026-11-09T11:00:00Z"),
    private val zone: ZoneId = ZoneId.of("Asia/Jakarta")
) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

    override fun instant(): Instant = now

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }

    fun advanceSeconds(seconds: Long) = advance(Duration.ofSeconds(seconds))
}

val BENCH = Exercise(
    key = "seed:barbell-bench-press",
    name = "Barbell Bench Press",
    muscleGroup = MuscleGroup.CHEST,
    equipment = Equipment.BARBELL,
    trackingType = TrackingType.WEIGHT_REPS,
    aliases = listOf("flat bench")
)

val PLANK = Exercise(
    key = "seed:plank",
    name = "Plank",
    muscleGroup = MuscleGroup.CORE,
    equipment = Equipment.BODYWEIGHT,
    trackingType = TrackingType.DURATION
)

val DIPS = Exercise(
    key = "seed:dips",
    name = "Dips",
    muscleGroup = MuscleGroup.CHEST,
    equipment = Equipment.BODYWEIGHT,
    trackingType = TrackingType.BODYWEIGHT_REPS
)

/**
 * Waits for state that Room's own threads update outside the test scheduler (view models writing Compose state
 * after a suspend DAO call). Fails after [timeoutMs].
 */
fun <T> pollUntil(read: () -> T, timeoutMs: Long = 5_000, predicate: (T) -> Boolean): T {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (true) {
        val value = read()
        if (predicate(value)) return value
        check(System.currentTimeMillis() < deadline) { "Timed out waiting, last value: $value" }
        Thread.sleep(5)
    }
}
