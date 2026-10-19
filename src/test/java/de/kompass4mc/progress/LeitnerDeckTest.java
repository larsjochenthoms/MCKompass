package de.kompass4mc.progress;

import static org.junit.jupiter.api.Assertions.*;
import de.kompass4mc.learning.*;
import java.util.Random;
import org.junit.jupiter.api.Test;

class LeitnerDeckTest {
    @Test void correctPromotesAndWrongAlwaysReturnsToFirstBox() {
        var p = new LearningProgress();
        var t = LearningCatalog.all().getFirst();
        for (int n = 0; n < 6; n++) {
            p.recordChallengeAnswer(t, t.expectedAnswer(), n + 1, false);
            assertEquals(Math.min(5, n + 2), p.cards.get(t.id()).box);
        }
        p.recordChallengeAnswer(t, "keine passende Antwort", 100, false);
        assertEquals(1, p.cards.get(t.id()).box);
        assertEquals(7, p.cards.get(t.id()).reviews);
        assertEquals(60_100, p.cards.get(t.id()).dueMillis);
        p.recordHint(t, 101);
        p.recordProceduralIssue(t, "Gegenstand fehlt", 102);
        assertEquals(7, p.cards.get(t.id()).reviews);
        p.validate();
    }
    @Test void rolesAndDueDatesRestrictRandomDrawsAndPreviousCardIsAvoided() {
        var p = new LearningProgress();
        var random = new Random(4721);
        for (var role : ChallengeRole.values()) {
            String previous = null;
            for (int i = 0; i < 200; i++) {
                var selected = LeitnerDeck.draw(p, role, 0, random, previous).orElseThrow();
                assertEquals(role, ChallengeRole.forTask(selected));
                assertNotEquals(previous, selected.id());
                previous = selected.id();
            }
        }
        for (var t : LearningCatalog.all()) LeitnerDeck.review(p, t.id(), true, 10);
        for (var role : ChallengeRole.values()) assertTrue(LeitnerDeck.draw(p, role, 100, random, null).isEmpty());
        assertTrue(LeitnerDeck.draw(p, ChallengeRole.JARO, 300_010, random, null).isPresent());
    }
    @Test void dueWeakCardsAreRepeatedDespiteManyUnseenCards() {
        var p = new LearningProgress();
        var t = LearningCatalog.all().stream().filter(c -> ChallengeRole.forTask(c) == ChallengeRole.JARO).findFirst().orElseThrow();
        LeitnerDeck.review(p, t.id(), false, 1);
        var random = new Random(9138);
        int count = 0;
        for (int n = 0; n < 1000; n++) if (LeitnerDeck.draw(p, ChallengeRole.JARO, 60_001, random, null).orElseThrow().id().equals(t.id())) count++;
        assertTrue(count > 650 && count < 850, "Repeated weak card: " + count);
    }
    @Test void copiesIsolateCardsAndOffersAndHintDoesNotLeakIntoNextOffer() {
        var p = new LearningProgress();
        var t = LearningCatalog.all().getFirst();
        p.recordHint(t, 1);
        p.recordChallengeAnswer(t, t.expectedAnswer(), 2, true);
        p.recordChallengeAnswer(t, t.expectedAnswer(), 3, false);
        assertFalse(p.attempts.getLast().hintUsed());
        var offer = new ChallengeOffer(t.id(), 3000);
        p.offers.put(t.npcName(), offer);
        p.validate();
        var copy = p.copy();
        copy.cards.get(t.id()).box = 1;
        copy.offers.get(t.npcName()).materialsIssued = true;
        assertEquals(3, p.cards.get(t.id()).box);
        assertFalse(p.offers.get(t.npcName()).materialsIssued);
    }
}
