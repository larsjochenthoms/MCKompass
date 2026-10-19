package de.kompass4mc.learning;

import de.kompass4mc.quest.Subject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Eigene Aufgaben in alltäglichen Minecraft-Situationen, unabhängig von gebauten Lernorten.
 * Die Inhalte sind Übungen für die vierte Klasse, keine offiziellen KOMPASS-4-Testaufgaben.
 * Konkrete Fehlantworten liefern vorsichtige Hinweise für ein gemeinsames Nachfragen.
 */
public final class LearningCatalog {
    private static final List<LearningTask> BASE = List.of(
            task("subtract_store", "Mila", "Nachschub für den Weg",
                    "Ich habe 867 Pflastersteine im Lager gezählt. Für einen Weg habe ich 243 herausgenommen.",
                    "Wie viele sind noch da? Gib die Zahl ein.",
                    List.of(), "624",
                    "Schreibe Einer unter Einer, Zehner unter Zehner und Hunderter unter Hunderter.",
                    "7 − 3 = 4, 6 − 4 = 2 und 8 − 2 = 6. Es bleiben 624 Pflastersteine.",
                    Skill.SUBTRACTION, Difficulty.EASY, List.of()),
            task("subtract_borrow_1", "Jaro", "Das große Baulager",
                    "Für einen Tunnel kamen 7 010 Steine an. Davon sind 3 856 schon verbaut.",
                    "Wie viele Steine bleiben? Gib die Zahl ein.",
                    List.of(), "3154",
                    "Du darfst einen Tausender in Hunderter, einen Hunderter in Zehner und einen Zehner in Einer entbündeln.",
                    "7 010 − 3 856 = 3 154. Die Probe ist 3 154 + 3 856 = 7 010.",
                    Skill.SUBTRACTION, Difficulty.HARD, List.of(borrowing("4846"))),
            task("multiply_saplings", "Mila", "Die Setzlingskisten",
                    "Ich packe 6 Kisten mit jeweils 24 Setzlingen für meine Reise.",
                    "Wie viele Setzlinge brauche ich insgesamt? Gib die Zahl ein.",
                    List.of(), "144", "Rechne 6 · 20 und 6 · 4 und addiere die Ergebnisse.",
                    "6 · 24 = 120 + 24 = 144 Setzlinge.",
                    Skill.MULTIPLICATION, Difficulty.MEDIUM, List.of(groups("30"))),
            stack("divide_boats", "Jaro", "Bretter für sieben Boote",
                    "Ich verteile 168 Eichenholzbretter gleichmäßig auf 7 Bootswerkstätten. Jede bekommt gleich viele.",
                    "Zeig mir den Anteil einer Werkstatt als genau abgezählten Stapel Eichenholzbretter in deiner Haupthand. Die Bretter behältst du.",
                    "24", "168 : 7 kannst du in 140 : 7 und 28 : 7 zerlegen.",
                    "168 : 7 = 20 + 4 = 24 Bretter je Werkstatt. 7 · 24 = 168.",
                    Skill.DIVISION, Difficulty.MEDIUM, "minecraft:oak_planks", List.of()),
            task("length_rope", "Lio", "Ein Seil für die Reise",
                    "Mein Seil ist 3 m 75 cm lang. Für einen Sack schneide ich 85 cm ab.",
                    "Wie lang ist das restliche Seil in Zentimetern? Gib die Zahl ein.",
                    List.of(), "290", "Wandle 3 m 75 cm zuerst vollständig in Zentimeter um.",
                    "3 m 75 cm = 375 cm. 375 cm − 85 cm = 290 cm.",
                    Skill.LENGTH, Difficulty.HARD, List.of()),
            task("perimeter_garden_1", "Mila", "Zaun für einen Garten",
                    "Ein rechteckiger Garten ist 8 m lang und 6 m breit. Ich plane einen Zaun um alle vier Seiten, ohne Lücke.",
                    "Wie viele Meter Zaun brauche ich? Gib die Zahl ein.",
                    List.of(), "28", "Gehe einmal um das Rechteck: zwei lange und zwei kurze Seiten.",
                    "8 + 6 + 8 + 6 = 28. 8 · 6 = 48 wäre die Fläche in Quadratmetern.",
                    Skill.PERIMETER, Difficulty.MEDIUM, List.of(areaInsteadOfPerimeter("48"))),
            task("subtract_borrow_2", "Lio", "Die Fackelrechnung",
                    "Ich hatte 402 Fackeln. An Reisende habe ich 158 abgegeben.",
                    "Wie viele Fackeln habe ich noch? Gib die Zahl ein.",
                    List.of(), "244", "Für die Einer brauchst du einen Zehner. Weil dort eine Null steht, beginne beim Hunderter.",
                    "402 − 158 = 244. Die Probe ist 244 + 158 = 402.",
                    Skill.SUBTRACTION, Difficulty.MEDIUM, List.of(borrowing("356"))),
            stack("groups_stone_1", "Jaro", "Steine für drei Pfeiler",
                    "Für einen Steg brauche ich 3 gleich große Pfeiler. Für jeden Pfeiler plane ich 6 Pflastersteine.",
                    "Zeig mir alle benötigten Pflastersteine als einen genau abgezählten Stapel in deiner Haupthand. Du behältst die Steine.",
                    "18", "Lege gedanklich 3 Gruppen mit jeweils 6 Steinen zusammen.",
                    "6 + 6 + 6 = 18. Also brauchst du 3 · 6 = 18 Pflastersteine.",
                    Skill.MULTIPLICATION, Difficulty.EASY, "minecraft:cobblestone", List.of(groups("9"))),
            stack("groups_planks_2", "Jaro", "Bretter für vier Bänke",
                    "Ich möchte 4 Bänke bauen. Für jede Bank habe ich 5 Eichenholzbretter eingeplant.",
                    "Zeig mir alle benötigten Eichenholzbretter als genau abgezählten Stapel in deiner Haupthand. Du behältst die Bretter.",
                    "20", "Es sind 4 gleich große Gruppen mit jeweils 5 Brettern.",
                    "5 + 5 + 5 + 5 = 20. 4 · 5 = 20 Eichenholzbretter.",
                    Skill.MULTIPLICATION, Difficulty.EASY, "minecraft:oak_planks", List.of(groups("9"))),
            task("units_rope_1", "Lio", "Das Seilmaß",
                    "Für mein Zelt brauche ich ein 3 m 20 cm langes Seil. Auf meinem Bestellzettel muss alles in Zentimetern stehen.",
                    "Wie viele Zentimeter sind 3 m 20 cm? Gib die Zahl ein.",
                    List.of(), "320", "1 m sind 100 cm. Rechne zuerst die drei Meter um.",
                    "3 m = 300 cm. Dazu kommen 20 cm: insgesamt 320 cm.",
                    Skill.LENGTH, Difficulty.EASY, List.of(metresAsTen("50"))),
            task("units_banner_2", "Mila", "Das Band für die Fahne",
                    "Mein Fahnenband ist 2 m 45 cm lang. Ich möchte die Länge in Zentimetern aufschreiben.",
                    "Wie viele Zentimeter sind 2 m 45 cm? Gib die Zahl ein.",
                    List.of(), "245", "2 m sind zweimal 100 cm. Die 45 cm kommen dazu.",
                    "2 m = 200 cm. 200 cm + 45 cm = 245 cm.",
                    Skill.LENGTH, Difficulty.EASY, List.of(metresAsTen("65"))),
            task("perimeter_garden_2", "Mila", "Ein zweiter Gartenzaun",
                    "Bei meinem nächsten Halt plane ich ein rechteckiges Beet: 7 m lang und 4 m breit. Alle vier Seiten bekommen einen Zaun.",
                    "Wie viele Meter Zaun brauche ich ohne eine Lücke? Gib die Zahl ein.",
                    List.of(), "22", "Eine lange und eine kurze Seite ergeben erst die Hälfte des Zauns.",
                    "7 + 4 + 7 + 4 = 22 m Zaun. 7 · 4 = 28 beschreibt die Fläche, nicht den Zaun.",
                    Skill.PERIMETER, Difficulty.MEDIUM, List.of(areaInsteadOfPerimeter("28"))),
            task("cause_bridge_1", "Mila", "Ein trockenes Buch",
                    "Ich brachte ein Buch zur Mühle. Wasser floss über die niedrige Brücke. Deshalb ging ich über den Hügel. Das Buch blieb trocken.",
                    "Warum ging Mila über den Hügel?",
                    List.of("Das Buch blieb trocken.", "Wasser floss über die niedrige Brücke.", "Die Mühle war geschlossen.", "Sie wollte einen Baum fällen."),
                    "Wasser floss über die niedrige Brücke.", "Lies den Satz vor dem Wort ‚deshalb‘. Dort steht der Grund.",
                    "Das Wasser auf der Brücke war der Grund für den Umweg. Dass das Buch trocken blieb, war eine Folge.",
                    Skill.READING_CAUSE, Difficulty.MEDIUM, List.of(causeAndConsequence("Das Buch blieb trocken."))),
            task("reading_workshop", "Lio", "Die Nachricht an der Tür",
                    "Jara schreibt: ‚Bis Mittag repariere ich die Brücke. Danach bin ich in der Werkstatt. Eilige Bestellungen nimmt Lio im Nachbarhaus an.‘",
                    "Wo kannst du eine eilige Bestellung abgeben?",
                    List.of("Bei Jara auf der Brücke.", "Am Brunnen.", "Bei Lio im Nachbarhaus.", "Im Wald."),
                    "Bei Lio im Nachbarhaus.", "Suche nach den Worten ‚Eilige Bestellungen‘.",
                    "Die Nachricht nennt Lio im Nachbarhaus als Anlaufstelle für eilige Bestellungen.",
                    Skill.READING_DETAIL, Difficulty.EASY, List.of()),
            task("sequence_lantern_1", "Lio", "Licht auf dem Turm",
                    "Ben holt eine Laterne aus dem Lager. Dann steigt er auf den Turm. Oben zündet er die Laterne an. Zum Schluss winkt er ins Dorf.",
                    "Was tut Ben unmittelbar bevor er die Laterne anzündet?",
                    List.of("Er winkt ins Dorf.", "Er steigt auf den Turm.", "Er legt sich schlafen.", "Er holt Wasser."),
                    "Er steigt auf den Turm.", "Suche das Anzünden. Welcher Schritt steht direkt davor?",
                    "Das Hinaufsteigen steht direkt vor dem Anzünden. Das Winken kommt erst danach.",
                    Skill.READING_SEQUENCE, Difficulty.MEDIUM, List.of(sequence("Er winkt ins Dorf."))),
            task("noun_garden_1", "Mila", "Ein lesbares Gartenschild",
                    "Auf meinem Schild steht: ‚DIE KINDER PFLANZEN BLUMEN.‘ Ich möchte es in normaler Schreibweise neu schreiben.",
                    "Welches Wort braucht mitten auf dem Schild einen großen Anfangsbuchstaben?",
                    List.of("pflanzen", "Blumen", "die", "für"), "Blumen",
                    "Man kann ‚die Blume‘ oder ‚die Blumen‘ sagen.",
                    "‚Blumen‘ ist ein Nomen. ‚pflanzen‘ sagt in diesem Satz, was die Kinder tun.",
                    Skill.SPELLING, Difficulty.EASY, List.of(nounAndVerb("pflanzen"))),
            task("spelling_dog_1", "Lio", "Das Schild am Hof",
                    "Mein Schild hat eine Lücke: ‚Ein Hun_ bewacht den Hof.‘ Ich kenne auch die Mehrzahl ‚Hunde‘.",
                    "Welche Schreibweise passt in die Lücke?",
                    List.of("Hunt", "Hundt", "Hund", "Hunn"), "Hund",
                    "Sprich ‚Hunde‘ deutlich. Den Laut vor dem e kannst du gut hören.",
                    "In ‚Hunde‘ hörst du das d. Deshalb schreiben wir auch ‚Hund‘ mit d.",
                    Skill.SPELLING, Difficulty.MEDIUM, List.of(finalSound("Hunt"))),
            task("grammar_carry_1", "Jaro", "Ein Bericht für heute",
                    "In meinem alten Baubuch steht: ‚Wir trugen die Kisten zum Boot.‘ Heute machen wir es wieder. Was schreibe ich heute?",
                    "Welcher Satz passt?",
                    List.of("Wir trugen die Kisten zum Boot.", "Wir tragen die Kisten zum Boot.", "Ihr tragt die Kisten zum Boot.", "Wir werden die Kisten zum Boot tragen."),
                    "Wir tragen die Kisten zum Boot.", "Behalte ‚wir‘ bei und erzähle, was jetzt passiert.",
                    "‚Wir tragen‘ ist Gegenwart mit der Person ‚wir‘. ‚trugen‘ ist Vergangenheit; ‚ihr‘ wäre eine andere Person.",
                    Skill.GRAMMAR, Difficulty.HARD,
                    List.of(pastInsteadOfPresent("Wir trugen die Kisten zum Boot."), differentPerson("Ihr tragt die Kisten zum Boot."))),
            task("cause_roof_2", "Jaro", "Bretter für das Dach",
                    "Am Haus fehlen Bretter für das Dach. Deshalb geht Jaro ins Lager und holt welche. Danach kann er das Dach abdichten.",
                    "Warum geht Jaro ins Lager?",
                    List.of("Danach kann er das Dach abdichten.", "Er möchte Äpfel kaufen.", "Am Haus fehlen Bretter für das Dach.", "Das Lager ist geschlossen."),
                    "Am Haus fehlen Bretter für das Dach.", "Was ist schon vor dem Gang zum Lager los?",
                    "Die fehlenden Bretter sind der Anlass. Das Abdichten ist erst danach möglich.",
                    Skill.READING_CAUSE, Difficulty.MEDIUM, List.of(causeAndConsequence("Danach kann er das Dach abdichten."))),
            task("sequence_bench_2", "Jaro", "Eine Bank reparieren",
                    "Jaro holt Werkzeug. Dann ersetzt er ein kaputtes Brett an der Bank. Danach streicht er die Bank. Zuletzt räumt er das Werkzeug weg.",
                    "Was tut Jaro unmittelbar bevor er die Bank streicht?",
                    List.of("Er räumt das Werkzeug weg.", "Er holt einen Apfel.", "Er ersetzt ein kaputtes Brett.", "Er geht schlafen."),
                    "Er ersetzt ein kaputtes Brett.", "Finde das Streichen im Text und gehe genau einen Schritt zurück.",
                    "Direkt vor dem Streichen ersetzt Jaro das Brett. Das Wegräumen passiert danach.",
                    Skill.READING_SEQUENCE, Difficulty.MEDIUM, List.of(sequence("Er räumt das Werkzeug weg."))),
            task("noun_torches_2", "Lio", "Ein Schild am Weg",
                    "Auf meinem neuen Schild steht in Großbuchstaben: ‚DIE FACKELN LEUCHTEN HELL.‘ Ich möchte normal schreiben.",
                    "Welches Wort braucht mitten auf dem Schild einen großen Anfangsbuchstaben?",
                    List.of("leuchten", "hell", "und", "Fackeln"), "Fackeln",
                    "Man kann ‚die Fackel‘ oder ‚die Fackeln‘ sagen.",
                    "‚Fackeln‘ ist ein Nomen. ‚leuchten‘ ist in diesem Satz das Verb.",
                    Skill.SPELLING, Difficulty.EASY, List.of(nounAndVerb("leuchten"))),
            task("spelling_mountain_2", "Mila", "Der Wegweiser zum Berg",
                    "Auf meinem Wegweiser fehlt ein Buchstabe: ‚Zum Ber_‘. Die verlängerte Form ist ‚Berge‘.",
                    "Welche Schreibweise ist richtig?",
                    List.of("Berk", "Berg", "Bergk", "Berck"), "Berg",
                    "Sprich ‚Berge‘ deutlich. Welcher Laut steht vor dem e?",
                    "In ‚Berge‘ hörst du das g. Daher wird ‚Berg‘ mit g geschrieben.",
                    Skill.SPELLING, Difficulty.MEDIUM, List.of(finalSound("Berk"))),
            task("grammar_bring_2", "Mila", "Ein neuer Gartenbericht",
                    "Gestern schrieb ich: ‚Wir brachten die Setzlinge zum Beet.‘ Heute machen wir es wieder. Was schreibe ich heute?",
                    "Welcher Satz passt?",
                    List.of("Ihr bringt die Setzlinge zum Beet.", "Wir werden die Setzlinge zum Beet bringen.", "Wir brachten die Setzlinge zum Beet.", "Wir bringen die Setzlinge zum Beet."),
                    "Wir bringen die Setzlinge zum Beet.", "Die Person bleibt ‚wir‘. Das Verb soll sagen, was jetzt passiert.",
                    "‚Wir bringen‘ ist Gegenwart mit ‚wir‘. ‚brachten‘ ist Vergangenheit. ‚ihr‘ verändert die Person.",
                    Skill.GRAMMAR, Difficulty.HARD,
                    List.of(pastInsteadOfPresent("Wir brachten die Setzlinge zum Beet."), differentPerson("Ihr bringt die Setzlinge zum Beet."))),
            stack("reading_delivery", "Lio", "Den Bestellzettel lesen",
                    "Auf meinem Zettel steht: ‚Bitte 12 Eichenholzbretter für die Reparatur bereitstellen. Die 5 Reservebretter bleiben im Lager.‘",
                    "Zeig mir genau die bestellte Menge Eichenholzbretter als Stapel in deiner Haupthand. Du behältst die Bretter.",
                    "12", "Lies genau: Welche Menge soll bereitstehen und welche bleibt im Lager?",
                    "Bestellt sind 12 Eichenholzbretter. Die 5 Reservebretter gehören nicht zur Lieferung.",
                    Skill.READING_DETAIL, Difficulty.EASY, "minecraft:oak_planks", List.of())
    );

