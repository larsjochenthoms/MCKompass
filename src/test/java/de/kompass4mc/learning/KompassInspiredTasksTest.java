package de.kompass4mc.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.kompass4mc.progress.LearningProgress;
import de.kompass4mc.progress.ParentReport;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class KompassInspiredTasksTest {
    @Test
    void newMathTasksHaveIndependentlyCalculatedAnswers() {
        number("numbers_packs", 4 * 100 + 17 * 10 + 6);
        number("numbers_way", (260 + 380) / 2);
        int[] sequence = {90, 130, 120, 160, 150};
        for (int index = 1; index < sequence.length; index++) {
            assertEquals(index % 2 == 1 ? 40 : -10, sequence[index] - sequence[index - 1]);
        }
        number("pattern_steps", sequence[sequence.length - 1] + 40);
        assertTrue(task("strategy_stones").matchesAnswer("442 + 2 = " + (642 - 198)));
        number("space_layers", 3 * 2 + 2);
        number("space_mirror", 2 * 4 - 2);
        number("area_floor", 7 * 5);
        assertTrue(task("time_trip").matchesAnswer(LocalTime.of(9, 35).plusMinutes(50) + " Uhr"));
        number("money_trade", 32 - (3 * 7 + 4));
        number("data_harvest", 27 - 18);
        int winning = Integer.parseInt(task("chance_fair").expectedAnswer());
        assertEquals(2 * 9, winning * 6, "The winning proportions must be equal.");
        assertTrue(winning > 0 && winning < 9);
        assertTrue(task("chance_certain").matchesAnswer("Ich ziehe eine Eiche oder eine Birke."));
    }

    @Test
    void textEvidenceComesFromTheNumberedSentencesAndChoicesStayDistinct() {
        LearningTask mine = task("evidence_mine_1");
        assertTrue(mine.story().contains("(3) Weil der Eingang verschüttet ist, sucht sie einen anderen Weg."));
        assertTrue(mine.matchesAnswer("C"));
        assertFalse(mine.matchesAnswer("D"), "The discovery is a later result, not the cause.");
        LearningTask maps = task("evidence_maps_1");
        assertTrue(maps.story().contains("(2) Am Fluss rutscht er aus und fällt ins Wasser."));
        assertTrue(maps.story().contains("(3) Danach ist die Schrift auf beiden Karten so verwischt"));
        assertTrue(maps.matchesAnswer("D"));
        assertFalse(maps.matchesAnswer("C"), "Finding the village does not document the fall.");
        assertTrue(task("summary_walkway_1").matchesAnswer("Gemeinsam reparieren sie den Steg."));
        assertTrue(task("vocabulary_supplies_1").matchesAnswer("Es ist nur noch wenig da."));
        assertTrue(task("sequence_shelter_1").matchesAnswer("Er deckt das Dach mit Brettern."));
        assertTrue(task("spelling_trees_1").matchesAnswer("Bäume"));
        assertFalse(task("spelling_trees_1").matchesAnswer("Beume"));
    }

    @Test
    void openWordAnswersAcceptCaseAndWhitespaceButRejectDifferentForms() {
        for (String answer : List.of("tragen", " TRAGEN ", "Tragen")) {
            assertTrue(task("grammar_infinitive_1").matchesAnswer(answer));
        }
        assertFalse(task("grammar_infinitive_1").matchesAnswer("trägt"));
        assertTrue(task("compound_holder").matchesAnswer("Fackelhalter"));
        assertFalse(task("compound_holder").matchesAnswer("Halterfackel"));
        assertTrue(task("stem_building").matchesAnswer("bau"));
        assertFalse(task("stem_building").matchesAnswer("bauen"));
        assertTrue(task("adjective_lantern").matchesAnswer("hellsten"));
        assertFalse(task("adjective_lantern").matchesAnswer("heller"));
        assertTrue(task("grammar_finished_roof").matchesAnswer("Perfekt (Vergangenheit)"));
        assertTrue(task("grammar_time_phrase_1").matchesAnswer("am Abend"));
    }

    @Test
    void newAreasAreAvailableWithoutInventingDiagnosticPatterns() {
        Set<Skill> covered = KompassInspiredTasks.all().stream().map(LearningTask::skill)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(covered.containsAll(Set.of(Skill.NUMBER_RELATIONS, Skill.NUMBER_PATTERNS, Skill.SPATIAL,
                Skill.AREA, Skill.TIME_MONEY, Skill.DATA, Skill.PROBABILITY, Skill.READING_EVIDENCE,
                Skill.READING_SUMMARY, Skill.VOCABULARY)));
        assertEquals(24, KompassInspiredTasks.all().size());
        assertTrue(KompassInspiredTasks.all().stream().allMatch(task -> task.patterns().isEmpty()));
    }

    @Test
    void fullCatalogStillFitsParentSummaryIncludingArchivedTasks() {
        LearningProgress progress = new LearningProgress();
        for (LearningTask task : LearningCatalog.all()) {
            String wrong = task.patterns().isEmpty() ? "unbekannt" : task.patterns().getFirst().answers().iterator().next();
            progress.recordAnswer(task, wrong, 1L, false);
        }
        progress.stats.put("archived_task", new LearningProgress.TaskStats());
        List<String> summary = ParentReport.summary(progress);
        assertTrue(summary.size() <= 38, "New skills and an archived task must fit the parent dialog.");
        assertTrue(summary.stream().anyMatch(line -> line.contains("frühere Aufgaben")));
        assertTrue(summary.stream().anyMatch(line -> line.contains("keine Diagnose")));
        String html = ParentReport.html(progress, "Kind");
        assertTrue(html.contains("Aussagen am Text belegen"));
        assertTrue(html.contains("Zufall und Gewinnchancen"));
    }

    private static LearningTask task(String id) {
        return LearningCatalog.byId(id).orElseThrow();
    }

    private static void number(String id, int expected) {
        assertEquals(Integer.toString(expected), task(id).expectedAnswer(), id);
    }
}
