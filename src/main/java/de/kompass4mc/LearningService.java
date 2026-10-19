package de.kompass4mc;

import de.kompass4mc.learning.BuildChallenges;
import de.kompass4mc.learning.ChallengeRole;
import de.kompass4mc.learning.Interaction;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.learning.LearningTask;
import de.kompass4mc.progress.ChallengeOffer;
import de.kompass4mc.progress.LearningProgress;
import de.kompass4mc.progress.LeitnerDeck;
import de.kompass4mc.reward.RewardCatalog;
import de.kompass4mc.reward.RewardItem;
import de.kompass4mc.reward.RewardSpec;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Per-player offers survive NPC replacement; short-lived session tokens protect every action. */
public final class LearningService {
    private static final Logger LOGGER = LoggerFactory.getLogger("kompass4mc");
    private final MinecraftServer server;
    private final WorldState state;
    private final RoamingNpcs residents;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public LearningService(MinecraftServer server, WorldState state, RoamingNpcs residents) {
        this.server = server; this.state = state; this.residents = residents;
        residents.busyWhen(this::busy); residents.conversationWhen(this::conversationPartner);
    }

    public void talk(ServerPlayer player, Entity npc) {
        if (!RoamingNpcs.isNpc(npc) || !player.isAlive() || player.isSpectator() || npc.level() != player.level()
                || npc.distanceToSqr(player) > 36) return;
        String name = RoamingNpcs.residentName(npc);
        LearningProgress progress = progress(player);
        LearningProgress before = progress.copy();
        ChallengeOffer offer = progress.offers.get(name);
        long now = System.currentTimeMillis();
        if (offer == null || (now >= offer.restockAt)
                || LearningCatalog.byId(offer.taskId).isEmpty()) {
            String previous = offer == null ? null : offer.taskId;
            var role = ChallengeRole.named(name);
            LearningTask legacy = offer == null ? LearningCatalog.byId(progress.activeTaskId)
                    .filter(t -> ChallengeRole.forTask(t) == role && !progress.completed.contains(t.id())).orElse(null) : null;
            LearningTask chosen = legacy != null ? legacy : LeitnerDeck.draw(progress, role, now,
                    new java.util.Random(player.level().getRandom().nextLong()), previous).orElse(null);
            if (chosen == null) {
                sessions.remove(player.getUUID());
                DialogDelivery.open(player, LearningDialogs.waiting(name));
                return;
            }
            offer = new ChallengeOffer(chosen.id(), now + (chosen.interaction() == Interaction.BUILD
                    ? LeitnerDeck.BUILD_MILLIS : LeitnerDeck.OFFER_MILLIS));
            progress.offers.put(name, offer);
        }
        LearningTask task = LearningCatalog.byId(offer.taskId).orElseThrow();
        if (!offer.completed) progress.activeTaskId = task.id();
        if (!save(player, before)) return;
        Session session = new Session(npc, task, server.overworld().getGameTime(), offer.ticket);
        session.finished = offer.completed; session.hintUsed = offer.hintUsed;
        sessions.put(player.getUUID(), session);
        if (offer.completed) DialogDelivery.open(player, LearningDialogs.completed(name, session.token, task, null));
        else showTask(player, session, null, false);
    }

    public int resume(ServerPlayer player, boolean hint) {
        if (!player.isAlive() || player.isSpectator()) return 0;
        Entity npc = residents.nearest(player).filter(candidate -> candidate.distanceToSqr(player) <= 36).orElse(null);
        if (npc == null) { tell(player, "Sprich Mila, Jaro oder Lio in deiner Nähe an."); find(player); return 0; }
        talk(player, npc);
        Session session = sessions.get(player.getUUID());
        if (hint && session != null && !session.finished) return action(player, session.token, "hinweis", "");
        return 1;
    }

    public boolean hasActive(ServerPlayer player) {
        var progress = progress(player);
        return progress.offers.values().stream().anyMatch(o -> !o.completed)
                || LearningCatalog.byId(progress.activeTaskId).isPresent();
    }

