package de.kompass4mc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Shared village projects, with a personal thank-you for each actual contributor. */
public final class VillageState {
    public String dimension;
    public int x;
    public int floorY;
    public int z;
    public Map<String, String> residents = new LinkedHashMap<>();
    public Map<String, Project> projects = new LinkedHashMap<>();

    public VillageState copy() {
        VillageState copy = new VillageState();
        copy.dimension = dimension;
        copy.x = x;
        copy.floorY = floorY;
        copy.z = z;
        copy.residents.putAll(residents);
        projects.forEach((id, project) -> copy.projects.put(id, project.copy()));
        return copy;
    }

    public static void validateDocument(JsonElement value) throws IOException {
        if (value == null || !value.isJsonObject()) {
            throw new IOException("Ungültiger Dorf-Spielstand");
        }
        JsonObject village = value.getAsJsonObject();
        for (String field : Set.of("dimension", "x", "floorY", "z", "residents", "projects")) {
            if (!village.has(field) || village.get(field).isJsonNull()) {
                throw new IOException("Unvollständiger Dorf-Spielstand: " + field);
            }
        }
        if (!village.get("residents").isJsonObject() || !village.get("projects").isJsonObject()) {
            throw new IOException("Ungültige Dorf-Bewohner oder Dorf-Projekte");
        }
        if (!isString(village.get("dimension")) || village.get("dimension").getAsString().isBlank()) {
            throw new IOException("Leere Dorf-Dimension");
        }
        for (String coordinate : Set.of("x", "floorY", "z")) {
            JsonElement valueCoordinate = village.get(coordinate);
            if (!valueCoordinate.isJsonPrimitive() || !valueCoordinate.getAsJsonPrimitive().isNumber()) {
                throw new IOException("Ungültige Dorf-Koordinate");
            }
            try {
                valueCoordinate.getAsBigDecimal().toBigIntegerExact().intValueExact();
            } catch (ArithmeticException | NumberFormatException exception) {
                throw new IOException("Ungültige Dorf-Koordinate", exception);
            }
        }
        Set<String> uuids = new LinkedHashSet<>();
        try {
            for (JsonElement resident : village.getAsJsonObject("residents").asMap().values()) {
                if (!isString(resident)) {
                    throw new IOException("Ungültige Bewohner-ID");
                }
                String uuid = UUID.fromString(resident.getAsString()).toString();
                if (!uuids.add(uuid)) {
                    throw new IOException("Doppelte Bewohner-ID");
                }
            }
            for (JsonElement valueProject : village.getAsJsonObject("projects").asMap().values()) {
                if (!valueProject.isJsonObject()) {
                    throw new IOException("Ungültiges Dorf-Projekt");
                }
                JsonObject project = valueProject.getAsJsonObject();
                Map<String, Set<String>> playerSets = new LinkedHashMap<>();
                for (String field : Set.of("accepted", "contributors", "rewardClaimed")) {
                    if (!project.has(field) || !project.get(field).isJsonArray()) {
                        throw new IOException("Unvollständiges Dorf-Projekt: " + field);
                    }
                    Set<String> ids = new LinkedHashSet<>();
                    for (JsonElement player : project.getAsJsonArray(field)) {
                        if (!isString(player)) {
                            throw new IOException("Ungültige Mitwirkenden-ID");
                        }
                        ids.add(UUID.fromString(player.getAsString()).toString());
                    }
                    playerSets.put(field, ids);
                }
                for (String field : Set.of("completed", "suppliesIssued")) {
                    if (!project.has(field) || !project.get(field).isJsonPrimitive()
                            || !project.getAsJsonPrimitive(field).isBoolean()) {
                        throw new IOException("Unvollständiges Dorf-Projekt: " + field);
                    }
                }
                if (!playerSets.get("accepted").containsAll(playerSets.get("contributors"))
                        || !playerSets.get("contributors").containsAll(playerSets.get("rewardClaimed"))
                        || (!project.get("completed").getAsBoolean() && !playerSets.get("rewardClaimed").isEmpty())
                        || (project.get("completed").getAsBoolean() && playerSets.get("contributors").isEmpty())) {
                    throw new IOException("Widersprüchliche Dorf-Projektdaten");
                }
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IOException("Ungültige Dorf-ID oder Projektdaten", exception);
        }
    }

    private static boolean isString(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }

    public static final class Project {
        public Set<String> accepted = new LinkedHashSet<>();
        public Set<String> contributors = new LinkedHashSet<>();
        public Set<String> rewardClaimed = new LinkedHashSet<>();
        public boolean completed;
        public boolean suppliesIssued;

        public boolean accept(String player) {
            return !completed && accepted.add(player);
        }

        public boolean contribute(String player) {
            return !completed && accepted.contains(player) && contributors.add(player);
        }

        public boolean canClaim(String player) {
            return completed && contributors.contains(player) && !rewardClaimed.contains(player);
        }

        public boolean claim(String player) {
            return canClaim(player) && rewardClaimed.add(player);
        }

        private Project copy() {
            Project copy = new Project();
            copy.accepted.addAll(accepted);
            copy.contributors.addAll(contributors);
            copy.rewardClaimed.addAll(rewardClaimed);
            copy.completed = completed;
            copy.suppliesIssued = suppliesIssued;
            return copy;
        }
    }
}
