package de.kompass4mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import de.kompass4mc.village.VillageJobs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class VillageStateTest {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final String PLAYER_ONE = "216990a7-2351-48ac-b8ee-82327e23179b";
    private static final String PLAYER_TWO = "b8b20796-67e7-4193-857f-aa962df68a1a";
    private static final String BYSTANDER = "81f23665-df5f-49d2-a219-0767fed0a901";
    private static final String GARDEN = "garden_fence";

    @TempDir
    Path directory;

    @Test
    void contributionRequiresAcceptanceAndAnUnfinishedProject() {
        VillageState.Project project = new VillageState.Project();

        assertFalse(project.contribute(PLAYER_ONE));
        assertTrue(project.accept(PLAYER_ONE));
        assertFalse(project.accept(PLAYER_ONE));
        assertTrue(project.contribute(PLAYER_ONE));
        assertFalse(project.contribute(PLAYER_ONE));
        assertFalse(project.contribute(BYSTANDER));
        assertEquals(Set.of(PLAYER_ONE), project.accepted);
        assertEquals(Set.of(PLAYER_ONE), project.contributors);

        project.completed = true;
        assertFalse(project.accept(PLAYER_TWO), "A finished village build cannot be accepted again.");
        assertFalse(project.contribute(PLAYER_TWO));
        assertFalse(project.contribute(PLAYER_ONE));
        assertEquals(Set.of(PLAYER_ONE), project.accepted);
        assertEquals(Set.of(PLAYER_ONE), project.contributors);
    }

    @Test
    void onlyActualContributorsCanClaimOnceAfterTheSharedCompletion() {
        VillageState.Project project = new VillageState.Project();
        project.accept(PLAYER_ONE);
        project.accept(PLAYER_TWO);
        project.contribute(PLAYER_ONE);

        assertFalse(project.canClaim(PLAYER_ONE));
        assertFalse(project.claim(PLAYER_ONE));
        project.completed = true;
        assertFalse(project.canClaim(PLAYER_TWO), "Accepting alone does not earn a building reward.");
        assertFalse(project.claim(PLAYER_TWO));
        assertFalse(project.claim(BYSTANDER));
        assertTrue(project.canClaim(PLAYER_ONE));
        assertTrue(project.claim(PLAYER_ONE));
        assertFalse(project.canClaim(PLAYER_ONE));
        assertFalse(project.claim(PLAYER_ONE));
        assertEquals(Set.of(PLAYER_ONE), project.rewardClaimed);
    }

    @Test
    void copyKeepsIndependentResidentsProjectsAndPlayerSetsForRollback() {
        VillageState original = village();
        VillageState.Project project = original.projects.get(GARDEN);
        project.accept(PLAYER_ONE);
        project.contribute(PLAYER_ONE);
        project.suppliesIssued = true;
        project.completed = true;
        project.claim(PLAYER_ONE);

        VillageState copy = original.copy();
        VillageState.Project copyProject = copy.projects.get(GARDEN);
        assertNotSame(original, copy);
        assertNotSame(original.residents, copy.residents);
        assertNotSame(original.projects, copy.projects);
        assertNotSame(project, copyProject);
        assertNotSame(project.accepted, copyProject.accepted);
        assertNotSame(project.contributors, copyProject.contributors);
        assertNotSame(project.rewardClaimed, copyProject.rewardClaimed);
        assertEquals(original.dimension, copy.dimension);
        assertEquals(original.x, copy.x);
        assertEquals(original.floorY, copy.floorY);
        assertEquals(original.z, copy.z);
        assertTrue(copyProject.completed);
        assertTrue(copyProject.suppliesIssued);

        copy.x++;
        copy.residents.clear();
        copyProject.accepted.clear();
        copyProject.contributors.clear();
        copyProject.rewardClaimed.clear();
        copyProject.completed = false;
        copyProject.suppliesIssued = false;
        copy.projects.remove("reading_bridge");

        assertEquals(-30, original.x);
        assertFalse(original.residents.isEmpty());
        assertTrue(original.projects.containsKey("reading_bridge"));
        assertEquals(Set.of(PLAYER_ONE), project.accepted);
        assertEquals(Set.of(PLAYER_ONE), project.contributors);
        assertEquals(Set.of(PLAYER_ONE), project.rewardClaimed);
        assertTrue(project.completed);
        assertTrue(project.suppliesIssued);
    }

    @Test
    void legacyVersionOneWithoutVillageLoadsAndCanGainAVillageWithoutLosingQuestProgress() throws IOException {
        Path path = directory.resolve("kompass4mc.json");
        String legacy = """
                {"version":1,"players":{"%s":{"discovered":["math_subtract_1"],
                "completed":["math_subtract_1"],"activeQuest":null}},"hidingPlaces":{}}
                """.formatted(PLAYER_ONE);
        Files.writeString(path, legacy, StandardCharsets.UTF_8);

        WorldState state = new WorldState(path);
        assertNull(state.village());
        assertEquals(Set.of("math_subtract_1"), state.progress(PLAYER_ONE).completed);
        state.village(village());
        state.save();

        WorldState restored = new WorldState(path);
        assertEquals("minecraft:overworld", restored.village().dimension);
        assertEquals(Set.of("math_subtract_1"), restored.progress(PLAYER_ONE).discovered);
        assertEquals(Set.of("math_subtract_1"), restored.progress(PLAYER_ONE).completed);
        assertTrue(restored.hidingPlaces().isEmpty());
    }

    @Test
    void sharedProjectAndEachContributorsSingleRewardSurviveRestarts() throws IOException {
        Path path = directory.resolve("world/data/kompass4mc.json");
        WorldState state = new WorldState(path);
        VillageState village = village();
        VillageState.Project project = village.projects.get(GARDEN);
        project.accept(PLAYER_ONE);
        project.accept(PLAYER_TWO);
        project.accept(BYSTANDER);
        project.contribute(PLAYER_ONE);
        project.contribute(PLAYER_TWO);
        project.suppliesIssued = true;
        project.completed = true;
        assertTrue(project.claim(PLAYER_ONE));
        state.village(village);
        state.save();

        WorldState restored = new WorldState(path);
        VillageState restoredVillage = restored.village();
        VillageState.Project restoredProject = restoredVillage.projects.get(GARDEN);
        assertEquals(village.dimension, restoredVillage.dimension);
        assertEquals(village.x, restoredVillage.x);
        assertEquals(village.floorY, restoredVillage.floorY);
        assertEquals(village.z, restoredVillage.z);
        assertEquals(village.residents, restoredVillage.residents);
        assertEquals(village.projects.keySet(), restoredVillage.projects.keySet());
        assertEquals(Set.of(PLAYER_ONE, PLAYER_TWO, BYSTANDER), restoredProject.accepted);
        assertEquals(Set.of(PLAYER_ONE, PLAYER_TWO), restoredProject.contributors);
        assertTrue(restoredProject.completed);
        assertTrue(restoredProject.suppliesIssued);
        assertFalse(restoredProject.claim(PLAYER_ONE));
        assertFalse(restoredProject.claim(BYSTANDER));
        assertTrue(restoredProject.claim(PLAYER_TWO));
        assertFalse(restoredProject.claim(PLAYER_TWO));
        restored.save();

        VillageState.Project savedAgain = new WorldState(path).village().projects.get(GARDEN);
        assertEquals(Set.of(PLAYER_ONE, PLAYER_TWO), savedAgain.rewardClaimed);
        assertFalse(savedAgain.canClaim(PLAYER_ONE));
        assertFalse(savedAgain.canClaim(PLAYER_TWO));
        assertFalse(savedAgain.canClaim(BYSTANDER));
    }

    static Stream<Arguments> invalidVillages() {
        List<Arguments> cases = new ArrayList<>();
        cases.add(Arguments.of(Named.of("null village", JsonNull.INSTANCE)));
        cases.add(Arguments.of(Named.of("array village", new JsonArray())));
        for (String field : List.of("dimension", "x", "floorY", "z", "residents", "projects")) {
            cases.add(invalid("missing " + field, village -> village.remove(field)));
            cases.add(invalid("null " + field, village -> village.add(field, JsonNull.INSTANCE)));
        }
        cases.add(invalid("blank dimension", village -> village.addProperty("dimension", "  ")));
        cases.add(invalid("numeric dimension", village -> village.addProperty("dimension", 17)));
        cases.add(invalid("array dimension", village -> village.add("dimension", new JsonArray())));
        cases.add(invalid("fractional x", village -> village.addProperty("x", 1.5)));
        cases.add(invalid("string floorY", village -> village.addProperty("floorY", "64")));
        cases.add(invalid("boolean z", village -> village.addProperty("z", false)));
        cases.add(invalid("overflowing x", village -> village.addProperty("x", 2147483648L)));
        cases.add(invalid("residents array", village -> village.add("residents", new JsonArray())));
        cases.add(invalid("resident invalid UUID", village -> village.getAsJsonObject("residents")
                .addProperty(GARDEN, "not-a-uuid")));
        cases.add(invalid("resident null", village -> village.getAsJsonObject("residents")
                .add(GARDEN, JsonNull.INSTANCE)));
        cases.add(invalid("duplicate resident UUID", village -> village.getAsJsonObject("residents")
                .addProperty("reading_bridge", village.getAsJsonObject("residents").get(GARDEN).getAsString())));
        cases.add(invalid("projects array", village -> village.add("projects", new JsonArray())));
        cases.add(invalid("null project", village -> village.getAsJsonObject("projects").add(GARDEN, JsonNull.INSTANCE)));
        for (String field : List.of("accepted", "contributors", "rewardClaimed")) {
            cases.add(invalid("missing project " + field, village -> project(village).remove(field)));
            cases.add(invalid("nonarray project " + field, village -> project(village).addProperty(field, "[]")));
            cases.add(invalid("invalid player in " + field, village -> {
                JsonArray players = new JsonArray();
                players.add("not-a-player-uuid");
                project(village).add(field, players);
            }));
        }
        for (String field : List.of("completed", "suppliesIssued")) {
            cases.add(invalid("missing project " + field, village -> project(village).remove(field)));
            cases.add(invalid("string project " + field, village -> project(village).addProperty(field, "false")));
        }
        cases.add(invalid("contributor never accepted", village -> project(village).getAsJsonArray("contributors")
                .add(PLAYER_ONE)));
        cases.add(invalid("reward claimed by noncontributor", village -> {
            project(village).addProperty("completed", true);
            project(village).getAsJsonArray("accepted").add(PLAYER_ONE);
            project(village).getAsJsonArray("rewardClaimed").add(PLAYER_ONE);
        }));
        cases.add(invalid("reward claimed before completion", village -> {
            project(village).getAsJsonArray("accepted").add(PLAYER_ONE);
            project(village).getAsJsonArray("contributors").add(PLAYER_ONE);
            project(village).getAsJsonArray("rewardClaimed").add(PLAYER_ONE);
        }));
        return cases.stream();
    }

    @ParameterizedTest(name = "reject damaged village without resetting it: {0}")
    @MethodSource("invalidVillages")
    void damagedVillageIsRejectedAndOriginalSaveRemainsUntouched(JsonElement invalidVillage) throws IOException {
        Path path = directory.resolve("kompass4mc.json");
        JsonObject document = new JsonObject();
        document.addProperty("version", 1);
        document.add("players", new JsonObject());
        document.add("hidingPlaces", new JsonObject());
        document.add("village", invalidVillage);
        String invalid = GSON.toJson(document);
        Files.writeString(path, invalid, StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> new WorldState(path));
        assertEquals(invalid, Files.readString(path, StandardCharsets.UTF_8));
        try (var files = Files.list(directory)) {
            assertEquals(Set.of(path), files.collect(java.util.stream.Collectors.toSet()));
        }
    }

    private static Arguments invalid(String name, Consumer<JsonObject> mutate) {
        JsonObject document = GSON.toJsonTree(village()).getAsJsonObject();
        mutate.accept(document);
        return Arguments.of(Named.of(name, document));
    }

    private static JsonObject project(JsonObject village) {
        return village.getAsJsonObject("projects").getAsJsonObject(GARDEN);
    }

    private static VillageState village() {
        VillageState village = new VillageState();
        village.dimension = "minecraft:overworld";
        village.x = -30;
        village.floorY = 64;
        village.z = 100;
        for (int index = 0; index < VillageJobs.all().size(); index++) {
            String id = VillageJobs.all().get(index).id();
            village.residents.put(id, new java.util.UUID(0, index + 1).toString());
            village.projects.put(id, new VillageState.Project());
        }
        return village;
    }
}
