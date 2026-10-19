package de.kompass4mc;

import de.kompass4mc.village.Kind;
import de.kompass4mc.village.MissionJob;
import de.kompass4mc.village.VillageJobs;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Places a small shared village in empty space, leaving the existing landscape intact. */
public final class VillageBuilder {
    public static final String NPC_TAG = "kompass4mc_resident";
    public static final int WIDTH = 25;
    public static final int DEPTH = 18;

    private VillageBuilder() {}

    public static VillageState build(ServerPlayer player, WorldState state) throws IOException {
        if (state.village() != null) {
            throw new IOException("Es gibt bereits ein Kompass-Dorf. Sprich dort mit den Bewohnern.");
        }
        ServerLevel level = player.level();
        if (level.dimension() != Level.OVERWORLD) {
            throw new IOException("Das Kompass-Dorf braucht einen Platz in der Oberwelt.");
        }

        VillageState village = new VillageState();
        village.dimension = level.dimension().identifier().toString();
        village.x = player.blockPosition().getX() + 4;
        village.z = player.blockPosition().getZ() - 8;
        village.floorY = findFloor(level, village);
        Map<BlockPos, BlockState> plan = plan(village);
        preflight(level, village);

        Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
        List<Villager> spawned = new ArrayList<>();
        try {
            for (var entry : plan.entrySet()) {
                BlockPos position = entry.getKey();
                BlockState original = level.getBlockState(position);
                // Recheck immediately before mutation, even after the complete preflight.
                if (!original.isAir()) {
                    throw new IOException("Am Bauplatz steht inzwischen etwas. Bitte wähle eine freie Wiese.");
                }
                originals.put(position, original);
                if (!level.setBlockAndUpdate(position, entry.getValue())) {
                    throw new IOException("Ein Teil des Dorfplatzes konnte nicht gesetzt werden.");
                }
            }
            fillSupplies(level, village);
            for (MissionJob job : VillageJobs.all()) {
                Villager resident = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
                if (resident == null) {
                    throw new IOException("Ein Bewohner konnte gerade nicht einziehen. Bitte versuche es erneut.");
                }
                BlockPos position = residentPosition(village, job.kind());
                resident.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
                resident.setYRot(180.0F);
                resident.setYHeadRot(180.0F);
                resident.setYBodyRot(180.0F);
                resident.setCustomName(Component.literal(job.residentName() + " · " + job.role()));
                resident.setCustomNameVisible(true);
                resident.setPersistenceRequired();
                resident.setNoAi(true);
                resident.setPermanentlyInvulnerable(true);
                resident.addTag(NPC_TAG);
                NpcAppearance.mark(resident);
                // Include failed additions in rollback as well.
                spawned.add(resident);
                if (!level.addFreshEntity(resident)) {
                    throw new IOException("Ein Bewohner konnte gerade nicht einziehen. Bitte versuche es erneut.");
                }
                village.residents.put(job.id(), resident.getUUID().toString());
                village.projects.put(job.id(), new VillageState.Project());
            }
            state.village(village);
            state.save();
            return village;
        } catch (IOException | RuntimeException failure) {
            state.village(null);
            spawned.forEach(Villager::discard);
            // Empty the newly created barrel before replacement, so rollback drops no supplies.
            BlockPos supplies = getOrigin(village).offset(21, 1, 13);
            if (originals.containsKey(supplies) && level.getBlockEntity(supplies) instanceof BarrelBlockEntity barrel) {
                barrel.clearContent();
            }
            List<Map.Entry<BlockPos, BlockState>> entries = new ArrayList<>(originals.entrySet());
            for (int index = entries.size() - 1; index >= 0; index--) {
                var entry = entries.get(index);
                level.setBlockAndUpdate(entry.getKey(), entry.getValue());
            }
            if (failure instanceof IOException problem) {
                throw problem;
            }
            throw new IOException("Das Dorf konnte nicht angelegt werden. Der Bauplatz wurde wiederhergestellt.", failure);
        }
    }

    public static BlockPos getOrigin(VillageState village) {
        return new BlockPos(village.x, village.floorY, village.z);
    }

    public static BlockPos jobOrigin(VillageState village, Kind kind) {
        return switch (kind) {
            case GARDEN -> getOrigin(village).offset(2, 1, 3);
            case BRIDGE -> getOrigin(village).offset(16, 1, 4);
            case DELIVERY -> getOrigin(village).offset(21, 1, 13);
        };
    }

    public static BlockPos residentPosition(VillageState village, Kind kind) {
        return switch (kind) {
            case GARDEN -> getOrigin(village).offset(6, 1, 1);
            case BRIDGE -> getOrigin(village).offset(18, 1, 1);
            case DELIVERY -> getOrigin(village).offset(18, 1, 13);
        };
    }

