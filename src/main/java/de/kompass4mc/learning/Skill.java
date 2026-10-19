package de.kompass4mc.learning;

import de.kompass4mc.quest.Subject;

/** Überschaubare Übungsbereiche; keine vollständige Abbildung des Bildungsplans. */
public enum Skill {
    SUBTRACTION("Subtrahieren und Stellenwerte", Subject.MATH),
    MULTIPLICATION("Gleich große Gruppen", Subject.MATH),
    DIVISION("Gleichmäßig verteilen", Subject.MATH),
    LENGTH("Längen und Einheiten", Subject.MATH),
    PERIMETER("Umfang eines Rechtecks", Subject.MATH),
    READING_DETAIL("Informationen genau lesen", Subject.GERMAN),
    READING_CAUSE("Gründe im Text verstehen", Subject.GERMAN),
    READING_SEQUENCE("Reihenfolge verstehen", Subject.GERMAN),
    SPELLING("Schreibweisen begründen", Subject.GERMAN),
    GRAMMAR("Wörter und Sätze untersuchen", Subject.GERMAN),
    NUMBER_RELATIONS("Stellenwerte und Rechenwege", Subject.MATH),
    NUMBER_PATTERNS("Zahlenmuster erkennen", Subject.MATH),
    SPATIAL("Raum und Spiegelbilder", Subject.MATH),
    AREA("Flächen auslegen", Subject.MATH),
    TIME_MONEY("Zeit und Geld", Subject.MATH),
    DATA("Daten vergleichen", Subject.MATH),
    PROBABILITY("Zufall und Gewinnchancen", Subject.MATH),
    READING_EVIDENCE("Aussagen am Text belegen", Subject.GERMAN),
    READING_SUMMARY("Texte zusammenfassen", Subject.GERMAN),
    VOCABULARY("Wörter im Zusammenhang verstehen", Subject.GERMAN);

    private final String label;
    private final Subject subject;

    Skill(String label, Subject subject) {
        this.label = label;
        this.subject = subject;
    }

    public String label() {
        return label;
    }

    public Subject subject() {
        return subject;
    }
}
