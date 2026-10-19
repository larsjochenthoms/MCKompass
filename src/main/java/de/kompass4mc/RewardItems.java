package de.kompass4mc;

import de.kompass4mc.reward.RewardEnchantment;
import de.kompass4mc.reward.RewardItem;
import de.kompass4mc.reward.RewardSpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Builds a complete, validated reward before any of its items are handed to a player. */
public final class RewardItems {
    private RewardItems() {}

    public static List<ItemStack> create(RewardSpec reward, HolderLookup.Provider registries) {
        Objects.requireNonNull(reward, "reward");
        Objects.requireNonNull(registries, "registries");
        List<ItemStack> stacks = new ArrayList<>();
        for (RewardItem item : reward.items()) stacks.add(createItem(item, registries));
        return List.copyOf(stacks);
    }

    private static ItemStack createItem(RewardItem reward, HolderLookup.Provider registries) {
        Identifier itemId = identifier(reward.itemId(), "Gegenstand");
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, itemId);
        Holder<Item> item = registries.lookupOrThrow(Registries.ITEM).get(itemKey)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Belohnungsgegenstand: " + itemId));
        ItemStack stack = new ItemStack(item, reward.count());
        if (stack.isEmpty() || reward.count() < 1 || reward.count() > stack.getMaxStackSize()) {
            throw new IllegalArgumentException("Ungültige Belohnungsmenge für " + itemId + ": " + reward.count());
        }

        if (!reward.enchantments().isEmpty()) {
            boolean stored = stack.getItem() == Items.ENCHANTED_BOOK;
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            Set<Identifier> seen = new HashSet<>();
            List<Holder<Enchantment>> added = new ArrayList<>();
            for (RewardEnchantment requested : reward.enchantments()) {
                Identifier enchantmentId = identifier(requested.enchantmentId(), "Verzauberung");
                if (!seen.add(enchantmentId)) {
                    throw new IllegalArgumentException("Doppelte Belohnungsverzauberung: " + enchantmentId);
                }
                Holder<Enchantment> enchantment = registries.lookupOrThrow(Registries.ENCHANTMENT)
                        .get(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId))
                        .orElseThrow(() -> new IllegalArgumentException("Unbekannte Belohnungsverzauberung: " + enchantmentId));
                if (requested.level() < enchantment.value().getMinLevel()
                        || requested.level() > enchantment.value().getMaxLevel()) {
                    throw new IllegalArgumentException("Ungültige Stufe für " + enchantmentId + ": " + requested.level());
                }
                if (!stored && !enchantment.value().isSupportedItem(stack)) {
                    throw new IllegalArgumentException("Verzauberung " + enchantmentId + " passt nicht zu " + itemId);
                }
                for (Holder<Enchantment> previous : added) {
                    if (!Enchantment.areCompatible(previous, enchantment)) {
                        throw new IllegalArgumentException("Unvereinbare Verzauberungen für " + itemId);
                    }
                }
                enchantments.set(enchantment, requested.level());
                added.add(enchantment);
            }
            stack.set(stored ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS,
                    enchantments.toImmutable());
        }
        ItemStack.validateStrict(stack).getOrThrow();
        return stack;
    }

    private static Identifier identifier(String value, String kind) {
        Identifier identifier = value == null ? null : Identifier.tryParse(value);
        if (identifier == null) throw new IllegalArgumentException("Ungültige " + kind + "-ID: " + value);
        return identifier;
    }
}
