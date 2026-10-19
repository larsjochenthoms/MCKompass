package de.kompass4mc;

import de.kompass4mc.learning.Difficulty;
import de.kompass4mc.learning.Interaction;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.learning.LearningTask;
import de.kompass4mc.progress.LearningProgress;
import de.kompass4mc.quest.Subject;
import de.kompass4mc.reward.RewardCatalog;
import de.kompass4mc.reward.RewardSpec;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Conversations bind every action to the player, nearby resident and current task. */
public final class LearningService {
    private static final Logger LOGGER = LoggerFactory.getLogger("kompass4mc");
    private final MinecraftServer server;
    private final WorldState state;
    private final RoamingNpcs residents;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public LearningService(MinecraftServer server, WorldState state, RoamingNpcs residents) {
        this.server = server;
        this.state = state;
        this.residents = residents;
        residents.busyWhen(this::busy);
    }

    public void talk(ServerPlayer player, Entity npc) {
        if (!RoamingNpcs.isNpc(npc) || player.isSpectator() || npc.level() != player.level()
                || npc.distanceToSqr(player) > 36) return;
        LearningProgress progress = progress(player);
        LearningTask active = LearningCatalog.byId(progress.activeTaskId).orElse(null);
        Session session = new Session(npc, active, server.overworld().getGameTime());
        sessions.put(player.getUUID(), session);
        if (active == null) showMenu(player, session);
        else showTask(player, session, null, false);
    }

    public int resume(ServerPlayer player, boolean hint) {
        if (!player.isAlive() || player.isSpectator()) return 0;
        Entity npc = residents.nearest(player).filter(candidate -> candidate.distanceToSqr(player) <= 36).orElse(null);
        if (npc == null) {
            tell(player, "Sprich einen Lernreisenden in deiner Nähe an. Dein Auftrag bleibt gespeichert.");
            find(player);
            return 0;
        }
        talk(player, npc);
        Session session = sessions.get(player.getUUID());
        if (hint && session != null && session.task != null) {
            return action(player, session.token, "hinweis", "");
        }
        return 1;
    }

    public boolean hasActive(ServerPlayer player) {
        return LearningCatalog.byId(progress(player).activeTaskId).isPresent();
    }

