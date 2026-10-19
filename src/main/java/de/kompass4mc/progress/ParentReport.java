package de.kompass4mc.progress;

import de.kompass4mc.learning.Difficulty;
import de.kompass4mc.learning.ErrorPattern;
import de.kompass4mc.learning.LearningCatalog;
import de.kompass4mc.learning.LearningTask;
import de.kompass4mc.learning.Skill;
import de.kompass4mc.progress.LearningProgress.Attempt;
import de.kompass4mc.progress.LearningProgress.AttemptType;
import de.kompass4mc.progress.LearningProgress.TaskStats;
import de.kompass4mc.quest.Subject;
import de.kompass4mc.reward.RewardCatalog;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Read-only German parent views. The HTML is standalone and performs no network requests. */
public final class ParentReport {
    private static final int SUMMARY_LINES = 38;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Berlin"));

    private ParentReport() {}

    /** Bounded text for Minecraft dialogs; the HTML export contains all available details. */
    public static List<String> summary(LearningProgress progress) {
        List<String> lines = new ArrayList<>();
        Totals totals = overallTotals(progress);
        lines.add("Lernnotizen für " + compact(progress.playerName, 40));
        lines.add(progress.completed.size() + " Aufgaben gelöst · " + totals.answers + " Antworten · "
                + totals.hints + " Hinweise");
        lines.add("Erster Versuch richtig: " + totals.firstCorrect + "; davon ohne Hinweis: "
                + totals.firstWithoutHint + ".");
        lines.add("Fachliche Fehlantworten: " + totals.incorrect + "; davon ohne hinterlegtes Muster: "
                + unmatchedAnswers(progress) + " (letzte " + progress.attempts.size() + " Ereignisse).");
        lines.add("Bedienfragen separat: " + totals.procedural + ". Hinweise sind keine Fehler.");
        lines.add("Nur neue Lernreisende: letzte 500 Ereignisse und Aufgaben-Gesamtzahlen; alte Dorfaufträge ohne rückwirkende Notizen.");
        lines.add("Übungsbereiche:");
        for (Skill skill : Skill.values()) {
            Totals group = totals(progress, task -> task.skill() == skill);
            if (group.started > 0) {
                lines.add(skill.label() + ": " + group.solved + "/" + group.available
                        + " gelöst, erster Versuch richtig " + group.firstCorrect
                        + ", Hinweise " + group.hints + ".");
            }
        }
        lines.add("Anforderung und Belohnung:");
        for (Difficulty difficulty : Difficulty.values()) {
            Totals group = totals(progress, task -> task.difficulty() == difficulty);
            lines.add(difficulty.label() + ": " + group.solved + "/" + group.available
                    + " gelöst, " + RewardCatalog.tierDescription(difficulty) + ".");
        }
        List<PatternObservation> observations = observations(progress);
        lines.add("Beobachtungen zu Denkwegen:");
        if (observations.isEmpty()) {
            lines.add("Bisher kein hinterlegtes Fehlermuster beobachtet. Daraus folgt keine sichere Aussage über Können.");
        } else {
            for (PatternObservation observation : observations) {
                if (lines.size() >= SUMMARY_LINES - 6) {
                    lines.add("Weitere Beobachtungen stehen im HTML-Bericht.");
                    break;
                }
                lines.add((observation.possibleMisconception() ? "Möglicher Gesprächsanlass: " : "Einzelbeobachtung: ")
                        + observation.label() + " (" + observation.evidence().size() + " verschiedene Aufgaben).");
                Evidence example = observation.evidence().getLast();
                lines.add("Beispiel: " + compact(example.task().question(), 70) + " → Antwort „"
                        + compact(example.answer(), 35) + "“, erwartet „"
                        + compact(example.task().expectedAnswer(), 35) + "“.");
                long subsequentlySolved = observation.evidence().stream()
                        .filter(item -> wasAnsweredCorrectlyAfter(progress, item)).count();
                if (subsequentlySolved > 0) {
                    lines.add("Frühere Fehlversuche: " + subsequentlySolved + "/"
                            + observation.evidence().size()
                            + " danach mindestens einmal richtig beantwortet; keine Aussage zur aktuellsten Wiederholung.");
                }
            }
        }
        long archived = progress.stats.keySet().stream().filter(id -> LearningCatalog.byId(id).isEmpty()).count();
        if (archived > 0) {
            lines.add(archived + " frühere Aufgaben sind gespeichert; Details im HTML-Bericht.");
        }
        lines.add("Eine Antwort zeigt keinen Denkweg. Auch mehrere Treffer sind eine Hypothese, keine Diagnose.");
        lines.add("Lass dir den Rechenweg oder die Textstelle erklären. Wiederholte Klicks zählen nicht als neue Aufgabe.");
        return List.copyOf(lines);
    }

