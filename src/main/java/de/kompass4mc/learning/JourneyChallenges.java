package de.kompass4mc.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Original short encounters with stable IDs, not random answers generated during play. */
final class JourneyChallenges {
    private JourneyChallenges() {}
    static List<LearningTask> all() {
        List<LearningTask> tasks = new ArrayList<>();
        String[] cargo = {"Fackeln", "Bretter", "Pflastersteine", "Setzlinge"};
        int[][] subtractions = {{503, 178}, {810, 267}, {704, 286}, {902, 458}, {630, 284}, {1005, 387},
                {820, 456}, {601, 238}, {1204, 567}, {950, 476}, {2003, 785}, {730, 348}};
        for (int i = 0; i < subtractions.length; i++) {
            int a = subtractions[i][0], b = subtractions[i][1];
            String item = cargo[i % cargo.length];
            String wrong = digitDifference(a, b);
            tasks.add(number("cargo_sub_" + i, "Die nächste Lieferung", "Im Lager waren " + a + " " + item
                    + ". Für den Hafen gingen " + b + " weg.", "Wie viele sind noch im Lager?", a - b,
                    Skill.SUBTRACTION, i < 4 ? Difficulty.MEDIUM : Difficulty.HARD,
                    "Rechne " + a + " − " + b + ". Die Gegenprobe ist das Zusammenzählen.",
                    List.of(new ErrorPattern("borrowing", "Hinweis auf stellenweises Kleiner-von-Größer-Abziehen",
                            "Die Zahl entsteht, wenn in jeder Stelle die kleinere Ziffer von der größeren abgezogen wird. Gemeinsam das Entbündeln erklären lassen.", Set.of(wrong)))));
        }
        for (int i = 0; i < 8; i++) {
            int groups = 3 + i, perGroup = 7 + i * 2;
            tasks.add(number("cargo_groups_" + i, "Kisten für den Hafen", "Für die Boote packe ich " + groups
                    + " Kisten. In jede kommen " + perGroup + " Fackeln.", "Wie viele Fackeln brauche ich?",
                    groups * perGroup, Skill.MULTIPLICATION, i < 4 ? Difficulty.EASY : Difficulty.MEDIUM,
                    "Zähle " + groups + " gleich große Gruppen mit je " + perGroup + " zusammen.",
                    List.of(new ErrorPattern("groups_added", "Hinweis auf Addieren statt gleich großer Gruppen",
                            "Die Antwort ergibt sich aus Gruppenanzahl plus Gruppengröße. Gemeinsam Gruppen legen und die Gesamtmenge erklären lassen.", Set.of("" + (groups + perGroup))))));
            int portion = 4 + i * 3, shops = 3 + i % 4;
            tasks.add(new LearningTask("cargo_share_" + i, "Jaro", "Proviant für die Reise",
                    "Ich verteile " + (portion * shops) + " Brote gleichmäßig auf " + shops + " Reisegruppen.",
                    "Zeig mir die Brote für eine Gruppe in deiner Haupthand. Du behältst sie.", List.of(), "" + portion,
                    "Teile " + (portion * shops) + " durch " + shops + ".", "Jede Gruppe bekommt " + portion + " Brote.",
                    de.kompass4mc.quest.Subject.MATH, Skill.DIVISION, Difficulty.MEDIUM, Interaction.SHOW_STACK,
                    "minecraft:bread", List.of()));
            int start = 4 + i * 3, step = 3 + i;
            tasks.add(number("cargo_pattern_" + i, "Die Laternen am Weg", "An den Wegpfosten stehen die Zahlen "
                    + start + ", " + (start + step) + ", " + (start + 2 * step) + ", " + (start + 3 * step) + ".",
                    "Welche Zahl bekommt der nächste Pfosten?", start + 4 * step, Skill.NUMBER_PATTERNS,
                    Difficulty.MEDIUM, "Vergleiche zwei benachbarte Zahlen.", List.of()));
            int coins = 24 + i * 4, price = 3 + i % 3, amount = 3 + i % 4;
            if (i % 2 == 0) tasks.add(number("cargo_trade_" + i, "Ein guter Handel", "Ich habe " + coins
                    + " Smaragde. Ein Bücherpaket kostet " + price + ". Ich kaufe " + amount + " Pakete.",
                    "Wie viele Smaragde bleiben?", coins - price * amount, Skill.TIME_MONEY,
                    Difficulty.HARD, "Finde zuerst den Preis aller Pakete zusammen.", List.of()));
            else tasks.add(number("cargo_harvest_" + i, "Die Erntekisten", "Heute kamen " + (8 + i)
                    + " Karottenkisten und " + (5 + i * 2) + " Kartoffelkisten an.",
                    "Wie viele Kisten sind es zusammen?", 13 + 3 * i, Skill.DATA,
                    Difficulty.EASY, "Beide Sorten zählen mit.", List.of()));
        }
        addStories(tasks);
        tasks.addAll(BuildChallenges.tasks());
        return List.copyOf(tasks);
    }

