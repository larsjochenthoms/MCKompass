package de.kompass4mc.village;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Eigenständig formulierte Lernaufträge, deren Lösung als Handlung im Dorf sichtbar wird. */
public final class VillageJobs {
    public static final String DELIVERY_ITEM_ID = "minecraft:apple";
    public static final int DELIVERY_COUNT = 18;

    private static final BuildPattern GARDEN = garden();
    private static final BuildPattern BRIDGE = bridge();
    private static final List<MissionJob> ALL = List.of(
            new MissionJob(
                    "garden_fence", "Mila", "Gärtnerin", "Ein Zaun für den Dorfgarten",
                    "Unsere Schafe knabbern die jungen Pflanzen an. Hilfst du mir, den rechteckigen Garten "
                            + "mit Eichenzaun einzuzäunen? Zwischen den Ecken liegen an der langen Seite "
                            + "8 Blockabstände und an der kurzen Seite 6 Blockabstände. Setze auf jedes "
                            + "Randfeld einen Zaun, auch auf die vier Ecken. Lass das Innere zunächst vollständig "
                            + "frei, damit wir später Beete anlegen können.",
                    "Danke! Die markierte Fläche ist dein Bauplatz. Baue den Zaun nur am Rand und "
                            + "komm anschließend zu mir zurück. Dann schauen wir uns den Garten zusammen an.",
                    "Zähle die Blockabstände einmal ringsherum: zwei lange und zwei kurze Seiten. "
                            + "Ein Eckzaun gehört zu beiden angrenzenden Seiten, wird aber nur einmal gesetzt.",
                    "Jetzt sind die Pflanzen geschützt und innen bleibt Platz zum Gärtnern! "
                            + "Am Rand stehen 28 Zaunblöcke: 8 + 6 + 8 + 6 = 28. Die vier Ecken werden jeweils nur einmal gezählt.",
                    4, Kind.GARDEN),
            new MissionJob(
                    "reading_bridge", "Jaro", "Baumeister", "Ein Steg zum anderen Ufer",
                    "Die Dorfbewohner möchten trockenen Fußes über den kleinen Graben. Auf meinem Bauzettel steht: "
                            + "‚Baue einen geraden Steg aus Eichenholzbrettern. Er ist fünf Blöcke lang und zwei "
                            + "Blöcke breit. Fülle die ganze markierte Fläche. Der Rand bleibt vollständig frei; "
                            + "nur der steinerne Anlegestein am Ufer bleibt stehen.‘ "
                            + "Kannst du den Steg genau nach diesem Zettel bauen?",
                    "Hier ist dein Bauplatz. Beginne seitlich am steinernen Anlegestein am Ufer. "
                            + "Die zehn Wasserfelder sind die Baufläche. Lies den Zettel sorgfältig und verlege "
                            + "die Bretter in der markierten Ebene. Wenn du fertig bist, prüfen wir gemeinsam, ob der Steg passt.",
                    "‚Fünf Blöcke lang und zwei Blöcke breit‘ bedeutet zwei Reihen mit je fünf Brettern. "
                            + "Achte auch auf die Holzart und darauf, was laut Bauzettel ringsherum frei bleiben soll.",
                    "Der Steg passt genau! Du hast die Bauanleitung in der Welt umgesetzt: "
                            + "5 × 2 = 10 Eichenholzbretter, mit freiem Rand. Jetzt erreichen wir das andere Ufer.",
                    4, Kind.BRIDGE),
            new MissionJob(
                    "travel_supplies", "Lio", "Lagerverwalter", "Proviant für die Reisenden",
                    "Drei Gäste brechen gleich zur nächsten Siedlung auf. Auf ihrem Zettel steht: "
                            + "‚Drei Reisende brauchen je sechs Äpfel. Brot bleibt im Lager.‘ "
                            + "Lies den Zettel und bring mir genau die benötigten Äpfel. Du kannst sie sammeln "
                            + "und anschließend bei mir abgeben.",
                    "Ich warte hier im Lager. Nimm den genau abgezählten Apfelstapel in deine Haupthand "
                            + "und sprich mich wieder an, wenn du liefern möchtest. Ich nehme nur den passenden Stapel entgegen.",
                    "Das Wort ‚je‘ bedeutet: Jeder der drei Reisenden bekommt sechs Äpfel. "
                            + "Brot steht zwar auf dem Zettel, soll aber im Lager bleiben.",
                    "Die Reisenden sind versorgt! Drei Portionen mit je sechs Äpfeln sind "
                            + "6 + 6 + 6 = 18 Äpfel. Das Brot bleibt wie gewünscht im Lager.",
                    4, Kind.DELIVERY));

    private VillageJobs() {
    }

    public static List<MissionJob> all() {
        return ALL;
    }

    public static Optional<MissionJob> byId(String id) {
        return ALL.stream().filter(job -> job.id().equals(id)).findFirst();
    }

    public static BuildPattern pattern(Kind kind) {
        return switch (kind) {
            case GARDEN -> GARDEN;
            case BRIDGE -> BRIDGE;
            case DELIVERY -> throw new IllegalArgumentException("Lieferaufträge haben keinen Bauplan.");
        };
    }

    private static BuildPattern garden() {
        Map<GridPos, String> required = new LinkedHashMap<>();
        Set<GridPos> forbidden = new LinkedHashSet<>();
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 6; z++) {
                GridPos position = new GridPos(x, z);
                if (x == 0 || x == 8 || z == 0 || z == 6) {
                    required.put(position, "minecraft:oak_fence");
                } else {
                    forbidden.add(position);
                }
            }
        }
        return new BuildPattern(required, forbidden, true);
    }

    private static BuildPattern bridge() {
        Map<GridPos, String> required = new LinkedHashMap<>();
        Set<GridPos> forbidden = new LinkedHashSet<>();
        for (int x = -1; x <= 5; x++) {
            for (int z = -1; z <= 2; z++) {
                GridPos position = new GridPos(x, z);
                if (x >= 0 && x < 5 && z >= 0 && z < 2) {
                    required.put(position, "minecraft:oak_planks");
                } else if (!position.equals(new GridPos(-1, 0))) {
                    forbidden.add(position);
                }
            }
        }
        return new BuildPattern(required, forbidden, true);
    }
}