    /** Each task contributes at most one concrete piece of evidence per error-pattern ID. */
    public static List<PatternObservation> observations(LearningProgress progress) {
        Map<String, ObservationBuilder> grouped = new LinkedHashMap<>();
        progress.stats.forEach((taskId, stats) -> LearningCatalog.byId(taskId).ifPresent(task -> {
            for (ErrorPattern pattern : task.patterns()) {
                LearningProgress.PatternEvidence evidence = stats.patternEvidence.get(pattern.id());
                if (evidence != null) {
                    grouped.computeIfAbsent(pattern.id(), ignored -> new ObservationBuilder(pattern))
                            .evidence.add(new Evidence(task, evidence.timeMillis(), evidence.answer()));
                }
            }
        }));
        return grouped.values().stream().map(builder -> new PatternObservation(builder.pattern.id(),
                builder.pattern.label(), builder.pattern.rationale(), builder.evidence.stream()
                        .sorted(Comparator.comparingLong(Evidence::timeMillis)).toList()))
                .sorted(Comparator.comparing(PatternObservation::possibleMisconception).reversed()
                        .thenComparing(PatternObservation::label))
                .toList();
    }

    public static String html(LearningProgress progress, String playerName) {
        StringBuilder html = new StringBuilder("""
                <!doctype html>
                <html lang="de"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'">
                <title>Kompass-Abenteuer · Lernnotizen</title>
                <style>
                :root{color-scheme:light}*{box-sizing:border-box}body{font:16px/1.55 system-ui,sans-serif;
                max-width:1100px;margin:32px auto;padding:0 24px;color:#1d302b;background:#fff}
                h1,h2,h3{line-height:1.2;color:#234c3a}h2{margin-top:2em}h3{margin-top:1.4em}
                .note{background:#edf5ef;border-left:5px solid #47775b;padding:12px 18px}
                .observation{border:1px solid #adbdaf;padding:12px 18px;margin:16px 0;break-inside:avoid}
                .muted{color:#4a5b51}table{border-collapse:collapse;width:100%;font-size:14px;margin:16px 0}
                th,td{padding:8px;border:1px solid #adbdaf;text-align:left;vertical-align:top;overflow-wrap:anywhere}
                th{background:#edf5ef}caption{text-align:left;font-weight:600;margin:8px 0}q{overflow-wrap:anywhere}
                .scroll{overflow-x:auto}dt{font-weight:600}dd{margin-left:0;margin-bottom:12px}
                @media print{@page{size:A4;margin:15mm}body{font-size:10pt;padding:0;margin:0;max-width:none}
                table{font-size:8pt}.scroll{overflow:visible}thead{display:table-header-group}tr{break-inside:avoid}
                h2,h3{break-after:avoid}.note{background:#f3f3f3}}
                </style></head><body>
                """);
        html.append("<h1>Lernnotizen für ").append(escape(playerName)).append("</h1>");
        html.append("<p class=\"muted\">Kompass-Abenteuer · Mathematik und Deutsch · private Elternübersicht</p>");
        html.append("<div class=\"note\"><strong>Gemeinsam über Denkwege sprechen.</strong> "
                + "Diese Notizen beschreiben Antworten im Spiel. Sie sind keine Diagnose und kein Kompass-4-Testergebnis. "
                + "Eine falsche Antwort kann auch durch Raten, Lesen oder Bedienung entstehen. "
                + "Lass dir den Rechenweg oder die passende Textstelle erklären.</div>");
        Totals totals = overallTotals(progress);
        html.append("<h2>Überblick</h2><p><strong>").append(progress.completed.size())
                .append(" Aufgaben gelöst</strong> · ").append(totals.answers).append(" Antworten · ")
                .append(totals.incorrect).append(" fachliche Fehlantworten · ").append(totals.hints)
                .append(" geöffnete Hinweise · ").append(totals.procedural).append(" Bedienhinweise.</p>");
        html.append("<p><strong>Datenumfang:</strong> Erfasst werden nur die neuen Aufträge der Lernreisenden. "
                + "Für frühere Dorfaufträge oder Übungsfässer entstehen keine rückwirkenden Lernnotizen. "
                + "Die letzten 500 Ereignisse und Aufgaben-Gesamtzahlen werden im Spielstand gespeichert; "
                + "dieser Bericht zeigt die letzten 50 Ereignisse.</p>");
        html.append("<p>Beim ersten Versuch richtig: ").append(totals.firstCorrect)
                .append("; davon ohne zuvor geöffneten Hinweis: ").append(totals.firstWithoutHint)
                .append(". Hinweise sind Hilfen und werden nicht als Fehler gezählt. "
                        + "Es gibt keine Zeitwertung und keine Abzüge von Belohnungen nach Fehlversuchen.</p>");
        html.append("<h2>Übungsbereiche</h2><p>„Erster Versuch richtig“ und „davon ohne Hinweis“ sind verschiedene Angaben. "
                + "Noch nicht begonnene Aufgaben sagen nichts über das Können des Kindes aus.</p>");
        startProgressTable(html, "Fortschritt nach Übungsbereich");
        for (Skill skill : Skill.values()) {
            String subject = skill.subject() == Subject.MATH ? "Mathematik" : "Deutsch";
            progressRow(html, subject + " · " + skill.label(), totals(progress, task -> task.skill() == skill));
        }
        html.append("</tbody></table></div><h2>Anforderung und Belohnung</h2>");
        html.append("<p>Die Anforderung gehört zur Aufgabe und stuft das Kind nicht ein. Jede Aufgabe wird pro Kind "
                + "einmal belohnt; Hinweise oder mehrere Versuche verringern die Belohnung nicht. "
                + "Zusätzlich zu den Smaragden hat jede Aufgabe ein festes Geschenk, das vorab im Gespräch steht. "
                + "Die aktuellen Geschenkpakete gelten für neu belohnte Aufgaben ab Version 0.3.1. "
                + "Bereits zuvor belohnte Aufgaben erhalten keine nachträgliche Auszahlung.</p>");
        startProgressTable(html, "Fortschritt nach Anforderung");
        for (Difficulty difficulty : Difficulty.values()) {
            progressRow(html, difficulty.label() + " · " + RewardCatalog.tierDescription(difficulty),
                    totals(progress, task -> task.difficulty() == difficulty));
        }
        html.append("</tbody></table></div>");
        html.append("<h2>Beobachtungen zu möglichen Misskonzepten</h2><p>Ein Muster wird erst als möglicher "
                + "Gesprächsanlass hervorgehoben, wenn dazu passende Antworten in mindestens zwei verschiedenen "
                + "Aufgaben vorliegen. Mehrere Antworten zur selben Aufgabe zählen nur einmal. Auch dann bleibt "
                + "die Deutung eine Hypothese, keine Diagnose. Korrekt gelöste Antworten lösen kein Fehlermuster aus. "
                + "Je Aufgabe und Muster bleibt die letzte passende Fehlantwort als Beispiel sichtbar. "
                + "Eine spätere richtige Antwort belegt kein dauerhaftes Beherrschen; es können weitere Fehler gefolgt sein. "
                + "Die letzten Antworten stehen im Verlauf.</p>");
        List<PatternObservation> observations = observations(progress);
        if (observations.isEmpty()) {
            html.append("<p>Bisher wurde kein hinterlegtes Fehlermuster beobachtet. Daraus lässt sich keine sichere "
                    + "Aussage über das Beherrschen eines Bereichs ableiten.</p>");
        }
        for (PatternObservation observation : observations) {
            html.append("<article class=\"observation\"><h3>")
                    .append(escape(observation.label())).append("</h3><p><strong>")
                    .append(observation.possibleMisconception()
                            ? "Möglicher Gesprächsanlass – noch kein Nachweis eines Misskonzepts."
                            : "Einzelbeobachtung, noch kein hinreichendes Muster.")
                    .append("</strong> Passende Antworten bei ").append(observation.evidence().size())
                    .append(" verschiedenen Aufgaben.</p><p>").append(escape(observation.rationale()))
                    .append("</p><dl>");
            for (Evidence evidence : observation.evidence()) {
                html.append("<dt>").append(escape(evidence.task().title())).append(" · ")
                        .append(escape(date(evidence.timeMillis()))).append("</dt><dd>")
                        .append(escape(evidence.task().question())).append("<br>Antwort: <q>")
                        .append(escape(evidence.answer())).append("</q> · Erwartet: <q>")
                        .append(escape(evidence.task().expectedAnswer())).append("</q><br>")
                        .append("Nach diesem letzten passenden Fehlversuch mindestens einmal richtig beantwortet: ")
                        .append(wasAnsweredCorrectlyAfter(progress, evidence) ? "ja" : "nicht beobachtet")
                        .append(". Dies beschreibt den historischen Verlauf, nicht das Ergebnis der aktuellsten Wiederholung.</dd>");
            }
            html.append("</dl><p>Lass dir erklären, wie das Kind zu dieser Antwort gekommen ist. "
                    + "Eine neue Aufgabe mit anderem Inhalt hilft beim gemeinsamen Nachprüfen.</p></article>");
        }
        html.append("<p>Nicht jede falsche Antwort passt zu einem bekannten Muster. ")
                .append(unmatchedAnswers(progress))
                .append(" Fehlantworten in den gespeicherten letzten ").append(progress.attempts.size())
                .append(" Ereignissen haben keine hinterlegte Zuordnung. Bedienfragen, etwa ein anderer "
                        + "Gegenstand in der Hand, werden getrennt erfasst.</p>");
        appendTaskDetails(html, progress);
        appendEvents(html, progress);
        html.append("<h2>Umgang mit diesen Notizen</h2><p>Die Daten gehören zum gespeicherten Minecraft-Spielstand. "
                + "Dieser HTML-Bericht ist eine Momentaufnahme und aktualisiert sich nicht von selbst. "
                + "Er enthält Antworten und sollte nur mit Personen geteilt werden, die das Kind begleiten. "
                + "Der Bericht lädt keine externen Ressourcen und sendet keine Daten. "
                + "Zeitangaben: Europe/Berlin; Zeiten werden nicht bewertet.</p></body></html>");
        return html.toString();
    }

