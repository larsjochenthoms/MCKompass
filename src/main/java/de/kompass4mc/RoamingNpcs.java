package de.kompass4mc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Small, temporary encounters in the existing landscape; never creates chunks or changes blocks. */
public final class RoamingNpcs implements AutoCloseable {
    public static final String NPC_TAG = "kompass4mc_learning_npc";
    private static final String BIRTH_TAG = NPC_TAG + "_born_";
    private static final String NAME_TAG = NPC_TAG + "_name_";
    private static final List<String> NAMES = List.of("Mila", "Jaro", "Lio");
    private static final int FIRST_DELAY_TICKS = 20 * 20;
    private static final int MIN_DISTANCE = 8;
    private static final int MAX_DISTANCE = 24;
    private static final int NEARBY_DISTANCE = 64;
    private static final int MAX_VERTICAL_DISTANCE = 6;
    private static RoamingNpcs active;
    private static boolean callbacksRegistered;

    private final MinecraftServer server;
    private final Settings settings;
    private final Map<UUID, Villager> loaded = new LinkedHashMap<>();
    private final Map<UUID, Long> nextAttempts = new HashMap<>();
    private Predicate<UUID> busy = ignored -> false;
    private boolean autoSpawn;
    private long ticks;

    public record Settings(boolean autoSpawn, int intervalSeconds, int maxLoaded, int lifetimeSeconds) {
        public Settings {
            if (intervalSeconds < 10 || intervalSeconds > 3600 || maxLoaded < 1 || maxLoaded > 32
                    || lifetimeSeconds < 30 || lifetimeSeconds > 86400) {
                throw new IllegalArgumentException("Ungültige Einstellungen für die Lernreisenden.");
            }
        }

        public static Settings defaults() {
            return new Settings(true, 120, 8, 600);
        }
    }

    public RoamingNpcs(MinecraftServer server) {
        this(server, Settings.defaults());
    }