    public int action(ServerPlayer player, String token, String action, String response) {
        Session session = sessions.get(player.getUUID());
        if (session == null || !session.token.equals(token) || !valid(player, session)) {
            tell(player, "Dieses Gespräch ist nicht mehr geöffnet. Sprich den Lernreisenden erneut an.");
            return 0;
        }
        session.lastAction = server.overworld().getGameTime();
        if (action.equals("menu")) {
            LearningProgress before = progress(player).copy();
            progress(player).activeTaskId = null;
            if (!save(player, before)) return 0;
            session.task = null;
            rotate(session);
            showMenu(player, session);
            return 1;
        }
        for (Subject subject : Subject.values()) {
            for (Difficulty difficulty : Difficulty.values()) {
                String choice = subject.name().toLowerCase(java.util.Locale.ROOT) + "_"
                        + difficulty.name().toLowerCase(java.util.Locale.ROOT);
                if (action.equals(choice)) {
                    if (session.task != null) return 0;
                    return assign(player, session, subject, difficulty);
                }
            }
        }
        LearningTask task = session.task;
        if (task == null || session.finished || !task.id().equals(progress(player).activeTaskId)) return 0;
        LearningProgress before = progress(player).copy();
        long now = System.currentTimeMillis();
        if (action.equals("hinweis")) {
            progress(player).recordHint(task, now);
            if (!save(player, before)) return 0;
            session.hintUsed = true;
            rotateToken(session);
            showTask(player, session, null, true);
            return 1;
        }
        if (action.equals("stapel") && task.interaction() == Interaction.SHOW_STACK) {
            ItemStack held = player.getMainHandItem();
            String item = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            if (held.isEmpty() || !item.equals(task.itemId())) {
                progress(player).recordProceduralIssue(task, "Haupthand: " + (held.isEmpty() ? "leer" : item), now);
                if (!save(player, before)) return 0;
                rotateToken(session);
                showTask(player, session, "Zeige bitte den genannten Gegenstand in deiner Haupthand. Die Menge habe ich noch nicht geprüft.", false);
                return 1;
            }
            response = Integer.toString(held.getCount());
        } else if (!action.equals("antwort") || task.interaction() != Interaction.ANSWER) {
            return 0;
        }
        if (response == null || response.isBlank()) {
            showTask(player, session, "Trage zuerst eine Antwort ein.", false);
            return 0;
        }
        LearningProgress progress = progress(player);
        boolean correct = progress.recordAnswer(task, response, now, session.hintUsed).correct();
        RewardSpec reward = correct && !progress.rewarded.contains(task.id()) ? RewardCatalog.forTask(task) : null;
        List<ItemStack> gifts;
        try {
            // Resolve data-driven enchantments before saving the reward marker.
            gifts = reward == null ? List.of() : RewardItems.create(reward, server.registryAccess());
        } catch (RuntimeException exception) {
            state.learningProgresses().put(player.getStringUUID(), before);
            LOGGER.error("Geschenk für Auftrag {} konnte nicht erstellt werden", task.id(), exception);
            tell(player, "Dein Geschenk konnte gerade nicht vorbereitet werden. Bitte versuche es erneut; der Auftrag wurde noch nicht als belohnt gespeichert.");
            return 0;
        }
        if (reward != null) progress.markRewarded(task.id());
        if (correct) progress.activeTaskId = null;
        if (!save(player, before)) return 0;
        rotateToken(session);
        if (correct) {
            session.finished = true;
            if (!gifts.isEmpty()) {
                boolean dropped = false;
                for (ItemStack thanks : gifts) {
                    if (!player.getInventory().add(thanks)) {
                        player.spawnAtLocation(player.level(), thanks);
                        dropped = true;
                    }
                }
                player.containerMenu.broadcastChanges();
                if (dropped) tell(player, "Dein Inventar war voll. Ein Teil deines Geschenks liegt neben dir auf dem Boden.");
            }
            DialogDelivery.open(player, LearningDialogs.completed(session.name, session.token, task, reward));
        } else {
            showTask(player, session, "Das passt noch nicht. Schau noch einmal auf die Angaben. Wenn du möchtest, kannst du einen Tipp öffnen.", false);
        }
        return 1;
    }

    private int assign(ServerPlayer player, Session session, Subject subject, Difficulty difficulty) {
        LearningProgress progress = progress(player);
        List<LearningTask> pool = LearningCatalog.all().stream().filter(task -> task.subject() == subject && task.difficulty() == difficulty
                && !progress.completed.contains(task.id())).toList();
        if (pool.isEmpty()) pool = LearningCatalog.all().stream().filter(task -> task.subject() == subject && task.difficulty() == difficulty).toList();
        List<LearningTask> own = pool.stream().filter(task -> task.npcName().equals(session.name)).toList();
        if (!own.isEmpty()) pool = own;
        LearningTask chosen = pool.get(player.level().getRandom().nextInt(pool.size()));
        LearningProgress before = progress.copy();
        progress.activeTaskId = chosen.id();
        if (!save(player, before)) return 0;
        session.task = chosen;
        rotate(session);
        showTask(player, session, null, false);
        return 1;
    }

    public void journal(ServerPlayer player) {
        LearningProgress progress = progress(player);
        tell(player, "Lernreise: " + progress.completed.size() + "/" + LearningCatalog.all().size()
                + " Aufträge gelöst. " + LearningCatalog.byId(progress.activeTaskId)
                .map(task -> "Offen: " + task.title() + ". Sprich einen Lernreisenden an.")
                .orElse("Mila, Jaro und Lio haben neue Aufträge für dich."));
    }