    private static void appendTaskDetails(StringBuilder html, LearningProgress progress) {
        html.append("<h2>Notizen zu einzelnen Aufgaben</h2><div class=\"scroll\"><table><thead><tr>"
                + "<th>Aufgabe</th><th>Status</th><th>Antworten / davon falsch</th><th>Hinweise</th>"
                + "<th>Bedienfragen</th><th>Erster Versuch richtig / ohne Hinweis</th></tr></thead><tbody>");
        progress.stats.forEach((taskId, stats) -> {
            LearningTask task = LearningCatalog.byId(taskId).orElse(null);
            html.append("<tr><td>").append(escape(task == null ? "Frühere Aufgabe: " + taskId : task.title()))
                    .append("</td><td>").append(progress.completed.contains(taskId) ? "Gelöst" : "Begonnen")
                    .append(progress.rewarded.contains(taskId) ? " · Belohnung vermerkt" : "")
                    .append("</td><td>").append(stats.attempts).append(" / ").append(stats.incorrect)
                    .append("</td><td>").append(stats.hints).append("</td><td>").append(stats.proceduralIssues)
                    .append("</td><td>").append(stats.firstAttemptCorrect == null ? "Noch keine Antwort"
                            : Boolean.TRUE.equals(stats.firstAttemptCorrect)
                                    ? "Ja / " + (stats.firstAttemptHintUsed ? "nein" : "ja") : "Nein / nein")
                    .append("</td></tr>");
        });
        html.append("</tbody></table></div>");
        if (progress.stats.isEmpty()) {
            html.append("<p>Noch keine Lerninteraktion gespeichert.</p>");
        }
    }

