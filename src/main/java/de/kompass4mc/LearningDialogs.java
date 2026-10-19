package de.kompass4mc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.kompass4mc.learning.Interaction;
import de.kompass4mc.learning.LearningTask;
import de.kompass4mc.reward.RewardCatalog;
import de.kompass4mc.reward.RewardSpec;

/** Short, diegetic conversations: one challenge, no subject or difficulty selection. */
public final class LearningDialogs {
    private LearningDialogs() {}
    public static JsonObject menu(String resident, String token) { return waiting(resident); }

    public static JsonObject waiting(String resident) {
        JsonObject dialog = base(resident);
        JsonArray body = new JsonArray();
        body.add(message("Danke fürs Vorbeischauen! Ich mache gerade eine Pause. Schau später wieder vorbei."));
        dialog.add("body", body); notice(dialog); return dialog;
    }

    public static JsonObject task(String resident, String token, LearningTask task, boolean practice,
                                  String feedback, boolean hint) {
        return task(resident, token, task, feedback, hint, false, false);
    }

    public static JsonObject task(String resident, String token, LearningTask task, String feedback,
                                  boolean hint, boolean materialsIssued, boolean siteChosen) {
        JsonObject dialog = base(resident + " · " + task.title());
        JsonArray body = new JsonArray();
        body.add(message(task.story())); body.add(message(task.question()));
        body.add(message("Dankeschön: " + RewardCatalog.forTask(task).description()));
        if (feedback != null && !feedback.isBlank()) body.add(message(feedback));
        if (hint) body.add(message(task.hint()));
        JsonArray actions = new JsonArray();
        if (task.interaction() == Interaction.BUILD) {
            if (!materialsIssued) actions.add(button("Bauplan und Material holen", prefix(token) + "material", false));
            else if (!siteChosen) body.add(message("Klicke mit dem Bauplan auf den Boden: Dort beginnt dein Bau."));
            if (siteChosen) actions.add(button("Bauwerk zeigen", prefix(token) + "bauen", false));
        } else if (task.interaction() == Interaction.SHOW_STACK) {
            actions.add(button("Stapel zeigen", prefix(token) + "stapel", false));
        } else if (task.choices().isEmpty()) {
            JsonObject input = new JsonObject();
            input.addProperty("type", "minecraft:text"); input.addProperty("key", "answer");
            input.addProperty("label", "Dein Vorschlag"); input.addProperty("max_length", 128); input.addProperty("width", 300);
            JsonArray inputs = new JsonArray(); inputs.add(input); dialog.add("inputs", inputs);
            actions.add(button("Sagen", prefix(token) + "antwort $(answer)", true));
        } else {
            for (int i = 0; i < task.choices().size(); i++) actions.add(button(choiceLabel(task, i),
                    prefix(token) + "antwort " + (char) ('A' + i), false));
        }
        actions.add(button("Ein Tipp?", prefix(token) + "hinweis", false));
        dialog.add("body", body); dialog.add("actions", actions); return dialog;
    }

    public static JsonObject completed(String resident, String token, LearningTask task, RewardSpec reward) {
        JsonObject dialog = base(resident);
        JsonArray body = new JsonArray();
        body.add(message(reward == null ? "Danke! Das hat mir sehr geholfen. Bis später!"
                : "Danke! Hier ist dein Geschenk: " + reward.description() + "."));
        dialog.add("body", body); notice(dialog); return dialog;
    }

    // Historical answer values remain stable in saved notes; players see messages, not grammar labels.
    private static String choiceLabel(LearningTask task, int index) {
        if (task.id().equals("grammar_finished_roof")) return java.util.List.of(
                "Mila deckt das Dach.", "Mila deckte das Dach.",
                "Mila hat das Dach gedeckt.", "Mila wird das Dach decken.").get(index);
        return task.choices().get(index);
    }

    private static void notice(JsonObject dialog) {
        dialog.addProperty("type", "minecraft:notice");
        dialog.remove("columns"); dialog.remove("exit_action");
        JsonObject action = new JsonObject(); action.addProperty("label", "Bis später");
        dialog.add("action", action);
    }

    private static String prefix(String token) { return "kompass challenge " + token + " "; }
    private static JsonObject base(String title) {
        JsonObject dialog = new JsonObject();
        dialog.addProperty("type", "minecraft:multi_action"); dialog.addProperty("title", title);
        dialog.addProperty("pause", true); dialog.addProperty("columns", 1);
        JsonObject exit = new JsonObject(); exit.addProperty("label", "Bis später"); dialog.add("exit_action", exit); return dialog;
    }
    private static JsonObject message(String text) {
        JsonObject result = new JsonObject(); result.addProperty("type", "minecraft:plain_message");
        result.addProperty("contents", text); result.addProperty("width", 360); return result;
    }
    private static JsonObject button(String label, String command, boolean dynamic) {
        JsonObject result = new JsonObject(); result.addProperty("label", label); result.addProperty("width", 360);
        JsonObject action = new JsonObject(); action.addProperty("type", dynamic ? "minecraft:dynamic/run_command" : "minecraft:run_command");
        action.addProperty(dynamic ? "template" : "command", command); result.add("action", action); return result;
    }
}
