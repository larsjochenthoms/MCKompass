package de.kompass4mc.reward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class RewardSpecTest {
    private static final RewardEnchantment MENDING = new RewardEnchantment("minecraft:mending", 1);
    private static final RewardItem BOOK = new RewardItem("minecraft:enchanted_book", 1, List.of(MENDING));

    @Test
    void recordsDefensivelyCopyBothLists() {
        List<RewardEnchantment> mutableEnchantments = new ArrayList<>(List.of(MENDING));
        RewardItem item = new RewardItem("minecraft:enchanted_book", 1, mutableEnchantments);
        mutableEnchantments.clear();
        assertEquals(List.of(MENDING), item.enchantments());
        List<RewardItem> mutableItems = new ArrayList<>(List.of(item));
        RewardSpec reward = new RewardSpec("test", "Reparatur I", mutableItems);
        mutableItems.clear();
        assertEquals(List.of(item), reward.items());
        assertThrows(UnsupportedOperationException.class, () -> item.enchantments().clear());
        assertThrows(UnsupportedOperationException.class, () -> reward.items().clear());
    }

    @Test
    void resourceIdsMustHaveAnExplicitValidNamespaceAndPath() {
        for (String invalid : List.of("", "mending", "minecraft:", ":mending", "Minecraft:mending", "minecraft:mending I")) {
            assertThrows(IllegalArgumentException.class, () -> new RewardEnchantment(invalid, 1), invalid);
            assertThrows(IllegalArgumentException.class, () -> new RewardItem(invalid, 1, List.of()), invalid);
        }
        assertThrows(IllegalArgumentException.class, () -> new RewardItem("minecraft:air", 1, List.of()));
    }

    @Test
    void itemCountsAndEnchantmentLevelsAreBounded() {
        for (int count : new int[]{-1, 0, 65, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new RewardItem("minecraft:bread", count, List.of()));
        }
        for (int level : new int[]{-1, 0, 6, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new RewardEnchantment("minecraft:efficiency", level));
        }
        assertEquals(64, new RewardItem("minecraft:torch", 64, List.of()).count());
        assertEquals(5, new RewardEnchantment("minecraft:efficiency", 5).level());
    }

    @Test
    void enchantedEquipmentIsSingleAndContainsNoDuplicateOrCurseEnchantments() {
        assertThrows(IllegalArgumentException.class, () -> new RewardItem("minecraft:enchanted_book", 2, List.of(MENDING)));
        assertThrows(IllegalArgumentException.class, () -> new RewardItem("minecraft:enchanted_book", 1, List.of(MENDING, MENDING)));
        assertThrows(IllegalArgumentException.class, () -> new RewardEnchantment("minecraft:binding_curse", 1));
        assertThrows(IllegalArgumentException.class, () -> new RewardEnchantment("minecraft:vanishing_curse", 1));
        assertThrows(IllegalArgumentException.class, () -> new RewardItem("minecraft:enchanted_book", 1,
                Collections.nCopies(5, MENDING)));
    }

    @Test
    void emptyOrExcessivePackagesAndMissingLabelsFail() {
        assertThrows(IllegalArgumentException.class, () -> new RewardSpec("test", "Buch", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RewardSpec("test", "Buch", Collections.nCopies(9, BOOK)));
        assertThrows(IllegalArgumentException.class, () -> new RewardSpec("test", " ", List.of(BOOK)));
        assertThrows(IllegalArgumentException.class, () -> new RewardSpec("test", "a".repeat(241), List.of(BOOK)));
        assertThrows(IllegalArgumentException.class, () -> new RewardSpec("invalid id", "Buch", List.of(BOOK)));
    }

    @Test
    void nullValuesCannotEnterTheDomain() {
        assertThrows(NullPointerException.class, () -> new RewardEnchantment(null, 1));
        assertThrows(NullPointerException.class, () -> new RewardItem(null, 1, List.of()));
        assertThrows(NullPointerException.class, () -> new RewardItem("minecraft:book", 1, null));
        assertThrows(NullPointerException.class, () -> new RewardSpec(null, "Buch", List.of(BOOK)));
        assertThrows(NullPointerException.class, () -> new RewardSpec("test", null, List.of(BOOK)));
        assertThrows(NullPointerException.class, () -> new RewardSpec("test", "Buch", null));
    }
}