    private static void appendEvents(StringBuilder html, LearningProgress progress) {
        int shown = Math.min(50, progress.attempts.size());
        html.append("<h2>Letzte ").append(shown).append(" Ereignisse</h2><p>Neueste zuerst. "
                + "Bis zu 500 Ereignisse werden gespeichert; Aufgaben-Gesamtzahlen und je Aufgabe eine "
                + "Beobachtung pro Fehlermuster bleiben darüber hinaus erhalten. Freitext wird auf 128 Zeichen begrenzt.</p>");
        html.append("<div class=\"scroll\"><table><thead><tr><th>Zeit</th><th>Aufgabe</th><th>Ereignis</th>"
                + "<th>Antwort / Notiz</th><th>Erwartete Antwort</th></tr></thead><tbody>");
        for (int index = progress.attempts.size() - 1; index >= progress.attempts.size() - shown; index--) {
            Attempt event = progress.attempts.get(index);
            LearningTask task = LearningCatalog.byId(event.taskId()).orElse(null);
            String label = switch (event.type()) {
                case HINT -> "Hinweis geöffnet";
                case PROCEDURAL -> "Bedienfrage – kein fachlicher Fehler";
                case ANSWER -> event.correct() ? "Richtig" : "Noch nicht richtig";
            };
            if (event.type() == AttemptType.ANSWER && event.hintUsed()) {
                label += " · mit Hinweis";
            }
            html.append("<tr><td>").append(escape(date(event.timeMillis()))).append("</td><td>")
                    .append(escape(task == null ? "Frühere Aufgabe: " + event.taskId() : task.title()));
            if (task != null) {
                html.append("<br>").append(escape(task.question()));
            }
            html.append("</td><td>").append(escape(label)).append("</td><td>")
                    .append(escape(event.answer())).append("</td><td>")
                    .append(event.type() == AttemptType.ANSWER && task != null ? escape(task.expectedAnswer()) : "—")
                    .append("</td></tr>");
        }
        html.append("</tbody></table></div>");
    }