    public int find(ServerPlayer player) {
        Entity npc = residents.nearest(player).filter(candidate -> candidate.distanceToSqr(player) <= 128 * 128).orElse(null);
        if (npc == null) return 0;
        int dx = npc.blockPosition().getX() - player.blockPosition().getX();
        int dz = npc.blockPosition().getZ() - player.blockPosition().getZ();
        String eastWest = Math.abs(dx) < 3 ? "" : Math.abs(dx) + " Blöcke " + (dx > 0 ? "nach Osten" : "nach Westen");
        String northSouth = Math.abs(dz) < 3 ? "" : Math.abs(dz) + " Blöcke " + (dz > 0 ? "nach Süden" : "nach Norden");
        String direction = eastWest + (!eastWest.isEmpty() && !northSouth.isEmpty() ? " und " : "") + northSouth;
        tell(player, RoamingNpcs.residentName(npc) + (direction.isEmpty() ? " steht direkt bei dir."
                : " wartet ungefähr " + direction + ".") + " Position: "
                + npc.blockPosition().getX() + " / " + npc.blockPosition().getY() + " / " + npc.blockPosition().getZ());
        return 1;
    }

    private LearningProgress progress(ServerPlayer player) {
        return state.learningProgress(player.getStringUUID(), player.getName().getString());
    }

    private boolean save(ServerPlayer player, LearningProgress before) {
        try {
            state.save();
            return true;
        } catch (IOException exception) {
            state.learningProgresses().put(player.getStringUUID(), before);
            LOGGER.error("Lernnotizen konnten nicht gespeichert werden", exception);
            tell(player, "Dein Fortschritt konnte nicht gespeichert werden. Bitte versuche es erneut; es wurde keine Belohnung vergeben.");
            return false;
        }
    }

    private void showTask(ServerPlayer player, Session session, String feedback, boolean hint) {
        DialogDelivery.open(player, LearningDialogs.task(session.name, session.token, session.task,
                progress(player).rewarded.contains(session.task.id()), feedback, hint));
    }

    private void showMenu(ServerPlayer player, Session session) {
        DialogDelivery.open(player, LearningDialogs.menu(session.name, session.token));
    }

    private static void rotateToken(Session session) { session.token = UUID.randomUUID().toString(); }
    private static void rotate(Session session) {
        rotateToken(session);
        session.finished = false;
        session.hintUsed = false;
    }

    private static boolean valid(ServerPlayer player, Session session) {
        return player.isAlive() && !player.isSpectator() && session.npc.isAlive() && !session.npc.isRemoved()
                && session.npc.level() == player.level() && session.npc.distanceToSqr(player) <= 36;
    }

    private boolean busy(UUID npc) {
        return sessions.entrySet().stream().anyMatch(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();
            long sinceAction = server.overworld().getGameTime() - session.lastAction;
            return player != null && session.task != null && !session.finished
                    && sinceAction >= 0 && sinceAction < 5 * 60 * 20L
                    && session.npc.getUUID().equals(npc) && session.npc.isAlive()
                    && session.npc.level() == player.level() && session.npc.distanceToSqr(player) <= 1024;
        });
    }

    public void tick() {
        sessions.entrySet().removeIf(entry -> server.getPlayerList().getPlayer(entry.getKey()) == null
                || entry.getValue().npc.isRemoved());
    }

    private static void tell(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("[Kompass] " + message));
    }

    private static final class Session {
        final Entity npc;
        final String name;
        String token = UUID.randomUUID().toString();
        LearningTask task;
        boolean finished;
        boolean hintUsed;
        long lastAction;

        Session(Entity npc, LearningTask task, long now) {
            this.npc = npc;
            this.name = RoamingNpcs.residentName(npc);
            this.task = task;
            this.lastAction = now;
        }
    }
}
