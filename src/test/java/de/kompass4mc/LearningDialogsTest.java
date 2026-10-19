package de.kompass4mc;

import static org.junit.jupiter.api.Assertions.*;
import de.kompass4mc.learning.Interaction;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.reward.RewardCatalog;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LearningDialogsTest {
    @Test
    void oneChallengeHasNoPickerAndClosingNeverSubmits() {
        var school = Pattern.compile("(?iu)\\b(lernen|lernreise|lernreisende|aufgabe[n]?|übung[en]*|mathematik|deutsch|sprachheft|nomen|perfekt|präteritum|präsens|futur|schwierigkeitsstufe[n]?)\\b");
        for (var task : LearningCatalog.all()) {
            var dialog = LearningDialogs.task(task.npcName(), "session-token", task, false, null, false);
            assertFalse(dialog.getAsJsonObject("exit_action").has("action"));
            assertTrue(dialog.get("pause").getAsBoolean());
            assertFalse(school.matcher(dialog.toString()).find(), task.id() + ": " + dialog);
            assertFalse(school.matcher(LearningDialogs.task(task.npcName(), "token", task, false, null, true).toString()).find(), task.id() + " hint");
            assertTrue(dialog.getAsJsonArray("body").size() <= 3);
            assertFalse(dialog.toString().contains("auswahl"));
            if (task.interaction() == Interaction.SHOW_STACK) {
                assertFalse(dialog.has("inputs"));
                assertTrue(dialog.getAsJsonArray("actions").get(0).getAsJsonObject()
                        .getAsJsonObject("action").get("command").getAsString().endsWith(" stapel"));
            } else if (task.interaction() == Interaction.BUILD) {
                assertFalse(dialog.has("inputs"));
                assertTrue(dialog.toString().contains("material"));
                assertFalse(dialog.toString().contains("antwort"));
                var issued = LearningDialogs.task("Mila", "token", task, null, false, true, true);
                assertFalse(issued.toString().contains("material\""));
                assertTrue(issued.toString().contains("bauen"));
            }
        }
        assertFalse(LearningDialogs.waiting("Jaro").has("actions"));
    }

    @Test
    void everyOfferPromisesTheActualGiftAndDoneHasNoFurtherAction() {
        for (var task : LearningCatalog.all()) {
            var reward = RewardCatalog.forTask(task);
            assertTrue(LearningDialogs.task(task.npcName(), "token", task, false, null, false)
                    .toString().contains(reward.description()));
            var completion = LearningDialogs.completed(task.npcName(), "token", task, reward);
            assertTrue(completion.toString().contains(reward.description()));
            assertFalse(completion.has("actions"));
            assertFalse(completion.getAsJsonObject("action").has("action"));
            assertFalse(LearningDialogs.completed(task.npcName(), "token", task, null).toString()
                    .contains("Geschenk:"));
        }
    }
}
