#!/usr/bin/env python3
"""Printable A4 parent and child guides for Kompass-Abenteuer 0.3.4."""
from pathlib import Path
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "output" / "pdf"
RUNTIME = Path("/Users/lars.thoms/.cache/codex-runtimes/codex-primary-runtime/dependencies")
FONT_DIR = RUNTIME / "node/node_modules/pdfjs-dist/standard_fonts"
MONO = RUNTIME / "native/libreoffice-headless/libreoffice/LibreOfficeDev.app/Contents/Resources/fonts/truetype/LiberationMono-Regular.ttf"
W, H = A4
M = 44
CW = W - 2 * M
INK = colors.HexColor("#20312C")
MUTED = colors.HexColor("#52645C")
GREEN = colors.HexColor("#316A4D")
PALE = colors.HexColor("#EDF4EB")
BLUE = colors.HexColor("#286C8C")
SKY = colors.HexColor("#EAF4F9")
TAN = colors.HexColor("#FAF1DE")
LINE = colors.HexColor("#CED8D0")
ORANGE = colors.HexColor("#8A581C")
INSTALLER = "https://fabricmc.net/use/installer/"
API = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.162.0%2B26.3/fabric-api-0.162.0%2B26.3.jar"
DOC_MODS = "https://docs.fabricmc.net/players/installing-mods"
DOC_JAVA = "https://docs.fabricmc.net/players/installing-java/"
RELEASES = "https://github.com/larsjochenthoms/MCKompass/releases/latest"
SETUP = "https://github.com/larsjochenthoms/MCKompass/blob/main/docs/github-releases.md"


def t(c, x, y, text, size=11, bold=False, color=INK):
    c.setFont("GuideBold" if bold else "Guide", size)
    c.setFillColor(color)
    c.drawString(x, y, text)


def para(c, text, x, y, width, size=11.3, leading=None, gap=8, color=INK):
    style = ParagraphStyle("p", fontName="Guide", fontSize=size,
                           leading=leading or size * 1.34, textColor=color,
                           splitLongWords=False, allowWidows=0, allowOrphans=0)
    p = Paragraph(text, style)
    _, height = p.wrap(width, 2000)
    assert y - height >= 47, (text[:65], y, height)
    p.drawOn(c, x, y - height)
    return y - height - gap


def link(url, label):
    return f'<a href="{escape(url, {chr(34): "&quot;"})}" color="#286C8C"><u>{escape(label)}</u></a>'


def panel(c, text, y, fill=PALE, size=11.3, title=None, child=False):
    title_h = 23 if title else 0
    p = Paragraph(text, ParagraphStyle("panel", fontName="Guide", fontSize=size,
                                     leading=size * 1.34, textColor=INK))
    _, height = p.wrap(CW - 28, 2000)
    box_h = height + 24 + title_h
    assert y - box_h >= 47, (title, y, box_h)
    c.setFillColor(fill)
    c.roundRect(M, y - box_h, CW, box_h, 9, fill=1, stroke=0)
    if title:
        t(c, M + 14, y - 23, title, 15 if child else 12.5, True)
    p.drawOn(c, M + 14, y - 12 - title_h - height)
    return y - box_h - 12


def step(c, number, title, body, y, child=False, color=GREEN):
    c.setFillColor(color)
    c.circle(M + 12, y - 12, 12, fill=1, stroke=0)
    c.setFillColor(colors.white)
    c.setFont("GuideBold", 12)
    c.drawCentredString(M + 12, y - 16, str(number))
    title_end = para(c, f"<b>{title}</b>", M + 37, y, CW - 37,
                     16 if child else 12.5, gap=5)
    return para(c, body, M + 37, title_end, CW - 37,
                14.2 if child else 11.3, gap=13)


def heading(c, title, subtitle, page, pages, child=False, color=GREEN):
    c.setPageSize(A4)
    t(c, M, H - 42, "KOMPASS-ABENTEUER", 9, True, color)
    c.setFont("Guide", 9)
    c.setFillColor(MUTED)
    c.drawRightString(W - M, H - 42, "FÜR DICH" if child else "FÜR ELTERN")
    t(c, M, H - 82, title, 29 if child else 27, True)
    para(c, subtitle, M, H - 96, CW, 13.5 if child else 11.5, gap=0, color=MUTED)
    c.setStrokeColor(LINE)
    c.setLineWidth(0.7)
    c.line(M, H - 138, W - M, H - 138)
    c.line(M, 41, W - M, 41)
    t(c, M, 27, "Version 0.3.4  |  Minecraft Java 26.3", 8, color=MUTED)
    c.setFont("Guide", 8)
    c.drawRightString(W - M, 27, f"{page} / {pages}")
    return H - 155


