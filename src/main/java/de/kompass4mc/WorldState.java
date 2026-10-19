package de.kompass4mc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.kompass4mc.progress.LearningProgress;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** World-local state. Every successful answer is saved before its reward is issued. */
public final class WorldState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private final Data data;

    public WorldState(Path path) throws IOException {
        this.path = path;
        if (Files.exists(path)) {
            try {
                JsonElement document = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
                if (!document.isJsonObject()) {
                    throw new IOException("Ungültiger Kompass-Spielstand: " + path);
                }
                JsonObject object = document.getAsJsonObject();
                if (!object.has("version") || !object.has("players") || !object.has("hidingPlaces")
                        || !object.get("players").isJsonObject() || !object.get("hidingPlaces").isJsonObject()) {
                    throw new IOException("Unvollständiger Kompass-Spielstand: " + path);
                }
                for (JsonElement player : object.getAsJsonObject("players").asMap().values()) {
                    if (!player.isJsonObject() || !player.getAsJsonObject().has("discovered")
                            || !player.getAsJsonObject().has("completed")
                            || !player.getAsJsonObject().get("discovered").isJsonArray()
                            || !player.getAsJsonObject().get("completed").isJsonArray()) {
                        throw new IOException("Unvollständiger Spielerfortschritt: " + path);
                    }
                }
                if (object.has("village")) {
                    VillageState.validateDocument(object.get("village"));
                }
                validateLearningDocument(object);
                data = GSON.fromJson(document, Data.class);
                if (data == null || data.version != 1 || data.players == null || data.hidingPlaces == null
                        || data.learning == null) {
                    throw new IOException("Ungültiger Kompass-Spielstand: " + path);
                }
                for (Progress progress : data.players.values()) {
                    if (progress == null || progress.discovered == null || progress.completed == null) {
                        throw new IOException("Unvollständiger Kompass-Spielstand: " + path);
                    }
                }
                for (HidingPlace place : data.hidingPlaces.values()) {
                    if (place == null || place.dimension() == null || place.questId() == null
                            || place.dimension().isBlank() || place.questId().isBlank()) {
                        throw new IOException("Unvollständiges Quest-Versteck: " + path);
                    }
                }
                for (LearningProgress progress : data.learning.values()) {
                    if (progress == null) {
                        throw new IOException("Unvollständiger Lernfortschritt: " + path);
                    }
                    progress.validate();
                }
            } catch (RuntimeException exception) {
                throw new IOException("Kompass-Spielstand kann nicht gelesen werden: " + path, exception);
            }
        } else {
            data = new Data();
        }
    }

    public Progress progress(String uuid) {
        return data.players.computeIfAbsent(uuid, ignored -> new Progress());
    }

    public Map<String, HidingPlace> hidingPlaces() {
        return data.hidingPlaces;
    }

    public VillageState village() {
        return data.village;
    }

    public void village(VillageState village) {
        data.village = village;
    }

    public Map<String, LearningProgress> learningProgresses() {
        return data.learning;
    }

    public LearningProgress learningProgress(String uuid, String name) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
        LearningProgress progress = data.learning.computeIfAbsent(uuid, ignored -> new LearningProgress());
        progress.playerName = name;
        return progress;
    }

    public boolean automaticLearningNpcs() {
        return data.automaticLearningNpcs;
    }

    public void automaticLearningNpcs(boolean enabled) {
        data.automaticLearningNpcs = enabled;
    }

    /** Missing new fields retain their defaults in older v1 saves; malformed fields never do. */
    private static void validateLearningDocument(JsonObject document) throws IOException {
        if (document.has("automaticLearningNpcs")) {
            requireBoolean(document.get("automaticLearningNpcs"));
        }
        if (!document.has("learning")) {
            return;
        }
        JsonObject learning = requireObject(document.get("learning"));
        for (Map.Entry<String, JsonElement> entry : learning.asMap().entrySet()) {
            if (entry.getKey().isBlank()) {
                throw new IOException("Ungültige Lernfortschritt-Spieler-ID");
            }
            JsonObject player = requireObject(entry.getValue());
            if (player.has("playerName")) {
                requireString(player.get("playerName"));
            }
            if (player.has("activeTaskId") && !player.get("activeTaskId").isJsonNull()) {
                requireString(player.get("activeTaskId"));
            }
            for (String field : Set.of("completed", "rewarded")) {
                if (player.has(field)) {
                    requireStringArray(player.get(field));
                }
            }
            if (player.has("attempts")) {
                JsonElement attempts = player.get("attempts");
                if (!attempts.isJsonArray()) {
                    throw new IOException("Ungültige Lernantworten");
                }
                for (JsonElement value : attempts.getAsJsonArray()) {
                    JsonObject attempt = requireObject(value);
                    requireString(attempt.get("taskId"));
                    requireInteger(attempt.get("timeMillis"), false);
                    requireString(attempt.get("answer"));
                    requireBoolean(attempt.get("correct"));
                    requireBoolean(attempt.get("hintUsed"));
                    requireStringArray(attempt.get("patternIds"));
                    requireString(attempt.get("type"));
                    if (!Set.of("ANSWER", "HINT", "PROCEDURAL").contains(attempt.get("type").getAsString())) {
                        throw new IOException("Unbekannte Lernantwort-Art");
                    }
                }
            }
            if (player.has("stats")) {
                for (JsonElement value : requireObject(player.get("stats")).asMap().values()) {
                    JsonObject stats = requireObject(value);
                    for (String field : Set.of("attempts", "incorrect", "hints", "proceduralIssues")) {
                        if (stats.has(field)) {
                            requireInteger(stats.get(field), true);
                        }
                    }
                    if (stats.has("firstAttemptHintUsed")) {
                        requireBoolean(stats.get("firstAttemptHintUsed"));
                    }
                    if (stats.has("firstAttemptCorrect") && !stats.get("firstAttemptCorrect").isJsonNull()) {
                        requireBoolean(stats.get("firstAttemptCorrect"));
                    }
                    for (String field : Set.of("solvedAt", "lastCorrectAt")) {
                        if (stats.has(field) && !stats.get(field).isJsonNull()) {
                            requireInteger(stats.get(field), false);
                        }
                    }
                    if (stats.has("patternEvidence")) {
                        for (JsonElement evidenceValue : requireObject(stats.get("patternEvidence")).asMap().values()) {
                            JsonObject evidence = requireObject(evidenceValue);
                            requireInteger(evidence.get("timeMillis"), false);
                            requireString(evidence.get("answer"));
                        }
                    }
                }
            }
        }
    }

    private static JsonObject requireObject(JsonElement value) throws IOException {
        if (value == null || !value.isJsonObject()) {
            throw new IOException("Ungültiges Lernfortschritt-Objekt");
        }
        return value.getAsJsonObject();
    }

    private static void requireString(JsonElement value) throws IOException {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IOException("Ungültiger Lernfortschritt-Text");
        }
    }

    private static void requireBoolean(JsonElement value) throws IOException {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IOException("Ungültiger Lernfortschritt-Wahrheitswert");
        }
    }

    private static void requireStringArray(JsonElement value) throws IOException {
        if (value == null || !value.isJsonArray()) {
            throw new IOException("Ungültige Lernfortschritt-Liste");
        }
        for (JsonElement item : value.getAsJsonArray()) {
            requireString(item);
        }
    }

    private static void requireInteger(JsonElement value, boolean intRange) throws IOException {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IOException("Ungültige Lernfortschritt-Zahl");
        }
        try {
            if (intRange) {
                value.getAsBigDecimal().toBigIntegerExact().intValueExact();
            } else {
                value.getAsBigDecimal().toBigIntegerExact().longValueExact();
            }
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IOException("Ungültige Lernfortschritt-Zahl", exception);
        }
    }

    public void save() throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "kompass4mc-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static String positionKey(String dimension, int x, int y, int z) {
        return dimension + ":" + x + ":" + y + ":" + z;
    }

    public static final class Progress {
        public Set<String> discovered = new LinkedHashSet<>();
        public Set<String> completed = new LinkedHashSet<>();
        public String activeQuest;
    }

    public record HidingPlace(String dimension, int x, int y, int z, String questId) {}

    private static final class Data {
        int version = 1;
        Map<String, Progress> players = new LinkedHashMap<>();
        Map<String, HidingPlace> hidingPlaces = new LinkedHashMap<>();
        VillageState village;
        Map<String, LearningProgress> learning = new LinkedHashMap<>();
        boolean automaticLearningNpcs = true;
    }
}
