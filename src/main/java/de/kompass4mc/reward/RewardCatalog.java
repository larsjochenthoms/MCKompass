package de.kompass4mc.reward;

import de.kompass4mc.learning.Difficulty;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.learning.LearningTask;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Feste, vorab sichtbare Sachbelohnung pro Aufgabe, zusätzlich zu 2/4/6 Smaragden.
 * Es gibt keine Zufallsziehung. Erneutes Üben derselben Aufgabe erzeugt kein neues Paket.
 */
public final class RewardCatalog {
    private static final List<RewardSpec> ALL = List.of(
            reward("subtract_store", "16 Fackeln", item("torch", 16)),
            reward("subtract_borrow_1", "Verzaubertes Buch: Reparatur I",
                    enchanted("enchanted_book", enchantment("mending", 1))),
            reward("multiply_saplings", "6 Eisenbarren", item("iron_ingot", 6)),
            reward("divide_boats", "Bogen: Stärke I", enchanted("bow", enchantment("power", 1))),
            reward("length_rope", "Bogen: Stärke III, Haltbarkeit II",
                    enchanted("bow", enchantment("power", 3), enchantment("unbreaking", 2))),
            reward("perimeter_garden_1", "Eisenaxt: Effizienz II", enchanted("iron_axe", enchantment("efficiency", 2))),
            reward("subtract_borrow_2", "24 Redstone-Staub", item("redstone", 24)),
            reward("groups_stone_1", "8 Brote", item("bread", 8)),
            reward("groups_planks_2", "16 Eichenstämme", item("oak_log", 16)),
            reward("units_rope_1", "24 Pfeile", item("arrow", 24)),
            reward("units_banner_2", "12 Weizenkörner", item("wheat_seeds", 12)),
            reward("perimeter_garden_2", "16 Lapislazuli", item("lapis_lazuli", 16)),
            reward("cause_bridge_1", "4 Bücher", item("book", 4)),
            reward("reading_workshop", "Lederstiefel", item("leather_boots", 1)),
            reward("sequence_lantern_1", "Eisenbrustpanzer", item("iron_chestplate", 1)),
            reward("noun_garden_1", "12 Karotten", item("carrot", 12)),
            reward("spelling_dog_1", "Schild", item("shield", 1)),
            reward("grammar_carry_1", "Diamantspitzhacke: Effizienz III, Haltbarkeit II",
                    enchanted("diamond_pickaxe", enchantment("efficiency", 3), enchantment("unbreaking", 2))),
            reward("cause_roof_2", "Eisenspitzhacke: Effizienz II", enchanted("iron_pickaxe", enchantment("efficiency", 2))),
            reward("sequence_bench_2", "Amboss", item("anvil", 1)),
            reward("noun_torches_2", "12 Kohle", item("coal", 12)),
            reward("spelling_mountain_2", "Uhr", item("clock", 1)),
            reward("grammar_bring_2", "Diamantstiefel: Federfall IV, Haltbarkeit II",
                    enchanted("diamond_boots", enchantment("feather_falling", 4), enchantment("unbreaking", 2))),
            reward("reading_delivery", "16 Glasblöcke", item("glass", 16)),
            reward("numbers_packs", "12 Eisenbarren", item("iron_ingot", 12)),
            reward("numbers_way", "24 Fackeln", item("torch", 24)),
            reward("pattern_steps", "Bogen: Stärke IV, Haltbarkeit III",
                    enchanted("bow", enchantment("power", 4), enchantment("unbreaking", 3))),
            reward("strategy_stones", "8 Bücher", item("book", 8)),
            reward("space_layers", "Eisenaxt: Effizienz III", enchanted("iron_axe", enchantment("efficiency", 3))),
            reward("space_mirror", "16 Eisenbarren", item("iron_ingot", 16)),
            reward("area_floor", "32 Eichenstämme", item("oak_log", 32)),
            reward("time_trip", "2 Uhren", item("clock", 2)),
            reward("money_trade", "Diamantspitzhacke: Effizienz IV, Haltbarkeit III",
                    enchanted("diamond_pickaxe", enchantment("efficiency", 4), enchantment("unbreaking", 3))),
            reward("data_harvest", "16 Karotten", item("carrot", 16)),
            reward("chance_certain", "16 Brote", item("bread", 16)),
            reward("chance_fair", "Diamantstiefel: Federfall III, Haltbarkeit III",
                    enchanted("diamond_boots", enchantment("feather_falling", 3), enchantment("unbreaking", 3))),
            reward("evidence_mine_1", "32 Redstone-Staub", item("redstone", 32)),
            reward("evidence_maps_1", "Bogen: Stärke V, Haltbarkeit III",
                    enchanted("bow", enchantment("power", 5), enchantment("unbreaking", 3))),
            reward("vocabulary_supplies_1", "16 Kohle", item("coal", 16)),
            reward("summary_walkway_1", "24 Lapislazuli", item("lapis_lazuli", 24)),
            reward("sequence_shelter_1", "2 Ambosse", item("anvil", 2)),
            reward("spelling_trees_1", "Eisenspitzhacke: Effizienz III", enchanted("iron_pickaxe", enchantment("efficiency", 3))),
            reward("grammar_infinitive_1", "8 Eisenbarren", item("iron_ingot", 8)),
            reward("grammar_time_phrase_1", "32 Pfeile", item("arrow", 32)),
            reward("compound_holder", "24 Glasblöcke", item("glass", 24)),
            reward("stem_building", "12 Bücher", item("book", 12)),
            reward("adjective_lantern", "Bogen", item("bow", 1)),
            reward("grammar_finished_roof", "32 Lapislazuli", item("lapis_lazuli", 32))
    );

