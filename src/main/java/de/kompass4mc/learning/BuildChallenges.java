package de.kompass4mc.learning;

import de.kompass4mc.village.BuildPattern;
import de.kompass4mc.village.GridPos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Small real builds. The checker only reads blocks; it never creates or removes a structure. */
public final class BuildChallenges {
    public enum Shape { FLOOR, FRAME, TOWERS }
    public record Plan(String id, String title, String story, int width, int depth, int height,
                       String material, String materialName, Shape shape) {
        public BuildPattern pattern() {
            Map<GridPos, String> required = new LinkedHashMap<>();
            var forbidden = new LinkedHashSet<GridPos>();
            for (int x = -1; x <= width; x++) for (int z = -1; z <= depth; z++) {
                for (int y = 0; y <= height; y++) {
                    boolean inside = x >= 0 && x < width && z >= 0 && z < depth;
                    boolean wanted = inside && y < height && switch (shape) {
                        case FLOOR -> y == 0;
                        case FRAME -> x == 0 || z == 0 || x == width - 1 || z == depth - 1;
                        case TOWERS -> z == 0 && (x == 0 || x == width - 1);
                    };
                    if (wanted) required.put(new GridPos(x, y, z), material);
                    else forbidden.add(new GridPos(x, y, z));
                }
            }
            return new BuildPattern(required, forbidden, true);
        }
        public int supplies() { return pattern().required().size(); }
        public String instruction() {
            return switch (shape) {
                case FLOOR -> "Lege einen Boden aus " + materialName + ": " + width + " Blöcke breit, "
                        + depth + " Blöcke lang, eine Lage.";
                case FRAME -> "Baue einen rechteckigen Rand aus " + materialName + ": außen " + width
                        + " Blöcke breit und " + depth + " lang, eine Lage. Die Mitte bleibt frei.";
                case TOWERS -> "Baue zwei Pfosten aus " + materialName + ", beide " + height
                        + " Blöcke hoch. Zwischen ihnen bleiben " + (width - 2) + " freie Blöcke.";
            };
        }
    }
    private static final List<Plan> PLANS = create();
    private BuildChallenges() {}
    public static List<Plan> all() { return PLANS; }
    public static Optional<Plan> byId(String id) { return PLANS.stream().filter(p -> p.id().equals(id)).findFirst(); }
    public static List<LearningTask> tasks() {
        return PLANS.stream().map(p -> new LearningTask(p.id(), "Mila", p.title(), p.story(), p.instruction(),
                List.of(), "gebaut", "Mit dem Bauplan auf die obere Seite eines Bodenblocks klicken. Die hellen Punkte zeigen den Rand; die Ecke ist der Anfang.",
                "Der Bau passt zum Plan.", de.kompass4mc.quest.Subject.MATH,
                p.shape() == Shape.FLOOR ? Skill.AREA : p.shape() == Shape.FRAME ? Skill.PERIMETER : Skill.SPATIAL,
                p.supplies() <= 12 ? Difficulty.EASY : Difficulty.MEDIUM, Interaction.BUILD,
                p.material(), p.shape() == Shape.FRAME ? List.of(new ErrorPattern("build_area_border",
                        "Gefüllte Fläche statt eines freien Randes gebaut",
                        "Der passende Außenrand ist vorhanden, aber auch die Innenfläche ist vollständig gefüllt. Nachfragen, ob Rand und Fläche unterschieden wurden.",
                        java.util.Set.of("flaeche_statt_rand"))) : List.of())).toList();
    }
    private static List<Plan> create() {
        List<Plan> plans = new ArrayList<>();
        String[] places = {"Ein Picknickplatz", "Vor dem Zelt", "Am Bootssteg", "Ein Platz für Kisten", "Die kleine Bühne", "Eine trockene Ecke"};
        int[][] sizes = {{3, 3}, {4, 3}, {5, 3}, {4, 4}, {5, 4}, {6, 3}};
        for (int i = 0; i < sizes.length; i++) plans.add(new Plan("build_floor_" + i, places[i],
                "Hier sollen meine Gäste trockene Füße behalten. Hilfst du mir mit einem Holzboden?",
                sizes[i][0], sizes[i][1], 1, "minecraft:oak_planks", "Eichenholzbrettern", Shape.FLOOR));
        for (int i = 0; i < 4; i++) plans.add(new Plan("build_frame_" + i, "Ein Rand für die Blumen",
                "Mein neuer Garten braucht einen Steinrand. In der Mitte sollen die Blumen wachsen.",
                4 + i % 2, 3 + i / 2, 1, "minecraft:cobblestone", "Pflastersteinen", Shape.FRAME));
        for (int i = 0; i < 2; i++) plans.add(new Plan("build_towers_" + i, "Pfosten für das Sonnensegel",
                "Ich möchte ein Sonnensegel aufhängen. Mir fehlen noch zwei gleich hohe Pfosten.",
                4 + i, 1, 3 + i, "minecraft:oak_planks", "Eichenholzbrettern", Shape.TOWERS));
        return List.copyOf(plans);
    }
}
