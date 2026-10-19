package de.kompass4mc.reward;

import java.util.Objects;
import java.util.regex.Pattern;

/** Eine ausdrücklich gewählte Verzauberung; enthält keine Minecraft-Laufzeitobjekte. */
public record RewardEnchantment(String enchantmentId, int level) {
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public RewardEnchantment {
        Objects.requireNonNull(enchantmentId, "enchantmentId");
        if (!RESOURCE_ID.matcher(enchantmentId).matches()) {
            throw new IllegalArgumentException("Ungültige Verzauberungs-ID: " + enchantmentId);
        }
        if (level < 1 || level > 5) {
            throw new IllegalArgumentException("Die Verzauberungsstufe muss zwischen 1 und 5 liegen.");
        }
        if (enchantmentId.equals("minecraft:binding_curse") || enchantmentId.equals("minecraft:vanishing_curse")) {
            throw new IllegalArgumentException("Lernbelohnungen enthalten keine Flüche.");
        }
    }
}
