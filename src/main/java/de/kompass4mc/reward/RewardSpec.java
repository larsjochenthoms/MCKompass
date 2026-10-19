package de.kompass4mc.reward;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Das vor der Aufgabe sichtbare, feste Paket; items enthält auch die Smaragde. */
public record RewardSpec(String id, String label, List<RewardItem> items) {
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    public RewardSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        if (!ID.matcher(id).matches() || label.isBlank() || label.length() > 240) {
            throw new IllegalArgumentException("Ungültige Belohnungsbeschreibung.");
        }
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (items.isEmpty() || items.size() > 8) {
            throw new IllegalArgumentException("Ein Belohnungspaket benötigt zwischen 1 und 8 Stapel.");
        }
    }

    public String description() {
        return label;
    }
}