    private static LearningTask number(String id, String title, String story, String question, int answer,
                                       Skill skill, Difficulty difficulty, String hint, List<ErrorPattern> patterns) {
        return new LearningTask(id, "Jaro", title, story, question, List.of(), "" + answer, hint,
                "Es sind " + answer + ".", skill.subject(), skill, difficulty, Interaction.ANSWER, "", patterns);
    }

    private record Question(Skill skill, String text, String answer, String firstWrong, String secondWrong) {}
    private static Question q(Skill skill, String text, String answer, String wrong1, String wrong2) {
        return new Question(skill, text, answer, wrong1, wrong2);
    }
    private static void story(List<LearningTask> tasks, String id, String title, String text, Question... questions) {
        for (int i = 0; i < questions.length; i++) {
            Question q = questions[i];
            var choices = new ArrayList<>(List.of(q.answer(), q.firstWrong(), q.secondWrong()));
            Collections.rotate(choices, Math.floorMod((id + i).hashCode(), choices.size()));
            tasks.add(new LearningTask("story_" + id + "_" + i, "Lio", title, text, q.text(), choices,
                    q.answer(), "Schau noch einmal auf die Nachricht und den Zusammenhang.", q.answer(),
                    q.skill().subject(), q.skill(), i == 0 ? Difficulty.EASY : Difficulty.MEDIUM,
                    Interaction.ANSWER, "", List.of()));
        }
    }

