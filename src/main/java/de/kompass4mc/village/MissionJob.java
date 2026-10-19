package de.kompass4mc.village;

import java.util.Objects;
import java.util.regex.Pattern;

/** Ein Dorfauftrag mit Anlass, Bau- oder Lieferanweisung und erklärendem Abschluss. */
public record MissionJob(
        String id,
        String residentName,
        String role,
        String title,
        String request,
        String acceptedText,
        String hint,
        String completionText,
        int emeraldReward,
        Kind kind) {

    private static final Pattern ID = Pattern.compile("[a-z0-9]+(?:_[a-z0-9]+)*");

    public MissionJob {
        id = nonBlank(id, "id");
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Ungültige Auftrags-ID: " + id);
        }
        residentName = nonBlank(residentName, "residentName");
        role = nonBlank(role, "role");
        title = nonBlank(title, "title");
        request = nonBlank(request, "request");
        acceptedText = nonBlank(acceptedText, "acceptedText");
        hint = nonBlank(hint, "hint");
        completionText = nonBlank(completionText, "completionText");
        kind = Objects.requireNonNull(kind, "kind");
        if (emeraldReward < 1 || emeraldReward > 64) {
            throw new IllegalArgumentException("Die Belohnung muss zwischen 1 und 64 Smaragden liegen.");
        }
    }

    private static String nonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.codePoints().allMatch(point -> Character.isWhitespace(point) || Character.isSpaceChar(point))) {
            throw new IllegalArgumentException(field + " darf nicht leer sein.");
        }
        return value.strip();
    }
}
