package de.kompass4mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import de.kompass4mc.quest.QuestCatalog;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.progress.LearningProgress;
import de.kompass4mc.reward.RewardCatalog;
import de.kompass4mc.village.VillageJobs;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.commands.DialogCommand;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DialogDeliveryTest {
    private static final String PLAYER = "216990a7-2351-48ac-b8ee-82327e23179b";
    private static Registry<Dialog> dialogRegistry;

    @BeforeAll
    static void bootstrapMinecraftCodecs() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        dialogRegistry = new MappedRegistry<Dialog>(Registries.DIALOG, Lifecycle.stable()).freeze();
    }

    static Stream<Arguments> definitions() {
        Stream<Arguments> residents = VillageJobs.all().stream().flatMap(job -> Stream.of(false, true)
                .map(accepted -> {
                    VillageState.Project project = new VillageState.Project();
                    if (accepted) {
                        project.accept(PLAYER);
                    }
                    return Arguments.of(Named.of(job.id() + (accepted ? " / accepted" : " / offer"),
                            VillageDialogs.conversation(job, project, PLAYER,
                                    accepted ? "Noch fehlen 2 Bretter.\nLies den Zettel „Länge & Breite“." : null)));
                }));
        Stream<Arguments> quests = QuestCatalog.all().stream().map(quest -> Arguments.of(
                Named.of(quest.id(), QuestDialogs.quest(quest, null))));
        Stream<Arguments> learning = LearningCatalog.all().stream().flatMap(task -> Stream.of(
                Arguments.of(Named.of(task.id() + " / task", LearningDialogs.task("Jaro", PLAYER, task, false, null, false))),
                Arguments.of(Named.of(task.id() + " / hint", LearningDialogs.task("Mila", PLAYER, task, true, "Versuche es noch einmal.", true))),
                Arguments.of(Named.of(task.id() + " / done", LearningDialogs.completed("Lio", PLAYER, task, RewardCatalog.forTask(task))))));
        LearningProgress notes = new LearningProgress();
        notes.playerName = "Beispielkind";
        for (var task : LearningCatalog.all()) {
            if (!task.patterns().isEmpty()) notes.recordAnswer(task, task.patterns().getFirst().answers().iterator().next(), 1, false);
            notes.recordHint(task, 2);
            notes.recordAnswer(task, task.expectedAnswer(), 3, true);
        }
        Stream<Arguments> menus = Stream.of(
                Arguments.of(Named.of("travel menu", LearningDialogs.menu("Mila", PLAYER))),
                Arguments.of(Named.of("practice completion", LearningDialogs.completed("Mila", PLAYER,
                        LearningCatalog.all().getFirst(), null))),
                Arguments.of(Named.of("parent report", ParentCommands.reportDialog(notes, PLAYER, true))));
        return Stream.of(residents, quests, learning, menus).flatMap(stream -> stream);
    }

    @ParameterizedTest(name = "{0}: direct dialog survives the game packet codec")
    @MethodSource("definitions")
    void directDeliveryPreservesResidentAndQuestDialogsThroughTheGamePacket(JsonObject definition) {
        Holder<Dialog> holder = DialogDelivery.dialog(definition);
        assertEquals(Holder.Kind.DIRECT, holder.kind(), "Opening a dialog must not use a command target selector.");
        ClientboundShowDialogPacket packet = new ClientboundShowDialogPacket(holder);
        RegistryAccess registries = new RegistryAccess.ImmutableRegistryAccess(List.of(dialogRegistry)).freeze();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            ClientboundShowDialogPacket.STREAM_CODEC.encode(buffer, packet);
            assertTrue(buffer.readableBytes() > 0);
            ClientboundShowDialogPacket decoded = ClientboundShowDialogPacket.STREAM_CODEC.decode(buffer);
            assertFalse(buffer.isReadable(), "The client must consume the complete dialog packet.");
            assertEquals(Holder.Kind.DIRECT, decoded.dialog().kind());
            assertEquals(canonical(holder.value()), canonical(decoded.dialog().value()),
                    "Text, choices, input fields and action commands must survive network delivery.");
        } finally {
            buffer.release();
        }
    }

    @Test
    void minecraftDialogCommandRejectsTheFormerUuidTargetBeforeReadingTheDialog() {
        CommandBuildContext context = CommandBuildContext.simple(
                HolderLookup.Provider.create(Stream.of(dialogRegistry)), FeatureFlags.VANILLA_SET);
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        DialogCommand.register(dispatcher, context);
        // A real owner source suffices for parsing; no server, world or player is created or executed.
        CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO,
                null, LevelBasedPermissionSet.OWNER, Component.literal("Server"), null).withSuppressedOutput();
        JsonObject definition = VillageDialogs.conversation(VillageJobs.all().getFirst(),
                new VillageState.Project(), PLAYER, null);

        var formerCommand = dispatcher.parse("dialog show " + PLAYER + " " + definition, source);
        assertEquals(List.of("targets"), formerCommand.getExceptions().keySet().stream()
                .map(node -> node.getName()).toList());
        assertEquals(List.of("dialog", "show"), formerCommand.getContext().getNodes().stream()
                .map(node -> node.getNode().getName()).toList());

        // Same permission source and definition: only the target changes, isolating the UUID bug.
        var namedTarget = dispatcher.parse("dialog show Learner " + definition, source);
        assertTrue(namedTarget.getExceptions().isEmpty());
        assertFalse(namedTarget.getReader().canRead());
        assertNotNull(namedTarget.getContext().getCommand());
        assertEquals(List.of("dialog", "show", "targets", "dialog"), namedTarget.getContext().getNodes()
                .stream().map(node -> node.getNode().getName()).toList());
    }

    private static JsonObject canonical(Dialog dialog) {
        return Dialog.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, dialog).getOrThrow().getAsJsonObject();
    }
}