    public int action(ServerPlayer player, String token, String action, String response) {
        Session session = sessions.get(player.getUUID());
        if (session == null || !session.token.equals(token) || !valid(player, session)) {
            tell(player, "Sprich mich bitte noch einmal an."); return 0;
        }
        var progress = progress(player);
        ChallengeOffer offer = progress.offers.get(session.name);
        if (offer == null || !offer.ticket.equals(session.offerTicket) || !offer.taskId.equals(session.task.id())
                || offer.completed || offer.rewarded || session.finished) return 0;
        session.lastAction = server.overworld().getGameTime();
        LearningProgress before = progress.copy();
        LearningTask task = session.task;
        long now = System.currentTimeMillis();
        if (action.equals("hinweis")) {
            progress.recordHint(task, now); offer.hintUsed = true;
            if (!save(player, before)) return 0;
            session.hintUsed = true; rotateToken(session); showTask(player, session, null, true); return 1;
        }
        if (action.equals("material") && task.interaction() == Interaction.BUILD) return materials(player, session, offer, before);
        if (action.equals("bauen") && task.interaction() == Interaction.BUILD) {
            var plan = BuildChallenges.byId(task.id()).orElseThrow();
            var result = ChallengeBuilding.inspect(player.level(), offer.site, plan);
            if (!result.available()) return procedural(player, session, before, "Markiere erst mit dem Bauplan eine freie Ecke in dieser Welt.");
            if (!result.complete() && result.wrong() == 0 && result.extra() == 0)
                return procedural(player, session, before, "Es fehlen noch " + result.missing() + " Blöcke. Bau in Ruhe weiter.");
            response = result.answer();
        } else if (action.equals("stapel") && task.interaction() == Interaction.SHOW_STACK) {
            ItemStack held = player.getMainHandItem();
            String item = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            if (held.isEmpty() || !item.equals(task.itemId()))
                return procedural(player, session, before, "Nimm bitte den passenden Gegenstand in die Haupthand.");
            response = Integer.toString(held.getCount());
        } else if (!action.equals("antwort") || task.interaction() != Interaction.ANSWER) return 0;
        if (response == null || response.isBlank()) { showTask(player, session, "Was meinst du?", false); return 0; }
        boolean correct = progress.recordChallengeAnswer(task, response, now, offer.hintUsed).correct();
        RewardSpec reward = correct ? RewardCatalog.forTask(task) : null;
        List<ItemStack> gifts;
        try { gifts = reward == null ? List.of() : RewardItems.create(reward, server.registryAccess()); }
        catch (RuntimeException error) {
            state.learningProgresses().put(player.getStringUUID(), before);
            LOGGER.error("Geschenk konnte nicht vorbereitet werden", error);
            tell(player, "Das Geschenk fehlt gerade. Versuch es bitte noch einmal."); return 0;
        }
        if (correct) {
            progress.markRewarded(task.id()); offer.completed = true; offer.rewarded = true;
            offer.restockAt = now + LeitnerDeck.RESTOCK_MILLIS;
            if (task.id().equals(progress.activeTaskId)) progress.activeTaskId = null;
        }
        if (!save(player, before)) return 0;
        rotateToken(session);
        if (correct) {
            session.finished = true; give(player, gifts);
            DialogDelivery.open(player, LearningDialogs.completed(session.name, session.token, task, reward));
        } else showTask(player, session, task.interaction() == Interaction.BUILD
                ? "Schau noch einmal auf Form und Material im Bauplan."
                : "Das passt noch nicht ganz. Schau noch einmal hin.", false);
        return 1;
    }

    private int materials(ServerPlayer player, Session session, ChallengeOffer offer, LearningProgress before) {
        if (offer.materialsIssued) return 0;
        var plan = BuildChallenges.byId(session.task.id()).orElseThrow();
        var supply = new RewardSpec(session.task.id() + "_supplies", "Baumaterial",
                List.of(new RewardItem(plan.material(), plan.supplies(), List.of())));
        List<ItemStack> gifts = new java.util.ArrayList<>(RewardItems.create(supply, server.registryAccess()));
        gifts.add(ChallengeBuilding.blueprint()); offer.materialsIssued = true;
        if (!save(player, before)) return 0;
        give(player, gifts); rotateToken(session);
        showTask(player, session, "Hier sind " + plan.supplies() + " Blöcke und dein Bauplan.", false); return 1;
    }

    private int procedural(ServerPlayer player, Session session, LearningProgress before, String text) {
        progress(player).recordProceduralIssue(session.task, text, System.currentTimeMillis());
        if (!save(player, before)) return 0;
        rotateToken(session); showTask(player, session, text, false); return 1;
    }

    /** Only chooses a site; ordinary vanilla placement supplies every building block. */
    public boolean useBlueprint(ServerPlayer player, ItemStack held, BlockHitResult hit) {
        if (!ChallengeBuilding.isBlueprint(held)) return false;
        var progress = progress(player);
        var offer = progress.offers.get("Mila");
        if (offer == null || offer.completed || !offer.materialsIssued) { tell(player, "Frag Mila nach einem neuen Bauplan."); return true; }
        var plan = BuildChallenges.byId(offer.taskId).orElse(null);
        if (plan == null) return true;
        var site = ChallengeBuilding.site(player, hit, plan);
        if (site.isEmpty()) { tell(player, "Hier ist es zu eng oder uneben. Suche eine freie, trockene Fläche."); return true; }
        var before = progress.copy(); offer.site = site.orElseThrow();
        if (!save(player, before)) return true;
        ChallengeBuilding.outline(player, offer.site, plan);
        tell(player, "Hier beginnt dein Bau. Die hellen Punkte zeigen den Rand. Zeig ihn Mila, wenn er fertig ist."); return true;
    }

