package de.kompass4mc.learning;

/** NPC personalities are visible; subject labels and the card index stay behind the scenes. */
public enum ChallengeRole {
    MILA("Mila"), JARO("Jaro"), LIO("Lio");

    private final String name;
    ChallengeRole(String name) { this.name = name; }
    public String residentName() { return name; }

    public static ChallengeRole named(String name) {
        for (var role : values()) if (role.name.equals(name)) return role;
        throw new IllegalArgumentException("Unbekannter Bewohner: " + name);
    }

    public static ChallengeRole forTask(LearningTask task) {
        if (task.subject() == de.kompass4mc.quest.Subject.GERMAN) return LIO;
        return switch (task.skill()) {
            case SPATIAL, AREA, PERIMETER, LENGTH -> MILA;
            default -> JARO;
        };
    }
}
