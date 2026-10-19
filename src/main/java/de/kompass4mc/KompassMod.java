package de.kompass4mc;

import com.mojang.brigadier.arguments.StringArgumentType;
import de.kompass4mc.quest.Quest;
import de.kompass4mc.quest.QuestCatalog;
import de.kompass4mc.learning.LearningCatalog;
import java.io.IOException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KompassMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("kompass4mc");
    private WorldState state;
    private MinecraftServer server;
    private VillageService village;
    private RoamingNpcs residents;
    private LearningService learning;
    private ParentCommands parents;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(started -> {
            server = started;
            try {
                state = new WorldState(started.getWorldPath(LevelResource.ROOT).resolve("kompass4mc.json"));
                village = new VillageService(started, state);
                residents = new RoamingNpcs(started);
                residents.setAutoSpawn(state.automaticLearningNpcs());
                learning = new LearningService(started, state, residents);
                parents = new ParentCommands(started, state, residents);
            } catch (IOException exception) {
                throw new IllegalStateException("Kompass-Spielstand konnte nicht geladen werden", exception);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(ticking -> {
            if (learning != null) learning.tick();
            if (residents != null) residents.tick();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(stopping -> {
            if (residents != null) residents.close();
            residents = null;
            learning = null;
            parents = null;
            if (village != null) {
                village.close();
                village = null;
            }
            if (state != null) {
                save();
            }
            state = null;
            server = null;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (RoamingNpcs.isNpc(entity)) {
                if (learning != null && hand == InteractionHand.MAIN_HAND && !player.isSpectator()
                        && player instanceof ServerPlayer learner) learning.talk(learner, entity);
                return InteractionResult.SUCCESS;
            }
            if (village == null || !village.isResident(entity)) {
                return InteractionResult.PASS;
            }
            if (hand == InteractionHand.MAIN_HAND && !player.isSpectator() && player instanceof ServerPlayer learner) {
                village.talk(learner, entity);
            }
            return InteractionResult.SUCCESS;
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                (entity.entityTags().contains(VillageBuilder.NPC_TAG) || RoamingNpcs.isNpc(entity))
                        ? InteractionResult.FAIL : InteractionResult.PASS);
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !(player instanceof ServerPlayer learner)
                    || state == null) {
                return InteractionResult.PASS;
            }
            WorldState.HidingPlace place = state.hidingPlaces().get(key(learner, hit.getBlockPos()));
            if (place == null) {
                return InteractionResult.PASS;
            }
            if (!isQuestContainer(learner.level(), hit.getBlockPos())) {
                state.hidingPlaces().remove(key(learner, hit.getBlockPos()));
                save();
                return InteractionResult.PASS;
            }
            QuestCatalog.byId(place.questId()).ifPresent(quest -> discover(learner, quest));
            return InteractionResult.SUCCESS;
        });
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, block, entity) -> {
            if (state == null || !(player instanceof ServerPlayer learner)) {
                return true;
            }
            if (village != null && village.protectsFoundation(learner, pos) && !gameMaster(learner.createCommandSourceStack())) {
                tell(learner, "Der Dorfplatz bleibt erhalten. Auf den Baustellen kannst du selbst bauen.");
                return false;
            }
            if (!state.hidingPlaces().containsKey(key(learner, pos))) {
                return true;
            }
            if (!gameMaster(learner.createCommandSourceStack())) {
                tell(learner, "Dieses Quest-Versteck bleibt für alle erhalten.");
                return false;
            }
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, block, entity) -> {
            if (state != null && player instanceof ServerPlayer learner
                    && state.hidingPlaces().remove(key(learner, pos)) != null) {
                save();
            }
        });
        registerCommands();
        LOGGER.info("Kompass-Abenteuer {}: {} Reiseaufträge und {} zusätzliche Lernquests geladen.",
                version(), LearningCatalog.all().size(), QuestCatalog.all().size());
    }

    private static boolean gameMaster(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("kompass")
                    .executes(context -> help(context.getSource()))
                    .then(Commands.literal("lernen")
                            .executes(context -> learning.resume(context.getSource().getPlayerOrException(), false))
                            .then(Commands.argument("gespraech", StringArgumentType.word())
                                    .then(Commands.argument("aktion", StringArgumentType.word())
                                            .executes(context -> learning.action(context.getSource().getPlayerOrException(),
                                                    StringArgumentType.getString(context, "gespraech"),
                                                    StringArgumentType.getString(context, "aktion"), ""))
                                            .then(Commands.argument("eingabe", StringArgumentType.greedyString())
                                                    .executes(context -> learning.action(context.getSource().getPlayerOrException(),
                                                            StringArgumentType.getString(context, "gespraech"),
                                                            StringArgumentType.getString(context, "aktion"),
                                                            StringArgumentType.getString(context, "eingabe")))))))
                    .then(Commands.literal("begegnung").requires(KompassMod::gameMaster)
                            .executes(context -> encounter(context.getSource().getPlayerOrException())))
                    .then(ParentCommands.registration(() -> parents))
                    .then(Commands.literal("tagebuch").executes(context -> journal(context.getSource().getPlayerOrException())))
                    .then(Commands.literal("weiter").executes(context -> resume(context.getSource().getPlayerOrException(), false)))
                    .then(Commands.literal("hinweis").executes(context -> resume(context.getSource().getPlayerOrException(), true)))
                    .then(Commands.literal("antwort")
                            .then(Commands.argument("quest", StringArgumentType.word())
                                    .executes(context -> answer(context.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(context, "quest"), ""))
                                    .then(Commands.argument("antwort", StringArgumentType.greedyString())
                                            .executes(context -> answer(context.getSource().getPlayerOrException(),
                                                    StringArgumentType.getString(context, "quest"),
                                                    StringArgumentType.getString(context, "antwort"))))))
                    .then(Commands.literal("suchen").executes(context -> search(context.getSource().getPlayerOrException()))
                            .then(Commands.literal("uebungen").executes(context -> searchExercises(context.getSource().getPlayerOrException()))))
                    .then(Commands.literal("abenteuer").requires(KompassMod::gameMaster)
                            .executes(context -> village.create(context.getSource().getPlayerOrException())))
                    .then(Commands.literal("dorf").requires(KompassMod::gameMaster)
                            .executes(context -> village.create(context.getSource().getPlayerOrException())))
                    .then(Commands.literal("bewohner")
                            .then(Commands.argument("auftrag", StringArgumentType.word())
                                    .then(Commands.argument("aktion", StringArgumentType.word())
                                            .executes(context -> village.action(context.getSource().getPlayerOrException(),
                                                    StringArgumentType.getString(context, "auftrag"),
                                                    StringArgumentType.getString(context, "aktion"))))))
                    .then(Commands.literal("uebungsrunde").requires(KompassMod::gameMaster)
                            .executes(context -> adventure(context.getSource().getPlayerOrException())))
                    .then(Commands.literal("versteck").requires(KompassMod::gameMaster)
                            .then(Commands.argument("quest", StringArgumentType.word())
                                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                            QuestCatalog.all().stream().map(Quest::id), builder))
                                    .then(Commands.argument("position", BlockPosArgument.blockPos())
                                            .executes(context -> bind(context.getSource(),
                                                    StringArgumentType.getString(context, "quest"),
                                                    BlockPosArgument.getLoadedBlockPos(context, "position"))))))
                    .then(Commands.literal("entfernen").requires(KompassMod::gameMaster)
                            .then(Commands.argument("position", BlockPosArgument.blockPos())
                                    .executes(context -> unbind(context.getSource(),
                                            BlockPosArgument.getLoadedBlockPos(context, "position"))))));
        });
    }

    private int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Kompass-Abenteuer " + version() + ": Mila, Jaro und Lio erscheinen auf Lernreise in deiner Oberwelt. "
                + "Sprich sie mit Rechtsklick an.\n/kompass lernen - Gespräch in der Nähe fortsetzen\n"
                + "/kompass suchen - einen erschienenen Lernreisenden finden\n"
                + "/kompass tagebuch - deine Aufträge und Fortschritte"), false);
        if (gameMaster(source)) {
            source.sendSuccess(() -> Component.literal("Eltern: /kompass eltern zeigt Lernnotizen; /kompass eltern export erstellt einen lokalen Bericht.\n"
                    + "/kompass begegnung versucht sofort eine Begegnung auf geeignetem Gelände.\n"
                    + "/kompass eltern begegnungen an|aus steuert automatische Begegnungen.\n"
                    + "/kompass dorf legt optional den früheren Dorfplatz mit drei Bewohnern an.\n"
                    + "/kompass uebungsrunde legt die zusätzliche Runde mit zwölf Übungsfässern an.\n"
                    + "/kompass versteck <quest> <x> <y> <z> bindet ein vorhandenes Fass oder eine Truhe.\n"
                    + "/kompass entfernen <x> <y> <z> löst eine Bindung."), false);
        }
        return 1;
    }

    private int encounter(ServerPlayer player) {
        if (residents.spawnNear(player)) return 1;
        residents.nearest(player).filter(npc -> npc.distanceToSqr(player) <= 4096).ifPresentOrElse(
                npc -> tell(player, RoamingNpcs.residentName(npc) + " ist schon in der Nähe: "
                        + npc.blockPosition().getX() + " / " + npc.blockPosition().getY() + " / " + npc.blockPosition().getZ()),
                () -> tell(player, "Hier ist gerade kein geeigneter Platz frei. Versuche es draußen in der Oberwelt auf trockenem, ebenem Gelände."));
        return 0;
    }

    private WorldState.Progress progress(ServerPlayer player) {
        return state.progress(player.getUUID().toString());
    }

    private static String key(ServerPlayer player, BlockPos pos) {
        return WorldState.positionKey(player.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean isQuestContainer(ServerLevel level, BlockPos pos) {
        var block = level.getBlockState(pos);
        return block.is(Blocks.BARREL) || block.is(Blocks.CHEST) || block.is(Blocks.TRAPPED_CHEST);
    }

    private void pruneMissingHidingPlaces(ServerLevel level) {
        String dimension = level.dimension().identifier().toString();
        boolean removed = state.hidingPlaces().values().removeIf(place -> {
            BlockPos pos = new BlockPos(place.x(), place.y(), place.z());
            return place.dimension().equals(dimension) && level.hasChunkAt(pos) && !isQuestContainer(level, pos);
        });
        if (removed) {
            save();
        }
    }

    private static void tell(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("[Kompass] " + message));
    }

    private boolean save() {
        try {
            state.save();
            return true;
        } catch (IOException exception) {
            LOGGER.error("Kompass-Spielstand konnte nicht gespeichert werden", exception);
            return false;
        }
    }

    private void discover(ServerPlayer player, Quest quest) {
        WorldState.Progress progress = progress(player);
        if (progress.completed.contains(quest.id())) {
            tell(player, "\"" + quest.title() + "\" hast du bereits gelöst. " + quest.explanation());
            return;
        }
        progress.discovered.add(quest.id());
        progress.activeQuest = quest.id();
        save();
        show(player, quest, null);
    }

    private void show(ServerPlayer player, Quest quest, String feedback) {
        DialogDelivery.open(player, QuestDialogs.quest(quest, feedback));
    }

    private static String version() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("kompass4mc")
                .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("unbekannt");
    }

    private int resume(ServerPlayer player, boolean hint) {
        if (learning != null && learning.hasActive(player)) return learning.resume(player, hint);
        Quest quest = QuestCatalog.byId(progress(player).activeQuest == null ? "" : progress(player).activeQuest).orElse(null);
        if (quest == null) {
            tell(player, "Du hast gerade keine offene Aufgabe. Suche ein Quest-Versteck.");
            return 0;
        }
        show(player, quest, hint ? "Hinweis: " + quest.hint() : null);
        return 1;
    }

    private int answer(ServerPlayer player, String id, String response) {
        WorldState.Progress progress = progress(player);
        Quest quest = QuestCatalog.byId(id).orElse(null);
        if (quest == null || !id.equals(progress.activeQuest) || !progress.discovered.contains(id)) {
            tell(player, "Diese Aufgabe ist gerade nicht geöffnet. Nutze /kompass weiter.");
            return 0;
        }
        if (progress.completed.contains(id)) {
            tell(player, "Diese Aufgabe hast du bereits gelöst.");
            return 0;
        }
        if (!quest.matchesAnswer(response)) {
            show(player, quest, "Noch nicht ganz. Probiere es erneut! Hinweis: " + quest.hint());
            return 0;
        }
        progress.completed.add(id);
        progress.activeQuest = null;
        if (!save()) {
            progress.completed.remove(id);
            progress.activeQuest = id;
            tell(player, "Deine Antwort stimmt. Der Spielstand konnte gerade nicht gespeichert werden. Bitte versuche es erneut.");
            return 0;
        }
        ItemStack reward = new ItemStack(Items.EMERALD, quest.rewardEmeralds());
        if (!player.getInventory().add(reward)) {
            player.spawnAtLocation(player.level(), reward);
        }
        tell(player, "Richtig! " + quest.explanation() + "\nBelohnung: " + quest.rewardEmeralds()
                + " Smaragde. Gelöst: " + progress.completed.size() + "/" + QuestCatalog.all().size() + ".");
        if (progress.completed.size() == QuestCatalog.all().size()) {
            tell(player, "Du hast alle Kompass-Quests gemeistert. Dein Abenteuer ist geschafft!");
        }
        return 1;
    }

    private int journal(ServerPlayer player) {
        if (learning != null) learning.journal(player);
        if (village != null && village.hasVillage()) {
            village.journal(player);
        }
        WorldState.Progress progress = progress(player);
        if (!progress.discovered.isEmpty()) tell(player, "Zusätzliche Übungsfässer: " + progress.completed.size()
                + "/" + QuestCatalog.all().size() + " Aufgaben gelöst.");
        for (Quest quest : QuestCatalog.all()) {
            if (progress.discovered.contains(quest.id())) {
                tell(player, (progress.completed.contains(quest.id()) ? "✓ " : "Offen: ") + quest.title());
            }
        }
        if (progress.activeQuest != null) {
            tell(player, "Mit /kompass weiter öffnest du deine letzte Aufgabe.");
        }
        return 1;
    }

    private int search(ServerPlayer player) {
        if (learning != null && learning.find(player) > 0) return 1;
        if (village != null && village.hasVillage()) {
            return village.journal(player);
        }
        return searchExercises(player);
    }

    private int searchExercises(ServerPlayer player) {
        pruneMissingHidingPlaces(player.level());
        WorldState.Progress progress = progress(player);
        WorldState.HidingPlace nearest = null;
        double distance = Double.MAX_VALUE;
        String dimension = player.level().dimension().identifier().toString();
        for (WorldState.HidingPlace place : state.hidingPlaces().values()) {
            if (!place.dimension().equals(dimension) || progress.completed.contains(place.questId())) {
                continue;
            }
            double squared = player.blockPosition().distSqr(new BlockPos(place.x(), place.y(), place.z()));
            if (squared < distance) {
                distance = squared;
                nearest = place;
            }
        }
        if (nearest == null) {
            tell(player, "Hier gibt es kein weiteres offenes Versteck. Die Spielleitung kann /kompass abenteuer nutzen.");
            return 0;
        }
        Quest quest = QuestCatalog.byId(nearest.questId()).orElseThrow();
        tell(player, "Eine Spur: " + quest.locationHint() + "\nSuche bei X " + nearest.x() + ", Z " + nearest.z()
                + " (etwa " + Math.round(Math.sqrt(distance)) + " Blöcke entfernt).");
        return 1;
    }

    private int bind(CommandSourceStack source, String id, BlockPos pos) {
        if (QuestCatalog.byId(id).isEmpty()) {
            source.sendFailure(Component.literal("Unbekannte Quest. Nutze die Tab-Vervollständigung."));
            return 0;
        }
        var level = source.getLevel();
        if (!isQuestContainer(level, pos)) {
            source.sendFailure(Component.literal("An dieser Position muss ein Fass oder eine Truhe stehen."));
            return 0;
        }
        String dimension = level.dimension().identifier().toString();
        String key = WorldState.positionKey(dimension, pos.getX(), pos.getY(), pos.getZ());
        WorldState.HidingPlace previous = state.hidingPlaces().put(key,
                new WorldState.HidingPlace(dimension, pos.getX(), pos.getY(), pos.getZ(), id));
        if (!save()) {
            if (previous == null) {
                state.hidingPlaces().remove(key);
            } else {
                state.hidingPlaces().put(key, previous);
            }
            source.sendFailure(Component.literal("Das Versteck konnte nicht gespeichert werden."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Quest-Versteck eingerichtet: " + id + " bei " + pos.toShortString()), true);
        return 1;
    }

    private int unbind(CommandSourceStack source, BlockPos pos) {
        String dimension = source.getLevel().dimension().identifier().toString();
        String key = WorldState.positionKey(dimension, pos.getX(), pos.getY(), pos.getZ());
        WorldState.HidingPlace removed = state.hidingPlaces().remove(key);
        if (removed == null) {
            source.sendFailure(Component.literal("Hier ist kein Quest-Versteck registriert."));
            return 0;
        }
        if (!save()) {
            state.hidingPlaces().put(key, removed);
            source.sendFailure(Component.literal("Die Quest-Bindung konnte nicht dauerhaft entfernt werden."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Quest-Bindung entfernt. Der Block bleibt stehen."), true);
        return 1;
    }

    private int adventure(ServerPlayer player) {
        var level = player.level();
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            tell(player, "Lege die Quest-Runde in der Oberwelt an.");
            return 0;
        }
        pruneMissingHidingPlaces(level);
        List<int[]> offsets = List.of(new int[]{12, 4}, new int[]{20, 12}, new int[]{26, 24},
                new int[]{12, 30}, new int[]{-4, 34}, new int[]{-18, 24}, new int[]{-30, 12},
                new int[]{-30, -6}, new int[]{-18, -22}, new int[]{-2, -30}, new int[]{14, -26}, new int[]{28, -12});
        int placed = 0;
        Map<BlockPos, BlockState> originalBlocks = new LinkedHashMap<>();
        for (int index = 0; index < QuestCatalog.all().size(); index++) {
            Quest quest = QuestCatalog.all().get(index);
            // A second invocation adds missing quests, rather than duplicating existing ones.
            if (state.hidingPlaces().values().stream().anyMatch(place -> place.questId().equals(quest.id()))) {
                continue;
            }
            int[] offset = offsets.get(index);
            int x = player.blockPosition().getX() + offset[0];
            int z = player.blockPosition().getZ() + offset[1];
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!level.getBlockState(pos).isAir() || !level.getFluidState(pos.below()).isEmpty()
                    || level.getBlockState(pos.below()).isAir() || !level.getWorldBorder().isWithinBounds(pos)) {
                continue;
            }
            BlockState original = level.getBlockState(pos);
            if (level.setBlockAndUpdate(pos, Blocks.BARREL.defaultBlockState())) {
                originalBlocks.put(pos, original);
                String dimension = level.dimension().identifier().toString();
                state.hidingPlaces().put(WorldState.positionKey(dimension, x, pos.getY(), z),
                        new WorldState.HidingPlace(dimension, x, pos.getY(), z, quest.id()));
                placed++;
            }
        }
        if (!save()) {
            for (var entry : originalBlocks.entrySet()) {
                state.hidingPlaces().remove(key(player, entry.getKey()));
                level.setBlockAndUpdate(entry.getKey(), entry.getValue());
            }
            tell(player, "Die Quest-Runde konnte nicht gespeichert werden. Neue Fässer wurden zurückgenommen.");
            return 0;
        }
        tell(player, placed + " neue Quest-Fässer verteilt. Öffne sie mit Rechtsklick. /kompass suchen gibt eine Spur.");
        return placed;
    }
}
