package de.kompass4mc.reward;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Ein Stapel des Belohnungspakets. Verzauberte Ausrüstung wird einzeln ausgegeben. */
public record RewardItem(String itemId, int count, List<RewardEnchantment> enchantments) {
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public RewardItem {
        Objects.requireNonNull(itemId, "itemId");
        if (!RESOURCE_ID.matcher(itemId).matches() || itemId.equals("minecraft:air")) {
            throw new IllegalArgumentException("Ungültiger Belohnungsgegenstand: " + itemId);
        }
        if (count < 1 || count > 64) {
            throw new IllegalArgumentException("Die Stapelgröße muss zwischen 1 und 64 liegen.");
        }
        enchantments = List.copyOf(Objects.requireNonNull(enchantments, "enchantments"));
        if (enchantments.size() > 4) {
            throw new IllegalArgumentException("Eine Belohnung darf höchstens vier Verzauberungen tragen.");
        }
        if (!enchantments.isEmpty() && count != 1) {
            throw new IllegalArgumentException("Verzauberte Belohnungen werden einzeln ausgegeben.");
        }
        Set<String> ids = new HashSet<>();
        for (RewardEnchantment enchantment : enchantments) {
            if (!ids.add(enchantment.enchantmentId())) {
                throw new IllegalArgumentException("Doppelte Verzauberung: " + enchantment.enchantmentId());
            }
        }
    }
}
