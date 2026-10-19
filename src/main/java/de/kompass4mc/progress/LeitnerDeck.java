package de.kompass4mc.progress;

import de.kompass4mc.learning.ChallengeRole;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.learning.LearningTask;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/** Five hidden boxes. Selection and cooldowns use persisted real-world milliseconds. */
public final class LeitnerDeck {
    private static final long[] DELAYS = {0, 60_000, 300_000, 1_800_000, 7_200_000, 86_400_000};
    private static final int[] WEIGHTS = {0, 8, 5, 3, 2, 1};
    public static final long OFFER_MILLIS = 5 * 60_000;
    public static final long BUILD_MILLIS = 20 * 60_000;
    public static final long RESTOCK_MILLIS = 60_000;
    private LeitnerDeck() {}

    public static final class Card {
        public int box = 1;
        public int reviews;
        public long lastReviewMillis;
        public long dueMillis;
        public Card copy() {
            var c = new Card(); c.box = box; c.reviews = reviews;
            c.lastReviewMillis = lastReviewMillis; c.dueMillis = dueMillis; return c;
        }
    }

    public static void review(LearningProgress progress, String taskId, boolean correct, long now) {
        Card card = progress.cards.computeIfAbsent(taskId, ignored -> new Card());
        card.box = correct ? Math.min(5, card.box + 1) : 1;
        if (card.reviews < Integer.MAX_VALUE) card.reviews++;
        card.lastReviewMillis = Math.max(0, now);
        card.dueMillis = card.lastReviewMillis > Long.MAX_VALUE - DELAYS[card.box]
                ? Long.MAX_VALUE : card.lastReviewMillis + DELAYS[card.box];
    }

    public static Optional<LearningTask> draw(LearningProgress progress, ChallengeRole role, long now,
                                            RandomGenerator random, String previousId) {
        List<LearningTask> due = LearningCatalog.all().stream().filter(t -> ChallengeRole.forTask(t) == role)
                .filter(t -> !progress.cards.containsKey(t.id()) || progress.cards.get(t.id()).dueMillis <= now).toList();
        List<LearningTask> different = due.stream().filter(t -> !t.id().equals(previousId)).toList();
        if (!different.isEmpty()) due = different;
        List<LearningTask> reviewed = due.stream().filter(t -> progress.cards.containsKey(t.id())).toList();
        // Due, previously troublesome cards are not buried beneath a large pile of unseen cards.
        if (!reviewed.isEmpty() && random.nextInt(10) < 7) due = reviewed;
        if (due.isEmpty()) return Optional.empty();
        int total = due.stream().mapToInt(t -> weight(progress, t)).sum();
        int pick = random.nextInt(total);
        for (LearningTask task : due) { pick -= weight(progress, task); if (pick < 0) return Optional.of(task); }
        throw new IllegalStateException("Ungültige Kartenauswahl");
    }

    private static int weight(LearningProgress progress, LearningTask task) {
        Card card = progress.cards.get(task.id());
        return WEIGHTS[card == null ? 1 : card.box];
    }
}