    public RoamingNpcs(MinecraftServer server, Settings settings) {
        this.server = server;
        this.settings = settings;
        this.autoSpawn = settings.autoSpawn();
        registerCallbacks();
        active = this;
        // SERVER_STARTED happens after some saved entities have already loaded.
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                remember(entity);
            }
        }
    }

    private static void registerCallbacks() {
        if (callbacksRegistered) {
            return;
        }
        callbacksRegistered = true;
        // A single delegating listener also works after leaving and reopening a single-player world.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            RoamingNpcs current = active;
            if (current != null && level.getServer() == current.server) {
                current.remember(entity);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            RoamingNpcs current = active;
            if (current != null && level.getServer() == current.server) {
                current.loaded.remove(entity.getUUID());
            }
        });
    }

    private void remember(Entity entity) {
        if (isNpc(entity) && entity instanceof Villager npc && !npc.isRemoved()) {
            loaded.put(npc.getUUID(), npc);
        }
    }

    public static boolean isNpc(Entity entity) {
        return entity instanceof Villager && entity.entityTags().contains(NPC_TAG);
    }

    public static String residentName(Entity entity) {
        for (String name : NAMES) {
            if (entity.entityTags().contains(NAME_TAG + name)) {
                return name;
            }
        }
        return "Mila";
    }

    public void busyWhen(Predicate<UUID> predicate) {
        busy = predicate == null ? ignored -> false : predicate;
    }

    public Settings settings() {
        return new Settings(autoSpawn, settings.intervalSeconds(), settings.maxLoaded(), settings.lifetimeSeconds());
    }

    public void setAutoSpawn(boolean enabled) {
        if (enabled && !autoSpawn) {
            nextAttempts.clear();
        }
        autoSpawn = enabled;
    }

    /** Called from END_SERVER_TICK; maintenance still runs when automatic encounters are disabled. */
    public void tick() {
        ticks++;
        if (ticks % 20 != 0) {
            return;
        }
        long now = server.overworld().getGameTime();
        for (Villager npc : new ArrayList<>(loaded.values())) {
            if (npc.isRemoved() || !npc.isAlive()) {
                loaded.remove(npc.getUUID());
                continue;
            }
            long born = bornAt(npc, now);
            if (now >= born && now - born >= settings.lifetimeSeconds() * 20L && !busy.test(npc.getUUID())) {
                npc.discard();
                loaded.remove(npc.getUUID());
            }
        }
        // Keep no per-player scheduler entries after players leave the server or the overworld.
        nextAttempts.keySet().removeIf(uuid -> server.overworld().players().stream()
                .noneMatch(player -> player.getUUID().equals(uuid)));
        if (!autoSpawn) {
            return;
        }
        for (ServerPlayer player : server.overworld().players()) {
            if (!eligible(player)) {
                continue;
            }
            long next = nextAttempts.computeIfAbsent(player.getUUID(), ignored -> ticks + FIRST_DELAY_TICKS);
            if (ticks >= next) {
                spawnNear(player);
                nextAttempts.put(player.getUUID(), ticks + settings.intervalSeconds() * 20L);
            }
        }
    }

    private long bornAt(Villager npc, long now) {
        for (String tag : npc.entityTags()) {
            if (tag.startsWith(BIRTH_TAG)) {
                try {
                    long birth = Long.parseLong(tag.substring(BIRTH_TAG.length()));
                    if (birth >= 0 && birth <= now) {
                        return birth;
                    }
                } catch (NumberFormatException ignored) {
                    // An old or manually changed tag must not break world loading.
                }
            }
        }
        // Reset malformed tags or a clock moved backwards by restoring an older world backup.
        npc.entityTags().removeIf(tag -> tag.startsWith(BIRTH_TAG));
        npc.addTag(BIRTH_TAG + now);
        return now;
    }

    public Optional<Villager> nearest(ServerPlayer player) {
        Villager nearest = null;
        double distance = Double.MAX_VALUE;
        for (Villager npc : loaded.values()) {
            if (npc.isRemoved() || !npc.isAlive() || npc.level() != player.level()) {
                continue;
            }
            double candidate = npc.distanceToSqr(player);
            if (candidate < distance) {
                nearest = npc;
                distance = candidate;
            }
        }
        return Optional.ofNullable(nearest);
    }

    /** Manual encounters obey the same safety, distance and population limits as automatic encounters. */
    public boolean spawnNear(ServerPlayer player) {
        loaded.values().removeIf(npc -> npc.isRemoved() || !npc.isAlive());
        if (!eligible(player) || loaded.size() >= settings.maxLoaded()
                || nearest(player).filter(npc -> npc.distanceToSqr(player) <= NEARBY_DISTANCE * NEARBY_DISTANCE).isPresent()) {
            return false;
        }
        ServerLevel level = player.level();
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            int radius = MIN_DISTANCE + level.getRandom().nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
            int x = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * radius);
            BlockPos column = new BlockPos(x, player.blockPosition().getY(), z);
            if (!level.hasChunkAt(column)) {
                continue;
            }
            BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            double dx = x + 0.5 - player.getX();
            double dz = z + 0.5 - player.getZ();
            double horizontalDistance = dx * dx + dz * dz;
            if (horizontalDistance < MIN_DISTANCE * MIN_DISTANCE || horizontalDistance > MAX_DISTANCE * MAX_DISTANCE
                    || Math.abs(feet.getY() - player.getY()) > MAX_VERTICAL_DISTANCE || !safeSurface(level, feet)) {
                continue;
            }
            Villager npc = EntityTypes.VILLAGER.create(level, EntitySpawnReason.EVENT);
            if (npc == null) {
                return false;
            }
            npc.setPos(x + 0.5, feet.getY(), z + 0.5);
            if (!level.noCollision(npc)) {
                npc.discard();
                continue;
            }
            String name = NAMES.get(level.getRandom().nextInt(NAMES.size()));
            npc.setCustomName(Component.literal(name + " · auf Lernreise"));
            npc.setCustomNameVisible(true);
            npc.setPersistenceRequired();
            npc.setNoAi(true);
            npc.setCanPickUpLoot(false);
            npc.setPermanentlyInvulnerable(true);
            npc.setYRot((float) Math.toDegrees(Math.atan2(player.getZ() - npc.getZ(), player.getX() - npc.getX())) - 90);
            npc.setYHeadRot(npc.getYRot());
            npc.setYBodyRot(npc.getYRot());
            npc.addTag(NPC_TAG);
            npc.addTag(NAME_TAG + name);
            npc.addTag(BIRTH_TAG + server.overworld().getGameTime());
            if (level.addFreshEntity(npc)) {
                loaded.put(npc.getUUID(), npc);
                player.sendSystemMessage(Component.literal("[Kompass] " + name
                        + " ist in deiner Nähe auf Lernreise. Mit Rechtsklick kannst du ein Gespräch beginnen."));
                return true;
            }
            npc.discard();
            return false;
        }
        return false;
    }

    private static boolean eligible(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && player.level().dimension() == Level.OVERWORLD;
    }

    private static boolean safeSurface(ServerLevel level, BlockPos feet) {
        if (level.isOutsideBuildHeight(feet.below()) || level.isOutsideBuildHeight(feet.above())
                || !level.getWorldBorder().isWithinBounds(feet)
                || !level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()
                || !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) {
            return false;
        }
        BlockState support = level.getBlockState(feet.below());
        if (!support.isFaceSturdy(level, feet.below(), Direction.UP) || !level.getFluidState(feet.below()).isEmpty()
                || hazardous(support)) {
            return false;
        }
        // Do not appear beside lava, fire, damaging plants or an unloaded chunk boundary.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos neighbor = feet.offset(dx, 0, dz);
                if (!level.hasChunkAt(neighbor) || !level.getWorldBorder().isWithinBounds(neighbor)
                        || !level.getFluidState(neighbor).isEmpty() || hazardous(level.getBlockState(neighbor))
                        || hazardous(level.getBlockState(neighbor.below()))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hazardous(BlockState state) {
        return state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.WITHER_ROSE);
    }

    @Override
    public void close() {
        if (active == this) {
            active = null;
        }
        loaded.clear();
        nextAttempts.clear();
    }
}
