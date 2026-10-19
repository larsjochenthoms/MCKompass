package de.kompass4mc.village;

/** Eine Position relativ zum Bauursprung; ältere flache Pläne liegen auf Höhe null. */
public record GridPos(int x, int y, int z) {
    public GridPos(int x, int z) { this(x, 0, z); }
}
