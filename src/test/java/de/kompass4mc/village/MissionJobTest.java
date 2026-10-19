package de.kompass4mc.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MissionJobTest {
    @Test
    void invalidContentCannotReachAnNpcDialog() {
        assertThrows(IllegalArgumentException.class, () -> job("Bad Id", "Titel", 3));
        assertThrows(IllegalArgumentException.class, () -> job("valid", "\u00a0", 3));
        assertThrows(IllegalArgumentException.class, () -> job("valid", "Titel", 0));
        assertThrows(IllegalArgumentException.class, () -> job("valid", "Titel", 65));
        assertEquals("Titel", job("valid", " Titel ", 3).title());
    }

    private static MissionJob job(String id, String title, int reward) {
        return new MissionJob(id, "Mila", "Gärtnerin", title, "Auftrag", "Angenommen", "Hinweis",
                "Abgeschlossen", reward, Kind.GARDEN);
    }
}
