package de.kompass4mc.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestCatalogTest {
    @Test
    void catalogCanResolveEveryStationAndRejectUnknownIds() {
        List<Quest> quests = QuestCatalog.all();
        assertEquals(12, quests.size());
        assertEquals(12, new HashSet<>(quests.stream().map(Quest::id).toList()).size());
        assertEquals(6, quests.stream().filter(quest -> quest.subject() == Subject.MATH).count());
        assertEquals(6, quests.stream().filter(quest -> quest.subject() == Subject.GERMAN).count());
        for (Quest quest : quests) {
            assertSame(quest, QuestCatalog.byId(quest.id()).orElseThrow());
            assertTrue(quest.matchesAnswer(quest.expectedAnswer()), quest.id());
            if (!quest.choices().isEmpty()) {
                int answerIndex = quest.choices().indexOf(quest.expectedAnswer());
                for (int i = 0; i < quest.choices().size(); i++) {
                    assertEquals(i == answerIndex, quest.matchesAnswer(Character.toString('A' + i)), quest.id());
                }
            }
        }
        assertTrue(QuestCatalog.byId("unknown").isEmpty());
        assertTrue(QuestCatalog.byId(null).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> quests.clear());
    }

    @Test
    void earlierSubtractionWorksheetsKeepTheSameResults() {
        assertTrue(QuestCatalog.byId("math_subtract_1").orElseThrow().matchesAnswer("624"));
        assertFalse(QuestCatalog.byId("math_subtract_1").orElseThrow().matchesAnswer("625"));
        assertTrue(QuestCatalog.byId("math_subtract_2").orElseThrow().matchesAnswer("3.154"));
        assertFalse(QuestCatalog.byId("math_subtract_2").orElseThrow().matchesAnswer("3.254"));
    }
}
