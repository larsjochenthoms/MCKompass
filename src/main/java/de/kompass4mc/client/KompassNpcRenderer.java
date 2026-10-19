package de.kompass4mc.client;

import de.kompass4mc.NpcAppearance;
import de.kompass4mc.NpcSkin;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;

/** Uses bundled player skins without changing the NPC's server-side entity or identity. */
public final class KompassNpcRenderer extends MobRenderer<Villager, KompassNpcRenderState, PlayerModel> {
    private static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath(
            "kompass4mc", "textures/entity/mila.png");
    private static final Map<NpcSkin, Identifier> TEXTURES = textures();

    public KompassNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    private static Map<NpcSkin, Identifier> textures() {
        var textures = new EnumMap<NpcSkin, Identifier>(NpcSkin.class);
        for (NpcSkin skin : NpcSkin.values()) {
            textures.put(skin, Identifier.fromNamespaceAndPath("kompass4mc", "textures/entity/" + skin.id() + ".png"));
        }
        return Map.copyOf(textures);
    }

    @Override
    public KompassNpcRenderState createRenderState() {
        return new KompassNpcRenderState();
    }

    @Override
    public void extractRenderState(Villager npc, KompassNpcRenderState state, float partialTicks) {
        // The living-entity extraction retains name tags, facing, lighting and the walk cycle.
        super.extractRenderState(npc, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(npc, state, partialTicks, itemModelResolver);
        state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        state.textureLocation = NpcAppearance.skin(npc).map(TEXTURES::get).orElse(DEFAULT_TEXTURE);
    }

    @Override
    public Identifier getTextureLocation(KompassNpcRenderState state) {
        return state.textureLocation;
    }
}
