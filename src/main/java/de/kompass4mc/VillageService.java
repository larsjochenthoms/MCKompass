package de.kompass4mc;

import de.kompass4mc.village.BuildPattern;
import de.kompass4mc.village.GridPos;
import de.kompass4mc.village.Kind;
import de.kompass4mc.village.MissionJob;
import de.kompass4mc.village.VillageJobs;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Residents ask for work, inspect real blocks, and accept a deliberately packed delivery. */
public final class VillageService {
    private static final Logger LOGGER = LoggerFactory.getLogger("kompass4mc");
    private static VillageService active;
    private final MinecraftServer server;
    private final WorldState state;

    public VillageService(MinecraftServer server, WorldState state) {
        this.server = server;
        this.state = state;
        active = this;
    }

    public void close() {
        if (active == this) {
            active = null;
        }
    }

    public static void onBlockPlaced(ServerPlayer player, BlockPos position) {
        VillageService service = active;
        if (service == null || service.server != player.level().getServer()) {
            return;
        }
        service.recordContribution(player, position);
    }

    private void recordContribution(ServerPlayer player, BlockPos position) {
        VillageState village = state.village();
        if (!sameDimension(player, village)) {
            return;
        }
        for (MissionJob job : VillageJobs.all()) {
            if (job.kind() == Kind.DELIVERY) {
                continue;
            }
            BlockPos origin = VillageBuilder.jobOrigin(village, job.kind());
            if (position.getY() != origin.getY()) {
                continue;
            }
            GridPos local = new GridPos(position.getX() - origin.getX(), position.getZ() - origin.getZ());
            String expected = VillageJobs.pattern(job.kind()).required().get(local);
            if (expected == null || !expected.equals(blockId(player.level().getBlockState(position)))) {
                continue;
            }
            VillageState.Project project = village.projects.get(job.id());
            String uuid = player.getStringUUID();
            if (project != null && project.contribute(uuid) && !save()) {
                project.contributors.remove(uuid);
                tell(player, "Dein Baubeitrag konnte gerade nicht gespeichert werden. Bitte setze noch einen passenden Block.");
            }
        }
    }

    public boolean isResident(Entity entity) {
        return entity.entityTags().contains(VillageBuilder.NPC_TAG);
    }

    public void talk(ServerPlayer player, Entity resident) {
        VillageState village = state.village();
        if (!sameDimension(player, village)) {
            return;
        }
        village.residents.entrySet().stream()
                .filter(entry -> entry.getValue().equals(resident.getStringUUID()))
                .filter(entry -> village.projects.containsKey(entry.getKey()))
                .findFirst().flatMap(entry -> VillageJobs.byId(entry.getKey()))
                .ifPresent(job -> show(player, job, null));
    }

    public int action(ServerPlayer player, String jobId, String action) {
        MissionJob job = VillageJobs.byId(jobId).orElse(null);
        VillageState village = state.village();
        if (job == null || !sameDimension(player, village)) {
            return 0;
        }
        String residentId = village.residents.get(jobId);
        Entity resident = residentId == null ? null : player.level().getEntity(UUID.fromString(residentId));
        if (resident == null || !isResident(resident) || player.distanceToSqr(resident) > 36 || player.isSpectator()) {
            tell(player, "Komm zum Bewohner zurück, damit ihr miteinander sprechen könnt.");
            return 0;
        }
        VillageState.Project project = village.projects.get(jobId);
        if (project == null) {
            return 0;
        }
        return switch (action) {
            case "annehmen" -> accept(player, job, project);
            case "pruefen" -> inspect(player, job, project);
            case "tipp" -> {
                show(player, job, project.accepted.contains(player.getStringUUID()) ? job.hint() : null);
                yield 1;
            }
            case "danke" -> claim(player, job, project);
            default -> 0;
        };
    }

