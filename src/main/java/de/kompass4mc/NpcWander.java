package de.kompass4mc;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Controlled strolls for tagged learning visitors; vanilla villager AI stays disabled. */
final class NpcWander {
    static final int RADIUS = 6;
    private static final String HOME_TAG = RoamingNpcs.NPC_TAG + "_home_";
    private static final double SPEED = 0.38;
    private final Villager npc;
    private final BlockPos home;
    private List<BlockPos> route = List.of();
    private int routeIndex;
    private long ticks;
    private long nextWalk;
    private long walkStarted;

    NpcWander(Villager npc) {
        this.npc = npc;
        this.home = rememberHome(npc);
        // Old saved visitors already have NoAI. Preserve that protection and supply only movement.
        npc.setNoAi(true);
        npc.getNavigation().stop();
        nextWalk = 40 + npc.getRandom().nextInt(60);
    }

    void tick(Optional<ServerPlayer> conversation, boolean busy) {
        if (!RoamingNpcs.isNpc(npc) || !(npc.level() instanceof ServerLevel level)) return;
        ticks++;
        npc.setNoAi(true);
        ServerPlayer nearby = conversation.filter(player -> validPartner(player, 36)).orElseGet(() ->
                level.players().stream().filter(player -> validPartner(player, 36))
                        .min(java.util.Comparator.comparingDouble(npc::distanceToSqr)).orElse(null));
        BlockPos feet = npc.blockPosition();
        boolean safeHere = feet.getY() == home.getY() && safeCell(level, feet)
                && Math.abs(npc.getY() - feet.getY()) < 0.05;
        if (nearby != null || busy || npc.isPassenger() || npc.isVehicle() || !safeHere) {
            pause();
            nextWalk = ticks + 40;
            if (nearby != null) {
                npc.getLookControl().setLookAt(nearby, 35, 30);
                npc.getLookControl().tick();
                npc.setYRot(npc.getYHeadRot());
                npc.setYBodyRot(npc.getYHeadRot());
            }
            return;
        }
        if (route.isEmpty() && ticks >= nextWalk) {
            List<List<WanderRoutes.Cell>> choices = WanderRoutes.find(home.getX(), home.getZ(),
                    feet.getX(), feet.getZ(), RADIUS,
                    (x, z) -> safeCell(level, new BlockPos(x, home.getY(), z)));
            if (!choices.isEmpty()) {
                route = choices.get(npc.getRandom().nextInt(choices.size())).stream()
                        .map(cell -> new BlockPos(cell.x(), home.getY(), cell.z())).toList();
                routeIndex = 0;
                walkStarted = ticks;
            }
            nextWalk = ticks + 120 + npc.getRandom().nextInt(120);
        }
        if (!route.isEmpty() && (ticks - walkStarted > 200
                || route.subList(routeIndex, route.size()).stream().anyMatch(pos -> !safeCell(level, pos)))) {
            pause();
            nextWalk = ticks + 120;
        }
        while (!route.isEmpty() && routeIndex < route.size()
                && horizontalDistanceSquared(route.get(routeIndex)) < 0.09) routeIndex++;
        if (!route.isEmpty() && routeIndex >= route.size()) {
            pause();
            nextWalk = ticks + 120 + npc.getRandom().nextInt(120);
        }
        if (!route.isEmpty()) {
            BlockPos target = route.get(routeIndex);
            npc.getMoveControl().setWantedPosition(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, SPEED);
            npc.getMoveControl().tick();
            npc.setYBodyRot(npc.getYRot());
            npc.setYHeadRot(npc.getYRot());
            // NoAI skips vanilla travel. Call it exactly once here for normal collision and gravity.
            npc.travel(new Vec3(npc.xxa, 0, npc.zza));
        } else {
            stopMovement();
            // Establish onGround for visitors saved by older versions, without horizontal drift.
            npc.travel(Vec3.ZERO);
        }
    }

    private boolean validPartner(ServerPlayer player, double distanceSquared) {
        return player.isAlive() && !player.isSpectator() && player.level() == npc.level()
                && npc.distanceToSqr(player) <= distanceSquared;
    }

    private double horizontalDistanceSquared(BlockPos position) {
        double dx = npc.getX() - position.getX() - 0.5;
        double dz = npc.getZ() - position.getZ() - 0.5;
        return dx * dx + dz * dz;
    }

    private void pause() {
        route = List.of();
        routeIndex = 0;
        stopMovement();
    }

    private void stopMovement() {
        npc.getNavigation().stop();
        npc.getMoveControl().setWait();
        npc.setSpeed(0);
        npc.setXxa(0);
        npc.setYya(0);
        npc.setZza(0);
        npc.setJumping(false);
        npc.setDeltaMovement(Vec3.ZERO);
    }

    private static BlockPos rememberHome(Villager npc) {
        for (String tag : npc.entityTags()) {
            if (!tag.startsWith(HOME_TAG)) continue;
            String[] values = tag.substring(HOME_TAG.length()).split("_", -1);
            if (values.length != 3) continue;
            try {
                BlockPos saved = new BlockPos(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]));
                if (!npc.level().isOutsideBuildHeight(saved)) return saved;
            } catch (NumberFormatException ignored) {
                // A malformed manual tag does not prevent old worlds from loading.
            }
        }
        BlockPos saved = npc.blockPosition().immutable();
        npc.entityTags().removeIf(tag -> tag.startsWith(HOME_TAG));
        npc.addTag(HOME_TAG + saved.getX() + "_" + saved.getY() + "_" + saved.getZ());
        return saved;
    }

    /** One block of clearance beside a path prevents corner cutting over water, drops or hazards. */
    private static boolean safeCell(ServerLevel level, BlockPos feet) {
        if (level.isOutsideBuildHeight(feet.below()) || level.isOutsideBuildHeight(feet.above())) return false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = feet.offset(dx, 0, dz);
                if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                        || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()
                        || !level.getFluidState(pos.below()).isEmpty()) return false;
                // Grass and flowers have no collision and can remain in place during a stroll.
                if (!walkThrough(level, pos) || !walkThrough(level, pos.above())) return false;
                BlockState ground = level.getBlockState(pos.below());
                if (!ground.isFaceSturdy(level, pos.below(), Direction.UP) || hazardous(ground)) return false;
            }
        }
        return true;
    }

    private static boolean walkThrough(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !hazardous(state) && state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean hazardous(BlockState state) {
        return state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.FROSTED_ICE) || state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)
                || state.is(Blocks.COBWEB) || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_GATEWAY) || state.is(Blocks.TRIPWIRE)
                || state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK);
    }
}