def travellers(c, y):
    """Small original block figures, drawn as vectors for a sharp printed guide."""
    height = 132
    width = (CW - 20) / 3
    for index, (name, accent, pale) in enumerate([
        ("Mila", GREEN, PALE), ("Jaro", BLUE, SKY), ("Lio", ORANGE, TAN)
    ]):
        x = M + index * (width + 10)
        c.setFillColor(pale)
        c.roundRect(x, y - height, width, height, 9, fill=1, stroke=0)
        cx = x + width / 2
        c.setFillColor(accent)
        c.rect(cx - 17, y - 99, 34, 40, fill=1, stroke=0)
        c.setFillColor(colors.HexColor("#D8B891"))
        c.rect(cx - 20, y - 66, 40, 36, fill=1, stroke=0)
        c.setFillColor(colors.HexColor("#5D493D"))
        c.rect(cx - 20, y - 34, 40, 10, fill=1, stroke=0)
        c.setFillColor(INK)
        c.rect(cx - 12, y - 48, 5, 4, fill=1, stroke=0)
        c.rect(cx + 7, y - 48, 5, 4, fill=1, stroke=0)
        c.setFillColor(colors.HexColor("#B18F6C"))
        c.rect(cx - 4, y - 58, 8, 10, fill=1, stroke=0)
        c.setFont("GuideBold", 16)
        c.setFillColor(accent)
        c.drawCentredString(cx, y - 119, name)
    return y - height - 17


def rewards(c, y):
    height = 77
    width = (CW - 20) / 3
    for index, (label, value, bonus, accent, pale) in enumerate([
        ("Einfach", "2 Smaragde", "+ Vorräte", GREEN, PALE),
        ("Mittel", "4 Smaragde", "+ Ausrüstung", BLUE, SKY),
        ("Knifflig", "6 Smaragde", "+ wertvolle Schätze", ORANGE, TAN)
    ]):
        x = M + index * (width + 10)
        c.setFillColor(pale)
        c.roundRect(x, y - height, width, height, 9, fill=1, stroke=0)
        c.setFont("GuideBold", 16)
        c.setFillColor(accent)
        c.drawCentredString(x + width / 2, y - 24, label)
        c.setFont("Guide", 14)
        c.drawCentredString(x + width / 2, y - 45, value)
        c.setFont("Guide", 11.2)
        c.drawCentredString(x + width / 2, y - 65, bonus)
    return y - height - 16


def hotbar(c, y):
    height = 132
    c.setFillColor(TAN)
    c.roundRect(M, y - height, CW, height, 9, stroke=0, fill=1)
    t(c, M + 15, y - 24, "Wähle deinen Stapel in der unteren Reihe.", 14, True)
    s, x0, by = 29, M + 20, y - 79
    for i in range(9):
        c.setStrokeColor(ORANGE if i == 2 else colors.HexColor("#9F9686"))
        c.setLineWidth(2.3 if i == 2 else 0.7)
        c.setFillColor(colors.white)
        c.rect(x0 + i * s, by, s - 3, s - 3, stroke=1, fill=1)
    cx, cy = x0 + 2 * s + 6, by + 5
    c.setFillColor(colors.HexColor("#8C9792"))
    c.rect(cx, cy, 15, 15, fill=1, stroke=0)
    c.setFillColor(colors.HexColor("#BEC7C1"))
    c.rect(cx + 2, cy + 8, 5, 5, fill=1, stroke=0)
    c.rect(cx + 9, cy + 2, 4, 5, fill=1, stroke=0)
    para(c, "<b>Gelber Rahmen</b><br/>Diesen Stapel hältst<br/>du in der Haupthand.", M + 313, y - 47, 170, 12.5, gap=0)
    t(c, M + 20, y - 116, "Auswählen: Mausrad drehen oder die passende Zifferntaste drücken.", 10.3, color=ORANGE)
    return y - height - 16