    private static void startProgressTable(StringBuilder html, String caption) {
        html.append("<div class=\"scroll\"><table><caption>").append(escape(caption))
                .append("</caption><thead><tr><th>Bereich</th><th>Begonnen / verfügbar</th><th>Gelöst</th>"
                        + "<th>Erster Versuch richtig</th><th>Davon ohne Hinweis</th><th>Fehlantworten</th>"
                        + "<th>Hinweise</th></tr></thead><tbody>");
    }

    private static void progressRow(StringBuilder html, String label, Totals totals) {
        html.append("<tr><td>").append(escape(label)).append("</td><td>").append(totals.started)
                .append(" / ").append(totals.available).append("</td><td>").append(totals.solved)
                .append("</td><td>").append(totals.firstCorrect).append("</td><td>")
                .append(totals.firstWithoutHint).append("</td><td>").append(totals.incorrect)
                .append("</td><td>").append(totals.hints).append("</td></tr>");
    }

    private static Totals totals(LearningProgress progress, Predicate<LearningTask> filter) {
        Totals result = new Totals();
        for (LearningTask task : LearningCatalog.all()) {
            if (!filter.test(task)) {
                continue;
            }
            result.available++;
            if (progress.completed.contains(task.id())) {
                result.solved++;
            }
            TaskStats stats = progress.stats.get(task.id());
            if (stats != null) {
                addStats(result, stats);
            }
        }
        return result;
    }

    private static Totals overallTotals(LearningProgress progress) {
        Totals result = totals(progress, ignored -> true);
        // Archived tasks remain in overall totals, without guessing their former skill or difficulty.
        progress.stats.forEach((taskId, stats) -> {
            if (LearningCatalog.byId(taskId).isEmpty()) {
                addStats(result, stats);
            }
        });
        return result;
    }

    private static void addStats(Totals totals, TaskStats stats) {
        totals.started++;
        totals.answers += stats.attempts;
        totals.incorrect += stats.incorrect;
        totals.hints += stats.hints;
        totals.procedural += stats.proceduralIssues;
        if (Boolean.TRUE.equals(stats.firstAttemptCorrect)) {
            totals.firstCorrect++;
            if (!stats.firstAttemptHintUsed) {
                totals.firstWithoutHint++;
            }
        }
    }

    private static long unmatchedAnswers(LearningProgress progress) {
        return progress.attempts.stream().filter(event -> event.type() == AttemptType.ANSWER
                && !event.correct() && event.patternIds().isEmpty()).count();
    }

    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String compact(String text, int maxCharacters) {
        String value = text == null ? "" : text.replaceAll("\\s+", " ").strip();
        return value.length() > maxCharacters ? value.substring(0, maxCharacters - 1) + "…" : value;
    }

    private static String date(long timestamp) {
        return DATE.format(Instant.ofEpochMilli(timestamp));
    }

    private static boolean wasAnsweredCorrectlyAfter(LearningProgress progress, Evidence evidence) {
        TaskStats stats = progress.stats.get(evidence.task().id());
        if (stats == null) {
            return false;
        }
        Long correctAt = stats.lastCorrectAt == null ? stats.solvedAt : stats.lastCorrectAt;
        return correctAt != null && correctAt > evidence.timeMillis();
    }

    public record Evidence(LearningTask task, long timeMillis, String answer) {}

    public record PatternObservation(String id, String label, String rationale, List<Evidence> evidence) {
        public PatternObservation {
            evidence = List.copyOf(evidence);
        }

        public boolean possibleMisconception() {
            return evidence.stream().map(item -> item.task().id()).distinct().count() >= 2;
        }
    }

    private static final class ObservationBuilder {
        private final ErrorPattern pattern;
        private final List<Evidence> evidence = new ArrayList<>();

        private ObservationBuilder(ErrorPattern pattern) {
            this.pattern = pattern;
        }
    }

    private static final class Totals {
        int available;
        int started;
        int solved;
        int answers;
        int incorrect;
        int hints;
        int procedural;
        int firstCorrect;
        int firstWithoutHint;
    }
}
