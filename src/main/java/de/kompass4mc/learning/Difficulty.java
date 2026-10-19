package de.kompass4mc.learning;

/** Aufgabenbezogene Anforderung; keine Einstufung oder Bewertung des Kindes. */
public enum Difficulty {
    EASY("Einfach", 2),
    MEDIUM("Mittel", 4),
    HARD("Knifflig", 6);

    private final String label;
    private final int emeralds;

    Difficulty(String label, int emeralds) {
        this.label = label;
        this.emeralds = emeralds;
    }

    public String label() {
        return label;
    }

    public int emeralds() {
        return emeralds;
    }
}
