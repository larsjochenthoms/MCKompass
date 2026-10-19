package de.kompass4mc.client;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

/** A player-shaped snapshot that still belongs to the original villager entity. */
public final class KompassNpcRenderState extends AvatarRenderState {
    public Identifier textureLocation = Identifier.fromNamespaceAndPath("kompass4mc", "textures/entity/mila.png");
}
