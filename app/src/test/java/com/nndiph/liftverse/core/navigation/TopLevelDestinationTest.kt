package com.nndiph.liftverse.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopLevelDestinationTest {
    @Test
    fun `routes are unique`() {
        val routes = TopLevelDestination.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `fromRoute resolves every top-level route`() {
        TopLevelDestination.entries.forEach { destination ->
            assertEquals(destination, TopLevelDestination.fromRoute(destination.route))
        }
    }

    @Test
    fun `fromRoute returns null for unknown or missing route`() {
        assertNull(TopLevelDestination.fromRoute("session/123"))
        assertNull(TopLevelDestination.fromRoute(null))
    }

    @Test
    fun `start destination is dashboard`() {
        assertEquals(TopLevelDestination.DASHBOARD, TopLevelDestination.START)
    }
}