    private static final List<LearningTask> ALL = java.util.stream.Stream.of(BASE, KompassInspiredTasks.all(), JourneyChallenges.all())
            .flatMap(List::stream).map(LearningCatalog::withRole).toList();

    private static final Map<String, LearningTask> BY_ID;

    static {
        Map<String, LearningTask> indexed = new LinkedHashMap<>();
        for (LearningTask task : ALL) {
            if (indexed.putIfAbsent(task.id(), task) != null) {
                throw new IllegalStateException("Doppelte Lernaufgabe: " + task.id());
            }
        }
        BY_ID = Map.copyOf(indexed);
    }

    private LearningCatalog() {
    }

    public static List<LearningTask> all() {
        return ALL;
    }

    public static Optional<LearningTask> byId(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(BY_ID.get(id));
    }

    private static LearningTask withRole(LearningTask task) {
        String owner = ChallengeRole.forTask(task).residentName();
        return new LearningTask(task.id(), owner, task.title(), task.story(),
                task.question().replace(" Gib die Zahl ein.", ""), task.choices(), task.expectedAnswer(),
                task.hint(), task.explanation(), task.subject(), task.skill(), task.difficulty(),
                task.interaction(), task.itemId(), task.patterns());
    }

    private static LearningTask task(String id, String npc, String title, String story, String question,
            List<String> choices, String answer, String hint, String explanation, Skill skill,
            Difficulty difficulty, List<ErrorPattern> patterns) {
        return new LearningTask(id, npc, title, story, question, choices, answer, hint, explanation,
                skill.subject(), skill, difficulty, Interaction.ANSWER, "", patterns);
    }

