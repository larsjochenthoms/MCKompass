package de.kompass4mc.village;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Prüft eine Baufläche ohne Minecraft-Abhängigkeit. Vorbereitete Bauplätze können
 * vollständig freie Verbotsfelder verlangen. Im allgemeinen Modus sind dort nur
 * die im Bauplan verwendeten Materialien verboten. Außerhalb wird nichts geprüft.
 */
public record BuildPattern(Map<GridPos, String> required, Set<GridPos> forbidden, boolean requireEmptyForbidden) {
    private static final Set<String> AIR = Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air");

    public BuildPattern(Map<GridPos, String> required, Set<GridPos> forbidden) {
        this(required, forbidden, false);
    }

    public BuildPattern {
        required = Map.copyOf(Objects.requireNonNull(required, "required"));
        forbidden = Set.copyOf(Objects.requireNonNull(forbidden, "forbidden"));
        if (required.isEmpty()) {
            throw new IllegalArgumentException("Ein Bauplan braucht mindestens ein Bauteil.");
        }
        for (Map.Entry<GridPos, String> entry : required.entrySet()) {
            if (entry.getValue().isBlank() || AIR.contains(entry.getValue())) {
                throw new IllegalArgumentException("Ein Bauteil braucht eine Block-ID und darf keine Luft sein.");
            }
            if (forbidden.contains(entry.getKey())) {
                throw new IllegalArgumentException("Ein Feld kann nicht zugleich benötigt und verboten sein.");
            }
        }
    }

    /** Fehlende Einträge im Ausschnitt gelten wie Luft. */
    public Inspection inspect(Map<GridPos, String> actual) {
        Objects.requireNonNull(actual, "actual");
        Set<GridPos> missing = new HashSet<>();
        Set<GridPos> wrongMaterial = new HashSet<>();
        Set<GridPos> extra = new HashSet<>();
        for (Map.Entry<GridPos, String> entry : required.entrySet()) {
            String current = actual.get(entry.getKey());
            if (current == null || AIR.contains(current)) {
                missing.add(entry.getKey());
            } else if (!entry.getValue().equals(current)) {
                wrongMaterial.add(entry.getKey());
            }
        }
        Set<String> buildingMaterials = Set.copyOf(required.values());
        for (GridPos position : forbidden) {
            String current = actual.get(position);
            if (current != null && (requireEmptyForbidden ? !AIR.contains(current) : buildingMaterials.contains(current))) {
                extra.add(position);
            }
        }
        return new Inspection(missing, wrongMaterial, extra);
    }

    /** Die drei Fehlergruppen sind disjunkt und enthalten die betroffenen Felder. */
    public record Inspection(Set<GridPos> missing, Set<GridPos> wrongMaterial, Set<GridPos> extra) {
        public Inspection {
            missing = Set.copyOf(missing);
            wrongMaterial = Set.copyOf(wrongMaterial);
            extra = Set.copyOf(extra);
        }

        public boolean complete() {
            return missing.isEmpty() && wrongMaterial.isEmpty() && extra.isEmpty();
        }
    }
}
