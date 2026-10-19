package de.kompass4mc.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VillageJobsTest {
    @Test
    void gardenCountsEachCornerOnlyOnceAndLeavesRoomForPlants() {
        BuildPattern garden = VillageJobs.pattern(Kind.GARDEN);

        assertEquals(28, garden.required().size());
        assertEquals(Set.of(new GridPos(0, 0), new GridPos(0, 6), new GridPos(8, 0), new GridPos(8, 6)),
                garden.required().keySet().stream()
                        .filter(position -> (position.x() == 0 || position.x() == 8)
                                && (position.z() == 0 || position.z() == 6))
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals("minecraft:oak_fence", garden.required().get(new GridPos(4, 0)));
        assertEquals("minecraft:oak_fence", garden.required().get(new GridPos(8, 3)));
        assertFalse(garden.required().containsKey(new GridPos(4, 3)));
        assertTrue(garden.forbidden().contains(new GridPos(4, 3)));
        assertEquals(35, garden.forbidden().size());
    }

    @Test
    void bridgeOccupiesTwoRowsOfFiveAndChecksItsImmediateOuterRing() {
        BuildPattern bridge = VillageJobs.pattern(Kind.BRIDGE);

        assertEquals(10, bridge.required().size());
        assertTrue(bridge.required().containsKey(new GridPos(4, 1)));
        assertFalse(bridge.required().containsKey(new GridPos(5, 1)));
        assertFalse(bridge.required().containsKey(new GridPos(0, 2)));
        assertEquals(17, bridge.forbidden().size());
        assertFalse(bridge.forbidden().contains(new GridPos(-1, 0)));
        assertTrue(bridge.forbidden().containsAll(Set.of(
                new GridPos(-1, -1), new GridPos(5, -1), new GridPos(-1, 2), new GridPos(5, 2))));
    }

    @Test
    void deliveryUsesTheQuantityPerPersonAndExplainsTheIrrelevantInformation() {
        MissionJob delivery = VillageJobs.byId("travel_supplies").orElseThrow();

        assertEquals(Kind.DELIVERY, delivery.kind());
        assertEquals("minecraft:apple", VillageJobs.DELIVERY_ITEM_ID);
        assertEquals(3 * 6, VillageJobs.DELIVERY_COUNT);
        assertTrue(delivery.request().contains("Drei Reisende brauchen je sechs Äpfel. Brot bleibt im Lager."));
        assertTrue(delivery.hint().contains("Jeder der drei Reisenden bekommt sechs Äpfel"));
        assertTrue(delivery.completionText().contains("6 + 6 + 6 = 18 Äpfel"));
        assertTrue(delivery.completionText().contains("Brot bleibt"));
        assertThrows(IllegalArgumentException.class, () -> VillageJobs.pattern(Kind.DELIVERY));
    }

    @Test
    void catalogExposesUniqueImmutableJobsForTheThreeResidents() {
        assertEquals(3, VillageJobs.all().size());
        assertEquals(3, VillageJobs.all().stream().map(MissionJob::id).distinct().count());
        assertEquals(Set.of("Mila", "Jaro", "Lio"), VillageJobs.all().stream()
                .map(MissionJob::residentName).collect(java.util.stream.Collectors.toSet()));
        assertTrue(VillageJobs.byId("missing").isEmpty());
        assertTrue(VillageJobs.byId(null).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> VillageJobs.all().clear());

        Map<GridPos, String> complete = new HashMap<>(VillageJobs.pattern(Kind.GARDEN).required());
        assertTrue(VillageJobs.pattern(Kind.GARDEN).inspect(complete).complete());
    }
}
