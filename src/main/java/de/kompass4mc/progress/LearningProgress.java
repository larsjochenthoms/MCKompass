package de.kompass4mc.progress;

import de.kompass4mc.learning.ErrorPattern;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.learning.LearningTask;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** World-local learning notes. Rewards and saves are managed by the Minecraft integration. */
public final class LearningProgress {
    public static final int MAX_EVENTS = 500;
    public static final int MAX_ANSWER_LENGTH = 128;

    public String playerName = "";
    public String activeTaskId;
    public Set<String> completed = new LinkedHashSet<>();
    public Set<String> rewarded = new LinkedHashSet<>();
    public List<Attempt> attempts = new ArrayList<>();
    public Map<String, TaskStats> stats = new LinkedHashMap<>();

    public Attempt recordAnswer(LearningTask task, String response, long timestamp, boolean hintUsed) {
        TaskStats taskStats = taskStats(task);
        String answer = limited(response);
        boolean correct = task.matchesAnswer(answer);
        List<String> patternIds = correct ? List.of()
                : task.responsePatterns(answer).stream().map(ErrorPattern::id).toList();
        boolean usedHint = hintUsed || taskStats.hints > 0;
        if (taskStats.attempts == 0) {
            taskStats.firstAttemptCorrect = correct;
            taskStats.firstAttemptHintUsed = usedHint;
        }
        taskStats.attempts++;
        if (!correct) {
            taskStats.incorrect++;
            for (String patternId : patternIds) {
                taskStats.patternEvidence.put(patternId,
                        new PatternEvidence(timestamp, answer));
            }
        } else {
            if (taskStats.solvedAt == null) {
                taskStats.solvedAt = timestamp;
            }
            taskStats.lastCorrectAt = timestamp;
            completed.add(task.id());
        }
        Attempt attempt = new Attempt(task.id(), timestamp, answer, correct, usedHint,
                patternIds, AttemptType.ANSWER);
        append(attempt);
        return attempt;
    }

    public void recordHint(LearningTask task, long timestamp) {
        taskStats(task).hints++;
        append(new Attempt(task.id(), timestamp, "", false, true, List.of(), AttemptType.HINT));
    }

    /** Wrong item, empty hand, etc. do not imply a subject-related error. */
    public void recordProceduralIssue(LearningTask task, String description, long timestamp) {
        taskStats(task).proceduralIssues++;
        append(new Attempt(task.id(), timestamp, limited(description), false, false,
                List.of(), AttemptType.PROCEDURAL));
    }

    /** Returns false if the reward was already marked; the caller persists before giving items. */
    public boolean markRewarded(String taskId) {
        if (!completed.contains(taskId)) {
            throw new IllegalStateException("Eine ungelöste Aufgabe darf nicht belohnt werden.");
        }
        return rewarded.add(taskId);
    }

    public LearningProgress copy() {
        LearningProgress copy = new LearningProgress();
        copy.playerName = playerName;
        copy.activeTaskId = activeTaskId;
        copy.completed = new LinkedHashSet<>(completed);
        copy.rewarded = new LinkedHashSet<>(rewarded);
        copy.attempts = new ArrayList<>(attempts);
        stats.forEach((taskId, taskStats) -> copy.stats.put(taskId, taskStats.copy()));
        return copy;
    }