    public static boolean withinFoundation(VillageState village, BlockPos position) {
        // The ten water cells must remain editable so an accidentally submerged plank can be removed.
        if (position.getY() == village.floorY && position.getX() >= village.x + 16 && position.getX() <= village.x + 20
                && position.getZ() >= village.z + 4 && position.getZ() <= village.z + 5) {
            return false;
        }
        if (position.equals(getOrigin(village).offset(15, 1, 4))) {
            return true;
        }
        return position.getX() >= village.x && position.getX() < village.x + WIDTH
                && position.getZ() >= village.z && position.getZ() < village.z + DEPTH
                && position.getY() >= village.floorY - 1 && position.getY() <= village.floorY;
    }

    private static int findFloor(ServerLevel level, VillageState village) throws IOException {
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        int maximumClear = Integer.MIN_VALUE;
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                int worldX = village.x + x;
                int worldZ = village.z + z;
                BlockPos column = new BlockPos(worldX, level.getMinY(), worldZ);
                if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) {
                    throw new IOException("Der Dorfplatz liegt zu nah am Weltrand oder in einem ungeladenen Gebiet. "
                            + "Gehe auf eine freie Wiese und versuche es dort erneut.");
                }
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
                BlockPos below = new BlockPos(worldX, surface - 1, worldZ);
                if (level.isOutsideBuildHeight(below) || level.getBlockState(below).isAir()
                        || !level.getFluidState(below).isEmpty()) {
                    throw new IOException("Das Dorf braucht eine trockene Wiese. Suche einen Platz ohne Wasser.");
                }
                minimum = Math.min(minimum, surface);
                maximum = Math.max(maximum, surface);
                maximumClear = Math.max(maximumClear,
                        level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ));
            }
        }
        if (maximum - minimum > 3) {
            throw new IOException("Hier ist das Gelände zu uneben. Suche eine freie, möglichst flache Wiese.");
        }
        if (maximumClear - maximum > 3) {
            throw new IOException("Über diesem Bauplatz stehen hohe Pflanzen oder Baumkronen. Suche eine freie Wiese.");
        }
        // WORLD_SURFACE includes non-solid plants, so both layers stay above grass and flowers as well.
        return maximumClear + 1;
    }

    private static void preflight(ServerLevel level, VillageState village) throws IOException {
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                for (int y = -1; y <= 4; y++) {
                    BlockPos position = getOrigin(village).offset(x, y, z);
                    if (level.isOutsideBuildHeight(position) || !level.getWorldBorder().isWithinBounds(position)) {
                        throw new IOException("Hier reicht der Platz bis zur Baugrenze nicht aus. Suche eine tiefere Wiese.");
                    }
                    if (!level.getBlockState(position).isAir() || !level.getFluidState(position).isEmpty()) {
                        throw new IOException("Über diesem Bauplatz stehen schon Blöcke oder Pflanzen. "
                                + "Suche eine freie Wiese; vorhandene Landschaft wird nicht entfernt.");
                    }
                }
            }
        }
    }

    private static Map<BlockPos, BlockState> plan(VillageState village) {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        BlockPos origin = getOrigin(village);
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                blocks.put(origin.offset(x, -1, z), Blocks.STONE_BRICKS.defaultBlockState());
                boolean path = x >= 11 && x <= 13 || z == 11 || z == 12;
                blocks.put(origin.offset(x, 0, z), (path ? Blocks.DIRT_PATH : Blocks.GRASS_BLOCK).defaultBlockState());
            }
        }
        for (int x : new int[]{2, 10}) {
            for (int z : new int[]{3, 9}) {
                blocks.put(origin.offset(x, 0, z), Blocks.CONCRETE.white().defaultBlockState());
            }
        }
        for (int x = 16; x <= 20; x++) {
            for (int z = 4; z <= 5; z++) {
                blocks.put(origin.offset(x, 0, z), Blocks.WATER.defaultBlockState());
            }
        }
        for (int x : new int[]{15, 21}) {
            for (int z : new int[]{3, 6}) {
                blocks.put(origin.offset(x, 0, z), Blocks.CONCRETE.white().defaultBlockState());
            }
        }
        // A solid face at deck height lets the first plank be placed without temporary scaffolding.
        blocks.put(origin.offset(15, 1, 4), Blocks.STONE_BRICKS.defaultBlockState());
        blocks.put(origin.offset(21, 1, 13), Blocks.BARREL.defaultBlockState());
        blocks.put(origin.offset(17, 1, 14), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        return blocks;
    }

    private static void fillSupplies(ServerLevel level, VillageState village) throws IOException {
        BlockPos position = getOrigin(village).offset(21, 1, 13);
        if (!(level.getBlockEntity(position) instanceof BarrelBlockEntity barrel)) {
            throw new IOException("Die Sammelkiste konnte nicht eingerichtet werden.");
        }
        ItemStack namedBarrel = new ItemStack(Items.BARREL);
        namedBarrel.set(DataComponents.CUSTOM_NAME, Component.literal("Sammelkiste"));
        barrel.applyComponentsFromItemStack(namedBarrel);
        barrel.setItem(0, new ItemStack(Items.APPLE, 30));
        barrel.setItem(1, new ItemStack(Items.BREAD, 6));
        barrel.setChanged();
    }
}