    private static void give(ServerPlayer player, List<ItemStack> gifts) {
        boolean dropped = false;
        for (ItemStack item : gifts) if (!player.getInventory().add(item)) { player.spawnAtLocation(player.level(), item); dropped = true; }
        player.containerMenu.broadcastChanges();
        if (dropped) tell(player, "Ein Teil liegt neben dir auf dem Boden; dein Inventar ist voll.");
    }
    public void journal(ServerPlayer player) {
        var progress = progress(player);
        tell(player, "Deine Geschichten: " + progress.completed.size() + " Challenges geschafft.");
        progress.offers.forEach((name, offer) -> {
            if (!offer.completed) LearningCatalog.byId(offer.taskId).ifPresent(task -> tell(player, name + ": " + task.title()));
        });
    }
    public int find(ServerPlayer player) {
        Entity npc = residents.nearest(player).filter(candidate -> candidate.distanceToSqr(player) <= 128 * 128).orElse(null);
        if (npc == null) return 0;
        int dx = npc.blockPosition().getX() - player.blockPosition().getX();
        int dz = npc.blockPosition().getZ() - player.blockPosition().getZ();
        String eastWest = Math.abs(dx) < 3 ? "" : Math.abs(dx) + " Blöcke " + (dx > 0 ? "nach Osten" : "nach Westen");
        String northSouth = Math.abs(dz) < 3 ? "" : Math.abs(dz) + " Blöcke " + (dz > 0 ? "nach Süden" : "nach Norden");
        String direction = eastWest + (!eastWest.isEmpty() && !northSouth.isEmpty() ? " und " : "") + northSouth;
        tell(player, RoamingNpcs.residentName(npc) + (direction.isEmpty() ? " steht direkt bei dir." : " ist " + direction + ".")
                + " Position: " + npc.blockPosition().getX() + " / " + npc.blockPosition().getY() + " / " + npc.blockPosition().getZ());
        return 1;
    }
    private LearningProgress progress(ServerPlayer player) { return state.learningProgress(player.getStringUUID(), player.getName().getString()); }
    private boolean save(ServerPlayer player, LearningProgress before) {
        try { state.save(); return true; }
        catch (IOException error) {
            state.learningProgresses().put(player.getStringUUID(), before); LOGGER.error("Spielnotizen konnten nicht gespeichert werden", error);
            tell(player, "Das konnte ich gerade nicht speichern. Versuch es bitte noch einmal."); return false;
        }
    }
    private void showTask(ServerPlayer player, Session session, String feedback, boolean hint) {
        ChallengeOffer offer = progress(player).offers.get(session.name);
        DialogDelivery.open(player, LearningDialogs.task(session.name, session.token, session.task, feedback, hint,
                offer != null && offer.materialsIssued, offer != null && offer.site != null));
    }
    private static void rotateToken(Session session) { session.token = UUID.randomUUID().toString(); }
    private static boolean valid(ServerPlayer player, Session session) {
        return player.isAlive() && !player.isSpectator() && session.npc.isAlive() && !session.npc.isRemoved()
                && session.npc.level() == player.level() && session.npc.distanceToSqr(player) <= 36;
    }
    private boolean busy(UUID npc) {
        return sessions.entrySet().stream().anyMatch(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey()); Session session = entry.getValue();
            long since = server.overworld().getGameTime() - session.lastAction;
            return player != null && !session.finished && since >= 0 && since < 5 * 60 * 20L
                    && session.npc.getUUID().equals(npc) && session.npc.isAlive()
                    && session.npc.level() == player.level() && session.npc.distanceToSqr(player) <= 1024;
        });
    }
    public Optional<ServerPlayer> conversationPartner(UUID npc) {
        long now = server.overworld().getGameTime();
        for (var entry : sessions.entrySet()) {
            Session session = entry.getValue(); ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && session.npc.getUUID().equals(npc) && now >= session.lastAction
                    && now - session.lastAction < 60 * 20L && valid(player, session)) return Optional.of(player);
        }
        return Optional.empty();
    }
    public void tick() {
        sessions.entrySet().removeIf(entry -> server.getPlayerList().getPlayer(entry.getKey()) == null || entry.getValue().npc.isRemoved());
        if (server.overworld().getGameTime() % 20 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var offer = progress(player).offers.get("Mila");
            if (offer != null && !offer.completed && offer.site != null)
                BuildChallenges.byId(offer.taskId).ifPresent(plan -> ChallengeBuilding.outline(player, offer.site, plan));
        }
    }
    private static void tell(ServerPlayer player, String text) { player.sendSystemMessage(Component.literal("[Kompass] " + text)); }
    private static final class Session {
        final Entity npc;
        final String name;
        final String offerTicket;
        final LearningTask task;
        String token = UUID.randomUUID().toString();
        boolean finished;
        boolean hintUsed;
        long lastAction;
        Session(Entity npc, LearningTask task, long now, String ticket) {
            this.npc = npc; this.task = task; this.name = RoamingNpcs.residentName(npc); this.lastAction = now; this.offerTicket = ticket;
        }
    }
}
