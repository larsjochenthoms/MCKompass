package de.kompass4mc;

import java.util.Arrays;
import java.util.Optional;

/** Stable, local skin identities; never resolves player accounts or remote textures. */
public enum NpcSkin {
    MILA("Mila", "mila"),
    JARO("Jaro", "jaro"),
    LIO("Lio", "lio");

    private final String residentName;
    private final String id;

    NpcSkin(String residentName, String id) {
        this.residentName = residentName;
        this.id = id;
    }

    public String residentName() { return residentName; }
    public String id() { return id; }
    public String marker() { return "kompass4mc:npc_skin/" + id; }

    public static Optional<NpcSkin> fromResidentName(String name) {
        return Arrays.stream(values()).filter(skin -> skin.residentName.equals(name)).findFirst();
    }

    public static Optional<NpcSkin> fromMarker(String marker) {
        return Arrays.stream(values()).filter(skin -> skin.marker().equals(marker)).findFirst();
    }
}
