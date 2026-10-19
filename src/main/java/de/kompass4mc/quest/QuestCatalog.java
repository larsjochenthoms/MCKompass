package de.kompass4mc.quest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Eigene Übungsaufgaben für die vierte Klasse in einer Minecraft-Rahmengeschichte.
 * Keine Übernahme offizieller Testaufgaben und keine normierte Leistungsbewertung.
 */
public final class QuestCatalog {
    private static final List<Quest> ALL = List.of(
            new Quest(
                    "math_subtract_1", "Das Lagerbuch", "Beim Vorratslager",
                    "Im Lager lagen 867 Pflastersteine. Für den neuen Weg wurden 243 Steine abgeholt.",
                    "Wie viele Pflastersteine sind noch im Lager? Rechne 867 − 243. Gib nur die Zahl ein.",
                    List.of(), "624",
                    "Schreibe die Zahlen untereinander: Einer unter Einer, Zehner unter Zehner. Beginne rechts.",
                    "Einer: 7 − 3 = 4. Zehner: 6 − 4 = 2. Hunderter: 8 − 2 = 6. Es bleiben 624 Steine.",
                    Subject.MATH, 3),
            new Quest(
                    "math_subtract_2", "Die große Baustelle", "Bei der Brückenbaustelle",
                    "Für die Brücke wurden 7 010 Steinblöcke geliefert. Die Baumeister haben schon 3 856 verbaut.",
                    "Wie viele Steinblöcke sind übrig? Rechne schriftlich: 7 010 − 3 856. Gib nur die Zahl ein.",
                    List.of(), "3154",
                    "Achte beim Entbündeln besonders auf die Null bei den Hundertern. Prüfe mit einer Plusaufgabe.",
                    "7 010 − 3 856 = 3 154. Die Probe lautet: 3 154 + 3 856 = 7 010.",
                    Subject.MATH, 4),
            new Quest(
                    "math_multiply", "Die Setzlingskisten", "In der Gärtnerei",
                    "Die Gärtnerin stellt 6 Kisten bereit. In jeder Kiste liegen genau 24 Setzlinge.",
                    "Wie viele Setzlinge liegen insgesamt in den Kisten? Gib nur die Zahl ein.",
                    List.of(), "144",
                    "Zerlege 24 in 20 und 4. Multipliziere beide Teile mit 6.",
                    "6 · 24 = 6 · 20 + 6 · 4 = 120 + 24 = 144 Setzlinge.",
                    Subject.MATH, 3),
            new Quest(
                    "math_divide", "Proviant für die Reise", "Am Rastplatz",
                    "Sieben Reisende teilen 168 Äpfel gleichmäßig unter sich auf. Kein Apfel bleibt übrig.",
                    "Wie viele Äpfel bekommt jede Person? Gib nur die Zahl ein.",
                    List.of(), "24",
                    "Zerlege 168 in zwei Zahlen, die du leicht durch 7 teilen kannst, zum Beispiel 140 und 28.",
                    "168 : 7 = 140 : 7 + 28 : 7 = 20 + 4 = 24. Die Probe ist 7 · 24 = 168.",
                    Subject.MATH, 3),
            new Quest(
                    "math_length", "Das Seil am Brunnen", "Beim Dorfbrunnen",
                    "Ein Seil ist 3 m 75 cm lang. Die Brunnenwärterin schneidet ein 85 cm langes Stück ab.",
                    "Wie lang ist das restliche Seil in Zentimetern? Gib nur die Zahl ein.",
                    List.of(), "290",
                    "Ein Meter hat 100 Zentimeter. Wandle zuerst die ganze Seillänge in Zentimeter um.",
                    "3 m 75 cm sind 375 cm. 375 cm − 85 cm = 290 cm. Das sind 2 m 90 cm.",
                    Subject.MATH, 3),
            new Quest(
                    "math_rectangle", "Der Zaun um den Garten", "Am Gemüsegarten",
                    "Ein rechteckiger Garten ist 8 m lang und 6 m breit. Ein Zaun soll alle vier Seiten vollständig umgeben; es gibt keine Lücke.",
                    "Wie viele Meter Zaun werden benötigt? Gib nur die Zahl ein.",
                    List.of(), "28",
                    "Gesucht ist der Umfang. Ein Rechteck hat zwei lange und zwei kurze Seiten.",
                    "8 m + 6 m + 8 m + 6 m = 28 m. Die Fläche des Gartens wird hier nicht gesucht.",
                    Subject.MATH, 3),
            new Quest(
                    "german_read_library", "Der trockene Weg", "In der kleinen Bibliothek",
                    "Mila wollte ein Buch zur Mühle bringen. Am Fluss sah sie, dass Wasser über die niedrige Brücke floss. Deshalb nahm sie den längeren Weg über den Hügel. Das Buch blieb trocken.",
                    "Warum nahm Mila den Weg über den Hügel?",
                    List.of("Die Mühle stand auf dem Hügel.", "Wasser floss über die niedrige Brücke.", "Sie wollte ein neues Buch kaufen.", "Der Weg über den Hügel war kürzer."),
                    "Wasser floss über die niedrige Brücke.",
                    "Lies den Satz vor dem Wort ‚deshalb‘. Dort steht der Grund für Milas Entscheidung.",
                    "Im Text steht, dass Wasser über die niedrige Brücke floss. Deshalb wich Mila auf den Hügelweg aus.",
                    Subject.GERMAN, 3),
            new Quest(
                    "german_read_workshop", "Eine Nachricht an der Werkstatt", "Bei der Werkstatt",
                    "An der Tür hängt ein Zettel: ‚Heute repariere ich bis zum Mittag die Dorfbrücke. Danach bin ich wieder in der Werkstatt. Eilige Bestellungen könnt ihr bei Lio im Nachbarhaus abgeben. Eure Schmiedin Jara.‘",
                    "Wo können eilige Bestellungen abgegeben werden?",
                    List.of("Bei Jara auf der Dorfbrücke.", "Am Brunnen hinter der Werkstatt.", "Bei Lio im Nachbarhaus.", "In der Mühle am Fluss."),
                    "Bei Lio im Nachbarhaus.",
                    "Suche im Zettel nach ‚Eilige Bestellungen‘ und lies den Satz bis zum Ende.",
                    "Der Zettel nennt Lio im Nachbarhaus ausdrücklich als Anlaufstelle für eilige Bestellungen.",
                    Subject.GERMAN, 3),
            new Quest(
                    "german_read_order", "Die Laterne im Turm", "Beim Aussichtsturm",
                    "Zuerst holte Ben eine Laterne aus dem Lager. Dann stieg er die Treppe zum Turm hinauf. Oben zündete er die Laterne an. Zum Schluss winkte er seiner Schwester im Dorf zu.",
                    "Was tat Ben unmittelbar bevor er die Laterne anzündete?",
                    List.of("Er winkte seiner Schwester zu.", "Er legte die Laterne ins Lager.", "Er holte seine Schwester ab.", "Er stieg die Treppe zum Turm hinauf."),
                    "Er stieg die Treppe zum Turm hinauf.",
                    "Achte auf die Reihenfolge: zuerst, dann, oben, zum Schluss.",
                    "Ben holte die Laterne, stieg die Treppe hinauf und zündete sie erst oben an. Das Hinaufsteigen geschah direkt davor.",
                    Subject.GERMAN, 3),
            new Quest(
                    "german_spelling_noun", "Ein Schild für den Garten", "Bei den Gartenbeeten",
                    "Auf einem alten Gartenschild steht alles in Großbuchstaben: ‚DIE KINDER PFLANZEN BLUMEN.‘ Die Gärtnerin möchte den Satz in normaler Schreibweise abschreiben.",
                    "Welches Wort ist ein Nomen und wird deshalb großgeschrieben?",
                    List.of("pflanzen", "Blumen", "die", "für"),
                    "Blumen",
                    "Kannst du einen Artikel vor das Wort setzen? Zum Beispiel: eine ... oder die ...",
                    "‚Blumen‘ ist ein Nomen: die Blumen, eine Blume. Nomen werden großgeschrieben.",
                    Subject.GERMAN, 3),
            new Quest(
                    "german_spelling_extend", "Ein Fund am Waldrand", "Am Waldrand",
                    "Am Waldrand findet Nuri ein Schild mit der Lücke ‚Ein Hun_ bewacht den Hof.‘ Auf der Rückseite steht die Mehrzahl: ‚Hunde‘.",
                    "Welche Schreibweise passt in die Lücke?",
                    List.of("Hunt", "Hundt", "Hund", "Hunn"),
                    "Hund",
                    "Sprich die verlängerte Form ‚Hunde‘ deutlich. Welchen Laut hörst du vor dem e?",
                    "In ‚Hunde‘ hört man das d. Deshalb wird die Einzahl ‚Hund‘ ebenfalls mit d geschrieben.",
                    Subject.GERMAN, 3),
            new Quest(
                    "german_language_verb", "Der Bericht vom Hafen", "Am kleinen Hafen",
                    "Gestern halfen die Kinder am Hafen. Heute erzählt Tarek: ‚Gestern trugen wir die Kisten zum Boot.‘ Für das Hafenbuch soll dieser Satz in der Gegenwart stehen.",
                    "Welcher Satz steht in der Gegenwart und behält ‚wir‘ als Person bei?",
                    List.of("Wir trugen die Kisten zum Boot.", "Wir tragen die Kisten zum Boot.", "Ihr tragt die Kisten zum Boot.", "Wir werden die Kisten zum Boot tragen."),
                    "Wir tragen die Kisten zum Boot.",
                    "Behalte ‚wir‘ als Person bei. Wie lautet das Verb ‚tragen‘ in der Gegenwart mit ‚wir‘?",
                    "‚Wir tragen‘ ist Gegenwart. ‚Wir trugen‘ ist Vergangenheit; ‚wir werden tragen‘ ist Zukunft. ‚Ihr‘ wäre eine andere Person.",
                    Subject.GERMAN, 3)
    );
    private static final Map<String, Quest> BY_ID;

    static {
        Map<String, Quest> index = new LinkedHashMap<>();
        for (Quest quest : ALL) {
            if (index.putIfAbsent(quest.id(), quest) != null) {
                throw new IllegalStateException("Doppelte Quest-ID: " + quest.id());
            }
        }
        BY_ID = Map.copyOf(index);
    }

    private QuestCatalog() {
    }

    /** Liefert die unveränderliche Liste in der Reihenfolge der Lernstationen. */
    public static List<Quest> all() {
        return ALL;
    }

    public static Optional<Quest> byId(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(BY_ID.get(id));
    }
}
