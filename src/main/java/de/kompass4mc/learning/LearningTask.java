package de.kompass4mc.learning;

import de.kompass4mc.quest.Quest;
import de.kompass4mc.quest.Subject;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Ein ortsunabhängiger Auftrag, der von einem reisenden Bewohner erteilt werden kann. */
public record LearningTask(
        String id,
        String npcName,
        String title,
        String story,
        String question,
        List<String> choices,
        String expectedAnswer,
        String hint,
        String explanation,
        Subject subject,
        Skill skill,
        Difficulty difficulty,
        Interaction interaction,
        String itemId,
        List<ErrorPattern> patterns) {

    public LearningTask {
        Objects.requireNonNull(difficulty, "difficulty");
        // Reuse the established data validation of the older, still supported quest mode.
        Quest validated = new Quest(id, title, "Unterwegs", story, question, choices,
                expectedAnswer, hint, explanation, subject, difficulty.emeralds());
        id = validated.id();
        title = validated.title();
        story = validated.story();
        question = validated.question();
        choices = validated.choices();
        expectedAnswer = validated.expectedAnswer();
        hint = validated.hint();
        explanation = validated.explanation();
        npcName = Objects.requireNonNull(npcName, "npcName").strip();
        if (AnswerNormalization.normalize(npcName).isEmpty()) {
            throw new IllegalArgumentException("Der Bewohner braucht einen Namen.");
        }
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(interaction, "interaction");
        if (skill.subject() != subject) {
            throw new IllegalArgumentException("Fach und Übungsbereich müssen zusammenpassen.");
        }
        itemId = itemId == null ? "" : itemId.strip();
        if (interaction == Interaction.SHOW_STACK) {
            if (!itemId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+") || !choices.isEmpty()
                    || !expectedAnswer.matches("[0-9]+")) {
                throw new IllegalArgumentException("Zeige-Aufträge brauchen Gegenstand und eine ganze Anzahl.");
            }
            try {
                int count = Integer.parseInt(expectedAnswer);
                if (count < 1 || count > 64) {
                    throw new IllegalArgumentException("Die gezeigte Menge muss in einen Stapel passen.");
                }
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Die gezeigte Menge muss in einen Stapel passen.", exception);
            }
        } else if (!itemId.isEmpty()) {
            throw new IllegalArgumentException("Nur Zeige-Aufträge haben einen Prüfgegenstand.");
        }
        Set<String> canonicalChoices = new HashSet<>();
        for (String choice : choices) {
            if (!canonicalChoices.add(AnswerNormalization.normalize(choice))) {
                throw new IllegalArgumentException("Auswahlantworten müssen unterscheidbar sein.");
            }
        }
        patterns = List.copyOf(Objects.requireNonNull(patterns, "patterns"));
        Set<String> patternIds = new HashSet<>();
        Set<String> wrongAnswers = new HashSet<>();
        String correct = AnswerNormalization.normalize(expectedAnswer);
        for (ErrorPattern pattern : patterns) {
            if (!patternIds.add(pattern.id())) {
                throw new IllegalArgumentException("Doppelte Fehlermuster-ID in einer Aufgabe: " + pattern.id());
            }
            for (String answer : pattern.answers()) {
                if (answer.equals(correct)) {
                    throw new IllegalArgumentException("Die richtige Antwort darf kein Fehlermuster auslösen.");
                }
                if (!wrongAnswers.add(answer)) {
                    throw new IllegalArgumentException("Eine Fehlantwort darf nur einem Muster zugeordnet sein.");
                }
            }
        }
    }

    public int rewardEmeralds() {
        return difficulty.emeralds();
    }

    /** Löst Auswahlbuchstaben auf und vereinheitlicht gültige Zahlen, Unicode und Leerraum. */
    public String canonicalAnswer(String answer) {
        String normalized = AnswerNormalization.normalize(answer);
        if (!choices.isEmpty() && normalized.length() == 1) {
            int index = normalized.charAt(0) - 'a';
            if (index >= 0 && index < choices.size()) {
                return AnswerNormalization.normalize(choices.get(index));
            }
        }
        return normalized;
    }

    public boolean matchesAnswer(String answer) {
        return answer != null && canonicalAnswer(answer).equals(AnswerNormalization.normalize(expectedAnswer));
    }

    /** Keine pauschale Diagnose: Nicht hinterlegte Antworten liefern ausdrücklich kein Muster. */
    public List<ErrorPattern> responsePatterns(String answer) {
        if (matchesAnswer(answer)) {
            return List.of();
        }
        String canonical = canonicalAnswer(answer);
        return patterns.stream().filter(pattern -> pattern.matches(canonical)).toList();
    }
}