    private static LearningTask stack(String id, String npc, String title, String story, String question,
            String answer, String hint, String explanation, Skill skill, Difficulty difficulty,
            String itemId, List<ErrorPattern> patterns) {
        return new LearningTask(id, npc, title, story, question, List.of(), answer, hint, explanation,
                skill.subject(), skill, difficulty, Interaction.SHOW_STACK, itemId, patterns);
    }

    private static ErrorPattern borrowing(String answer) {
        return pattern("borrowing", "Hinweis auf stellenweises Kleiner-von-Größer-Abziehen",
                "Die Zahl entsteht, wenn in jeder Stelle die kleinere Ziffer von der größeren abgezogen wird. Gemeinsam das Entbündeln erklären lassen.", answer);
    }

    private static ErrorPattern groups(String answer) {
        return pattern("groups_added", "Hinweis auf Addieren statt gleich großer Gruppen",
                "Die Antwort ergibt sich aus Gruppenanzahl plus Gruppengröße. Gemeinsam Gruppen legen und die Gesamtmenge erklären lassen.", answer);
    }

    private static ErrorPattern areaInsteadOfPerimeter(String answer) {
        return pattern("area_perimeter", "Hinweis auf Verwechseln von Fläche und Umfang",
                "Die Antwort ergibt sich aus Länge mal Breite, obwohl die Strecke um das Rechteck gefragt ist. Gemeinsam den Rand abfahren.", answer);
    }

