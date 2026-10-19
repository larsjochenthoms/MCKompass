package de.kompass4mc.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestTest {
    @Test
    void numericAnswersAllowGermanThousandsSeparatorsWithoutIgnoringMalformedNumbers() {
        Quest quest = numericQuest("3154");
        for (String answer : List.of("3154", "3.154", "3 154", " 3\u00a0154 ", "3\u202f154", "+3154", "003154")) {
            assertTrue(quest.matchesAnswer(answer), answer);
        }
        for (String answer : List.of("31.54", "3,154", "3.154,0", "31 54", "3 1 5 4", "3.154 Steine", "-3154", "3154.0", "3e154", "", "3.154.000")) {
            assertFalse(quest.matchesAnswer(answer), answer);
        }
        assertFalse(quest.matchesAnswer(null));
        assertFalse(numericQuest("1234567").matchesAnswer("1.234 567"));
        assertTrue(numericQuest("1234567").matchesAnswer("1.234.567"));
    }

    @Test
    void numbersCannotOverflowIntoAnotherAnswer() {
        Quest quest = numericQuest("24");
        assertFalse(quest.matchesAnswer("18446744073709551640"));
        assertTrue(numericQuest("18446744073709551640").matchesAnswer("18.446.744.073.709.551.640"));
    }

    @Test
    void choicesAcceptLettersAndTextButNeverAnIncorrectOption() {
        Quest quest = textQuest(List.of("Hunt", "Hund", "Hunn"), "Hund");
        assertTrue(quest.matchesAnswer("B"));
        assertTrue(quest.matchesAnswer(" b "));
        assertTrue(quest.matchesAnswer("HUND"));
        assertFalse(quest.matchesAnswer("A"));
        assertFalse(quest.matchesAnswer("C"));
        assertFalse(quest.matchesAnswer("D"));
        assertFalse(quest.matchesAnswer("2"));
        assertFalse(quest.matchesAnswer("Hund."));
    }

    @Test
    void freeTextNormalizesGermanCaseUnicodeAndWhitespaceButPreservesSpelling() {
        Quest quest = textQuest(List.of(), "Die schöne Straße");
        assertTrue(quest.matchesAnswer("  DIE\nSCHÖNE\tSTRAßE  "));
        assertTrue(quest.matchesAnswer("Die scho\u0308ne Straße"));
        assertTrue(quest.matchesAnswer("Die\u00a0schöne\u202fStraße"));
        assertFalse(quest.matchesAnswer("Die schone Straße"));
        assertFalse(quest.matchesAnswer("Die schöne Strasse"));
    }

    @Test
    void badCatalogDataFailsBeforeAQuestCanBeShown() {
        assertThrows(IllegalArgumentException.class, () -> textQuest(List.of("Hund", "Hund"), "Hund"));
        assertThrows(IllegalArgumentException.class, () -> textQuest(List.of("Hund", " HUND "), "Hund"));
        assertThrows(IllegalArgumentException.class, () -> textQuest(List.of("Hunt", "Hunn"), "Hund"));
        assertThrows(IllegalArgumentException.class, () -> textQuest(List.of("Hund"), "Hund"));
        assertThrows(IllegalArgumentException.class, () -> textQuest(List.of(), "\u00a0"));
        assertThrows(IllegalArgumentException.class, () -> new Quest("Bad Id", "Titel", "Ort", "Text", "Frage", List.of(), "1", "Hinweis", "Lösung", Subject.MATH, 3));
        assertThrows(IllegalArgumentException.class, () -> new Quest("valid", "Titel", "Ort", "Text", "Frage", List.of(), "1", "Hinweis", "Lösung", Subject.MATH, 0));
    }

    @Test
    void answerChoicesAreCopiedInsteadOfSharingMutableCallerState() {
        List<String> source = new ArrayList<>(List.of("Hunt", "Hund"));
        Quest quest = textQuest(source, "Hund");
        source.set(1, "Hunn");
        assertTrue(quest.matchesAnswer("B"));
        assertEquals("Hund", quest.choices().get(1));
        assertThrows(UnsupportedOperationException.class, () -> quest.choices().add("Hunn"));
    }

    private static Quest numericQuest(String answer) {
        return new Quest("number", "Titel", "Ort", "Text", "Frage", List.of(), answer, "Hinweis", "Lösung", Subject.MATH, 3);
    }

    private static Quest textQuest(List<String> choices, String answer) {
        return new Quest("text", "Titel", "Ort", "Text", "Frage", choices, answer, "Hinweis", "Lösung", Subject.GERMAN, 3);
    }
}
