package de.kompass4mc.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.kompass4mc.quest.Subject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LearningCatalogTest {
    @Test
    void bothSubjectsHaveVariedTasksAndEveryIdResolves() {
        List<LearningTask> tasks = LearningCatalog.all();
        assertEquals(48, tasks.size());
        assertEquals(48, new HashSet<>(tasks.stream().map(LearningTask::id).toList()).size());
        assertEquals(24, tasks.stream().filter(task -> task.subject() == Subject.MATH).count());
        assertEquals(24, tasks.stream().filter(task -> task.subject() == Subject.GERMAN).count());
        assertEquals(Set.of("Mila", "Jaro", "Lio"), new HashSet<>(tasks.stream().map(LearningTask::npcName).toList()));
        for (Difficulty difficulty : Difficulty.values()) {
            assertTrue(tasks.stream().anyMatch(task -> task.difficulty() == difficulty), difficulty.name());
        }
        for (LearningTask task : tasks) {
            assertSame(task, LearningCatalog.byId(task.id()).orElseThrow());
            assertTrue(task.matchesAnswer(task.expectedAnswer()), task.id());
            assertTrue(task.responsePatterns(task.expectedAnswer()).isEmpty(), task.id());
            assertEquals(task.subject(), task.skill().subject());
            assertEquals(task.difficulty().emeralds(), task.rewardEmeralds());
        }
        assertTrue(LearningCatalog.byId("unknown").isEmpty());
        assertTrue(LearningCatalog.byId(null).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> tasks.clear());
    }

    @Test
    void allChoiceButtonsMatchTheirTextAndOnlyOneChoiceSolvesEachTask() {
        for (LearningTask task : LearningCatalog.all()) {
            int correct = 0;
            for (int index = 0; index < task.choices().size(); index++) {
                String letter = Character.toString('A' + index);
                String choice = task.choices().get(index);
                assertEquals(task.matchesAnswer(choice), task.matchesAnswer(letter), task.id() + ": " + letter);
                assertEquals(task.responsePatterns(choice), task.responsePatterns(letter), task.id() + ": " + letter);
                correct += task.matchesAnswer(letter) ? 1 : 0;
            }
            if (!task.choices().isEmpty()) {
                assertEquals(1, correct, task.id());
            }
        }
    }

    @Test
    void eachSpecificErrorPatternCanBeObservedOnAtLeastTwoIndependentTasks() {
        Map<String, Set<String>> tasksByPattern = new HashMap<>();
        Map<String, String> labels = new HashMap<>();
        Map<String, String> rationales = new HashMap<>();
        for (LearningTask task : LearningCatalog.all()) {
            assertTrue(task.responsePatterns("Ich weiß es nicht").isEmpty(), task.id());
            for (ErrorPattern pattern : task.patterns()) {
                tasksByPattern.computeIfAbsent(pattern.id(), ignored -> new HashSet<>()).add(task.id());
                String previousLabel = labels.putIfAbsent(pattern.id(), pattern.label());
                String previousRationale = rationales.putIfAbsent(pattern.id(), pattern.rationale());
                if (previousLabel != null) {
                    assertEquals(previousLabel, pattern.label(), pattern.id());
                    assertEquals(previousRationale, pattern.rationale(), pattern.id());
                }
                for (String wrong : pattern.answers()) {
                    assertFalse(task.matchesAnswer(wrong), task.id() + ": " + wrong);
                    assertEquals(List.of(pattern), task.responsePatterns(wrong), task.id() + ": " + wrong);
                    if (!task.choices().isEmpty()) {
                        assertTrue(task.choices().stream().anyMatch(choice -> task.canonicalAnswer(choice).equals(wrong)),
                                task.id() + ": Diagnose muss zu einer tatsächlichen Auswahl passen.");
                    }
                }
            }
        }
        assertEquals(Set.of("borrowing", "groups_added", "area_perimeter", "units", "sequence",
                "cause_consequence", "noun_verb", "final_sound", "tense", "person"), tasksByPattern.keySet());
        tasksByPattern.forEach((pattern, tasks) -> assertTrue(tasks.size() >= 2, pattern));
    }

    @Test
    void numericDistractorsActuallyCorrespondToTheirStatedComputation() {
        assertNumericTask("subtract_borrow_1", 7010 - 3856, "borrowing", digitwiseDifference(7010, 3856));
        assertNumericTask("subtract_borrow_2", 402 - 158, "borrowing", digitwiseDifference(402, 158));
        assertNumericTask("perimeter_garden_1", 2 * (8 + 6), "area_perimeter", 8 * 6);
        assertNumericTask("perimeter_garden_2", 2 * (7 + 4), "area_perimeter", 7 * 4);
        assertNumericTask("multiply_saplings", 6 * 24, "groups_added", 6 + 24);
        assertNumericTask("groups_stone_1", 3 * 6, "groups_added", 3 + 6);
        assertNumericTask("groups_planks_2", 4 * 5, "groups_added", 4 + 5);
        assertNumericTask("units_rope_1", 3 * 100 + 20, "units", 3 * 10 + 20);
        assertNumericTask("units_banner_2", 2 * 100 + 45, "units", 2 * 10 + 45);
    }

    @Test
    void showingTasksUseCommonVanillaBlocksAndFitInASingleStack() {
        List<LearningTask> physical = LearningCatalog.all().stream()
                .filter(task -> task.interaction() == Interaction.SHOW_STACK).toList();
        assertEquals(4, physical.size());
        assertEquals(Set.of("minecraft:cobblestone", "minecraft:oak_planks"),
                new HashSet<>(physical.stream().map(LearningTask::itemId).toList()));
        for (LearningTask task : physical) {
            int count = Integer.parseInt(task.expectedAnswer());
            assertTrue(count >= 1 && count <= 64, task.id());
            assertTrue(task.choices().isEmpty(), task.id());
        }
    }

    private static void assertNumericTask(String id, int expected, String patternId, int specificWrong) {
        LearningTask task = LearningCatalog.byId(id).orElseThrow();
        assertEquals(Integer.toString(expected), task.expectedAnswer());
        assertEquals(List.of(patternId), task.responsePatterns(Integer.toString(specificWrong)).stream().map(ErrorPattern::id).toList());
        assertTrue(task.responsePatterns(Integer.toString(specificWrong + 10000)).isEmpty(), id);
    }

    private static int digitwiseDifference(int first, int second) {
        String top = Integer.toString(first);
        String bottom = Integer.toString(second);
        assertEquals(top.length(), bottom.length());
        StringBuilder result = new StringBuilder();
        for (int digit = 0; digit < top.length(); digit++) {
            result.append(Math.abs(top.charAt(digit) - bottom.charAt(digit)));
        }
        return Integer.parseInt(result.toString());
    }
}
