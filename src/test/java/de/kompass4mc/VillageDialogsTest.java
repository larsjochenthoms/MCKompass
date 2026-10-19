package de.kompass4mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import de.kompass4mc.village.MissionJob;
import de.kompass4mc.village.VillageJobs;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.body.PlainMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class VillageDialogsTest {
    private static final String PLAYER = "216990a7-2351-48ac-b8ee-82327e23179b";

    private enum Stage {
        OFFER, ACCEPTED, COMPLETED_CONTRIBUTOR, REWARD_CLAIMED, COMPLETED_NONCONTRIBUTOR, COMPLETED_BYSTANDER
    }

    @BeforeAll
    static void bootstrapMinecraftCodecs() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static Stream<Arguments> conversations() {
        return VillageJobs.all().stream().flatMap(job -> Stream.of(Stage.values())
                .map(stage -> Arguments.of(Named.of(job.id() + " / " + stage, job), stage)));
    }

    @ParameterizedTest(name = "{0}: native dialog, world actions and safe Escape")
    @MethodSource("conversations")
    void everyResidentStageDecodesWithoutNumericInputsAndClosesWithoutAnAction(MissionJob job, Stage stage) {
        MultiActionDialog dialog = decode(VillageDialogs.conversation(job, project(stage), PLAYER, null));

        assertEquals(job.residentName() + " · " + job.role(), dialog.common().title().getString());
        assertTrue(bodyText(dialog).contains(job.title()));
        assertTrue(dialog.common().inputs().isEmpty(), "Village jobs are completed by actions in the world.");
        assertTrue(dialog.common().canCloseWithEscape());
        assertTrue(dialog.exitAction().isPresent());
        assertTrue(dialog.exitAction().orElseThrow().action().isEmpty(),
                "Leaving a conversation must not accept, inspect, deliver or claim a reward.");
        assertFalse(dialog.actions().isEmpty());
        assertTrue(commands(dialog).stream().noneMatch(command -> command.startsWith("kompass antwort")));
        assertTrue(commands(dialog).stream().allMatch(command -> command.startsWith("kompass bewohner " + job.id() + " ")));

        List<String> expected = switch (stage) {
            case OFFER -> List.of("kompass bewohner " + job.id() + " annehmen");
            case ACCEPTED -> List.of("kompass bewohner " + job.id() + " pruefen", "kompass bewohner " + job.id() + " tipp");
            case COMPLETED_CONTRIBUTOR -> List.of("kompass bewohner " + job.id() + " danke");
            case REWARD_CLAIMED, COMPLETED_NONCONTRIBUTOR, COMPLETED_BYSTANDER -> List.of();
        };
        assertEquals(expected, commands(dialog));
        if (stage == Stage.OFFER) {
            assertTrue(bodyText(dialog).contains(job.request()));
        } else if (stage == Stage.ACCEPTED) {
            assertTrue(bodyText(dialog).contains(job.acceptedText()));
        } else {
            assertTrue(bodyText(dialog).contains(job.completionText()));
        }
    }

    @ParameterizedTest(name = "{0}: feedback stays visible without changing permitted actions")
    @MethodSource("conversations")
    void feedbackDoesNotGrantNewActionsOrReintroduceAnswerForms(MissionJob job, Stage stage) {
        VillageState.Project project = project(stage);
        String feedback = "Noch fehlen 2 Bretter.\nTipp: „Länge & Breite“ <genau> lesen!";
        MultiActionDialog original = decode(VillageDialogs.conversation(job, project, PLAYER, null));
        MultiActionDialog retry = decode(VillageDialogs.conversation(job, project, PLAYER, feedback));

        assertTrue(bodyText(retry).contains(feedback));
        assertEquals(bodyText(original).size() + 1, bodyText(retry).size());
        assertEquals(commands(original), commands(retry));
        assertTrue(retry.common().inputs().isEmpty());
        assertEquals(bodyText(original), bodyText(decode(VillageDialogs.conversation(job, project, PLAYER, " \n "))));
    }

    private static VillageState.Project project(Stage stage) {
        VillageState.Project project = new VillageState.Project();
        if (stage != Stage.OFFER && stage != Stage.COMPLETED_BYSTANDER) {
            project.accept(PLAYER);
        }
        if (stage == Stage.COMPLETED_CONTRIBUTOR || stage == Stage.REWARD_CLAIMED) {
            project.contribute(PLAYER);
        }
        if (stage == Stage.COMPLETED_CONTRIBUTOR || stage == Stage.REWARD_CLAIMED
                || stage == Stage.COMPLETED_NONCONTRIBUTOR || stage == Stage.COMPLETED_BYSTANDER) {
            project.completed = true;
        }
        if (stage == Stage.REWARD_CLAIMED) {
            project.claim(PLAYER);
        }
        return project;
    }

    private static MultiActionDialog decode(JsonObject json) {
        return assertInstanceOf(MultiActionDialog.class,
                Dialog.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    private static List<String> bodyText(MultiActionDialog dialog) {
        return dialog.common().body().stream()
                .map(body -> assertInstanceOf(PlainMessage.class, body).contents().getString())
                .toList();
    }

    private static List<String> commands(MultiActionDialog dialog) {
        return dialog.actions().stream().map(ActionButton::action)
                .flatMap(java.util.Optional::stream)
                .map(action -> action.createAction(Map.of()).orElseThrow())
                .map(event -> assertInstanceOf(ClickEvent.RunCommand.class, event).command())
                .toList();
    }
}
