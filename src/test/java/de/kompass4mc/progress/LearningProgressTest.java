package de.kompass4mc.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.learning.LearningTask;
import de.kompass4mc.progress.LearningProgress.AttemptType;
import org.junit.jupiter.api.Test;

class LearningProgressTest {
    @Test
    void recordsSubjectAnswersAndHelpSeparatelyAndMarksCompletion() {
        LearningProgress progress = new LearningProgress();
        LearningTask task = task("subtract_borrow_1");
        progress.recordProceduralIssue(task, "Noch keine Antwort eingegeben", 1L);
        progress.recordHint(task, 2L);
        LearningProgress.Attempt wrong = progress.recordAnswer(task, "4846", 3L, false);
        LearningProgress.Attempt right = progress.recordAnswer(task, "3 154", 4L, false);

        LearningProgress.TaskStats stats = progress.stats.get(task.id());
        assertEquals(2, stats.attempts);
        assertEquals(1, stats.incorrect);
        assertEquals(1, stats.hints);
        assertEquals(1, stats.proceduralIssues);
        assertEquals(4L, stats.solvedAt);
        assertEquals(4L, stats.lastCorrectAt);
        assertEquals(Boolean.FALSE, stats.firstAttemptCorrect);
        assertTrue(stats.firstAttemptHintUsed);
        assertEquals(AttemptType.ANSWER, wrong.type());
        assertEquals(java.util.List.of("borrowing"), wrong.patternIds());
        assertFalse(wrong.correct());
        assertTrue(right.correct());
        assertTrue(right.hintUsed());
        assertTrue(right.patternIds().isEmpty());
        assertTrue(progress.completed.contains(task.id()));
        progress.validate();
    }

    @Test
    void firstAttemptCorrectAndWithoutHintAreDistinct() {
        LearningProgress progress = new LearningProgress();
        LearningTask first = task("subtract_store");
        LearningTask second = task("subtract_borrow_1");
        progress.recordHint(first, 1L);
        progress.recordAnswer(first, first.expectedAnswer(), 2L, false);
        progress.recordAnswer(second, second.expectedAnswer(), 3L, false);

        assertTrue(progress.stats.get(first.id()).firstAttemptCorrect);
        assertTrue(progress.stats.get(first.id()).firstAttemptHintUsed);
        assertTrue(progress.stats.get(second.id()).firstAttemptCorrect);
        assertFalse(progress.stats.get(second.id()).firstAttemptHintUsed);
        assertTrue(ParentReport.summary(progress).contains("Erster Versuch richtig: 2; davon ohne Hinweis: 1."));
    }

    @Test
    void wrongItemOnlyProducesAProceduralNoteAndNeverAMisconception() {
        LearningProgress progress = new LearningProgress();
        LearningTask task = task("groups_stone_1");
        progress.recordProceduralIssue(task, "Bitte Pflastersteine in der Haupthand zeigen.", 1L);
        progress.recordProceduralIssue(task, "Bitte Pflastersteine in der Haupthand zeigen.", 2L);

        assertEquals(0, progress.stats.get(task.id()).attempts);
        assertEquals(0, progress.stats.get(task.id()).incorrect);
        assertNull(progress.stats.get(task.id()).firstAttemptCorrect);
        assertTrue(progress.attempts.stream().allMatch(event -> event.type() == AttemptType.PROCEDURAL));
        assertTrue(ParentReport.observations(progress).isEmpty());
        progress.validate();
    }

    @Test
    void copiesSupportSaveFailureRollbackWithoutSharingMutableCollections() {
        LearningProgress original = new LearningProgress();
        original.playerName = "Lina";
        original.activeTaskId = "subtract_borrow_1";
        original.recordAnswer(task("subtract_borrow_1"), "4846", 1L, false);
        LearningProgress copy = original.copy();
        copy.recordAnswer(task("subtract_borrow_1"), "3154", 2L, false);
        copy.stats.get("subtract_borrow_1").patternEvidence.clear();
        copy.markRewarded("subtract_borrow_1");

        assertNotSame(original.stats, copy.stats);
        assertNotSame(original.stats.get("subtract_borrow_1"), copy.stats.get("subtract_borrow_1"));
        assertEquals("Lina", copy.playerName);
        assertEquals("subtract_borrow_1", copy.activeTaskId);
        assertEquals(1, original.attempts.size());
        assertEquals(1, original.stats.get("subtract_borrow_1").attempts);
        assertFalse(original.stats.get("subtract_borrow_1").patternEvidence.isEmpty());
        assertTrue(original.completed.isEmpty());
        assertTrue(original.rewarded.isEmpty());
    }

