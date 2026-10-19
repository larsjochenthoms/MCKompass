package de.kompass4mc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.kompass4mc.village.Kind;
import de.kompass4mc.village.MissionJob;

/** Conversations lead to actions in the world, not a numeric answer form. */
public final class VillageDialogs {
    private VillageDialogs() {}

    public static JsonObject conversation(MissionJob job, VillageState.Project project, String player, String feedback) {
        JsonObject dialog = new JsonObject();
        dialog.addProperty("type", "minecraft:multi_action");
        dialog.addProperty("title", job.residentName() + " · " + job.role());
        dialog.addProperty("pause", true);
        dialog.addProperty("columns", 1);
        JsonArray body = new JsonArray();
        body.add(message(job.title()));
        JsonArray actions = new JsonArray();
        String prefix = "kompass bewohner " + job.id() + " ";
        if (project.completed) {
            body.add(message(job.completionText()));
            if (project.canClaim(player)) {
                body.add(message("Danke, dass du mitgeholfen hast. Hier ist dein persönliches Dankeschön."));
                actions.add(button("Dankeschön annehmen", prefix + "danke"));
            } else {
                body.add(message(project.rewardClaimed.contains(player)
                        ? "Deine Hilfe macht unser Dorf schöner. Schön, dich wiederzusehen!"
                        : "Das Projekt ist schon fertig. Schau dich um und sprich auch mit den anderen Bewohnern."));
            }
        } else if (!project.accepted.contains(player)) {
            body.add(message(job.request()));
            actions.add(button("Ich helfe dir", prefix + "annehmen"));
        } else {
            body.add(message(job.request()));
            body.add(message(job.acceptedText()));
            if (job.kind() == Kind.DELIVERY) {
                body.add(message("Packe den passenden Apfelstapel in deine Haupthand und sprich mich wieder an."));
            }
            actions.add(button(job.kind() == Kind.DELIVERY ? "Meine Lieferung übergeben" : "Schau dir mein Bauwerk an",
                    prefix + "pruefen"));
            actions.add(button("Kannst du mir einen Tipp geben?", prefix + "tipp"));
        }
        if (feedback != null && !feedback.isBlank()) {
            body.add(message(feedback));
        }
        dialog.add("body", body);
        if (actions.isEmpty()) {
            actions.add(button("Bis bald!", null));
        }
        dialog.add("actions", actions);
        JsonObject exit = new JsonObject();
        exit.addProperty("label", "Bis später");
        dialog.add("exit_action", exit);
        return dialog;
    }

    private static JsonObject message(String text) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "minecraft:plain_message");
        message.addProperty("contents", text);
        message.addProperty("width", 360);
        return message;
    }

    private static JsonObject button(String label, String command) {
        JsonObject button = new JsonObject();
        button.addProperty("label", label);
        button.addProperty("width", 300);
        if (command != null) {
            JsonObject action = new JsonObject();
            action.addProperty("type", "minecraft:run_command");
            action.addProperty("command", command);
            button.add("action", action);
        }
        return button;
    }
}
