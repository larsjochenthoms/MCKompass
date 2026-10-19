package de.kompass4mc;

import de.kompass4mc.learning.BuildChallenges;
import de.kompass4mc.progress.ChallengeOffer;
import de.kompass4mc.village.GridPos;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

/** A blueprint selects an empty site. Building and removing blocks always remain player actions. */
final class ChallengeBuilding {
    private static final String BLUEPRINT = "kompass4mc:blueprint";
    private ChallengeBuilding() {}
    static ItemStack blueprint() {
        ItemStack paper = new ItemStack(Items.PAPER);
        paper.set(DataComponents.CUSTOM_NAME, Component.literal("Mila · Bauplan")
                .withStyle(style -> style.withInsertion(BLUEPRINT)));
        return paper;
    }
    static boolean isBlueprint(ItemStack stack) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        return stack.is(Items.PAPER) && name != null && BLUEPRINT.equals(name.getStyle().getInsertion());
    }
    static Optional<ChallengeOffer.Site> site(ServerPlayer player, BlockHitResult hit, BuildChallenges.Plan plan) {
        ServerLevel level = player.level();
        BlockPos origin = hit.getBlockPos().above();
        if (hit.getDirection() != Direction.UP || !player.mayBuild()
                || player.distanceToSqr(origin.getX() + .5, origin.getY(), origin.getZ() + .5) > 64) return Optional.empty();
        for (int x = -1; x <= plan.width(); x++) for (int z = -1; z <= plan.depth(); z++) {
            BlockPos ground = origin.offset(x, -1, z);
            if (!level.hasChunkAt(ground) || level.isOutsideBuildHeight(ground)
                    || !level.getWorldBorder().isWithinBounds(ground)
                    || !level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
                    || !level.getFluidState(ground).isEmpty() || dangerous(level, ground)) return Optional.empty();
            for (int y = 0; y <= plan.height(); y++) {
                BlockPos pos = origin.offset(x, y, z);
                if (level.isOutsideBuildHeight(pos) || !level.getFluidState(pos).isEmpty()
                        || dangerous(level, pos) || !level.getBlockState(pos).canBeReplaced()) return Optional.empty();
            }
        }
        return Optional.of(new ChallengeOffer.Site(level.dimension().identifier().toString(), origin.getX(), origin.getY(), origin.getZ()));
    }
    private static boolean dangerous(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.COBWEB)
                || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY);
    }
    record Result(boolean available, boolean complete, int missing, int wrong, int extra, String answer) {}
    static Result inspect(ServerLevel level, ChallengeOffer.Site site, BuildChallenges.Plan plan) {
        if (site == null || !site.dimension().equals(level.dimension().identifier().toString()))
            return new Result(false, false, 0, 0, 0, "");
        Map<GridPos, String> actual = new LinkedHashMap<>();
        var pattern = plan.pattern();
        var positions = new java.util.LinkedHashSet<>(pattern.required().keySet()); positions.addAll(pattern.forbidden());
        for (GridPos offset : positions) {
            BlockPos pos = new BlockPos(site.x() + offset.x(), site.y() + offset.y(), site.z() + offset.z());
            if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos))
                return new Result(false, false, 0, 0, 0, "");
            var state = level.getBlockState(pos);
            // Harmless plants are not extra building blocks, and are never removed by this check.
            String block = pattern.forbidden().contains(offset) && state.canBeReplaced()
                    && level.getFluidState(pos).isEmpty() && !dangerous(level, pos) ? "minecraft:air"
                    : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            actual.put(offset, block);
        }
        var result = pattern.inspect(actual);
        String answer = result.complete() ? "gebaut" : "bauform_passt_nicht";
        if (plan.shape() == BuildChallenges.Shape.FRAME && result.missing().isEmpty()
                && result.wrongMaterial().isEmpty() && !result.extra().isEmpty()) {
            boolean filled = true;
            for (int x = 1; x < plan.width() - 1; x++) for (int z = 1; z < plan.depth() - 1; z++)
                filled &= plan.material().equals(actual.get(new GridPos(x, 0, z)));
            if (filled) answer = "flaeche_statt_rand";
        }
        return new Result(true, result.complete(), result.missing().size(), result.wrongMaterial().size(), result.extra().size(), answer);
    }
    static void outline(ServerPlayer player, ChallengeOffer.Site site, BuildChallenges.Plan plan) {
        ServerLevel level = player.level();
        if (!level.dimension().identifier().toString().equals(site.dimension())
                || player.distanceToSqr(site.x(), site.y(), site.z()) > 48 * 48) return;
        for (int x = 0; x <= plan.width(); x++) for (int z = 0; z <= plan.depth(); z++) {
            if (x != 0 && x != plan.width() && z != 0 && z != plan.depth()) continue;
            BlockPos pos = new BlockPos(site.x() + x, site.y(), site.z() + z);
            if (level.hasChunkAt(pos)) level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX(), pos.getY() + .08, pos.getZ(), 1, 0, .01, 0, 0);
        }
    }
}
