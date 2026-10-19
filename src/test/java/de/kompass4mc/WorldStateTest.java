package de.kompass4mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.progress.LearningProgress;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WorldStateTest {
    private static final String PLAYER_ONE = "216990a7-2351-48ac-b8ee-82327e23179b";
    private static final String PLAYER_TWO = "b8b20796-67e7-4193-857f-aa962df68a1a";

    @TempDir
    Path directory;

    @Test
    void cardsAndThreeIndependentOffersSurviveReloadIncludingIssuedBlueprint() throws IOException {
        Path path = directory.resolve("cards.json");
        WorldState state = new WorldState(path);
        var progress = state.learningProgress(PLAYER_ONE, "Kind");
        for (var role : de.kompass4mc.learning.ChallengeRole.values()) {
            var task = LearningCatalog.all().stream().filter(t -> t.npcName().equals(role.residentName()))
                    .findFirst().orElseThrow();
            var offer = new de.kompass4mc.progress.ChallengeOffer(task.id(), 5000);
            progress.offers.put(role.residentName(), offer);
            progress.recordChallengeAnswer(task, task.expectedAnswer(), 1000, false);
        }
        var build = new de.kompass4mc.progress.ChallengeOffer("build_towers_0", 6000);
        build.materialsIssued = true; build.hintUsed = true;
        build.site = new de.kompass4mc.progress.ChallengeOffer.Site("minecraft:overworld", -25, 65, 42);
        progress.offers.put("Mila", build);
        state.save();
        var loaded = new WorldState(path).learningProgress(PLAYER_ONE, "Kind");
        assertEquals(3, loaded.offers.size());
        assertEquals(3, loaded.cards.size());
        assertEquals(build.ticket, loaded.offers.get("Mila").ticket);
        assertTrue(loaded.offers.get("Mila").materialsIssued);
        assertTrue(loaded.offers.get("Mila").hintUsed);
        assertEquals(build.site, loaded.offers.get("Mila").site);
        loaded.cards.values().forEach(c -> assertEquals(2, c.box));
        assertTrue(new WorldState(path).learningProgress(PLAYER_TWO, "Anderes Kind").offers.isEmpty());
    }

    @Test
    void preCardProgressKeepsItsHistoryAndStartsWithEmptyDeckAndOffers() throws IOException {
        Path path = directory.resolve("old-progress.json");
        Files.writeString(path, "{\"version\":1,\"players\":{},\"hidingPlaces\":{},\"learning\":{\"p\":{\"playerName\":\"Kind\","
                + "\"completed\":[\"subtract_store\"],\"rewarded\":[\"subtract_store\"],"
                + "\"activeTaskId\":\"grammar_infinitive_1\"}}}");
        var loaded = new WorldState(path).learningProgress("p", "Kind");
        assertTrue(loaded.completed.contains("subtract_store"));
        assertTrue(loaded.rewarded.contains("subtract_store"));
        assertEquals("grammar_infinitive_1", loaded.activeTaskId);
        assertTrue(loaded.cards.isEmpty()); assertTrue(loaded.offers.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\"cards\":null", "\"cards\":{\"subtract_store\":{}}",
        "\"cards\":{\"subtract_store\":{\"box\":6,\"reviews\":1,\"lastReviewMillis\":2,\"dueMillis\":3}}",
        "\"cards\":{\"subtract_store\":{\"box\":2,\"reviews\":1,\"lastReviewMillis\":4,\"dueMillis\":3}}",
        "\"offers\":null", "\"offers\":{\"Jaro\":{}}",
        "\"offers\":{\"Lio\":{\"ticket\":\"a\",\"taskId\":\"subtract_store\",\"restockAt\":1,\"completed\":false,\"rewarded\":false,\"materialsIssued\":false}}"
    })
    void malformedCardsAndOffersNeverOverwriteOriginal(String fragment) throws IOException {
        Path path = directory.resolve("invalid-card.json");
        String original = "{\"version\":1,\"players\":{},\"hidingPlaces\":{},\"learning\":{\"p\":{" + fragment + "}}}";
        Files.writeString(path, original);
        assertThrows(IOException.class, () -> new WorldState(path));
        assertEquals(original, Files.readString(path));
    }

    @Test
    void discoveriesCompletionsActiveQuestAndHidingPlacesSurviveReload() throws IOException {
        Path path = directory.resolve("world/data/kompass4mc.json");
        WorldState state = new WorldState(path);
        WorldState.Progress first = state.progress(PLAYER_ONE);
        first.discovered.addAll(Set.of("math_subtract_1", "german_read_library"));
        first.completed.add("math_subtract_1");
        first.activeQuest = "german_read_library";
        WorldState.Progress second = state.progress(PLAYER_TWO);
        second.discovered.add("math_length");
        second.activeQuest = "math_length";

        WorldState.HidingPlace library = new WorldState.HidingPlace(
                "minecraft:overworld", -12, 73, 89, "german_read_library");
        WorldState.HidingPlace tunnel = new WorldState.HidingPlace(
                "minecraft:the_nether", -12, 73, 89, "math_length");
        String libraryKey = WorldState.positionKey(library.dimension(), library.x(), library.y(), library.z());
        String tunnelKey = WorldState.positionKey(tunnel.dimension(), tunnel.x(), tunnel.y(), tunnel.z());
        state.hidingPlaces().put(libraryKey, library);
        state.hidingPlaces().put(tunnelKey, tunnel);
        state.save();

        WorldState restored = new WorldState(path);
        assertEquals(first.discovered, restored.progress(PLAYER_ONE).discovered);
        assertEquals(Set.of("math_subtract_1"), restored.progress(PLAYER_ONE).completed);
        assertEquals("german_read_library", restored.progress(PLAYER_ONE).activeQuest);
        assertEquals(Set.of("math_length"), restored.progress(PLAYER_TWO).discovered);
        assertTrue(restored.progress(PLAYER_TWO).completed.isEmpty());
        assertEquals("math_length", restored.progress(PLAYER_TWO).activeQuest);
        assertEquals(2, restored.hidingPlaces().size());
        assertEquals(library, restored.hidingPlaces().get(libraryKey));
        assertEquals(tunnel, restored.hidingPlaces().get(tunnelKey));

        restored.progress(PLAYER_ONE).completed.add("german_read_library");
        restored.progress(PLAYER_ONE).activeQuest = null;
        restored.hidingPlaces().remove(tunnelKey);
        restored.save();
        WorldState savedAgain = new WorldState(path);
        assertEquals(Set.of("math_subtract_1", "german_read_library"), savedAgain.progress(PLAYER_ONE).completed);
        assertNull(savedAgain.progress(PLAYER_ONE).activeQuest);
        assertEquals(Set.of(libraryKey), savedAgain.hidingPlaces().keySet());
        assertEquals("math_length", savedAgain.progress(PLAYER_TWO).activeQuest);
        try (var files = Files.list(path.getParent())) {
            assertEquals(Set.of(path), files.collect(java.util.stream.Collectors.toSet()),
                    "Atomic saves must not leave temporary files behind.");
        }
    }

    @Test
    void absentFileStartsWithIndependentEmptyPlayerProgressWithoutWritingAnything() throws IOException {
        Path path = directory.resolve("new-world/data/kompass4mc.json");
        WorldState state = new WorldState(path);
        assertTrue(state.progress(PLAYER_ONE).discovered.isEmpty());
        assertTrue(state.progress(PLAYER_ONE).completed.isEmpty());
        assertNull(state.progress(PLAYER_ONE).activeQuest);
        assertTrue(state.hidingPlaces().isEmpty());
        state.progress(PLAYER_ONE).discovered.add("math_subtract_1");
        assertTrue(state.progress(PLAYER_TWO).discovered.isEmpty());
        assertFalse(Files.exists(path));
    }

    @Test
    void oldVersionOneSavesGainLearningDefaultsWithoutChangingLegacyProgress() throws IOException {
        Path path = directory.resolve("kompass4mc.json");
        String original = """
                {"version":1,"players":{"old-player":{"discovered":["math_length"],
                "completed":[],"activeQuest":"math_length"}},"hidingPlaces":{}}
                """;
        Files.writeString(path, original, StandardCharsets.UTF_8);

        WorldState restored = new WorldState(path);
        assertTrue(restored.automaticLearningNpcs());
        assertTrue(restored.learningProgresses().isEmpty());
        assertEquals(Set.of("math_length"), restored.progress("old-player").discovered);
        assertEquals("math_length", restored.progress("old-player").activeQuest);
        assertNull(restored.village());
        assertEquals(original, Files.readString(path, StandardCharsets.UTF_8), "Loading is read-only.");

        restored.save();
        var saved = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals(1, saved.get("version").getAsInt());
        assertTrue(saved.getAsJsonObject("learning").isEmpty());
        assertTrue(saved.get("automaticLearningNpcs").getAsBoolean());
        assertEquals(Set.of("math_length"), new WorldState(path).progress("old-player").discovered);
    }

    @Test
    void learningAttemptsHintsRewardsAndSpawnerSettingSurviveReloadPerPlayer() throws IOException {
        Path path = directory.resolve("world/data/kompass4mc.json");
        WorldState state = new WorldState(path);
        var firstTask = LearningCatalog.byId("subtract_borrow_1").orElseThrow();
        var secondTask = LearningCatalog.byId("reading_workshop").orElseThrow();
        LearningProgress first = state.learningProgress(PLAYER_ONE, "Lena");
        first.activeTaskId = firstTask.id();
        first.recordAnswer(firstTask, "4846", 100, false);
        first.recordHint(firstTask, 200);
        first.recordAnswer(firstTask, "3154", 300, true);
        assertTrue(first.markRewarded(firstTask.id()));
        LearningProgress second = state.learningProgress(PLAYER_TWO, "Ben");
        second.activeTaskId = secondTask.id();
        second.recordProceduralIssue(secondTask, "Dialog noch offen", 400);
        state.automaticLearningNpcs(false);
        state.progress(PLAYER_ONE).completed.add("math_length");
        state.save();

        WorldState restored = new WorldState(path);
        assertFalse(restored.automaticLearningNpcs());
        assertEquals(2, restored.learningProgresses().size());
        LearningProgress loadedFirst = restored.learningProgresses().get(PLAYER_ONE);
        assertEquals("Lena", loadedFirst.playerName);
        assertEquals(firstTask.id(), loadedFirst.activeTaskId);
        assertEquals(Set.of(firstTask.id()), loadedFirst.completed);
        assertEquals(Set.of(firstTask.id()), loadedFirst.rewarded);
        assertEquals(first.attempts, loadedFirst.attempts);
        assertEquals(2, loadedFirst.stats.get(firstTask.id()).attempts);
        assertEquals(1, loadedFirst.stats.get(firstTask.id()).incorrect);
        assertEquals(1, loadedFirst.stats.get(firstTask.id()).hints);
        assertEquals("4846", loadedFirst.stats.get(firstTask.id()).patternEvidence.get("borrowing").answer());
        assertEquals("Ben", restored.learningProgresses().get(PLAYER_TWO).playerName);
        assertTrue(restored.learningProgresses().get(PLAYER_TWO).completed.isEmpty());
        assertEquals(1, restored.learningProgresses().get(PLAYER_TWO).stats.get(secondTask.id()).proceduralIssues);
        assertEquals(Set.of("math_length"), restored.progress(PLAYER_ONE).completed);

        assertSame(loadedFirst, restored.learningProgress(PLAYER_ONE, "LenaNeu"));
        assertEquals("LenaNeu", loadedFirst.playerName);
        assertEquals("Ben", restored.learningProgresses().get(PLAYER_TWO).playerName);
        restored.automaticLearningNpcs(true);
        restored.save();
        assertTrue(new WorldState(path).automaticLearningNpcs());
        assertEquals("LenaNeu", new WorldState(path).learningProgresses().get(PLAYER_ONE).playerName);
    }

    @Test
    void unseenPlayersHaveIndependentLearningNotesAndUnknownTaskIdsRemainReadable() throws IOException {
        Path path = directory.resolve("kompass4mc.json");
        WorldState state = new WorldState(path);
        LearningProgress first = state.learningProgress(PLAYER_ONE, "Lena");
        first.activeTaskId = "a_task_from_a_later_version";
        first.completed.add("a_removed_task");
        first.rewarded.add("a_removed_task");
        LearningProgress second = state.learningProgress(PLAYER_TWO, "Ben");
        assertTrue(second.completed.isEmpty());
        assertTrue(second.stats.isEmpty());
        assertFalse(Files.exists(path));
        state.save();

        WorldState restored = new WorldState(path);
        assertEquals("a_task_from_a_later_version", restored.learningProgresses().get(PLAYER_ONE).activeTaskId);
        assertEquals(Set.of("a_removed_task"), restored.learningProgresses().get(PLAYER_ONE).rewarded);
        assertTrue(restored.learningProgresses().get(PLAYER_TWO).rewarded.isEmpty());
    }

    @ParameterizedTest(name = "malformed learning fields do not replace saved progress: {index}")
    @ValueSource(strings = {
            "\"learning\":null",
            "\"learning\":[]",
            "\"learning\":true",
            "\"automaticLearningNpcs\":null",
            "\"automaticLearningNpcs\":\"true\"",
            "\"automaticLearningNpcs\":0",
            "\"automaticLearningNpcs\":[]",
            "\"learning\":{\"p\":null}",
            "\"learning\":{\"p\":[]}",
            "\"learning\":{\"p\":{\"playerName\":null}}",
            "\"learning\":{\"p\":{\"playerName\":23}}",
            "\"learning\":{\"p\":{\"activeTaskId\":42}}",
            "\"learning\":{\"p\":{\"completed\":null}}",
            "\"learning\":{\"p\":{\"completed\":[false]}}",
            "\"learning\":{\"p\":{\"rewarded\":null}}",
            "\"learning\":{\"p\":{\"rewarded\":[\"subtract_store\"]}}",
            "\"learning\":{\"p\":{\"attempts\":null}}",
            "\"learning\":{\"p\":{\"attempts\":[null]}}",
            "\"learning\":{\"p\":{\"attempts\":[{}]}}",
            "\"learning\":{\"p\":{\"stats\":null}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":null}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"attempts\":-1}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"attempts\":1}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"attempts\":1.5}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"attempts\":\"2\"}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"attempts\":null}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"hints\":2147483648}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"solvedAt\":1.5}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"lastCorrectAt\":1.5}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"lastCorrectAt\":\"100\"}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"firstAttemptCorrect\":\"true\"}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"firstAttemptHintUsed\":0}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"patternEvidence\":null}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_store\":{\"patternEvidence\":{\"borrowing\":null}}}}}",
            "\"learning\":{\"p\":{\"stats\":{\"subtract_borrow_1\":{\"patternEvidence\":{\"borrowing\":{\"timeMillis\":1,\"answer\":\"3154\"}}}}}}",
            "\"learning\":{\"p\":{\"attempts\":[{\"taskId\":\"subtract_store\",\"timeMillis\":1,\"answer\":\"624\",\"correct\":false,\"hintUsed\":false,\"patternIds\":[],\"type\":\"ANSWER\"}]}}",
            "\"learning\":{\"p\":{\"attempts\":[{\"taskId\":\"subtract_store\",\"timeMillis\":1,\"answer\":\"624\",\"correct\":true,\"hintUsed\":false,\"patternIds\":[],\"type\":\"OTHER\"}]}}"
    })
    void malformedLearningFieldsAreRejectedWithoutOverwritingTheOriginal(String fragment) throws IOException {
        Path path = directory.resolve("kompass4mc.json");
        String original = "{\"version\":1,\"players\":{},\"hidingPlaces\":{}," + fragment + "}";
        Files.writeString(path, original, StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> new WorldState(path), fragment);
        assertEquals(original, Files.readString(path, StandardCharsets.UTF_8));
        try (var files = Files.list(directory)) {
            assertEquals(Set.of(path), files.collect(java.util.stream.Collectors.toSet()));
        }
    }

    @ParameterizedTest(name = "damaged state is rejected without replacing it: {index}")
    @ValueSource(strings = {
            "",
            "{",
            "null",
            "[]",
            "{}",
            "{\"version\":1,\"players\":{}}",
            "{\"version\":1,\"hidingPlaces\":{}}",
            "{\"players\":{},\"hidingPlaces\":{}}",
            "{\"version\":2,\"players\":{},\"hidingPlaces\":{}}",
            "{\"version\":1,\"players\":null,\"hidingPlaces\":{}}",
            "{\"version\":1,\"players\":{\"player\":null},\"hidingPlaces\":{}}",
            "{\"version\":1,\"players\":{\"player\":{\"discovered\":null,\"completed\":[]}},\"hidingPlaces\":{}}",
            "{\"version\":1,\"players\":{\"player\":{\"discovered\":[],\"completed\":null}},\"hidingPlaces\":{}}",
            "{\"version\":1,\"players\":{\"player\":{}},\"hidingPlaces\":{}}",
            "{\"version\":1,\"players\":{\"player\":{\"discovered\":[\"math_subtract_1\"]"
    })
    void invalidOrTruncatedStateFailsWithoutResettingOrOverwritingOriginal(String invalid) throws IOException {
        Path path = directory.resolve("kompass4mc.json");
        Files.writeString(path, invalid, StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> new WorldState(path));
        assertEquals(invalid, Files.readString(path, StandardCharsets.UTF_8));
        try (var files = Files.list(directory)) {
            assertEquals(Set.of(path), files.collect(java.util.stream.Collectors.toSet()));
        }
    }
}
