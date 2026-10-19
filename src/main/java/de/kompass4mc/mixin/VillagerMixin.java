package de.kompass4mc.mixin;

import de.kompass4mc.VillageBuilder;
import de.kompass4mc.RoamingNpcs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lightning conversion must not remove a learning character or a village resident. */
@Mixin(Villager.class)
public abstract class VillagerMixin {
    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void kompass4mc$keepResident(ServerLevel level, LightningBolt lightning, CallbackInfo callback) {
        var tags = ((Villager) (Object) this).entityTags();
        if (tags.contains(VillageBuilder.NPC_TAG) || tags.contains(RoamingNpcs.NPC_TAG)) {
            callback.cancel();
        }
    }
}
