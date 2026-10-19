package de.kompass4mc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.kompass4mc.progress.LearningProgress;
import de.kompass4mc.progress.ParentReport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Parent-only, world-local learning notes and encounter settings. */
public final class ParentCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger("kompass4mc");
    private final MinecraftServer server;
    private final WorldState state;
    private final RoamingNpcs roaming;

    public ParentCommands(MinecraftServer server, WorldState state, RoamingNpcs roaming) {
        this.server = server;
        this.state = state;
        this.roaming = roaming;
    }

    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return registration(() -> this);
    }

    /** Registration precedes SERVER_STARTED; resolve the world service only when commands execute. */
    public static LiteralArgumentBuilder<CommandSourceStack> registration(Supplier<ParentCommands> provider) {
        return Commands.literal("eltern")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> execute(provider, context.getSource(), service -> service.showOwn(context.getSource())))
                .then(Commands.literal("spieler")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(names(provider), builder))
                                .executes(context -> execute(provider, context.getSource(), service -> service.showNamed(
                                        context.getSource(), StringArgumentType.getString(context, "name"))))))
                .then(Commands.literal("export")
                        .executes(context -> execute(provider, context.getSource(), service -> service.exportOwn(context.getSource())))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(names(provider), builder))
                                .executes(context -> execute(provider, context.getSource(), service -> service.exportNamed(
                                        context.getSource(), StringArgumentType.getString(context, "name"))))))
                .then(Commands.literal("export_uuid")
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .executes(context -> execute(provider, context.getSource(), service -> service.exportUuid(
                                        context.getSource(), StringArgumentType.getString(context, "uuid"))))))
                .then(Commands.literal("begegnungen")
                        .then(Commands.literal("an").executes(context -> execute(provider, context.getSource(),
                                service -> service.setEncounters(context.getSource(), true))))
                        .then(Commands.literal("aus").executes(context -> execute(provider, context.getSource(),
                                service -> service.setEncounters(context.getSource(), false)))));
    }

    private static int execute(Supplier<ParentCommands> provider, CommandSourceStack source,
            ParentOperation operation) throws CommandSyntaxException {
        ParentCommands service = provider.get();
        if (service == null) {
            source.sendFailure(Component.literal("[Kompass] Die Lernnotizen sind noch nicht verfügbar. "
                    + "Bitte warte, bis die Welt geladen ist."));
            return 0;
        }
        return operation.run(service);
    }

    private static List<String> names(Supplier<ParentCommands> provider) {
        ParentCommands service = provider.get();
        return service == null ? List.of() : service.playerNames();
    }

    private List<String> playerNames() {
        List<String> names = new ArrayList<>();
        for (LearningProgress progress : state.learningProgresses().values()) {
            if (!progress.playerName.isBlank()) {
                names.add(progress.playerName);
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            names.add(player.getName().getString());
        }
        return names.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private int showOwn(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return show(source, new Target(player.getStringUUID(), player.getName().getString(),
                state.learningProgresses().get(player.getStringUUID())));
    }

    private int showNamed(CommandSourceStack source, String name) {
        Target target = find(source, name);
        return target == null ? 0 : show(source, target);
    }

    private int show(CommandSourceStack source, Target target) {
        if (!hasNotes(source, target)) {
            return 0;
        }
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null) {
            ParentReport.summary(target.progress()).forEach(line -> success(source, line));
            success(source, "HTML-Bericht: /kompass eltern export " + target.name());
            return 1;
        }
        return DialogDelivery.open(viewer,
                reportDialog(target.progress(), target.uuid(), state.automaticLearningNpcs())) ? 1 : 0;
    }

    private int exportOwn(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return export(source, new Target(player.getStringUUID(), player.getName().getString(),
                state.learningProgresses().get(player.getStringUUID())));
    }

    private int exportNamed(CommandSourceStack source, String name) {
        Target target = find(source, name);
        return target == null ? 0 : export(source, target);
    }

    private int exportUuid(CommandSourceStack source, String rawUuid) {
        String uuid;
        try {
            uuid = UUID.fromString(rawUuid).toString();
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("[Kompass] Diese Spieler-ID ist ungültig."));
            return 0;
        }
        LearningProgress progress = state.learningProgresses().get(uuid);
        return export(source, new Target(uuid, progress == null ? uuid : progress.playerName, progress));
    }

    private int export(CommandSourceStack source, Target target) {
        if (!hasNotes(source, target)) {
            return 0;
        }
        try {
            Path output = writeReport(server.getWorldPath(LevelResource.ROOT), target.uuid(),
                    ParentReport.html(target.progress(), target.name()));
            success(source, "Elternbericht für " + target.name() + " gespeichert: " + output.toAbsolutePath());
            success(source, "Die HTML-Datei im Weltordner unter kompass-berichte kannst du mit einem Browser öffnen.");
            return 1;
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.error("Lokaler Elternbericht konnte nicht gespeichert werden", exception);
            source.sendFailure(Component.literal("[Kompass] Der Bericht konnte nicht gespeichert werden. "
                    + "Der Lernfortschritt bleibt erhalten; Details stehen in latest.log."));
            return 0;
        }
    }

    private Target find(CommandSourceStack source, String name) {
        Map<String, Target> matches = new LinkedHashMap<>();
        for (String uuid : savedMatches(state.learningProgresses(), name)) {
            LearningProgress progress = state.learningProgresses().get(uuid);
            matches.put(uuid, new Target(uuid, progress.playerName, progress));
        }
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            matches.put(online.getStringUUID(), new Target(online.getStringUUID(), online.getName().getString(),
                    state.learningProgresses().get(online.getStringUUID())));
        }
        if (matches.isEmpty()) {
            source.sendFailure(Component.literal("[Kompass] Für „" + name + "“ wurden keine Lernnotizen gefunden. "
                    + "Verwende den Minecraft-Spielernamen. Frühere Dorfaufträge werden nicht nachträglich erfasst."));
            return null;
        }
        if (matches.size() > 1) {
            source.sendFailure(Component.literal("[Kompass] Der Name „" + name + "“ ist nicht eindeutig. "
                    + "Bitte die Übersicht im Konto des Kindes mit /kompass eltern öffnen."));
            return null;
        }
        return matches.values().iterator().next();
    }

    private boolean hasNotes(CommandSourceStack source, Target target) {
        if (target.progress() == null || target.progress().stats.isEmpty()) {
            source.sendFailure(Component.literal("[Kompass] Für " + target.name()
                    + " gibt es noch keine Lernnotizen der neuen Lernreisenden. "
                    + "Sie entstehen bei Antworten, geöffneten Hinweisen und Bedienfragen. "
                    + "Frühere Dorfaufträge werden nicht nachträglich erfasst."));
            return false;
        }
        return true;
    }

    private int setEncounters(CommandSourceStack source, boolean enabled) {
        try {
            saveEncounterSetting(state, enabled);
        } catch (IOException exception) {
            LOGGER.error("Begegnungseinstellung konnte nicht gespeichert werden", exception);
            source.sendFailure(Component.literal("[Kompass] Die Einstellung konnte nicht gespeichert werden. "
                    + "Die bisherige Einstellung bleibt bestehen; Details stehen in latest.log."));
            return 0;
        }
        roaming.setAutoSpawn(enabled);
        success(source, enabled ? "Automatische Lernbegegnungen sind eingeschaltet."
                : "Automatische Lernbegegnungen sind ausgeschaltet. Bereits erschienene Reisende bleiben noch eine Weile ansprechbar.");
        return 1;
    }

    private static void success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[Kompass] " + message), false);
    }

    static List<String> savedMatches(Map<String, LearningProgress> notes, String name) {
        return notes.entrySet().stream().filter(entry -> entry.getValue().playerName.equalsIgnoreCase(name))
                .map(Map.Entry::getKey).toList();
    }

    public static JsonObject reportDialog(LearningProgress progress, String uuid, boolean encounters) {
        JsonObject dialog = new JsonObject();
        dialog.addProperty("type", "minecraft:multi_action");
        dialog.addProperty("title", "Elternübersicht · " + progress.playerName);
        dialog.addProperty("pause", true);
        JsonArray body = new JsonArray();
        ParentReport.summary(progress).forEach(line -> body.add(message(line)));
        body.add(message("Automatische Lernbegegnungen: " + (encounters ? "an" : "aus")
                + ". Der vollständige HTML-Bericht wird nur im Weltordner gespeichert."));
        dialog.add("body", body);
        dialog.addProperty("columns", 1);
        JsonArray actions = new JsonArray();
        JsonObject export = new JsonObject();
        export.addProperty("label", "HTML-Bericht speichern");
        export.addProperty("width", 300);
        JsonObject action = new JsonObject();
        action.addProperty("type", "minecraft:run_command");
        action.addProperty("command", "kompass eltern export_uuid " + UUID.fromString(uuid));
        export.add("action", action);
        actions.add(export);
        dialog.add("actions", actions);
        JsonObject close = new JsonObject();
        close.addProperty("label", "Schließen");
        dialog.add("exit_action", close);
        return dialog;
    }

    private static JsonObject message(String text) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "minecraft:plain_message");
        message.addProperty("contents", text);
        message.addProperty("width", 480);
        return message;
    }

    /** The filename is derived solely from a parsed UUID, never a player-supplied name. */
    static Path writeReport(Path worldRoot, String playerUuid, String html) throws IOException {
        String uuid = UUID.fromString(playerUuid).toString();
        Path directory = worldRoot.resolve("kompass-berichte");
        Files.createDirectories(directory);
        Path destination = directory.resolve(uuid + ".html");
        Path temporary = Files.createTempFile(directory, "kompass-bericht-", ".tmp");
        try {
            Files.writeString(temporary, html, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return destination;
    }

    /** Persist the setting before the caller changes the live NPC scheduler. */
    static void saveEncounterSetting(WorldState state, boolean enabled) throws IOException {
        boolean previous = state.automaticLearningNpcs();
        state.automaticLearningNpcs(enabled);
        try {
            state.save();
        } catch (IOException exception) {
            state.automaticLearningNpcs(previous);
            throw exception;
        }
    }

    private record Target(String uuid, String name, LearningProgress progress) {}

    @FunctionalInterface
    private interface ParentOperation {
        int run(ParentCommands service) throws CommandSyntaxException;
    }
}
