package de.kompass4mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.progress.LearningProgress;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.body.PlainMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParentCommandsTest {
    private static final String PLAYER = "216990a7-2351-48ac-b8ee-82327e23179b";

    @TempDir
    Path directory;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void reportExportUsesUuidFilenameWritesUtf8AndAtomicallyReplacesAnExistingReport() throws IOException {
        Path report = ParentCommands.writeReport(directory, PLAYER, "<html>Mädchen & Äpfel</html>");
        assertEquals(directory.resolve("kompass-berichte").resolve(PLAYER + ".html"), report);
        assertEquals("<html>Mädchen & Äpfel</html>", Files.readString(report, StandardCharsets.UTF_8));
        ParentCommands.writeReport(directory, PLAYER, "Neue Übersicht");
        assertEquals("Neue Übersicht", Files.readString(report, StandardCharsets.UTF_8));
        try (var files = Files.list(report.getParent())) {
            assertEquals(List.of(report), files.toList(), "A completed export must leave no temporary files.");
        }
    }

    @Test
    void playerNameCannotBecomeAReportFilenameOrTraversalPath() {
        assertThrows(IllegalArgumentException.class,
                () -> ParentCommands.writeReport(directory, "../../outside", "private"));
        assertFalse(Files.exists(directory.resolve("kompass-berichte")));
    }

    @Test
    void playerLookupIsCaseInsensitiveAndKeepsAmbiguousNamesSeparate() {
        Map<String, LearningProgress> notes = new LinkedHashMap<>();
        LearningProgress first = new LearningProgress();
        first.playerName = "Lina";
        LearningProgress second = new LearningProgress();
        second.playerName = "LINA";
        LearningProgress third = new LearningProgress();
        third.playerName = "Amir";
        notes.put("one", first);
        notes.put("two", second);
        notes.put("three", third);
        assertEquals(List.of("one", "two"), ParentCommands.savedMatches(notes, "lina"));
        assertEquals(List.of("three"), ParentCommands.savedMatches(notes, "Amir"));
        assertTrue(ParentCommands.savedMatches(notes, "unknown").isEmpty());
    }

    @Test
    void encounterSettingIsSavedAndRolledBackIfSavingFails() throws IOException {
        Path statePath = directory.resolve("world/kompass4mc.json");
        WorldState state = new WorldState(statePath);
        ParentCommands.saveEncounterSetting(state, false);
        assertFalse(new WorldState(statePath).automaticLearningNpcs());

        Path blocked = directory.resolve("blocked");
        WorldState unsavable = new WorldState(blocked.resolve("kompass4mc.json"));
        Files.writeString(blocked, "This is a file, not a world directory.");
        assertTrue(unsavable.automaticLearningNpcs());
        assertThrows(IOException.class, () -> ParentCommands.saveEncounterSetting(unsavable, false));
        assertTrue(unsavable.automaticLearningNpcs(), "A save failure must preserve the previous setting.");
    }

    @Test
    void nativeReportDialogDisplaysNotesAndExportsOnlyTheSelectedUuid() {
        LearningProgress progress = new LearningProgress();
        progress.playerName = "Name with \"command\" text";
        progress.recordAnswer(LearningCatalog.byId("subtract_store").orElseThrow(), "624", 1L, false);
        MultiActionDialog dialog = assertInstanceOf(MultiActionDialog.class,
                Dialog.DIRECT_CODEC.parse(JsonOps.INSTANCE,
                        ParentCommands.reportDialog(progress, PLAYER, true)).getOrThrow());
        assertTrue(dialog.common().title().getString().contains(progress.playerName));
        List<String> body = dialog.common().body().stream()
                .map(part -> assertInstanceOf(PlainMessage.class, part).contents().getString()).toList();
        assertTrue(body.stream().anyMatch(line -> line.contains("1 Aufgaben gelöst")));
        assertTrue(body.stream().anyMatch(line -> line.contains("Nur neue Lernreisende")));
        assertEquals(1, dialog.actions().size());
        var export = dialog.actions().getFirst().action().orElseThrow().createAction(Map.of()).orElseThrow();
        assertEquals("kompass eltern export_uuid " + PLAYER,
                assertInstanceOf(ClickEvent.RunCommand.class, export).command());
        assertTrue(dialog.exitAction().orElseThrow().action().isEmpty());
    }

    @Test
    void commandTreeCanBeRegisteredBeforeWorldServicesExist() {
        var command = ParentCommands.registration(() -> null).build();
        assertEquals("eltern", command.getName());
        assertTrue(command.getChildren().stream().anyMatch(child -> child.getName().equals("spieler")));
        assertTrue(command.getChildren().stream().anyMatch(child -> child.getName().equals("begegnungen")));
        assertTrue(command.getChildren().stream().anyMatch(child -> child.getName().equals("export")));
    }
}