def parents(path):
    c = canvas.Canvas(str(path), pagesize=A4, pageCompression=1)
    c.setTitle("Kompass-Abenteuer - Einrichten und begleiten für Eltern")
    c.setAuthor("Kompass-Abenteuer")
    y = heading(c, "Einrichten für Eltern", "Lernreisende in der normalen Minecraft-Welt - ohne Dorfaufbau.", 1, 5)
    y = panel(c, "<b>Version 0.3.4</b><br/>Minecraft <b>Java Edition 26.3</b> · Java <b>25</b><br/>Fabric Loader <b>ab 0.19.5</b> · Fabric API <b>0.162.0+26.3</b>", y)
    y = step(c, 1, "Minecraft und Fabric vorbereiten", "Minecraft Java 26.3 im offiziellen Launcher einmal starten und wieder schließen. Danach den " + link(INSTALLER, "offiziellen Fabric Installer") + " verwenden: Client, Minecraft 26.3 und Loader ab 0.19.5 wählen. Unter Windows ist der EXE-Installer der einfache Einstieg.", y)
    y = step(c, 2, "Den passenden Spielordner merken", "Die Fabric-Installation im Launcher auswählen und ihren eingestellten <b>Spielordner</b> nachsehen. Ein eigener Spielordner für das Abenteuer erleichtert die Übersicht. Minecraft jetzt vollständig beenden; bei einer vorhandenen Welt den gesamten Weltordner sichern.", y)
    y = step(c, 3, "Einrichtungshilfe herunterladen", "Unter " + link(RELEASES, "GitHub Releases: neueste Version") + " die passende Datei laden: <b>install-simple-mod-sync.ps1</b> für Windows oder <b>install-simple-mod-sync.command</b> für Mac. Beide gehören zu unserem Repository <b>larsjochenthoms/MCKompass</b>.", y)
    y = panel(c, "<b>Was die Einrichtung übernimmt</b><br/>Sie installiert die Kompass-Mod, die passende Fabric API und Simple Mod Sync im gewählten Spielordner. Bisherige verwaltete JARs und die Sync-Konfiguration werden zuvor unter <b>.kompass-backups</b> im Spielordner gesichert. Das ersetzt keine Sicherung Ihrer Welt.", y, SKY, size=10.9)
    y = step(c, 4, "Die Hilfe mit dem Spielordner starten", "Auf Seite 2 stehen die Schritte für Windows und Mac. Nach der Einrichtung die Fabric-Installation starten. Nach einem durch Simple Mod Sync geladenen Update Minecraft vollständig neu starten, bevor die Welt geöffnet wird.", y)
    para(c, "<b>Ohne automatische Updates:</b> Aus demselben Release die normale <b>kompass4mc-0.3.4.jar</b> und die " + link(API, "passende Fabric API") + " in den mods-Unterordner legen. Nur eine Kompass-JAR verwenden. Die Datei mit <b>-sources</b> ist keine installierbare Mod; JARs nicht entpacken oder einzeln starten.", M, y, CW, 10.2, color=MUTED)
    c.showPage()

    y = heading(c, "Updates über GitHub", "Einmal einrichten; später beim Start die freigegebene Version prüfen.", 2, 5)
    y = para(c, "<b>Windows: PowerShell 5.1 oder neuer</b><br/>Im Ordner der heruntergeladenen Datei eine PowerShell öffnen. Den folgenden Befehl eingeben und den Beispielpfad durch den tatsächlichen Fabric-Spielordner ersetzen:", M, y, CW, 11.2, gap=10)
    y = panel(c, "<font name='GuideMono' size='9.1'>powershell -NoProfile -ExecutionPolicy Bypass<br/>-File .\\install-simple-mod-sync.ps1<br/>-GameDirectory 'C:\\Minecraft\\Kompass'</font><br/><br/>Als <b>einen Befehl in einer Zeile</b> eingeben. Der Pfad ist ein Beispiel. Den Befehl nur mit der Datei aus unserem GitHub-Release verwenden.", y, SKY, size=10.5)
    y = para(c, "<b>Mac: macOS 12 oder neuer</b><br/><b>install-simple-mod-sync.command</b> doppelklicken und den Fabric-Spielordner angeben. Falls der Doppelklick nicht startet, im Terminal <font name='GuideMono'>bash</font> und ein Leerzeichen eingeben, die heruntergeladene Datei hineinziehen und Enter drücken. Danach den Spielordner eingeben.", M, y, CW, 11.2, gap=12)
    y = panel(c, "<b>Nach der Einrichtung</b><br/>Die Fabric-Installation starten. Simple Mod Sync prüft die freigegebene Liste auf GitHub. Wurde etwas aktualisiert, Minecraft <b>vollständig schließen und erneut starten</b>, bevor Sie die Welt öffnen. Im Spiel zeigt <font name='GuideMono'>/kompass</font> die geladene Version.", y, PALE, size=11.0)
    y = para(c, "<b>Bei späteren Updates</b><br/>Vor einer neuen Version die Welt sichern. Die Einrichtung gilt nur für den gewählten Spielordner. Ein anderer PC oder eine andere Installation braucht eine eigene Einrichtung. Bei Updateproblemen Minecraft schließen und die Einrichtungshilfe erneut mit demselben Spielordner ausführen.", M, y, CW, 10.9, gap=12)
    y = para(c, "<b>Die Konfiguration liegt lokal</b><br/><font name='GuideMono' size='9.8'>config/simplemodsync.json</font> im Spielordner. Die dort eingestellte Adresse bleibt gleich und verweist auf die neueste stabile Veröffentlichung. Das Update lädt Mod-Dateien; Welten, Antworten und Elternberichte werden dadurch nicht hochgeladen.", M, y, CW, 10.9, gap=12)
    y = para(c, "Die ausführliche " + link(SETUP, "GitHub- und Update-Anleitung") + " enthält die vollständigen Befehle und den Ablauf für neue Veröffentlichungen. Für das erste Update sollte ein Erwachsener dabei sein und den Spielstart prüfen.", M, y, CW, 10.5, gap=12)
    para(c, "Auf macOS wurde das echte Update von 0.3.1 auf 0.3.2 mit Neustart geprüft. Automatisierte Minecraft-Tests prüfen Aufgaben und Spielabläufe ohne Bildschirmsteuerung. Installation, Neustart und eine Aufgabe auf Ihrem Spiel-PC zuerst gemeinsam in einer gesicherten Welt ausprobieren.", M, y, CW, 9.8, color=MUTED)
    c.showPage()

    y = heading(c, "Einfach weiterspielen", "Die Mod fügt Begegnungen in bestehende und neue Welten ein.", 3, 5)
    y = step(c, 1, "Die Welt mit Fabric starten", "Dieselbe Welt oder eine neue Einzelspielerwelt öffnen. <b>Survival und Kreativ</b> sind möglich. Für automatische Lernreisende sind keine Befehle nötig. Zum ruhigen Einstieg kann eine friedliche Schwierigkeit hilfreich sein.", y)
    y = step(c, 2, "Eine Begegnung abwarten", "Auf eine freie, trockene Oberfläche in der <b>Oberwelt</b> gehen. Nach etwa <b>20 Sekunden</b> wird erstmals ein Platz in der Nähe gesucht; weitere Versuche folgen etwa alle <b>zwei Minuten</b>. In Höhlen oder unter Wasser kann kein passender Platz entstehen.", y)
    y = panel(c, "<b>Mila, Jaro und Lio sind auf Lernreise.</b><br/>Sie erscheinen ungefähr 8 bis 24 Blöcke entfernt. Vorhandene Blöcke werden nicht verändert. Im Umkreis von 64 Blöcken wird kein weiterer Lernreisender hinzugefügt. Die Figuren bleiben an ihrem Begegnungsort und können später weiterziehen.", y, PALE)
    y = step(c, 3, "Gemeinsam ein Gespräch ausprobieren", "Bis auf wenige Blöcke herangehen und Rechtsklick auf die Figur. <b>Mathematik</b> oder <b>Deutsch</b> mit einer Anforderung wählen: <b>Einfach</b>, <b>Mittel</b> oder <b>Knifflig</b>. Im Einzelspieler pausiert das Gespräch die Welt; im Mehrspieler läuft sie weiter. Bei Stapelaufgaben werden die Gegenstände nur gezeigt und bleiben erhalten.", y)
    y = panel(c, "<b>Smaragde und Sachbelohnungen</b><br/>Einfach: <b>2 Smaragde</b> plus Vorräte, etwa Brot oder Fackeln. Mittel: <b>4 Smaragde</b> plus Rohstoffe oder Ausrüstung. Knifflig: <b>6 Smaragde</b> plus wertvolle Dinge, etwa ein Buch mit <b>Reparatur I (Mending)</b> oder verzauberte Diamantausrüstung.<br/><br/>Das feste Paket steht vorab im Auftrag. Keine Zufallsziehung, keine Abzüge für Fehler oder Hinweise; pro Spieler und Aufgabe einmal. Beim Update keine Nachzahlung für bereits belohnte Aufgaben.", y, SKY, size=10.8)
    y = para(c, "<b>Fortsetzen und nachsehen</b><br/><font name='GuideMono'>/kompass lernen</font> setzt eine offene Aufgabe bei einem nahen Lernreisenden fort; höchstens sechs Blöcke entfernt stehen. <font name='GuideMono'>/kompass tagebuch</font> zeigt den persönlichen Fortschritt. <font name='GuideMono'>/kompass suchen</font> hilft beim Wiederfinden. Zum Prüfen der geladenen Version: <font name='GuideMono'>/kompass</font>.", M, y, CW, 10.8)
    panel(c, "<b>Nur für die Spielleitung:</b> Mit <font name='GuideMono'>/kompass begegnung</font> lässt sich eine Begegnung versuchen. Der alte Dorfmodus bleibt optional über <font name='GuideMono'>/kompass dorf</font> erhalten. Beide Befehle benötigen aktivierte Befehle beziehungsweise Spielleitungsrechte.", y, TAN, size=10.5)
    c.showPage()

    y = heading(c, "Lernfortschritt begleiten", "Antworten, Hilfen und Denkwege gemeinsam besprechen.", 4, 5)
    y = panel(c, "<b>Elternfunktionen benötigen Spielleitungsrechte (Stufe 2).</b><br/>Im Einzelspieler dafür Befehle aktivieren. Ein Kind mit denselben Rechten kann die Übersicht ebenfalls öffnen; ein eigenes Elternkennwort gibt es nicht.", y, SKY)
    y = para(c, "<b>Übersicht im Spiel</b><br/><font name='GuideMono'>/kompass eltern</font><br/><font name='GuideMono'>/kompass eltern spieler &lt;Name&gt;</font><br/>Der Name ist der gespeicherte Minecraft-Spielername. Sichtbar sind begonnene und gelöste Aufgaben, erste Antworten, Hinweise sowie Schwierigkeiten. Hinweise sind Hilfen; Bedienfragen werden getrennt von fachlichen Fehlantworten erfasst.", M, y, CW, 11.2, gap=12)
    y = para(c, "<b>Einen Bericht zum Lesen oder Drucken speichern</b><br/><font name='GuideMono'>/kompass eltern export</font><br/><font name='GuideMono'>/kompass eltern export &lt;Name&gt;</font><br/>Im Weltordner entsteht <b>kompass-berichte/Spieler-UUID.html</b>. Die UUID ist eine lange Spielerkennung. Die Datei mit einem Browser öffnen. Im Einzelspieler liegt der Weltordner normalerweise unter <b>Spielordner/saves/Weltname/</b>. Beim Exportknopf kann Minecraft den geschützten Befehl zusätzlich bestätigen lassen: angezeigten Exportbefehl prüfen und bestätigen.", M, y, CW, 11.0, gap=10)
    y = panel(c, "<b>Mögliche Misskonzepte vorsichtig einordnen</b><br/>Bestimmte falsche Antworten können zu einem bekannten Fehlermuster passen, etwa Fläche statt Umfang. Erst passende Antworten bei <b>zwei verschiedenen Aufgaben</b> werden als möglicher Gesprächsanlass hervorgehoben. Mehrere Versuche derselben Aufgabe zählen dafür nur einmal.<br/><br/>Das ist eine <b>Hypothese, keine Diagnose</b>. Fragen Sie: „Wie hast du gerechnet?“ oder „Welche Textstelle hilft dir?“ Auch Lesen, Raten oder Bedienung können eine Antwort erklären.", y, PALE)
    y = para(c, "<b>Neu in 0.3.4:</b> Auch nach allen 20 Lernbereichen bleibt in der kurzen Spielübersicht die wichtigste Beobachtung mit Beispiel sichtbar; weitere Beobachtungen stehen im vollständigen HTML-Bericht. Dort ist auch sichtbar, ob eine zunächst schwierige Aufgabe <b>später gelöst</b> wurde. Nicht jede falsche Antwort ist erklärt. Für alte Dorfprojekte und Übungsfässer wird kein früherer Antwortverlauf erzeugt. Die Übungen sind keine offiziellen Kompass-4-Aufgaben.", M, y, CW, 10.3, gap=10)
    y = para(c, "<b>Begegnungen steuern</b><br/><font name='GuideMono'>/kompass eltern begegnungen aus</font><br/><font name='GuideMono'>/kompass eltern begegnungen an</font><br/>Damit automatische neue Begegnungen abschalten oder wieder einschalten.", M, y, CW, 10.7, gap=9)
    para(c, "Die Lernnotizen bleiben lokal im Spielstand. Ein HTML-Export ist eine Momentaufnahme und sendet keine Daten. Nach weiterem Spielen erneut exportieren. Berichte nur mit den Personen teilen, die das Kind begleiten.", M, y, CW, 9.7, color=MUTED)
    c.showPage()

    y = heading(c, "Wenn etwas hakt", "Ein kurzer Check genügt oft für den nächsten Spielversuch.", 5, 5)
    for title, body in [
        ("Keine Begegnung zu sehen", "Oberwelt und freie trockene Oberfläche prüfen, etwas abwarten. Steht bereits ein Lernreisender in der Nähe, wird kein zweiter erzeugt. Als Spielleitung /kompass begegnung versuchen oder automatische Begegnungen wieder einschalten."),
        ("Rechtsklick öffnet kein Gespräch", "Mit /kompass die Version 0.3.4 prüfen. Näher an die Figur herangehen. Nach einem Update Minecraft vollständig neu starten und doppelte Kompass-JAR-Dateien entfernen."),
        ("Stapel wird nicht erkannt", "Material und Menge im Auftrag lesen. Genau einen passenden Stapel in der unteren Reihe auswählen, sodass er in der Haupthand liegt. Danach den Lernreisenden ansprechen und den Stapel zeigen."),
        ("Elternbefehl wird nicht erlaubt", "Dieser Bereich braucht Spielleitungsrecht / Stufe 2. Im Einzelspieler sind aktivierte Befehle nötig. Zum Nachsehen den gespeicherten Minecraft-Spielernamen verwenden."),
        ("Minecraft meldet unpassende Mods", "Java Edition 26.3, Fabric Loader ab 0.19.5, Fabric API für 26.3 und die JAR von Seite 1 prüfen. Den tatsächlichen Spielordner der gewählten Fabric-Installation verwenden."),
    ]:
        y = para(c, f"<b>{title}</b><br/>{body}", M, y, CW, 10.9, gap=11)
    y = panel(c, "<b>Pause und Sicherung</b><br/>Normal speichern und beenden. Beim nächsten Start dieselbe Welt wählen. Für eine Sicherung bei geschlossenem Spiel den gesamten Weltordner kopieren; <b>kompass4mc.json</b> und die Bewohnerdaten gehören dazu. Eine offene Aufgabe kann bei einem nahen Lernreisenden mit <b>/kompass lernen</b> fortgesetzt werden.", y, PALE, size=10.6)
    y = para(c, "<b>Standardpfade ohne eigenen Spielordner</b><br/>Windows: <font name='GuideMono' size='9.2'>%appdata%\\.minecraft\\mods</font><br/>macOS: <font name='GuideMono' size='9.2'>~/Library/Application Support/minecraft/mods</font><br/>Linux: <font name='GuideMono' size='9.2'>~/.minecraft/mods</font>", M, y, CW, 10.2, gap=11)
    para(c, "<b>Offizielle Hilfe</b><br/>" + link(INSTALLER, "Fabric Installer") + " · " + link(DOC_MODS, "Mods und Spielordner") + " · " + link(DOC_JAVA, "Java installieren") + "<br/>Stand: 19.10.2026. Optische Lesbarkeit, Windows-Spieltest und alter Dorfmodus sind noch ungeprüft. Das erste Gespräch und eine Aufgabe gemeinsam ausprobieren.", M, y, CW, 9.4, color=MUTED)
    c.showPage()
    c.save()


