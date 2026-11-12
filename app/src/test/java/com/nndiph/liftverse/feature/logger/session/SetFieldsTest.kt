package com.nndiph.liftverse.feature.logger.session

import com.nndiph.liftverse.domain.model.SetValues
import com.nndiph.liftverse.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Test

class SetFieldsTest {
    @Test
    fun `each tracking type gets its steppers in prototype order`() {
        assertEquals(listOf(SetField.WEIGHT, SetField.REPS), fieldsFor(TrackingType.WEIGHT_REPS))
        assertEquals(listOf(SetField.REPS, SetField.WEIGHT), fieldsFor(TrackingType.BODYWEIGHT_REPS))
        assertEquals(listOf(SetField.DURATION), fieldsFor(TrackingType.DURATION))
        assertEquals(listOf(SetField.DISTANCE, SetField.DURATION), fieldsFor(TrackingType.DISTANCE_DURATION))
    }

    @Test
    fun `values convert between storage and display units without drift`() {
        val values = SetValues(distanceM = 2_000, durationSec = 900)
        val step = stepFor(SetField.DISTANCE, TrackingType.DISTANCE_DURATION, weightStepKg = 2.5)
        val further = values.withValue(SetField.DISTANCE, values.valueOf(SetField.DISTANCE) + step)
        assertEquals(2_100, further.distanceM)
        assertEquals(SetValues(82.5, 8), SetValues(80.0, 8).withValue(SetField.WEIGHT, 80.0 + 2.5))
        assertEquals(0, SetValues(reps = 1).withValue(SetField.REPS, -1.0).reps)
        assertEquals(30.0, stepFor(SetField.DURATION, TrackingType.DISTANCE_DURATION, weightStepKg = 2.5), 0.0)
    }
}
