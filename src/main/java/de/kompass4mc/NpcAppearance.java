package de.kompass4mc;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;

/** The name style is synchronized by Vanilla; entity command tags are server-only. */
public final class NpcAppearance {
    private NpcAppearance() {}

    /** Mark only our server-owned NPCs, including residents saved before skins were added. */
    public static void mark(Entity entity) {
        if (!(entity instanceof Villager) || !(entity.level() instanceof ServerLevel)) return;
        Optional<NpcSkin> skin;
        if (RoamingNpcs.isNpc(entity)) {
            skin = NpcSkin.fromResidentName(RoamingNpcs.residentName(entity));
        } else if (entity.entityTags().contains(VillageBuilder.NPC_TAG)) {
            String name = entity.getCustomName() == null ? "" : entity.getCustomName().getString();
            skin = java.util.Arrays.stream(NpcSkin.values())
                    .filter(candidate -> name.equals(candidate.residentName())
                            || name.startsWith(candidate.residentName() + " · ")).findFirst();
        } else {
            return;
        }
        skin.ifPresent(value -> {
            Component original = entity.getCustomName();
            if (original == null) original = Component.literal(value.residentName());
            if (!Objects.equals(original.getStyle().getInsertion(), value.marker())) {
                entity.setCustomName(original.copy().withStyle(style -> style.withInsertion(value.marker())));
            }
        });
    }

    /** Rendering uses a strict whitelist, independently of the displayed name or profession. */
    public static Optional<NpcSkin> skin(Entity entity) {
        if (!(entity instanceof Villager) || entity.getCustomName() == null) return Optional.empty();
        return NpcSkin.fromMarker(entity.getCustomName().getStyle().getInsertion());
    }
}