    private int accept(ServerPlayer player, MissionJob job, VillageState.Project project) {
        VillageState before = state.village().copy();
        if (!project.accept(player.getStringUUID())) {
            show(player, job, null);
            return 0;
        }
        boolean supplies = job.kind() != Kind.DELIVERY && !project.suppliesIssued;
        if (supplies) {
            project.suppliesIssued = true;
        }
        if (!save()) {
            state.village(before);
            tell(player, "Der Auftrag konnte gerade nicht gespeichert werden. Sprich mich gleich noch einmal an.");
            return 0;
        }
        if (supplies) {
            give(player, new ItemStack(job.kind() == Kind.GARDEN ? Items.OAK_FENCE : Items.OAK_PLANKS,
                    job.kind() == Kind.GARDEN ? 32 : 12));
        }
        show(player, job, supplies ? "Hier ist Baumaterial für unser gemeinsames Projekt. Überschüssiges Material darfst du behalten." : null);
        return 1;
    }

    private int inspect(ServerPlayer player, MissionJob job, VillageState.Project project) {
        String uuid = player.getStringUUID();
        if (project.completed || !project.accepted.contains(uuid)) {
            show(player, job, null);
            return 0;
        }
        ItemStack deliveryBefore = ItemStack.EMPTY;
        if (job.kind() == Kind.DELIVERY) {
            ItemStack held = player.getMainHandItem();
            if (held.getItem() != Items.APPLE || held.getCount() != VillageJobs.DELIVERY_COUNT) {
                show(player, job, "Diese Lieferung passt noch nicht zum Zettel. Packe genau die bestellte Menge Äpfel in deine Haupthand.");
                return 0;
            }
            deliveryBefore = held.copy();
        } else {
            BuildPattern pattern = VillageJobs.pattern(job.kind());
            BlockPos origin = VillageBuilder.jobOrigin(state.village(), job.kind());
            Map<GridPos, String> blocks = new LinkedHashMap<>();
            var positions = new java.util.LinkedHashSet<>(pattern.required().keySet());
            positions.addAll(pattern.forbidden());
            for (GridPos position : positions) {
                BlockPos worldPosition = origin.offset(position.x(), 0, position.z());
                if (!player.level().hasChunkAt(worldPosition)) {
                    show(player, job, "Lass uns erst zur Baustelle gehen. Ich kann sie von hier aus gerade nicht prüfen.");
                    return 0;
                }
                blocks.put(position, blockId(player.level().getBlockState(worldPosition)));
            }
            BuildPattern.Inspection inspection = pattern.inspect(blocks);
            if (!inspection.complete()) {
                show(player, job, "Ich habe nachgesehen: Noch " + inspection.missing().size()
                        + " Stellen sind frei, an " + inspection.wrongMaterial().size()
                        + " Stellen passt das Material nicht und " + inspection.extra().size()
                        + " Bauteile stehen außerhalb des Plans. " + job.hint());
                return 0;
            }
            if (!project.contributors.contains(uuid)) {
                show(player, job, "Das Bauwerk sieht gut aus. Für dein persönliches Dankeschön musst du selbst mitbauen: "
                        + "Setze nach der Auftragsannahme mindestens ein passendes Bauteil.");
                return 0;
            }
        }

        VillageState before = state.village().copy();
        Map<BlockPos, BlockState> previousBlocks = new LinkedHashMap<>();
        if (job.kind() == Kind.DELIVERY) {
            project.contribute(uuid);
            player.getMainHandItem().shrink(VillageJobs.DELIVERY_COUNT);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        project.completed = true;
        decorateCompletion(player, job.kind(), previousBlocks);
        if (!save()) {
            state.village(before);
            if (!deliveryBefore.isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, deliveryBefore);
                player.containerMenu.broadcastChanges();
            }
            previousBlocks.forEach((pos, block) -> player.level().setBlockAndUpdate(pos, block));
            show(player, job, "Der Abschluss konnte gerade nicht gespeichert werden. Bitte versuche es noch einmal.");
            return 0;
        }
        show(player, job, null);
        return 1;
    }

