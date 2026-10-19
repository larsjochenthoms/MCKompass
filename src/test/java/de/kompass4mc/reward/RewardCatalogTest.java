package de.kompass4mc.reward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.kompass4mc.learning.Difficulty;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.learning.LearningTask;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RewardCatalogTest {
    @Test
    void everyTaskHasExactlyOneFixedRewardAndUnknownTasksFailExplicitly() {
        assertEquals(LearningCatalog.all().size(), RewardCatalog.all().size());
        Set<String> rewardIds = new HashSet<>();
        for (LearningTask task : LearningCatalog.all()) {
            RewardSpec reward = RewardCatalog.forTask(task);
            assertTrue(rewardIds.add(reward.id()), task.id());
            assertEquals(task.id(), reward.id());
            assertSame(reward, RewardCatalog.forTask(task.id()));
            assertSame(reward, RewardCatalog.forTask(task), "Wiederholung darf keine neue Auswahl treffen.");
            assertFalse(reward.description().isBlank());
            assertEquals(reward.label(), reward.description());
        }
        assertEquals(new HashSet<>(LearningCatalog.all().stream().map(LearningTask::id).toList()), rewardIds);
        assertThrows(IllegalArgumentException.class, () -> RewardCatalog.forTask("unknown"));
        assertThrows(IllegalArgumentException.class, () -> RewardCatalog.forTask((String) null));
        assertThrows(UnsupportedOperationException.class, () -> RewardCatalog.all().clear());
    }

    @Test
    void everyPackageIncludesTheKnownEmeraldBaseAndAUsefulDistinctBonus() {
        Set<String> bonusItems = new HashSet<>();
        Set<String> descriptions = new HashSet<>();
        for (LearningTask task : LearningCatalog.all()) {
            RewardSpec reward = RewardCatalog.forTask(task);
            assertEquals(2, reward.items().size(), task.id());
            RewardItem emeralds = reward.items().getFirst();
            assertEquals("minecraft:emerald", emeralds.itemId());
            assertEquals(task.difficulty().emeralds(), emeralds.count());
            assertTrue(emeralds.enchantments().isEmpty());
            RewardItem bonus = reward.items().get(1);
            assertFalse(bonus.itemId().equals("minecraft:emerald"));
            assertFalse(bonus.itemId().equals("minecraft:air"));
            assertTrue(bonus.count() >= 1 && bonus.count() <= 64);
            assertTrue(reward.description().startsWith(emeralds.count() + " Smaragde + "));
            assertTrue(descriptions.add(reward.description()), task.id());
            bonusItems.add(bonus.itemId());
        }
        assertTrue(bonusItems.size() >= 15, "Mindestens 15 verschiedene Sachbelohnungen sind zugesagt.");
    }

    @Test
    void allDifficultTasksGiveEnchantedTreasuresIncludingGuaranteedMendingBook() {
        int hard = 0;
        for (LearningTask task : LearningCatalog.all()) {
            if (task.difficulty() != Difficulty.HARD) {
                continue;
            }
            hard++;
            RewardItem bonus = RewardCatalog.forTask(task).items().get(1);
            assertFalse(bonus.enchantments().isEmpty(), task.id());
            assertEquals(1, bonus.count(), task.id());
        }
        assertEquals(8, hard);
        assertBonus("subtract_borrow_1", "minecraft:enchanted_book",
                Map.of("minecraft:mending", 1));
        assertBonus("grammar_carry_1", "minecraft:diamond_pickaxe",
                Map.of("minecraft:efficiency", 3, "minecraft:unbreaking", 2));
        assertBonus("grammar_bring_2", "minecraft:diamond_boots",
                Map.of("minecraft:feather_falling", 4, "minecraft:unbreaking", 2));
        assertBonus("length_rope", "minecraft:bow",
                Map.of("minecraft:power", 3, "minecraft:unbreaking", 2));
    }

    @Test
    void suppliesAndEquipmentAreSeparatedByTaskDemand() {
        Set<String> simpleSupplies = Set.of("minecraft:torch", "minecraft:bread", "minecraft:oak_log",
                "minecraft:arrow", "minecraft:wheat_seeds", "minecraft:leather_boots", "minecraft:carrot",
                "minecraft:coal", "minecraft:glass");
        Set<String> mediumEquipment = Set.of("minecraft:iron_ingot", "minecraft:bow", "minecraft:iron_axe",
                "minecraft:redstone", "minecraft:lapis_lazuli", "minecraft:book", "minecraft:iron_chestplate",
                "minecraft:shield", "minecraft:iron_pickaxe", "minecraft:anvil", "minecraft:clock");
        for (LearningTask task : LearningCatalog.all()) {
            RewardItem bonus = RewardCatalog.forTask(task).items().get(1);
            if (task.difficulty() == Difficulty.EASY) {
                assertTrue(simpleSupplies.contains(bonus.itemId()), task.id());
                assertTrue(bonus.enchantments().isEmpty(), task.id());
            } else if (task.difficulty() == Difficulty.MEDIUM) {
                assertTrue(mediumEquipment.contains(bonus.itemId()), task.id());
            }
        }
        for (Difficulty difficulty : Difficulty.values()) {
            String description = RewardCatalog.tierDescription(difficulty);
            assertTrue(description.startsWith(difficulty.emeralds() + " Smaragde + "));
            assertTrue(description.length() <= 50, description);
        }
    }

    @Test
    void catalogEnchantmentsHaveCompatibleEquipmentAndLegalVanillaLevels() {
        Map<String, Integer> maximumLevels = Map.of("minecraft:mending", 1, "minecraft:efficiency", 5,
                "minecraft:unbreaking", 3, "minecraft:feather_falling", 4, "minecraft:power", 5);
        Map<String, Set<String>> compatible = Map.of(
                "minecraft:mending", Set.of("minecraft:enchanted_book"),
                "minecraft:efficiency", Set.of("minecraft:iron_axe", "minecraft:iron_pickaxe", "minecraft:diamond_pickaxe"),
                "minecraft:unbreaking", Set.of("minecraft:diamond_pickaxe", "minecraft:diamond_boots", "minecraft:bow"),
                "minecraft:feather_falling", Set.of("minecraft:diamond_boots"),
                "minecraft:power", Set.of("minecraft:bow"));
        Map<Integer, String> roman = Map.of(1, "I", 2, "II", 3, "III", 4, "IV", 5, "V");
        Map<String, String> german = Map.of("minecraft:mending", "Reparatur", "minecraft:efficiency", "Effizienz",
                "minecraft:unbreaking", "Haltbarkeit", "minecraft:feather_falling", "Federfall", "minecraft:power", "Stärke");
        for (RewardSpec reward : RewardCatalog.all()) {
            for (RewardItem item : reward.items()) {
                for (RewardEnchantment enchantment : item.enchantments()) {
                    assertTrue(maximumLevels.containsKey(enchantment.enchantmentId()), reward.id());
                    assertTrue(enchantment.level() <= maximumLevels.get(enchantment.enchantmentId()), reward.id());
                    assertTrue(compatible.get(enchantment.enchantmentId()).contains(item.itemId()), reward.id());
                    assertTrue(reward.description().contains(german.get(enchantment.enchantmentId()) + " " + roman.get(enchantment.level())),
                            reward.description());
                    assertFalse(enchantment.enchantmentId().contains("curse"));
                }
            }
        }
    }

    private static void assertBonus(String taskId, String itemId, Map<String, Integer> expectedEnchantments) {
        RewardItem bonus = RewardCatalog.forTask(taskId).items().get(1);
        assertEquals(itemId, bonus.itemId());
        Map<String, Integer> actual = bonus.enchantments().stream()
                .collect(java.util.stream.Collectors.toMap(RewardEnchantment::enchantmentId, RewardEnchantment::level));
        assertEquals(expectedEnchantments, actual);
    }
}
