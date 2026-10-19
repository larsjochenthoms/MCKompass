package de.kompass4mc.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.kompass4mc.quest.Subject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LearningTaskTest {
    @Test
    void numberParsingAcceptsRealThousandsSeparatorsButDoesNotDiscardAmbiguousCharacters() {
        LearningTask task = LearningCatalog.byId("subtract_borrow_1").orElseThrow();
        for (String answer : List.of("3154", "3.154", "3 154", " 3\u202f154 ", "+003154")) {
            assertTrue(task.matchesAnswer(answer), answer);
            assertEquals("3154", task.canonicalAnswer(answer));
            assertTrue(task.responsePatterns(answer).isEmpty());
        }
        for (String answer : List.of("3,154", "31.54", "3 1 5 4", "3154.0", "3.154 Steine", "-3154", "3e154")) {
            assertFalse(task.matchesAnswer(answer), answer);
            assertTrue(task.responsePatterns(answer).isEmpty(), answer);
        }
        assertFalse(task.matchesAnswer(null));
        assertFalse(task.matchesAnswer(""));
        assertTrue(task.responsePatterns(null).isEmpty());
    }

    @Test
    void choiceLettersAndTextProduceTheSameSpecificFeedback() {
        LearningTask task = LearningCatalog.byId("sequence_lantern_1").orElseThrow();
        assertTrue(task.matchesAnswer(" b "));
        assertEquals(task.canonicalAnswer("Er steigt auf den Turm."), task.canonicalAnswer("B"));
        assertFalse(task.matchesAnswer("A"));
        assertEquals("sequence", task.responsePatterns("A").getFirst().id());
        assertEquals(task.responsePatterns("A"), task.responsePatterns("ER\nWINKT\u00a0INS DORF."));
        assertTrue(task.responsePatterns("C").isEmpty(), "Ein unpassender Ablenker ist keine erklärte Strategie.");
        assertTrue(task.responsePatterns("Ich weiß es nicht").isEmpty());
        assertTrue(task.responsePatterns("E").isEmpty());
        assertTrue(task.responsePatterns("B").isEmpty());
    }

    @Test
    void explicitNumberPatternMatchesEquivalentNotationAndNothingNearIt() {
        LearningTask task = LearningCatalog.byId("subtract_borrow_1").orElseThrow();
        for (String answer : List.of("4846", "4.846", "4\u00a0846", "+04846")) {
            assertEquals(List.of("borrowing"), task.responsePatterns(answer).stream().map(ErrorPattern::id).toList());
        }
        for (String answer : List.of("4845", "4847", "4846 Steine", "48.46", "weiß nicht", "", "3154")) {
            assertTrue(task.responsePatterns(answer).isEmpty(), answer);
        }
    }

    @Test
    void patternNormalizationPreservesGermanSpellingAndPunctuation() {
        ErrorPattern pattern = new ErrorPattern("specific", "Hinweis", "Begründung", Set.of("Die schöne Straße."));
        assertTrue(pattern.matches(" DIE\nSCHO\u0308NE\u00a0STRAßE. "));
        assertFalse(pattern.matches("Die schone Straße."));
        assertFalse(pattern.matches("Die schöne Strasse."));
        assertFalse(pattern.matches("Die schöne Straße"));
        assertFalse(pattern.matches(null));
    }

    @Test
    void difficultyRewardsIncreaseWithTaskDemand() {
        assertEquals(2, Difficulty.EASY.emeralds());
        assertEquals(4, Difficulty.MEDIUM.emeralds());
        assertEquals(6, Difficulty.HARD.emeralds());
        assertTrue(Difficulty.EASY.emeralds() < Difficulty.MEDIUM.emeralds());
        assertTrue(Difficulty.MEDIUM.emeralds() < Difficulty.HARD.emeralds());
    }

    @Test
    void correctOrAmbiguousDiagnosticMetadataFailsBeforeTheTaskIsPlayed() {
        ErrorPattern correct = new ErrorPattern("bad", "Hinweis", "Begründung", Set.of("+024"));
        assertThrows(IllegalArgumentException.class, () -> numericTask(List.of(correct)));
        ErrorPattern first = new ErrorPattern("first", "Hinweis", "Begründung", Set.of("30"));
        ErrorPattern second = new ErrorPattern("second", "Hinweis", "Begründung", Set.of("030"));
        assertThrows(IllegalArgumentException.class, () -> numericTask(List.of(first, second)));
        assertThrows(IllegalArgumentException.class, () -> numericTask(List.of(first, first)));
        assertThrows(IllegalArgumentException.class, () -> new ErrorPattern("pattern", "Hinweis", "Begründung", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new ErrorPattern("Bad ID", "Hinweis", "Begründung", Set.of("30")));
        assertThrows(IllegalArgumentException.class, () -> new ErrorPattern("pattern", "Hinweis", "Begründung", Set.of("\u00a0")));
    }

    @Test
    void taskAndPatternCollectionsDoNotShareMutableCallerState() {
        Set<String> answers = new HashSet<>(Set.of("30"));
        ErrorPattern pattern = new ErrorPattern("groups", "Hinweis", "Begründung", answers);
        answers.clear();
        List<ErrorPattern> patterns = new ArrayList<>(List.of(pattern));
        LearningTask task = numericTask(patterns);
        patterns.clear();
        assertEquals("groups", task.responsePatterns("30").getFirst().id());
        assertThrows(UnsupportedOperationException.class, () -> task.patterns().clear());
        assertThrows(UnsupportedOperationException.class, () -> pattern.answers().clear());
        assertThrows(UnsupportedOperationException.class, () -> task.responsePatterns("30").clear());
    }

    @Test
    void physicalTasksRequireAnUnambiguousItemAndASingleStack() {
        assertThrows(IllegalArgumentException.class, () -> stackTask("0", "minecraft:cobblestone"));
        assertThrows(IllegalArgumentException.class, () -> stackTask("65", "minecraft:cobblestone"));
        assertThrows(IllegalArgumentException.class, () -> stackTask("999999999999999", "minecraft:cobblestone"));
        assertThrows(IllegalArgumentException.class, () -> stackTask("24 Steine", "minecraft:cobblestone"));
        assertThrows(IllegalArgumentException.class, () -> stackTask("24", "cobblestone"));
        assertEquals(Interaction.SHOW_STACK, stackTask("64", "minecraft:oak_planks").interaction());
        assertThrows(IllegalArgumentException.class, () -> new LearningTask("bad_subject", "Mila", "Titel", "Text", "Frage",
                List.of(), "24", "Hinweis", "Erklärung", Subject.GERMAN, Skill.MULTIPLICATION,
                Difficulty.EASY, Interaction.ANSWER, "", List.of()));
    }

    private static LearningTask numericTask(List<ErrorPattern> patterns) {
        return new LearningTask("example", "Mila", "Titel", "Text", "Frage", List.of(), "24", "Hinweis", "Erklärung",
                Subject.MATH, Skill.MULTIPLICATION, Difficulty.EASY, Interaction.ANSWER, "", patterns);
    }

    private static LearningTask stackTask(String expected, String itemId) {
        return new LearningTask("example", "Jaro", "Titel", "Text", "Frage", List.of(), expected, "Hinweis", "Erklärung",
                Subject.MATH, Skill.MULTIPLICATION, Difficulty.EASY, Interaction.SHOW_STACK, itemId, List.of());
    }
}