    /** Used when loading a world: malformed notes must never silently replace saved progress. */
    public void validate() {
        if (playerName == null || completed == null || rewarded == null || attempts == null || stats == null
                || attempts.size() > MAX_EVENTS || !completed.containsAll(rewarded)) {
            throw new IllegalArgumentException("Unvollständiger Lernfortschritt.");
        }
        if (activeTaskId != null) {
            validateTaskId(activeTaskId);
        }
        completed.forEach(LearningProgress::validateTaskId);
        rewarded.forEach(LearningProgress::validateTaskId);
        for (Map.Entry<String, TaskStats> entry : stats.entrySet()) {
            validateTaskId(entry.getKey());
            LearningTask task = LearningCatalog.byId(entry.getKey()).orElse(null);
            TaskStats taskStats = entry.getValue();
            if (taskStats == null || taskStats.attempts < 0 || taskStats.incorrect < 0
                    || taskStats.incorrect > taskStats.attempts || taskStats.hints < 0
                    || taskStats.proceduralIssues < 0 || taskStats.patternEvidence == null
                    || (taskStats.attempts > 0 && taskStats.firstAttemptCorrect == null)) {
                throw new IllegalArgumentException("Ungültige Aufgabenstatistik.");
            }
            for (Map.Entry<String, PatternEvidence> evidence : taskStats.patternEvidence.entrySet()) {
                if (evidence.getKey() == null || evidence.getValue() == null
                        || evidence.getValue().answer() == null || tooLong(evidence.getValue().answer())
                        || (task != null && (task.patterns().stream()
                                .noneMatch(pattern -> pattern.id().equals(evidence.getKey()))
                                || task.responsePatterns(evidence.getValue().answer()).stream()
                                        .noneMatch(pattern -> pattern.id().equals(evidence.getKey()))))) {
                    throw new IllegalArgumentException("Ungültige Musterbeobachtung.");
                }
            }
        }
        for (Attempt attempt : attempts) {
            if (attempt == null) {
                throw new IllegalArgumentException("Leere Antwortbeobachtung.");
            }
            validateTaskId(attempt.taskId());
            LearningTask task = LearningCatalog.byId(attempt.taskId()).orElse(null);
            if (attempt.type() == AttemptType.ANSWER && task != null) {
                if (task.matchesAnswer(attempt.answer()) != attempt.correct()
                        || !task.responsePatterns(attempt.answer()).stream().map(ErrorPattern::id).toList()
                                .equals(attempt.patternIds())) {
                    throw new IllegalArgumentException("Ungültige Antwortbeobachtung.");
                }
            } else if (attempt.type() != AttemptType.ANSWER
                    && (attempt.correct() || !attempt.patternIds().isEmpty())) {
                throw new IllegalArgumentException("Bedienhinweise dürfen keine fachlichen Fehlermuster enthalten.");
            }
        }
    }

    private TaskStats taskStats(LearningTask task) {
        Objects.requireNonNull(task, "task");
        requireTask(task.id());
        return stats.computeIfAbsent(task.id(), ignored -> new TaskStats());
    }

    private static LearningTask requireTask(String taskId) {
        validateTaskId(taskId);
        return LearningCatalog.byId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannte Lernaufgabe: " + taskId));
    }

    private static void validateTaskId(String taskId) {
        if (taskId == null || taskId.isBlank() || taskId.length() > 128) {
            throw new IllegalArgumentException("Aufgaben-ID fehlt oder ist zu lang.");
        }
    }

    private void append(Attempt attempt) {
        attempts.add(attempt);
        if (attempts.size() > MAX_EVENTS) {
            attempts.subList(0, attempts.size() - MAX_EVENTS).clear();
        }
    }

    private static boolean tooLong(String text) {
        return text.codePointCount(0, text.length()) > MAX_ANSWER_LENGTH;
    }

    static String limited(String text) {
        if (text == null) {
            return "";
        }
        return tooLong(text) ? text.substring(0, text.offsetByCodePoints(0, MAX_ANSWER_LENGTH)) : text;
    }

    public enum AttemptType {
        ANSWER, HINT, PROCEDURAL
    }

    public record Attempt(String taskId, long timeMillis, String answer, boolean correct,
            boolean hintUsed, List<String> patternIds, AttemptType type) {
        public Attempt {
            Objects.requireNonNull(taskId, "taskId");
            Objects.requireNonNull(answer, "answer");
            Objects.requireNonNull(type, "type");
            if (tooLong(answer)) {
                throw new IllegalArgumentException("Antwortnotiz ist zu lang.");
            }
            patternIds = List.copyOf(Objects.requireNonNull(patternIds, "patternIds"));
        }
    }

    public record PatternEvidence(long timeMillis, String answer) {}

    /** Lifetime totals are independent of the bounded event list. */
    public static final class TaskStats {
        public int attempts;
        public int incorrect;
        public int hints;
        public int proceduralIssues;
        public Long solvedAt;
        public Long lastCorrectAt;
        public Boolean firstAttemptCorrect;
        public boolean firstAttemptHintUsed;
        public Map<String, PatternEvidence> patternEvidence = new LinkedHashMap<>();

        private TaskStats copy() {
            TaskStats copy = new TaskStats();
            copy.attempts = attempts;
            copy.incorrect = incorrect;
            copy.hints = hints;
            copy.proceduralIssues = proceduralIssues;
            copy.solvedAt = solvedAt;
            copy.lastCorrectAt = lastCorrectAt;
            copy.firstAttemptCorrect = firstAttemptCorrect;
            copy.firstAttemptHintUsed = firstAttemptHintUsed;
            copy.patternEvidence = new LinkedHashMap<>(patternEvidence);
            return copy;
        }
    }
}
