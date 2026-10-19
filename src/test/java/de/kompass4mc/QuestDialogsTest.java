package de.kompass4mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import de.kompass4mc.quest.Quest;
import de.kompass4mc.quest.QuestCatalog;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.CommandTemplate;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.TextInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class QuestDialogsTest {
    @BeforeAll
    static void bootstrapMinecraftCodecs() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static Stream<Arguments> quests() {
        return QuestCatalog.all().stream().map(quest -> Arguments.of(Named.of(quest.id(), quest)));
    }

    @ParameterizedTest(name = "{0}: native dialog and safe close")
    @MethodSource("quests")
    void everyQuestIsAcceptedByMinecraftAndCanCloseWithoutSubmitting(Quest quest) {
        MultiActionDialog dialog = decode(QuestDialogs.quest(quest, null));

        assertEquals(quest.title(), dialog.common().title().getString());
        assertTrue(dialog.common().canCloseWithEscape());
        assertTrue(dialog.exitAction().isPresent());
        assertTrue(dialog.exitAction().orElseThrow().action().isEmpty(),
                "Closing a quest must not answer it or request a hint.");
        assertTrue(bodyText(dialog).contains(quest.story()));
        assertTrue(bodyText(dialog).contains(quest.question()));
        assertTrue(commands(dialog, Map.of()).contains("kompass hinweis"));
    }

    @ParameterizedTest(name = "{0}: feedback survives native decoding")
    @MethodSource("quests")
    void feedbackIsVisibleAndDoesNotChangeTheQuestionOrAnswerCommands(Quest quest) {
        String feedback = "Noch einmal versuchen!\nTipp: „Einer & Zehner“ <bleiben> erhalten.";
        MultiActionDialog original = decode(QuestDialogs.quest(quest, null));
        MultiActionDialog retry = decode(QuestDialogs.quest(quest, feedback));

        assertTrue(bodyText(retry).contains(feedback));
        assertTrue(bodyText(retry).contains(quest.question()));
        assertEquals(bodyText(original).size() + 1, bodyText(retry).size());
        Map<String, Action.ValueGetter> answer = Map.of("answer", Action.ValueGetter.of("3154"));
        assertEquals(commands(original, answer), commands(retry, answer));
        assertEquals(bodyText(original), bodyText(decode(QuestDialogs.quest(quest, " \n "))));
    }

    @ParameterizedTest(name = "{0}: usable answer controls")
    @MethodSource("quests")
    void answerControlsProduceCommandsForTheCorrectQuest(Quest quest) {
        MultiActionDialog dialog = decode(QuestDialogs.quest(quest, null));
        if (quest.choices().isEmpty()) {
            assertEquals(1, dialog.common().inputs().size());
            var input = dialog.common().inputs().getFirst();
            assertEquals("answer", input.key());
            TextInput textInput = assertInstanceOf(TextInput.class, input.control());
            assertTrue(textInput.maxLength() >= quest.expectedAnswer().length());
            assertFalse(textInput.label().getString().isBlank());
            assertEquals(1, dialog.actions().stream()
                    .flatMap(button -> button.action().stream())
                    .filter(CommandTemplate.class::isInstance).count());
            for (String answer : List.of(quest.expectedAnswer(), "3.154", "3 154", "-42")) {
                assertTrue(commands(dialog, Map.of("answer", Action.ValueGetter.of(answer)))
                        .contains("kompass antwort " + quest.id() + " " + answer));
            }
        } else {
            assertTrue(dialog.common().inputs().isEmpty());
            List<String> commands = commands(dialog, Map.of());
            assertEquals(quest.choices().size() + 1, commands.size());
            for (int index = 0; index < quest.choices().size(); index++) {
                String letter = Character.toString('A' + index);
                assertEquals("kompass antwort " + quest.id() + " " + letter, commands.get(index));
                assertTrue(dialog.actions().get(index).button().label().getString()
                        .contains(quest.choices().get(index)));
            }
        }
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

    private static List<String> commands(MultiActionDialog dialog, Map<String, Action.ValueGetter> values) {
        return dialog.actions().stream().map(ActionButton::action)
                .flatMap(java.util.Optional::stream)
                .map(action -> action.createAction(values).orElseThrow())
                .map(event -> assertInstanceOf(ClickEvent.RunCommand.class, event).command())
                .toList();
    }
}
