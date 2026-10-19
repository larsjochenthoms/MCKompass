package de.kompass4mc;

import static org.junit.jupiter.api.Assertions.*;
import de.kompass4mc.learning.Interaction;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.reward.RewardCatalog;
import org.junit.jupiter.api.Test;

class LearningDialogsTest {
    @Test
    void closingNeverSubmitsAndShowingStacksNeverOffersTextBypass() {
        for (var task : LearningCatalog.all()) {
            var dialog = LearningDialogs.task("Mila", "session-token", task, false, null, false);
            assertFalse(dialog.getAsJsonObject("exit_action").has("action"));
            assertTrue(dialog.get("pause").getAsBoolean());
            if (task.interaction() == Interaction.SHOW_STACK) {
                assertFalse(dialog.has("inputs"));
                assertTrue(dialog.getAsJsonArray("actions").get(0).getAsJsonObject()
                        .getAsJsonObject("action").get("command").getAsString().endsWith(" stapel"));
            }
        }
    }

    @Test
    void menuShowsEachRewardAndPracticeMakesNoFurtherRewardExplicit() {
        var menu = LearningDialogs.menu("Jaro", "session-token");
        assertEquals(6, menu.getAsJsonArray("actions").size());
        assertTrue(menu.toString().contains("2 Smaragde"));
        assertTrue(menu.toString().contains("4 Smaragde"));
        assertTrue(menu.toString().contains("6 Smaragde"));
        var task = LearningCatalog.all().getFirst();
        assertTrue(LearningDialogs.task("Mila", "token", task, true, null, false)
                .toString().contains("ohne weitere Belohnung"));
    }

    @Test
    void promisedGiftMatchesTheCompletionAndPracticePromisesNothing() {
        for (var task : LearningCatalog.all()) {
            var reward = RewardCatalog.forTask(task);
            String offer = LearningDialogs.task("Mila", "token", task, false, null, false).toString();
            String completion = LearningDialogs.completed("Mila", "token", task, reward).toString();
            assertTrue(offer.contains(reward.description()));
            assertTrue(completion.contains(reward.description()));
            assertFalse(LearningDialogs.task("Mila", "token", task, true, null, false).toString()
                    .contains("Dein Dankeschön:"));
            assertFalse(LearningDialogs.completed("Mila", "token", task, null).toString()
                    .contains("Dein Geschenk:"));
        }
    }
}
