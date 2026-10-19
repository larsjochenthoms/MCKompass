package de.kompass4mc.quest;

import java.math.BigInteger;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Eine eigenständig formulierte Aufgabe mit Hinweis und erklärter Lösung. */
public record Quest(
        String id,
        String title,
        String locationHint,
        String story,
        String question,
        List<String> choices,
        String expectedAnswer,
        String hint,
        String explanation,
        Subject subject,
        int rewardEmeralds) {

    private static final Pattern ID = Pattern.compile("[a-z0-9]+(?:_[a-z0-9]+)*");
    private static final Pattern INTEGER = Pattern.compile("[+-]?[0-9]+");
    private static final Pattern GROUPED_DOTS =
            Pattern.compile("[+-]?[1-9][0-9]{0,2}(?:\\.[0-9]{3})+");
    private static final Pattern GROUPED_SPACES =
            Pattern.compile("[+-]?[1-9][0-9]{0,2}(?: [0-9]{3})+");

    public Quest {
        id = nonBlank(id, "id");
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Ungültige Quest-ID: " + id);
        }
        title = nonBlank(title, "title");
        locationHint = nonBlank(locationHint, "locationHint");
        story = nonBlank(story, "story");
        question = nonBlank(question, "question");
        hint = nonBlank(hint, "hint");
        explanation = nonBlank(explanation, "explanation");
        expectedAnswer = nonBlank(expectedAnswer, "expectedAnswer");
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        subject = Objects.requireNonNull(subject, "subject");
        if (!choices.isEmpty() && (choices.size() < 2 || choices.size() > 26)) {
            throw new IllegalArgumentException("Auswahlfragen brauchen 2 bis 26 Antworten.");
        }
        Set<String> uniqueChoices = new HashSet<>();
        for (String choice : choices) {
            nonBlank(choice, "choice");
            if (!uniqueChoices.add(normalizeText(choice))) {
                throw new IllegalArgumentException("Doppelte Auswahlantwort: " + choice);
            }
        }
        if (!choices.isEmpty() && !choices.contains(expectedAnswer)) {
            throw new IllegalArgumentException("Die Lösung muss exakt einer Auswahlantwort entsprechen.");
        }
        if (rewardEmeralds < 1 || rewardEmeralds > 64) {
            throw new IllegalArgumentException("Die Belohnung muss zwischen 1 und 64 Smaragden liegen.");
        }
    }

    /**
     * Prüft Auswahlbuchstaben A–Z, Antworttexte oder ganze Zahlen.
     * Ganze Zahlen dürfen korrekt gruppierte Tausenderpunkte oder Leerzeichen enthalten.
     * Schreibweise und Satzzeichen bleiben bei Textantworten erhalten; nur Groß-/Kleinschreibung,
     * Unicode-Komposition und Leerraum werden vereinheitlicht.
     */
    public boolean matchesAnswer(String answer) {
        if (answer == null) {
            return false;
        }
        String normalized = normalizeText(answer);
        if (normalized.isEmpty()) {
            return false;
        }
        if (!choices.isEmpty()) {
            if (normalized.length() == 1) {
                int index = normalized.charAt(0) - 'a';
                if (index >= 0 && index < choices.size()) {
                    return choices.get(index).equals(expectedAnswer);
                }
            }
            return normalized.equals(normalizeText(expectedAnswer));
        }
        if (INTEGER.matcher(expectedAnswer).matches()) {
            String numericAnswer = normalizeWhitespace(answer);
            if (!INTEGER.matcher(numericAnswer).matches()
                    && !GROUPED_DOTS.matcher(numericAnswer).matches()
                    && !GROUPED_SPACES.matcher(numericAnswer).matches()) {
                return false;
            }
            String digits = numericAnswer.replace(".", "").replace(" ", "");
            return new BigInteger(digits).equals(new BigInteger(expectedAnswer));
        }
        return normalized.equals(normalizeText(expectedAnswer));
    }

    private static String nonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (normalizeWhitespace(value).isEmpty()) {
            throw new IllegalArgumentException(field + " darf nicht leer sein.");
        }
        return value.strip();
    }

    private static String normalizeText(String value) {
        return Normalizer.normalize(normalizeWhitespace(value), Normalizer.Form.NFC)
                .toLowerCase(Locale.GERMAN);
    }

    private static String normalizeWhitespace(String value) {
        StringBuilder result = new StringBuilder();
        boolean pendingSpace = false;
        for (int point : value.codePoints().toArray()) {
            if (Character.isWhitespace(point) || Character.isSpaceChar(point)) {
                pendingSpace = !result.isEmpty();
            } else {
                if (pendingSpace) {
                    result.append(' ');
                    pendingSpace = false;
                }
                result.appendCodePoint(point);
            }
        }
        return result.toString();
    }
}
