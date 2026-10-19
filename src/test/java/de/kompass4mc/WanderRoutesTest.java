package de.kompass4mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WanderRoutesTest {
    @Test
    void routesStayNearPersistentHomeAndUseOnlyAdjacentSafeCells() {
        var routes = WanderRoutes.find(0, 0, 2, 0, 6, (x, z) -> x != 3 || z == 2);
        assertFalse(routes.isEmpty());
        for (var route : routes) {
            var previous = new WanderRoutes.Cell(2, 0);
            assertTrue(route.size() >= 2 && route.size() <= 8);
            for (var cell : route) {
                assertTrue(cell.x() * cell.x() + cell.z() * cell.z() <= 36);
                assertTrue(cell.x() != 3 || cell.z() == 2);
                assertEquals(1, Math.abs(cell.x() - previous.x()) + Math.abs(cell.z() - previous.z()));
                previous = cell;
            }
        }
    }

    @Test
    void barriersNeverBecomeAJumpOrAnUnloadedShortcut() {
        var routes = WanderRoutes.find(0, 0, 0, 0, 6, (x, z) -> x != 1);
        assertFalse(routes.isEmpty());
        assertTrue(routes.stream().flatMap(java.util.Collection::stream).allMatch(cell -> cell.x() < 1));
    }

    @Test
    void unsafeOrIsolatedVisitorStaysPut() {
        assertTrue(WanderRoutes.find(0, 0, 0, 0, 6, (x, z) -> false).isEmpty());
        assertTrue(WanderRoutes.find(0, 0, 0, 0, 6, (x, z) -> x == 0 && z == 0).isEmpty());
        assertTrue(WanderRoutes.find(0, 0, 7, 0, 6, (x, z) -> true).isEmpty());
    }
}