    @Test
    void rewardMarkerOnlyAllowsSolvedTasksAndMarksEachTaskOnce() {
        LearningProgress progress = new LearningProgress();
        LearningTask task = task("subtract_store");
        assertThrows(IllegalStateException.class, () -> progress.markRewarded(task.id()));
        progress.recordAnswer(task, task.expectedAnswer(), 1L, false);
        assertTrue(progress.markRewarded(task.id()));
        assertFalse(progress.markRewarded(task.id()));
        assertEquals(java.util.Set.of(task.id()), progress.rewarded);
    }

    @Test
    void gsonRoundTripPreservesTotalsAndEvidenceAndIndependentCollections() {
        LearningProgress original = new LearningProgress();
        original.playerName = "Amir";
        original.recordHint(task("subtract_borrow_1"), 10L);
        original.recordAnswer(task("subtract_borrow_1"), "4846", 20L, true);
        original.recordAnswer(task("subtract_borrow_2"), "356", 30L, false);
        original.recordAnswer(task("subtract_borrow_1"), "3154", 40L, true);
        original.markRewarded("subtract_borrow_1");
        Gson gson = new Gson();
        LearningProgress restored = gson.fromJson(gson.toJson(original), LearningProgress.class);
        restored.validate();

        assertEquals(original.attempts, restored.attempts);
        assertEquals(original.completed, restored.completed);
        assertEquals(original.rewarded, restored.rewarded);
        assertEquals(original.stats.get("subtract_borrow_1").solvedAt,
                restored.stats.get("subtract_borrow_1").solvedAt);
        assertEquals(original.stats.get("subtract_borrow_1").lastCorrectAt,
                restored.stats.get("subtract_borrow_1").lastCorrectAt);
        assertTrue(ParentReport.observations(restored).getFirst().possibleMisconception());
    }

    @Test
    void retainedEventsAreBoundedWithoutLosingLifetimeTotalsOrDistinctTaskEvidence() {
        LearningProgress progress = new LearningProgress();
        progress.recordAnswer(task("subtract_borrow_1"), "4846", 1L, false);
        progress.recordAnswer(task("subtract_borrow_2"), "356", 2L, false);
        for (int index = 0; index < 510; index++) {
            progress.recordHint(task("subtract_borrow_1"), 3L + index);
        }

        assertEquals(LearningProgress.MAX_EVENTS, progress.attempts.size());
        assertTrue(progress.attempts.stream().allMatch(attempt -> attempt.type() == AttemptType.HINT));
        assertEquals(510, progress.stats.get("subtract_borrow_1").hints);
        assertEquals(1, progress.stats.get("subtract_borrow_1").attempts);
        assertTrue(ParentReport.observations(progress).getFirst().possibleMisconception());
        assertTrue(ParentReport.summary(progress).get(1).contains("2 Antworten · 510 Hinweise"));
        progress.validate();
    }

    @Test
    void longAnswersAndUnicodeAreBoundedWithoutBreakingSurrogatePairs() {
        LearningProgress progress = new LearningProgress();
        String response = "😀".repeat(200);
        LearningProgress.Attempt attempt = progress.recordAnswer(task("subtract_store"), response, 1L, false);
        assertEquals(128, attempt.answer().codePointCount(0, attempt.answer().length()));
        assertEquals("😀".repeat(128), attempt.answer());
        assertFalse(attempt.correct());
        progress.validate();
    }

    @Test
    void removedCatalogTasksRemainSavedAndAreNotGivenAGuessedMisconception() {
        LearningProgress progress = new LearningProgress();
        progress.activeTaskId = "removed_old_task";
        progress.completed.add("removed_old_task");
        progress.rewarded.add("removed_old_task");
        LearningProgress.TaskStats old = new LearningProgress.TaskStats();
        old.attempts = 1;
        old.firstAttemptCorrect = true;
        old.solvedAt = 1L;
        progress.stats.put("removed_old_task", old);
        progress.attempts.add(new LearningProgress.Attempt("removed_old_task", 1L, "alte Antwort",
                true, false, java.util.List.of(), AttemptType.ANSWER));
        progress.validate();

        assertTrue(ParentReport.observations(progress).isEmpty());
        assertTrue(ParentReport.html(progress, "Kind").contains("Frühere Aufgabe: removed_old_task"));
        assertTrue(ParentReport.summary(progress).get(1).contains("1 Antworten"));
        assertTrue(progress.completed.contains("removed_old_task"));
    }

    @Test
    void malformedRewardAndEvidenceAreRejectedWithoutInventingProgress() {
        LearningProgress progress = new LearningProgress();
        progress.rewarded.add("subtract_store");
        assertThrows(IllegalArgumentException.class, progress::validate);
        progress.rewarded.clear();
        progress.recordAnswer(task("subtract_borrow_1"), "4846", 1L, false);
        progress.stats.get("subtract_borrow_1").patternEvidence.put("borrowing",
                new LearningProgress.PatternEvidence(2L, "3154"));
        assertThrows(IllegalArgumentException.class, progress::validate);
    }

    private static LearningTask task(String id) {
        return LearningCatalog.byId(id).orElseThrow();
    }
}