    private static void addStories(List<LearningTask> tasks) {
        story(tasks, "mill", "Post für die Mühle",
                "Nora bringt eine Karte zur Mühle. Die Brücke ist überflutet, deshalb nimmt sie den Hügelweg. Danach legt sie die Karte in die blaue Kiste.",
                q(Skill.READING_DETAIL, "Was bringt Nora?", "Eine Karte", "Ein Brot", "Eine Laterne"),
                q(Skill.READING_CAUSE, "Warum nimmt sie den Hügelweg?", "Die Brücke ist überflutet.", "Die Kiste ist blau.", "Die Mühle ist geschlossen."),
                q(Skill.READING_SEQUENCE, "Was tut sie nach dem Hügelweg?", "Sie legt die Karte in die Kiste.", "Sie baut die Brücke.", "Sie holt eine neue Karte."),
                q(Skill.VOCABULARY, "Was bedeutet hier ‚überflutet‘?", "Unter Wasser", "Frisch gestrichen", "Sehr steil"),
                q(Skill.SPELLING, "Welches Schild passt an die blaue Kiste?", "Hier liegen die Karten.", "Hier liegen die Kahrten.", "Hier ligen die Karten."));
        story(tasks, "boat", "Ein Boot vor Sonnenuntergang",
                "Ben dichtet sein Boot ab. Erst dann lädt er die Äpfel ein. Weil Wind aufzieht, fährt er dicht am Ufer entlang. Im Hafen wartet Fina.",
                q(Skill.READING_DETAIL, "Wer wartet im Hafen?", "Fina", "Nora", "Ben"),
                q(Skill.READING_CAUSE, "Warum bleibt Ben nah am Ufer?", "Wind zieht auf.", "Die Äpfel sind rot.", "Das Boot ist neu."),
                q(Skill.READING_SEQUENCE, "Was passiert vor dem Einladen?", "Ben dichtet das Boot ab.", "Ben erreicht den Hafen.", "Fina isst einen Apfel."),
                q(Skill.VOCABULARY, "Was bedeutet ‚abdichten‘?", "So schließen, dass kein Wasser eindringt", "Mit Farbe schmücken", "Schneller fahren"),
                q(Skill.GRAMMAR, "Ben erzählt es jetzt selbst. Was passt?", "Ich lade die Äpfel ein.", "Du ladest die Äpfel ein.", "Ich laden die Äpfel ein."));
        story(tasks, "lantern", "Licht am Waldweg",
                "Ava holt eine Laterne aus dem Schuppen. Sie hängt sie an den Wegpfosten, damit Reisende die Abzweigung finden. Zum Schluss räumt sie die Leiter weg.",
                q(Skill.READING_DETAIL, "Woher holt Ava die Laterne?", "Aus dem Schuppen", "Aus dem Brunnen", "Aus dem Hafen"),
                q(Skill.READING_CAUSE, "Wozu hängt sie das Licht auf?", "Damit Reisende die Abzweigung finden.", "Damit die Leiter trocknet.", "Damit das Boot schneller fährt."),
                q(Skill.READING_SEQUENCE, "Was tut sie zuletzt?", "Sie räumt die Leiter weg.", "Sie holt die Laterne.", "Sie pflanzt einen Baum."),
                q(Skill.VOCABULARY, "Was ist eine ‚Abzweigung‘?", "Eine Stelle, an der sich Wege teilen", "Ein kaputter Ast", "Eine geschlossene Tür"),
                q(Skill.SPELLING, "Welche Notiz passt?", "Die Laternen leuchten.", "Die Latärnen leuchten.", "Die Laternen leuschten."));
        story(tasks, "roof", "Ein Dach für den Markt",
                "Tarek entdeckt ein Loch im Dach. Er holt Bretter und ersetzt die kaputten Teile. Danach trägt er die Körbe zurück unter das trockene Dach.",
                q(Skill.READING_DETAIL, "Was holt Tarek?", "Bretter", "Wolle", "Äpfel"),
                q(Skill.READING_CAUSE, "Warum repariert er das Dach?", "Es hat ein Loch.", "Die Körbe sind neu.", "Der Markt ist zu groß."),
                q(Skill.READING_SEQUENCE, "Wann bringt er die Körbe zurück?", "Nach der Reparatur", "Vor der Reparatur", "Bevor er das Loch sieht"),
                q(Skill.VOCABULARY, "Was bedeutet ‚ersetzen‘?", "Etwas anderes an die Stelle setzen", "Alles verstecken", "Etwas genau zählen"),
                q(Skill.GRAMMAR, "Tarek und Ava berichten gemeinsam. Was passt?", "Wir tragen die Körbe.", "Wir trägt die Körbe.", "Ihr trage die Körbe."));
        story(tasks, "well", "Wasser für die Beete",
                "Fina füllt ihre Kanne am Brunnen. Im Garten gießt sie zuerst die Setzlinge. Die großen Bäume gießt sie nicht: Unter ihnen ist die Erde noch feucht.",
                q(Skill.READING_DETAIL, "Was gießt Fina zuerst?", "Die Setzlinge", "Die großen Bäume", "Den Brunnen"),
                q(Skill.READING_CAUSE, "Warum bekommen die großen Bäume kein Wasser?", "Die Erde darunter ist noch feucht.", "Die Bäume sind zu klein.", "Die Kanne ist kaputt."),
                q(Skill.READING_SEQUENCE, "Was passiert vor dem Gießen?", "Fina füllt die Kanne.", "Fina leert den Brunnen.", "Fina fällt die Bäume."),
                q(Skill.VOCABULARY, "Was sind ‚Setzlinge‘?", "Junge Pflanzen", "Große Steine", "Leere Kannen"),
                q(Skill.SPELLING, "Welche Mehrzahl passt auf Finas Schild?", "Bäume", "Beume", "Baume"));
        story(tasks, "parcel", "Eine zerbrechliche Lieferung",
                "Emil wickelt die Glasflaschen in Wolle. Dann legt er sie vorsichtig in eine Kiste. Auf den Deckel schreibt er: ‚Vorsicht, zerbrechlich!‘ Die Kiste geht zum Hafen.",
                q(Skill.READING_DETAIL, "Wohin geht die Kiste?", "Zum Hafen", "Zur Mine", "Zum Wald"),
                q(Skill.READING_CAUSE, "Warum benutzt Emil Wolle?", "Damit die Flaschen heil bleiben.", "Damit die Kiste schwerer wird.", "Damit das Glas wächst."),
                q(Skill.READING_SEQUENCE, "Was macht er vor dem Einpacken in die Kiste?", "Er wickelt die Flaschen in Wolle.", "Er bringt die Kiste zum Hafen.", "Er öffnet eine Mine."),
                q(Skill.VOCABULARY, "Was bedeutet ‚zerbrechlich‘?", "Es kann leicht kaputtgehen.", "Es ist besonders schwer.", "Es wird schnell warm."),
                q(Skill.SPELLING, "Welche Beschriftung passt?", "Glasflaschen", "Glaßflaschen", "Glasflaschn"));
        story(tasks, "map", "Ein Zeichen auf der Karte",
                "Ari zeichnet den neuen Steg auf die Karte. Einen umgestürzten Baum markiert er mit einem Kreuz. So können die Händler rechtzeitig einen anderen Weg wählen.",
                q(Skill.READING_DETAIL, "Was markiert Ari mit einem Kreuz?", "Einen umgestürzten Baum", "Den neuen Steg", "Eine blaue Kiste"),
                q(Skill.READING_CAUSE, "Wozu zeichnet er das Kreuz?", "Damit Händler ausweichen können.", "Damit der Baum wieder wächst.", "Damit der Steg verschwindet."),
                q(Skill.READING_EVIDENCE, "Welche Stelle zeigt, dass der Weg versperrt ist?", "‚einen umgestürzten Baum‘", "‚den neuen Steg‘", "‚auf die Karte‘"),
                q(Skill.VOCABULARY, "Was heißt ‚rechtzeitig‘?", "Früh genug", "Immer rechts", "Erst am nächsten Tag"),
                q(Skill.GRAMMAR, "Ari erzählt von gestern. Was passt?", "Ich zeichnete den Steg ein.", "Ich zeichne den Steg ein.", "Ich werde den Steg einzeichnen."));
        story(tasks, "shelter", "Ein Platz für die Nacht",
                "Leni findet eine trockene Höhle. Zuerst stellt sie eine Laterne auf, dann breitet sie ihre Decke aus. Draußen beginnt es zu regnen. Leni bleibt im Unterschlupf.",
                q(Skill.READING_DETAIL, "Wo übernachtet Leni?", "In einer Höhle", "Auf dem Steg", "Im Boot"),
                q(Skill.READING_CAUSE, "Warum bleibt sie im Unterschlupf?", "Draußen regnet es.", "Die Laterne ist blau.", "Ihre Karte ist leer."),
                q(Skill.READING_SEQUENCE, "Was tut sie vor dem Ausbreiten der Decke?", "Sie stellt eine Laterne auf.", "Sie geht zum Hafen.", "Sie gießt die Pflanzen."),
                q(Skill.READING_SUMMARY, "Welche Nachricht erzählt das Wichtigste?", "Leni findet für die regnerische Nacht einen trockenen Platz.", "Leni baut ein großes Boot.", "Leni verliert ihre Laterne im Fluss."),
                q(Skill.VOCABULARY, "Was ist ein ‚Unterschlupf‘?", "Ein geschützter Platz", "Ein schmales Werkzeug", "Eine offene Kiste"));
    }

    private static String digitDifference(int a, int b) {
        int value = 0, place = 1;
        while (a > 0 || b > 0) { value += Math.abs(a % 10 - b % 10) * place; place *= 10; a /= 10; b /= 10; }
        return "" + value;
    }
}