    private void decorateCompletion(ServerPlayer player, Kind kind, Map<BlockPos, BlockState> previous) {
        VillageState village = state.village();
        if (kind == Kind.GARDEN) {
            BlockPos origin = VillageBuilder.jobOrigin(village, kind);
            for (int x = 2; x <= 6; x += 2) {
                for (int z = 2; z <= 4; z += 2) {
                    BlockPos pos = origin.offset(x, 0, z);
                    if (player.level().getBlockState(pos).isAir()) {
                        previous.put(pos, player.level().getBlockState(pos));
                        player.level().setBlockAndUpdate(pos, Blocks.DANDELION.defaultBlockState());
                    }
                }
            }
        } else if (kind == Kind.DELIVERY) {
            BlockPos pos = new BlockPos(village.x + 17, village.floorY + 1, village.z + 14);
            BlockState block = player.level().getBlockState(pos);
            if (block.is(Blocks.CAMPFIRE)) {
                previous.put(pos, block);
                player.level().setBlockAndUpdate(pos, block.setValue(CampfireBlock.LIT, true));
            }
        }
    }

    private int claim(ServerPlayer player, MissionJob job, VillageState.Project project) {
        String uuid = player.getStringUUID();
        if (!project.claim(uuid)) {
            show(player, job, null);
            return 0;
        }
        if (!save()) {
            project.rewardClaimed.remove(uuid);
            show(player, job, "Ich konnte dein Dankeschön gerade nicht vermerken. Versuche es bitte noch einmal.");
            return 0;
        }
        give(player, new ItemStack(Items.EMERALD, job.emeraldReward()));
        show(player, job, "Danke für deine Hilfe! Du erhältst " + job.emeraldReward() + " Smaragde.");
        return 1;
    }

    private void show(ServerPlayer player, MissionJob job, String feedback) {
        VillageState.Project project = state.village().projects.get(job.id());
        DialogDelivery.open(player, VillageDialogs.conversation(job, project, player.getStringUUID(), feedback));
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static boolean sameDimension(ServerPlayer player, VillageState village) {
        return village != null && village.dimension.equals(player.level().dimension().identifier().toString());
    }

    private boolean save() {
        try {
            state.save();
            return true;
        } catch (IOException exception) {
            LOGGER.error("Dorfprojekt konnte nicht gespeichert werden", exception);
            return false;
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.spawnAtLocation(player.level(), stack);
        }
    }

    private static void tell(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal("[Dorf] " + text));
    }

    public int create(ServerPlayer player) {
        if (state.village() != null) {
            return journal(player);
        }
        try {
            VillageState village = VillageBuilder.build(player, state);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                    "tp " + player.getStringUUID() + " " + (village.x + 12.5) + " "
                            + (village.floorY + 1) + " " + (village.z + 12.5));
            tell(player, "Willkommen auf dem Dorfplatz! Sprich Mila, Jaro und Lio mit Rechtsklick an. "
                    + "Die Bewohner brauchen Hilfe beim Bauen und Versorgen.");
            return 1;
        } catch (IOException | IllegalStateException exception) {
            LOGGER.error("Dorf konnte nicht angelegt werden", exception);
            tell(player, "Das Dorf konnte hier nicht angelegt werden: " + exception.getMessage());
            return 0;
        }
    }

    public int journal(ServerPlayer player) {
        VillageState village = state.village();
        if (village == null) {
            tell(player, "Hier gibt es noch keinen Dorfplatz. Die Spielleitung kann ihn mit /kompass dorf anlegen.");
            return 0;
        }
        tell(player, "Dorfplatz: X " + (village.x + 12) + ", Y " + (village.floorY + 1) + ", Z " + (village.z + 9));
        for (MissionJob job : VillageJobs.all()) {
            VillageState.Project project = village.projects.get(job.id());
            if (project == null) {
                continue;
            }
            tell(player, job.residentName() + ": " + job.title() + " — "
                    + (project.completed ? "fertig" : project.accepted.contains(player.getStringUUID()) ? "du hilfst mit" : "noch offen"));
        }
        return 1;
    }

    public boolean hasVillage() {
        return state.village() != null;
    }

    public boolean protectsFoundation(ServerPlayer player, BlockPos position) {
        return sameDimension(player, state.village()) && VillageBuilder.withinFoundation(state.village(), position);
    }
}
