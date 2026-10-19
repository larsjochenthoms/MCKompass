package de.kompass4mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import de.kompass4mc.reward.RewardCatalog;
import de.kompass4mc.reward.RewardEnchantment;
import de.kompass4mc.reward.RewardItem;
import de.kompass4mc.reward.RewardSpec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RewardItemsTest {
    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapVanillaItemsTagsAndEnchantments() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Enchantment applicability comes from the actual 26.3 item tags, not a hand-built fixture.
        var pack = new VanillaPackResourcesBuilder().pushJarResources().exposeNamespace("minecraft")
                .build(new PackLocationInfo("vanilla", Component.literal("Vanilla"), PackSource.BUILT_IN, Optional.empty()));
        try (var resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(pack.fullResources()))) {
            RegistryAccess items = new RegistryAccess.ImmutableRegistryAccess(List.of(BuiltInRegistries.ITEM)).freeze();
            TagLoader.loadTagsForExistingRegistries(resources, items).forEach(Registry.PendingTags::apply);
            var world = VanillaRegistries.createWorldLookup();
            // Datagen lookups contain enchantment values, but do not bind their exclusive-set tags.
            // Load the real enchantment registry, including tags, through the server's resource loader.
            var context = world.listRegistries().filter(lookup -> !lookup.key().equals(Registries.ENCHANTMENT)).toList();
            var data = RegistryDataLoader.WORLD_REGISTRIES.stream()
                    .filter(entry -> entry.key().equals(Registries.ENCHANTMENT)).toList();
            var enchantments = RegistryDataLoader.load(resources, context, data, Runnable::run).join();
            registries = HolderLookup.Provider.create(Stream.concat(context.stream(), enchantments.listRegistries()));
        }
        // Since 26.3 item prototypes are bound during the resource reload, after tag loading.
        // Use the same initializer and apply step as ReloadableServerResources.
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(DataComponentInitializers.PendingComponents::apply);
    }

    static Stream<RewardSpec> rewards() {
        return RewardCatalog.all().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rewards")
    void everyActualRewardBuildsWithVanillaRegistryAndSurvivesItemCodec(RewardSpec reward) {
        List<ItemStack> stacks = RewardItems.create(reward, registries);
        assertEquals(reward.items().size(), stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            RewardItem requested = reward.items().get(i);
            ItemStack stack = stacks.get(i);
            assertFalse(stack.isEmpty());
            assertEquals(requested.itemId(), BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            assertEquals(requested.count(), stack.getCount());
            var ops = registries.createSerializationContext(JsonOps.INSTANCE);
            var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
            ItemStack decoded = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
            assertTrue(ItemStack.matches(stack, decoded), encoded.toString());
        }
    }

    @Test
    void mendingBookStoresItsEnchantmentForAnvilUse() {
        ItemStack book = RewardItems.create(RewardCatalog.forTask("subtract_borrow_1"), registries).getLast();
        assertEquals(Items.ENCHANTED_BOOK, book.getItem());
        assertTrue(book.hasFoil());
        assertEquals(1, book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(enchantment("mending")));
        assertTrue(book.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(),
                "A book must store its enchantment; applying it as equipment would not make an anvil reward.");
    }

    @Test
    void equipmentHasActiveEnchantmentsAndNoStoredBookEnchantments() {
        ItemStack pickaxe = RewardItems.create(RewardCatalog.forTask("grammar_carry_1"), registries).getLast();
        assertEquals(Items.DIAMOND_PICKAXE, pickaxe.getItem());
        assertEquals(3, pickaxe.getEnchantments().getLevel(enchantment("efficiency")));
        assertEquals(2, pickaxe.getEnchantments().getLevel(enchantment("unbreaking")));
        assertTrue(pickaxe.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty());
        assertTrue(pickaxe.hasFoil());
    }

    @Test
    void returnedItemsAreFreshForEachDelivery() {
        RewardSpec reward = RewardCatalog.forTask("grammar_carry_1");
        List<ItemStack> first = RewardItems.create(reward, registries);
        List<ItemStack> second = RewardItems.create(reward, registries);
        assertNotSame(first.getLast(), second.getLast());
        first.getLast().setCount(0);
        assertEquals(1, second.getLast().getCount());
    }

    @Test
    void unknownItemDoesNotBecomeTheDefaultAirItem() {
        assertThrows(IllegalArgumentException.class, () -> RewardItems.create(
                spec("kompass4mc:unknown_item", List.of()), registries));
        assertThrows(IllegalArgumentException.class, () -> RewardItems.create(
                spec("minecraft:air", List.of()), registries));
    }

    @Test
    void unknownEnchantmentIsRejectedBeforeAnyItemsAreDelivered() {
        assertThrows(IllegalArgumentException.class, () -> RewardItems.create(
                spec("minecraft:enchanted_book", List.of(new RewardEnchantment("kompass4mc:unknown_enchantment", 1))),
                registries));
    }

    @Test
    void validDomainLevelsStillHaveToMeetTheActualVanillaMaximum() {
        assertThrows(IllegalArgumentException.class, () -> RewardItems.create(
                spec("minecraft:enchanted_book", List.of(new RewardEnchantment("minecraft:mending", 2))), registries));
    }

    @Test
    void enchantmentsHaveToSupportTheirEquipment() {
        assertThrows(IllegalArgumentException.class, () -> RewardItems.create(
                spec("minecraft:diamond_boots", List.of(new RewardEnchantment("minecraft:efficiency", 2))), registries));
    }

    @Test
    void incompatibleEnchantmentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RewardItems.create(
                spec("minecraft:diamond_pickaxe", List.of(new RewardEnchantment("minecraft:fortune", 2),
                        new RewardEnchantment("minecraft:silk_touch", 1))), registries));
    }

    @Test
    void vanillaMaximumStackSizeIsRespectedForEquipment() {
        RewardSpec reward = new RewardSpec("test", "Zwei Spitzhacken", List.of(
                new RewardItem("minecraft:diamond_pickaxe", 2, List.of())));
        assertThrows(IllegalArgumentException.class, () -> RewardItems.create(reward, registries));
    }

    private static RewardSpec spec(String itemId, List<RewardEnchantment> enchantments) {
        return new RewardSpec("test", "Testbelohnung", List.of(new RewardItem(itemId, 1, enchantments)));
    }

    private static Holder<Enchantment> enchantment(String name) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace(name)));
    }
}
