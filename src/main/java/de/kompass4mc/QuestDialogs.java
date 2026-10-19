package de.kompass4mc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.kompass4mc.quest.Quest;
import de.kompass4mc.quest.Subject;

/** Vanilla dialog definitions: no custom client, textures, or network protocol required. */
public final class QuestDialogs {
    private QuestDialogs() {}

    public static JsonObject quest(Quest quest, String feedback) {
        JsonObject dialog = new JsonObject();
        dialog.addProperty("type", "minecraft:multi_action");
        dialog.addProperty("title", quest.title());
        dialog.addProperty("pause", false);
        JsonArray body = new JsonArray();
        body.add(message(quest.subject() == Subject.MATH ? "Mathematik" : "Deutsch"));
        body.add(message(quest.story()));
        body.add(message(quest.question()));
        if (feedback != null && !feedback.isBlank()) {
            body.add(message(feedback));
        }
        body.add(message("Ohne Zeitlimit. Du kannst schließen und später mit /kompass weiter fortsetzen."));
        dialog.add("body", body);
        dialog.addProperty("columns", 1);
        JsonArray actions = new JsonArray();
        if (quest.choices().isEmpty()) {
            JsonArray inputs = new JsonArray();
            JsonObject input = new JsonObject();
            input.addProperty("type", "minecraft:text");
            input.addProperty("key", "answer");
            input.addProperty("label", "Deine Antwort");
            input.addProperty("max_length", 128);
            input.addProperty("width", 300);
            inputs.add(input);
            dialog.add("inputs", inputs);
            actions.add(button("Antwort prüfen", "kompass antwort " + quest.id() + " $(answer)", true));
        } else {
            for (int index = 0; index < quest.choices().size(); index++) {
                String letter = Character.toString((char) ('A' + index));
                JsonObject action = button(letter + ": " + quest.choices().get(index),
                        "kompass antwort " + quest.id() + " " + letter, false);
                action.addProperty("width", 360);
                actions.add(action);
            }
        }
        actions.add(button("Hinweis", "kompass hinweis", false));
        dialog.add("actions", actions);
        JsonObject exit = new JsonObject();
        exit.addProperty("label", "Später weiter");
        dialog.add("exit_action", exit);
        return dialog;
    }

    private static JsonObject message(String text) {
        JsonObject body = new JsonObject();
        body.addProperty("type", "minecraft:plain_message");
        body.addProperty("contents", text);
        body.addProperty("width", 360);
        return body;
    }

    private static JsonObject button(String label, String command, boolean dynamic) {
        JsonObject button = new JsonObject();
        button.addProperty("label", label);
        JsonObject action = new JsonObject();
        action.addProperty("type", dynamic ? "minecraft:dynamic/run_command" : "minecraft:run_command");
        action.addProperty(dynamic ? "template" : "command", command);
        button.add("action", action);
        return button;
    }
}
