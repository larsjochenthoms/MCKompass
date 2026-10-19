package de.kompass4mc.mixin;

import de.kompass4mc.VillageService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Credit actual successful placements, rather than a click or a guessed block count. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void kompass4mc$placed(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> result) {
        if (result.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player) {
            VillageService.onBlockPlaced(player, context.getClickedPos());
        }
    }
}