    private static ErrorPattern metresAsTen(String answer) {
        return pattern("units", "Hinweis auf 10 statt 100 Zentimeter pro Meter",
                "Die Antwort passt dazu, die Meterzahl mit 10 statt mit 100 zu vervielfachen und die Zentimeter zu addieren. Gemeinsam an einem Metermaß prüfen.", answer);
    }

    private static ErrorPattern sequence(String answer) {
        return pattern("sequence", "Hinweis auf Verwechseln von davor und danach",
                "Die Antwort nennt den Schritt nach dem Ereignis, obwohl direkt davor gefragt ist. Gemeinsam die Handlungsschritte ordnen.", answer);
    }

    private static ErrorPattern causeAndConsequence(String answer) {
        return pattern("cause_consequence", "Hinweis auf Verwechseln von Grund und Folge",
                "Die Antwort nennt eine spätere Folge statt des vorherigen Anlasses. Gemeinsam nachfragen: Was war zuerst der Grund?", answer);
    }

    private static ErrorPattern nounAndVerb(String answer) {
        return pattern("noun_verb", "Hinweis auf Verwechseln von Nomen und Verb im Satz",
                "Die Antwort nennt das Verb des Satzes statt eines Nomens. Gemeinsam mit Artikelprobe und Satzbedeutung begründen lassen.", answer);
    }

    private static ErrorPattern finalSound(String answer) {
        return pattern("final_sound", "Hinweis auf Schreiben allein nach dem Endlaut",
                "Die Antwort passt zum stimmlosen Endlaut, nutzt aber die angegebene verlängerte Form nicht. Gemeinsam Einzahl und verlängerte Form vergleichen.", answer);
    }

    private static ErrorPattern pastInsteadOfPresent(String answer) {
        return pattern("tense", "Hinweis auf Verwechseln von Vergangenheit und Gegenwart",
                "Die Antwort behält die Vergangenheit bei, obwohl die Gegenwart verlangt ist. Gemeinsam mit gestern und heute vergleichen.", answer);
    }

    private static ErrorPattern differentPerson(String answer) {
        return pattern("person", "Hinweis auf Verändern der grammatischen Person",
                "Die Antwort steht in der Gegenwart, wechselt aber von wir zu ihr. Gemeinsam das Personalpronomen und die Verbform prüfen.", answer);
    }

    private static ErrorPattern pattern(String id, String label, String rationale, String answer) {
        return new ErrorPattern(id, label, rationale, Set.of(answer));
    }
}
