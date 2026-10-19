package de.kompass4mc.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BuildPatternTest {
    @Test
    void inspectionSeparatesMissingWrongAndExtraComponents() {
        BuildPattern bridge = VillageJobs.pattern(Kind.BRIDGE);
        Map<GridPos, String> actual = new HashMap<>(bridge.required());
        GridPos missing = new GridPos(0, 0);
        GridPos wrong = new GridPos(2, 1);
        GridPos extra = new GridPos(-1, 1);
        actual.remove(missing);
        actual.put(wrong, "minecraft:birch_planks");
        actual.put(extra, "minecraft:oak_planks");

        BuildPattern.Inspection result = bridge.inspect(actual);

        assertFalse(result.complete());
        assertEquals(Set.of(missing), result.missing());
        assertEquals(Set.of(wrong), result.wrongMaterial());
        assertEquals(Set.of(extra), result.extra());
    }

    @Test
    void allVanillaAirTypesReportAMissingComponent() {
        BuildPattern bridge = VillageJobs.pattern(Kind.BRIDGE);
        for (String air : Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air")) {
            Map<GridPos, String> actual = new HashMap<>(bridge.required());
            actual.put(new GridPos(0, 0), air);
            BuildPattern.Inspection result = bridge.inspect(actual);
            assertEquals(Set.of(new GridPos(0, 0)), result.missing(), air);
            assertTrue(result.wrongMaterial().isEmpty(), air);
        }
    }

    @Test
    void surroundingTerrainDoesNotMakeAnOtherwiseCorrectBuildFail() {
        BuildPattern villageGarden = VillageJobs.pattern(Kind.GARDEN);
        BuildPattern garden = new BuildPattern(villageGarden.required(), villageGarden.forbidden());
        Map<GridPos, String> actual = new HashMap<>(garden.required());
        actual.put(new GridPos(4, 3), "minecraft:wheat");
        actual.put(new GridPos(1, 1), "minecraft:grass_block");
        actual.put(new GridPos(90, 90), "minecraft:oak_fence");

        assertTrue(garden.inspect(actual).complete());

        actual.put(new GridPos(4, 3), "minecraft:oak_fence");
        assertEquals(Set.of(new GridPos(4, 3)), garden.inspect(actual).extra());
    }

    @Test
    void preparedVillageSitesNeedFreeInteriorAndBridgeEdgesRegardlessOfMaterial() {
        BuildPattern garden = VillageJobs.pattern(Kind.GARDEN);
        Map<GridPos, String> actualGarden = new HashMap<>(garden.required());
        actualGarden.put(new GridPos(4, 3), "minecraft:stone");
        assertEquals(Set.of(new GridPos(4, 3)), garden.inspect(actualGarden).extra());

        BuildPattern bridge = VillageJobs.pattern(Kind.BRIDGE);
        Map<GridPos, String> actualBridge = new HashMap<>(bridge.required());
        actualBridge.put(new GridPos(5, 0), "minecraft:birch_planks");
        actualBridge.put(new GridPos(-1, 0), "minecraft:stone_bricks");
        assertEquals(Set.of(new GridPos(5, 0)), bridge.inspect(actualBridge).extra());
        actualBridge.put(new GridPos(5, 0), "minecraft:air");
        assertTrue(bridge.inspect(actualBridge).complete());
    }

    @Test
    void emptyFieldProducesMissingBlocksAndNoSpuriousExtras() {
        BuildPattern bridge = VillageJobs.pattern(Kind.BRIDGE);
        BuildPattern.Inspection result = bridge.inspect(Map.of());

        assertEquals(10, result.missing().size());
        assertTrue(result.wrongMaterial().isEmpty());
        assertTrue(result.extra().isEmpty());
        assertFalse(result.complete());
    }

    @Test
    void fieldAndInspectionCollectionsAreDefensiveCopies() {
        GridPos target = new GridPos(0, 0);
        GridPos edge = new GridPos(1, 0);
        Map<GridPos, String> required = new HashMap<>(Map.of(target, "minecraft:oak_planks"));
        Set<GridPos> forbidden = new HashSet<>(Set.of(edge));
        BuildPattern pattern = new BuildPattern(required, forbidden);
        required.clear();
        forbidden.clear();

        assertEquals(1, pattern.required().size());
        assertEquals(Set.of(edge), pattern.forbidden());
        assertThrows(UnsupportedOperationException.class, () -> pattern.required().clear());
        assertThrows(UnsupportedOperationException.class, () -> pattern.forbidden().clear());
        assertThrows(UnsupportedOperationException.class, () -> pattern.inspect(Map.of()).missing().clear());
    }

    @Test
    void invalidPlansFailBeforeTheyArePlacedInTheWorld() {
        GridPos target = new GridPos(0, 0);
        assertThrows(IllegalArgumentException.class, () -> new BuildPattern(Map.of(), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuildPattern(Map.of(target, "minecraft:air"), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuildPattern(Map.of(target, ""), Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildPattern(Map.of(target, "minecraft:oak_planks"), Set.of(target)));
    }
}
