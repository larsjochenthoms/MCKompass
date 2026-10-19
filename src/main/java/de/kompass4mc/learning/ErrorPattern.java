package de.kompass4mc.learning;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fachlich erklärbare, ausdrücklich hinterlegte Fehlantworten.
 * Ein Treffer ist ein Gesprächsanlass und allein noch kein Nachweis einer Denkstrategie.
 */
public record ErrorPattern(String id, String label, String rationale, Set<String> answers) {
    public ErrorPattern {
        Objects.requireNonNull(id, "id");
        if (!id.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Ungültige Fehlermuster-ID: " + id);
        }
        label = nonBlank(label, "label");
        rationale = nonBlank(rationale, "rationale");
        Objects.requireNonNull(answers, "answers");
        if (answers.isEmpty()) {
            throw new IllegalArgumentException("Ein Fehlermuster braucht konkrete Fehlantworten.");
        }
        answers = answers.stream().map(answer -> AnswerNormalization.normalize(nonBlank(answer, "answer")))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean matches(String answer) {
        return answer != null && answers.contains(AnswerNormalization.normalize(answer));
    }

    private static String nonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (AnswerNormalization.normalize(value).isEmpty()) {
            throw new IllegalArgumentException(field + " darf nicht leer sein.");
        }
        return value.strip();
    }
}