    private static final Map<String, RewardSpec> BY_TASK;

    static {
        Map<String, RewardSpec> indexed = new LinkedHashMap<>();
        for (RewardSpec reward : ALL) {
            if (indexed.putIfAbsent(reward.id(), reward) != null) {
                throw new IllegalStateException("Doppelte Aufgabenbelohnung: " + reward.id());
            }
        }
        for (LearningTask task : LearningCatalog.all()) {
            if (!indexed.containsKey(task.id())) {
                throw new IllegalStateException("Fehlende Aufgabenbelohnung: " + task.id());
            }
        }
        if (indexed.size() != LearningCatalog.all().size()) {
            throw new IllegalStateException("Belohnungskatalog enthält unbekannte Aufgaben.");
        }
        BY_TASK = Map.copyOf(indexed);
    }

    private RewardCatalog() {
    }

    public static List<RewardSpec> all() {
        return ALL;
    }

    public static RewardSpec forTask(LearningTask task) {
        return forTask(Objects.requireNonNull(task, "task").id());
    }

    public static RewardSpec forTask(String taskId) {
        RewardSpec reward = taskId == null ? null : BY_TASK.get(taskId);
        if (reward == null) {
            throw new IllegalArgumentException("Keine Belohnung für die Aufgabe: " + taskId);
        }
        return reward;
    }

    public static String tierDescription(Difficulty difficulty) {
        return switch (Objects.requireNonNull(difficulty, "difficulty")) {
            case EASY -> "2 Smaragde + Vorräte";
            case MEDIUM -> "4 Smaragde + Rohstoffe oder Ausrüstung";
            case HARD -> "6 Smaragde + verzauberter Schatz";
        };
    }

    private static RewardSpec reward(String taskId, String bonusLabel, RewardItem bonus) {
        LearningTask task = LearningCatalog.byId(taskId)
                .orElseThrow(() -> new IllegalStateException("Unbekannte Belohnungsaufgabe: " + taskId));
        return new RewardSpec(taskId, task.difficulty().emeralds() + " Smaragde + " + bonusLabel,
                List.of(item("emerald", task.difficulty().emeralds()), bonus));
    }

    private static RewardItem item(String name, int count) {
        return new RewardItem("minecraft:" + name, count, List.of());
    }

    private static RewardItem enchanted(String name, RewardEnchantment... enchantments) {
        return new RewardItem("minecraft:" + name, 1, List.of(enchantments));
    }

    private static RewardEnchantment enchantment(String name, int level) {
        return new RewardEnchantment("minecraft:" + name, level);
    }
}