def children(path):
    c = canvas.Canvas(str(path), pagesize=A4, pageCompression=1)
    c.setTitle("Kompass-Abenteuer - Deine Spielanleitung")
    c.setAuthor("Kompass-Abenteuer")
    y = heading(c, "Dein Lernabenteuer", "Entdecke Reisende und hilf bei ihren Plänen!", 1, 4, True)
    y = travellers(c, y)
    y = panel(c, "Mila, Jaro und Lio reisen durch die Minecraft-Welt. Sie brauchen Hilfe beim Planen, Rechnen und Lesen. Du kannst sie beim normalen Spielen treffen.", y, size=14)
    y = step(c, 1, "Schau dich draußen um", "Auf einer freien Wiese in der Oberwelt kann ein Lernreisender erscheinen. Das kann einen Moment dauern. Du brauchst dafür kein Dorf zu bauen.", y, True)
    y = step(c, 2, "Beginne ein Gespräch", "Geh nah an die Figur heran. Klicke sie mit der <b>rechten Maustaste</b> an. Jetzt kannst du lesen, was auf ihrer Reise passiert.", y, True)
    y = panel(c, "<b>Deine wichtigsten Tasten</b><br/><b>W A S D</b> gehen · <b>Maus</b> umsehen<br/><b>Leertaste</b> springen · <b>Rechtsklick</b> sprechen<br/><b>E</b> Inventar öffnen · <b>1 bis 9</b> etwas auswählen", y, SKY, size=12.8)
    para(c, "Das sind die Standardtasten am Computer. Deine Eltern können dir zeigen, welche Tasten bei dir eingestellt sind.", M, y, CW, 11, color=MUTED)
    c.showPage()

    y = heading(c, "Hilf mit deinem Wissen", "Du darfst überlegen, nachfragen und es noch einmal versuchen.", 2, 4, True, BLUE)
    y = rewards(c, y)
    y = step(c, 1, "Wähle einen Auftrag", "Wähle <b>Mathematik</b> oder <b>Deutsch</b> und dazu <b>Einfach</b>, <b>Mittel</b> oder <b>Knifflig</b>. Lies die Geschichte und die Frage genau. Ein Blatt Karopapier kann beim Rechnen helfen.", y, True, BLUE)
    y = step(c, 2, "Besprich deine Antwort", "Manchmal tippst du eine Zahl ein und wählst <b>Antwort besprechen</b>. Manchmal klickst du eine Antwort mit <b>A, B, C oder D</b> an. Passt sie zur Frage?", y, True, BLUE)
    y = step(c, 3, "Lass dir einen Tipp geben", "Du kommst gerade nicht weiter? Wähle <b>Kannst du mir einen Tipp geben?</b> Lies den Hinweis und versuche es erneut. Du darfst dir Zeit lassen.", y, True, BLUE)
    y = panel(c, "<b>Deine Belohnung steht im Auftrag.</b><br/>Dazu gehören Smaragde und ein Extra: zum Beispiel Brot, Fackeln oder ein Werkzeug. Bei kniffligen Aufgaben gibt es auch verzauberte Schätze, etwa ein Reparatur-Buch. Tipps und weitere Versuche nehmen dir nichts weg.", y, PALE, size=13.3)
    para(c, "Jede Aufgabe belohnt dich einmal. Du kannst sie später ohne weitere Belohnung üben. Lies auch die Erklärung zur Lösung.", M, y, CW, 12.0, color=MUTED)
    c.showPage()

    y = heading(c, "Zeig deinen Stapel", "Bei manchen Aufträgen beantwortest du die Frage mit Gegenständen.", 3, 4, True, ORANGE)
    y = hotbar(c, y)
    y = step(c, 1, "Lies, was gebraucht wird", "Welche Gegenstände passen zum Auftrag? Wie viele brauchst du? Überlege und zähle selbst. Sammle die Gegenstände in deiner Welt.", y, True, ORANGE)
    y = step(c, 2, "Zähle einen eigenen Stapel ab", "Öffne mit <b>E</b> dein Inventar. Nimm einen Stapel mit Linksklick auf. Klicke mit Rechtsklick <b>immer auf denselben freien Platz</b> in der unteren Reihe: Mit jedem Klick kommt ein Stück dazu. Lege den Rest zurück auf einen anderen freien Platz.", y, True, ORANGE)
    y = step(c, 3, "Zeig deinen Stapel", "Schließe das Inventar. Wähle deinen abgezählten Stapel mit dem Mausrad oder einer Zifferntaste. Geh zum Lernreisenden und wähle <b>Stapel in meiner Haupthand zeigen</b>.", y, True, ORANGE)
    y = panel(c, "<b>Du behältst deine Gegenstände.</b><br/>Der Lernreisende schaut sich Material und Menge an. Er nimmt den Stapel nicht weg. Stimmt etwas noch nicht, lies seine Rückmeldung und verbessere deinen Stapel.", y, TAN, size=13.5)
    para(c, "Diese Seite erklärt die neuen Lernreisenden. Im alten Dorf kann es zusätzlich echte Lieferaufträge geben.", M, y, CW, 10.8, color=MUTED)
    c.showPage()

    y = heading(c, "Dein Abenteuer geht weiter", "Auch kleine Schritte helfen beim Entdecken und Lernen.", 4, 4, True)
    y = step(c, 1, "Wähle selbst, wie es weitergeht", "Möchtest du etwas anderes probieren? Wähle <b>Einen anderen Auftrag wählen</b>. Zum Schließen des Gesprächs wählst du <b>Bis später</b>.", y, True)
    y = step(c, 2, "Komm später wieder", "Ein Reisender kann irgendwann weiterziehen. Dein Lernfortschritt bleibt gespeichert. Geh nah an einen Lernreisenden heran und sprich ihn wieder an.", y, True)
    y = para(c, "<b>Du findest niemanden?</b> Mit <font name='GuideMono'>/kompass suchen</font> bekommst du eine Spur zu einem Reisenden in der Nähe.", M, y, CW, 12.3, gap=12)
    y = panel(c, "<b>Deine offene Aufgabe fortsetzen</b><br/>Drücke <b>T</b>, tippe den Befehl und drücke <b>Enter</b>:<br/><br/><font name='GuideMono' size='19'>/kompass lernen</font><br/><br/>Dafür muss ein Lernreisender ganz in deiner Nähe stehen.", y, SKY, size=13.2)
    y = panel(c, "<b>In dein Tagebuch schauen</b><br/><br/><font name='GuideMono' size='19'>/kompass tagebuch</font><br/><br/>Hier kannst du deinen Fortschritt nachsehen.", y, PALE, size=13.2)
    y = step(c, 3, "Sprich über deinen Lösungsweg", "Zeig deinen Eltern, wie du gerechnet hast oder welche Stelle im Text dir geholfen hat. Eine noch nicht richtige Antwort kann euch auf eine gute Idee bringen.", y, True)
    para(c, "Für eine Pause: Mit deinen Eltern normal speichern und beenden. Wenn beim Start eine Update-Meldung erscheint, hol deine Eltern dazu. Nach dem Neustart kannst du dieselbe Welt weiterspielen.", M, y, CW, 12.3, color=MUTED)
    c.showPage()
    c.save()


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    pdfmetrics.registerFont(TTFont("Guide", str(FONT_DIR / "LiberationSans-Regular.ttf")))
    pdfmetrics.registerFont(TTFont("GuideBold", str(FONT_DIR / "LiberationSans-Bold.ttf")))
    pdfmetrics.registerFont(TTFont("GuideMono", str(MONO)))
    pdfmetrics.registerFontFamily("Guide", normal="Guide", bold="GuideBold", italic="Guide", boldItalic="GuideBold")
    for filename, author in [
        ("Kompass_Abenteuer_Eltern_Einrichtung.pdf", parents),
        ("Kompass_Abenteuer_Kinder_Spielanleitung.pdf", children),
    ]:
        path = OUT / filename
        author(path)
        print(path)


if __name__ == "__main__":
    main()
