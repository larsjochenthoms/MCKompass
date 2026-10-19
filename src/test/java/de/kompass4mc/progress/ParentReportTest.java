package de.kompass4mc.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.learning.LearningTask;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParentReportTest {
    @Test
    void repeatedWrongAnswersToOneTaskAreOnlyOneIndependentObservation() {
        LearningProgress progress = new LearningProgress();
        for (int index = 0; index < 10; index++) {
            progress.recordAnswer(task("subtract_borrow_1"), "4846", index, false);
        }
        List<ParentReport.PatternObservation> observations = ParentReport.observations(progress);

        assertEquals(1, observations.size());
        assertEquals(1, observations.getFirst().evidence().size());
        assertFalse(observations.getFirst().possibleMisconception());
        String html = ParentReport.html(progress, "Kind");
        assertTrue(html.contains("Einzelbeobachtung, noch kein hinreichendes Muster."));
        assertFalse(html.contains("Möglicher Gesprächsanlass – noch kein Nachweis eines Misskonzepts."));
    }

    @Test
    void twoDistinctMatchedTasksAreAHypothesisWithConcreteEvidenceNeverADiagnosis() {
        LearningProgress progress = new LearningProgress();
        progress.recordAnswer(task("subtract_borrow_1"), "4846", 1L, false);
        progress.recordAnswer(task("subtract_borrow_2"), "356", 2L, false);
        ParentReport.PatternObservation observation = ParentReport.observations(progress).getFirst();

        assertTrue(observation.possibleMisconception());
        assertEquals("borrowing", observation.id());
        assertEquals(2, observation.evidence().size());
        String html = ParentReport.html(progress, "Kind");
        assertTrue(html.contains("Möglicher Gesprächsanlass – noch kein Nachweis eines Misskonzepts."));
        assertTrue(html.contains("Antwort: <q>4846</q> · Erwartet: <q>3154</q>"));
        assertTrue(html.contains("Antwort: <q>356</q> · Erwartet: <q>244</q>"));
        assertTrue(html.contains("Lass dir erklären, wie das Kind zu dieser Antwort gekommen ist."));
    }

    @Test
    void laterCorrectAnswersAreVisibleAndDoNotCreateNewEvidenceOrClaimMastery() {
        LearningProgress progress = new LearningProgress();
        progress.recordAnswer(task("subtract_borrow_1"), "4846", 1L, false);
        progress.recordAnswer(task("subtract_borrow_2"), "244", 2L, false);
        assertFalse(ParentReport.observations(progress).getFirst().possibleMisconception());
        progress.recordAnswer(task("subtract_borrow_1"), "3154", 3L, false);

        assertEquals(1, ParentReport.observations(progress).getFirst().evidence().size());
        String html = ParentReport.html(progress, "Kind");
        assertTrue(html.contains("Nach diesem letzten passenden Fehlversuch mindestens einmal richtig beantwortet: ja."));
        assertTrue(html.contains("letzte passende Fehlantwort als Beispiel sichtbar"));
        assertTrue(ParentReport.summary(progress).stream()
                .anyMatch(line -> line.contains("Frühere Fehlversuche: 1/1 danach mindestens einmal richtig beantwortet")));
    }

    @Test
    void correctThenLaterWrongIsNeverReportedAsCorrectedAfterTheError() {
        LearningProgress progress = new LearningProgress();
        progress.recordAnswer(task("subtract_borrow_1"), "3154", 1L, false);
        progress.recordAnswer(task("subtract_borrow_1"), "4846", 2L, false);

        String html = ParentReport.html(progress, "Kind");
        assertTrue(html.contains("Nach diesem letzten passenden Fehlversuch mindestens einmal richtig beantwortet: nicht beobachtet."));
        assertFalse(html.contains("Nach diesem letzten passenden Fehlversuch mindestens einmal richtig beantwortet: ja."));
        assertTrue(ParentReport.summary(progress).stream().noneMatch(line -> line.startsWith("Frühere Fehlversuche:")));
    }

    @Test
    void anotherPatternErrorAfterCorrectionUpdatesEvidenceWithoutClaimingItWasCorrected() {
        LearningProgress progress = new LearningProgress();
        progress.recordAnswer(task("subtract_borrow_1"), "4846", 1L, false);
        progress.recordAnswer(task("subtract_borrow_1"), "3154", 2L, false);
        progress.recordAnswer(task("subtract_borrow_1"), "4846", 3L, false);

        assertEquals(3L, ParentReport.observations(progress).getFirst().evidence().getFirst().timeMillis());
        assertEquals(1, ParentReport.observations(progress).getFirst().evidence().size());
        assertEquals(2L, progress.stats.get("subtract_borrow_1").lastCorrectAt);
        String html = ParentReport.html(progress, "Kind");
        assertTrue(html.contains("Nach diesem letzten passenden Fehlversuch mindestens einmal richtig beantwortet: nicht beobachtet."));
        assertFalse(html.contains("Nach diesem letzten passenden Fehlversuch mindestens einmal richtig beantwortet: ja."));
        assertTrue(html.contains("nicht das Ergebnis der aktuellsten Wiederholung"));
        assertTrue(html.contains("es können weitere Fehler gefolgt sein"));
    }

    @Test
    void lastCorrectAtUsesLatestAnswerWhileOlderSavedNotesFallBackToFirstSolveTime() {
        LearningProgress progress = new LearningProgress();
        progress.recordAnswer(task("subtract_borrow_1"), "3154", 1L, false);
        progress.recordAnswer(task("subtract_borrow_1"), "4846", 2L, false);
        progress.recordAnswer(task("subtract_borrow_1"), "3154", 3L, false);
        assertEquals(1L, progress.stats.get("subtract_borrow_1").solvedAt);
        assertEquals(3L, progress.stats.get("subtract_borrow_1").lastCorrectAt);
        assertTrue(ParentReport.html(progress, "Kind").contains(
                "Nach diesem letzten passenden Fehlversuch mindestens einmal richtig beantwortet: ja."));

        progress.stats.get("subtract_borrow_1").lastCorrectAt = null;
        assertTrue(ParentReport.html(progress, "Kind").contains(
                "Nach diesem letzten passenden Fehlversuch mindestens einmal richtig beantwortet: nicht beobachtet."));
    }

    @Test
    void aCorrectAnswerThatIsAWrongAnswerForAnotherTaskDoesNotCreateEvidence() {
        LearningProgress progress = new LearningProgress();
        // 28 is correct in the first task, but a specific area/perimeter mix-up in the second.
        progress.recordAnswer(task("perimeter_garden_1"), "28", 1L, false);
        progress.recordAnswer(task("perimeter_garden_2"), "28", 2L, false);
        ParentReport.PatternObservation observation = ParentReport.observations(progress).getFirst();

        assertEquals("area_perimeter", observation.id());
        assertEquals(1, observation.evidence().size());
        assertEquals("perimeter_garden_2", observation.evidence().getFirst().task().id());
        assertFalse(observation.possibleMisconception());
    }

    @Test
    void unknownWrongAnswersAreReportedWithoutInventingAConceptualExplanation() {
        LearningProgress progress = new LearningProgress();
        progress.recordAnswer(task("subtract_borrow_1"), "999999", 1L, false);
        progress.recordAnswer(task("subtract_borrow_2"), "keine Ahnung", 2L, false);
        assertTrue(ParentReport.observations(progress).isEmpty());
        assertTrue(ParentReport.html(progress, "Kind").contains("2 Fehlantworten in den gespeicherten letzten 2 Ereignissen"));
        assertTrue(ParentReport.summary(progress).stream()
                .anyMatch(line -> line.contains("davon ohne hinterlegtes Muster: 2")));
    }

    @Test
    void htmlEscapesPlayerNamesAnswersNotesAndArchivedIdentifiers() {
        LearningProgress progress = new LearningProgress();
        String malicious = "<script>alert(\"x\")</script>&'";
        progress.playerName = malicious;
        progress.recordAnswer(task("subtract_store"), malicious, 1L, false);
        progress.recordProceduralIssue(task("subtract_store"), "<img src=x onerror=alert(1)>", 2L);
        progress.stats.put("old_<b>unsafe</b>", new LearningProgress.TaskStats());
        String html = ParentReport.html(progress, malicious);

        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<img src="));
        assertFalse(html.contains("old_<b>unsafe</b>"));
        assertTrue(html.contains("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;&amp;&#39;"));
        assertTrue(html.contains("&lt;img src=x onerror=alert(1)&gt;"));
        assertTrue(html.contains("old_&lt;b&gt;unsafe&lt;/b&gt;"));
        assertTrue(html.contains("default-src 'none'"));
        assertTrue(html.contains("<meta charset=\"utf-8\">"));
    }

    @Test
    void lastFiftyEventsAreShownWhileTotalsStillCoverAllEvents() {
        LearningProgress progress = new LearningProgress();
        for (int index = 0; index < 60; index++) {
            progress.recordProceduralIssue(task("subtract_store"), "EVENT_" + index + "_END", index);
        }
        String html = ParentReport.html(progress, "Kind");
        assertTrue(html.contains("Letzte 50 Ereignisse"));
        assertFalse(html.contains("EVENT_9_END"));
        assertTrue(html.contains("EVENT_10_END"));
        assertTrue(html.contains("EVENT_59_END"));
        assertTrue(html.indexOf("EVENT_59_END") < html.indexOf("EVENT_10_END"));
        assertTrue(html.contains("60 Bedienhinweise"));
    }

    @Test
    void fullCatalogOfWrongResponsesKeepsDialogSummaryBounded() {
        LearningProgress progress = new LearningProgress();
        for (LearningTask task : LearningCatalog.all()) {
            String response = task.patterns().isEmpty() ? "unbekannt"
                    : task.patterns().getFirst().answers().iterator().next();
            progress.recordAnswer(task, response, 1L, false);
        }
        List<String> summary = ParentReport.summary(progress);
        assertTrue(summary.size() <= 38, "The Minecraft view must remain a compact overview.");
        assertTrue(summary.stream().anyMatch(line -> line.contains("keine Diagnose")));
        assertTrue(summary.stream().anyMatch(line -> line.contains("HTML-Bericht")));
    }

    @Test
    void completedFullCatalogStillShowsPossibleMisconceptionInTheBoundedGameSummary() {
        LearningProgress progress = new LearningProgress();
        progress.recordAnswer(task("subtract_borrow_1"), "4846", 1L, false);
        progress.recordAnswer(task("subtract_borrow_2"), "356", 2L, false);
        for (LearningTask task : LearningCatalog.all()) {
            progress.recordAnswer(task, task.expectedAnswer(), 3L, false);
        }

        List<String> summary = ParentReport.summary(progress);

        assertTrue(summary.size() <= 38, "Completed areas must not use the observation budget.");
        assertTrue(summary.stream().anyMatch(line -> line.startsWith("Möglicher Gesprächsanlass:")),
                "Parents must see a two-task observation even after practicing every area.");
        assertTrue(summary.stream().anyMatch(line -> line.startsWith("Beispiel:")));
        assertTrue(summary.stream().anyMatch(line -> line.contains("Frühere Fehlversuche: 2/2")));
        assertTrue(summary.stream().anyMatch(line -> line.contains("Hypothese, keine Diagnose")));
        assertTrue(summary.stream().anyMatch(line -> line.contains("Wiederholte Klicks zählen nicht")));
    }

    @Test
    void fullCatalogManyCorrectedPatternsAndArchivedNotesKeepFirstObservationAndWarnings() {
        LearningProgress progress = new LearningProgress();
        for (LearningTask task : LearningCatalog.all()) {
            if (!task.patterns().isEmpty()) {
                progress.recordAnswer(task, task.patterns().getFirst().answers().iterator().next(), 1L, false);
            }
            progress.recordAnswer(task, task.expectedAnswer(), 2L, false);
        }
        progress.stats.put("earlier_task", new LearningProgress.TaskStats());

        List<String> summary = ParentReport.summary(progress);

        assertTrue(summary.size() <= 38);
        assertTrue(summary.stream().anyMatch(line -> line.startsWith("Möglicher Gesprächsanlass:")));
        assertTrue(summary.stream().anyMatch(line -> line.contains("Weitere Beobachtungen stehen im HTML-Bericht")));
        assertTrue(summary.stream().anyMatch(line -> line.contains("frühere Aufgaben sind gespeichert")));
        assertTrue(summary.stream().anyMatch(line -> line.contains("Hypothese, keine Diagnose")));
        assertTrue(summary.stream().anyMatch(line -> line.contains("Wiederholte Klicks zählen nicht")));
    }

    @Test
    void emptyProgressProducesAUsefulReportWithoutSuggestingMastery() {
        LearningProgress progress = new LearningProgress();
        String html = ParentReport.html(progress, "Kind");
        assertTrue(html.startsWith("<!doctype html>"));
        assertTrue(html.endsWith("</body></html>"));
        assertTrue(html.contains("Noch keine Lerninteraktion gespeichert."));
        assertTrue(html.contains("keine sichere Aussage"));
        assertTrue(html.contains("2 Smaragde"));
        assertTrue(html.contains("4 Smaragde"));
        assertTrue(html.contains("6 Smaragde"));
    }

    private static LearningTask task(String id) {
        return LearningCatalog.byId(id).orElseThrow();
    }
}
