package de.kompass4mc.mixin.client;

import de.kompass4mc.NpcAppearance;
import de.kompass4mc.client.KompassNpcRenderer;
import de.kompass4mc.client.KompassNpcRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Selects player-shaped rendering only for the synchronized Kompass NPC identity. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Unique
    private KompassNpcRenderer kompass4mc$npcRenderer;

    @ModifyArg(method = "onResourceManagerReload", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/entity/EntityRenderers;createEntityRenderers("
                    + "Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;)Ljava/util/Map;"), index = 0)
    private EntityRendererProvider.Context kompass4mc$reloadNpcRenderer(EntityRendererProvider.Context context) {
        kompass4mc$npcRenderer = new KompassNpcRenderer(context);
        return context;
    }

    @Inject(method = "getRenderer(Lnet/minecraft/world/entity/Entity;)"
            + "Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
    private void kompass4mc$rendererForNpc(Entity entity, CallbackInfoReturnable<EntityRenderer<?, ?>> callback) {
        if (kompass4mc$npcRenderer != null && entity instanceof Villager && NpcAppearance.skin(entity).isPresent()) {
            callback.setReturnValue(kompass4mc$npcRenderer);
        }
    }

    @Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)"
            + "Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
    private void kompass4mc$rendererForNpcState(EntityRenderState state,
            CallbackInfoReturnable<EntityRenderer<?, ?>> callback) {
        // Rendering is deferred in 26.3: the second lookup receives only the extracted snapshot.
        if (kompass4mc$npcRenderer != null && state instanceof KompassNpcRenderState) {
            callback.setReturnValue(kompass4mc$npcRenderer);
        }
    }
}
