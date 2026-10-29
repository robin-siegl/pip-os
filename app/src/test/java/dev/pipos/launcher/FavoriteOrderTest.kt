package dev.pipos.launcher

import dev.pipos.launcher.data.FavoriteOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteOrderTest {
    @Test fun migratesLegacyFavoritesDeterministically() {
        assertEquals(listOf("a", "b"), FavoriteOrder.reconcile(emptyList(), setOf("b", "a")))
    }

    @Test fun retainsSavedOrderAndAppendsNewLegacyEntries() {
        assertEquals(listOf("b", "a", "c"), FavoriteOrder.reconcile(listOf("b", "a", "b", "gone"), setOf("a", "b", "c")))
    }

    @Test fun reordersAndKeepsAllKeys() {
        assertEquals(listOf("b", "c", "a"), FavoriteOrder.move(listOf("a", "b", "c"), 0, 2))
        assertEquals(listOf("a", "b", "c"), FavoriteOrder.move(listOf("a", "b", "c"), -1, 1))
    }

    @Test fun toggleAddsToEndAndRemoves() {
        assertEquals(listOf("a", "b"), FavoriteOrder.toggle(listOf("a"), "b"))
        assertEquals(listOf("b"), FavoriteOrder.toggle(listOf("a", "b"), "a"))
    }
}
