package de.kompass4mc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.kompass4mc.learning.Difficulty;
import de.kompass4mc.learning.Interaction;
import de.kompass4mc.learning.LearningTask;
import de.kompass4mc.quest.Subject;
import de.kompass4mc.reward.RewardCatalog;
import de.kompass4mc.reward.RewardSpec;

/** Native conversations shared by single-player and dedicated servers. */
public final class LearningDialogs {
    private LearningDialogs() {}

    public static JsonObject menu(String resident, String token) {
        JsonObject dialog = base(resident + " · auf Lernreise");
        JsonArray body = new JsonArray();
        body.add(message("Hallo! Auf meiner Reise plane und repariere ich vieles und lese Nachrichten. Hilfst du mir?"));
        body.add(message("Wähle einen Auftrag aus Mathematik oder Deutsch. Für jeden erstmals gelösten Auftrag bekommst du Smaragde und ein Geschenk. Im Auftrag siehst du dein genaues Dankeschön. Hinweise und Fehlversuche kosten nichts."));
        JsonArray actions = new JsonArray();
        for (Subject subject : Subject.values()) {
            for (Difficulty difficulty : Difficulty.values()) {
                actions.add(button((subject == Subject.MATH ? "Mathematik" : "Deutsch") + " · "
                                + difficulty.label() + " · " + RewardCatalog.tierDescription(difficulty),
                        prefix(token) + subject.name().toLowerCase(java.util.Locale.ROOT) + "_"
                                + difficulty.name().toLowerCase(java.util.Locale.ROOT), false));
            }
        }
        dialog.add("body", body);
        dialog.add("actions", actions);
        return dialog;
    }

    public static JsonObject task(String resident, String token, LearningTask task, boolean practice,
            String feedback, boolean hint) {
        JsonObject dialog = base(resident + " · " + task.title());
        JsonArray body = new JsonArray();
        body.add(message((task.subject() == Subject.MATH ? "Mathematik" : "Deutsch") + " · "
                + task.difficulty().label() + (practice ? " · Wiederholung ohne weitere Belohnung" : "")));
        if (!practice) body.add(message("Dein Dankeschön: " + RewardCatalog.forTask(task).description()));
        if (!resident.equals(task.npcName())) {
            body.add(message("Ich habe einen Zettel von " + task.npcName() + " dabei:"));
        }
        body.add(message(task.story()));
        body.add(message(task.question()));
        if (feedback != null && !feedback.isBlank()) body.add(message(feedback));
        if (hint) body.add(message("Tipp: " + task.hint()));
        body.add(message("Kein Zeitlimit. Du kannst schließen, Dinge sammeln und später wieder mit mir sprechen."));
        JsonArray actions = new JsonArray();
        if (task.interaction() == Interaction.SHOW_STACK) {
            actions.add(button("Stapel in meiner Haupthand zeigen", prefix(token) + "stapel", false));
        } else if (task.choices().isEmpty()) {
            JsonObject input = new JsonObject();
            input.addProperty("type", "minecraft:text");
            input.addProperty("key", "answer");
            input.addProperty("label", "Deine Antwort");
            input.addProperty("max_length", 128);
            input.addProperty("width", 300);
            JsonArray inputs = new JsonArray();
            inputs.add(input);
            dialog.add("inputs", inputs);
            actions.add(button("Antwort besprechen", prefix(token) + "antwort $(answer)", true));
        } else {
            for (int i = 0; i < task.choices().size(); i++) {
                String letter = Character.toString((char) ('A' + i));
                actions.add(button(letter + ": " + task.choices().get(i), prefix(token) + "antwort " + letter, false));
            }
        }
        actions.add(button("Kannst du mir einen Tipp geben?", prefix(token) + "hinweis", false));
        actions.add(button("Einen anderen Auftrag wählen", prefix(token) + "menu", false));
        dialog.add("body", body);
        dialog.add("actions", actions);
        return dialog;
    }

    public static JsonObject completed(String resident, String token, LearningTask task, RewardSpec reward) {
        JsonObject dialog = base(resident + " · Danke für deine Hilfe!");
        JsonArray body = new JsonArray();
        body.add(message(task.explanation()));
        body.add(message(reward != null ? "Danke für deine Hilfe! Dein Geschenk: " + reward.description() + "."
                : "Gut gelöst! Diese Aufgabe hast du schon einmal belohnt bekommen. Die Wiederholung hilft beim Üben."));
        JsonArray actions = new JsonArray();
        actions.add(button("Wie kann ich noch helfen?", prefix(token) + "menu", false));
        dialog.add("body", body);
        dialog.add("actions", actions);
        return dialog;
    }

    private static String prefix(String token) { return "kompass lernen " + token + " "; }

    private static JsonObject base(String title) {
        JsonObject dialog = new JsonObject();
        dialog.addProperty("type", "minecraft:multi_action");
        dialog.addProperty("title", title);
        dialog.addProperty("pause", true);
        dialog.addProperty("columns", 1);
        JsonObject exit = new JsonObject();
        exit.addProperty("label", "Bis später");
        dialog.add("exit_action", exit);
        return dialog;
    }

    private static JsonObject message(String text) {
        JsonObject result = new JsonObject();
        result.addProperty("type", "minecraft:plain_message");
        result.addProperty("contents", text);
        result.addProperty("width", 360);
        return result;
    }

    private static JsonObject button(String label, String command, boolean dynamic) {
        JsonObject result = new JsonObject();
        result.addProperty("label", label);
        result.addProperty("width", 360);
        JsonObject action = new JsonObject();
        action.addProperty("type", dynamic ? "minecraft:dynamic/run_command" : "minecraft:run_command");
        action.addProperty(dynamic ? "template" : "command", command);
        result.add("action", action);
        return result;
    }
}
