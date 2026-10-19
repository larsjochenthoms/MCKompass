# Kompass-Abenteuer für Minecraft Java 26.3

**Version 0.3.3:** Die kurze Elternübersicht im Spiel zeigt die wichtigste Beobachtung zu einem Fehlermuster mit Beispiel auch dann, wenn bereits alle 20 Lernbereiche bearbeitet wurden. Die Hinweise zur vorsichtigen Einordnung bleiben erhalten; weitere Beobachtungen stehen im vollständigen HTML-Bericht.

Mila, Jaro und Lio erscheinen als Lernreisende in normalen Minecraft-Welten. Sie erzählen von ihrer Reise, ihren Bauvorhaben oder Bestellungen und bitten um Hilfe. Ein Dorf muss dafür nicht angelegt werden. Die 48 eigenen Aufgaben verbinden Mathematik und Deutsch mit Minecraft-Situationen; vier Aufgaben werden durch einen selbst abgezählten Gegenstandsstapel in der Haupthand beantwortet. Zu den Smaragden kommen feste Sachbelohnungen hinzu, von Vorräten bis zu verzauberter Diamantausrüstung.

## GitHub Releases und automatische Updates

Quellcode und Versionsgeschichte liegen im [GitHub-Repository MCKompass](https://github.com/larsjochenthoms/MCKompass). Fertige, getestete JARs und die PDF-Anleitungen stehen unter [Releases](https://github.com/larsjochenthoms/MCKompass/releases). Zum Spielen immer die normale Kompass-JAR verwenden; die Quellen-JAR mit `-sources` dient der Entwicklung.

Für automatische Updates lässt sich **Simple Mod Sync** einmalig im verwendeten Fabric-Spielordner einrichten. Die Einrichtungshilfen für Windows und Mac sowie der genaue Ablauf stehen in der [Anleitung für GitHub Releases und Simple Mod Sync](docs/github-releases.md). Danach prüft Simple Mod Sync die freigegebene Versionsliste beim Spielstart. Wenn ein Update geladen wurde, Minecraft vollständig neu starten, bevor die Welt geöffnet wird. Vor Updates den Weltordner sichern.

Die feste Updateadresse lautet:

```text
https://github.com/larsjochenthoms/MCKompass/releases/latest/download/simple-mod-sync.json
```

Diese Adresse liefert die Liste aus dem neuesten stabilen Release. Sie verweist auf dessen konkrete JAR und die passende Fabric API. Simple Mod Sync lädt die freigegebenen JAR-Dateien über HTTPS; Aufgaben, Antworten, Welten und Elternberichte bleiben im lokalen Spielordner.

## Installieren oder aktualisieren

1. Minecraft **Java Edition 26.3** mit **Fabric Loader ab 0.19.5** verwenden. Die Mod benötigt Java 25; beim normalen Spielen übernimmt der offizielle Launcher die Java-Laufzeit.
2. **Fabric API für 26.3** und **`kompass4mc-0.3.3.jar`** aus dem [neuesten Release](https://github.com/larsjochenthoms/MCKompass/releases/latest) (bei einem lokalen Build aus `build/libs/`) in den `mods`-Ordner der verwendeten Fabric-Installation legen. Entwickelt wird mit Fabric API `0.162.0+26.3`.
3. Zum Aktualisieren Minecraft vollständig beenden und den Weltordner sichern. Die alte Kompass-JAR, beispielsweise `kompass4mc-0.3.0.jar`, aus `mods` herausnehmen und durch die neue ersetzen. **Genau eine Kompass-JAR** in `mods` lassen; die Datei mit `-sources` ist keine installierbare Mod.
4. Dieselbe Welt mit der Fabric-Installation öffnen. **Survival und Kreativ** werden unterstützt. Automatische Begegnungen brauchen keine aktivierten Befehle. `/kompass` zeigt die geladene Version.

Bei einem eigenen Spielordner gilt dessen `mods`-Unterordner. Ohne eigenen Spielordner sind die Standardpfade unter Windows `%appdata%\.minecraft\mods`, unter macOS `~/Library/Application Support/minecraft/mods` und unter Linux `~/.minecraft/mods`. Auf einem Fabric-Server gehört die Mod in dessen `mods`-Ordner; die beschriebenen Installationsschritte für den Einzelspieler beziehen sich auf den Minecraft Launcher.

Eine bereits eingerichtete Windows-Freigabe kann zum Austauschen der JAR vom Mac aus genutzt werden. Dabei muss Minecraft auf dem Windows-PC geschlossen sein. Der tatsächliche Freigabepfad hängt von der dort eingerichteten Freigabe ab.

Die druckbaren Anleitungen stehen unter `output/pdf/Kompass_Abenteuer_Eltern_Einrichtung.pdf` und `output/pdf/Kompass_Abenteuer_Kinder_Spielanleitung.pdf`.

## Lernreisenden begegnen

In der Oberwelt wird etwa **20 Sekunden nach dem ersten geeigneten Spielstart** ein freier Platz in der Nähe gesucht; danach etwa alle **120 Sekunden**. Eine Begegnung entsteht nur in bereits geladenem Gelände, auf einer trockenen und freien Oberfläche, ungefähr 8 bis 24 Blöcke vom Spieler entfernt. Gebäude und Landschaft werden nicht verändert. In Höhlen, unter Wasser oder in anderen Dimensionen kann deshalb keine Begegnung entstehen. Gehe dafür auf eine freie Wiese in der Oberwelt.

Innerhalb von 64 Blöcken wird kein weiterer Lernreisender hinzugefügt; insgesamt sind höchstens acht geladene Lernreisende vorgesehen. Die Figuren stehen an ihrem Begegnungsort, statt einem Tagesablauf zu folgen. Nach ungefähr zehn Spielminuten können sie weiterziehen; ein laufendes Gespräch wird dabei berücksichtigt. Eine offene Aufgabe bleibt im persönlichen Fortschritt gespeichert.

- Einen Lernreisenden mit **Rechtsklick** ansprechen, den Anlass lesen und Mathematik oder Deutsch mit der gewünschten Anforderung wählen. Im Einzelspieler pausiert das Gespräch die Welt; im Mehrspieler läuft sie weiter.
- Eine Antwort im Gespräch eingeben oder auswählen. Bei einer Stapelaufgabe das geforderte Material selbst sammeln, passend abzählen und in der **Haupthand zeigen**. Diese Gegenstände werden nicht verbraucht.
- Hinweise verwenden und erneut versuchen. Es gibt keine Zeitwertung und keinen Belohnungsabzug nach Fehlern oder Hinweisen.
- Mit **`/kompass lernen`** eine offene Aufgabe fortsetzen, mit **`/kompass tagebuch`** den eigenen Stand ansehen. Zum Gespräch höchstens sechs Blöcke von einem Lernreisenden entfernt stehen.

Jede der 48 Aufgaben belohnt pro Spieler **einmal** mit Smaragden **und einem zusätzlichen Gegenstand oder Stapel**. Die genaue Belohnung steht schon vor dem Lösen im Aufgabendialog. Jede Aufgabe hat ihr festes Paket; es gibt keine Zufallsziehung.

| Anforderung | Smaragde | Beispiele für den zusätzlichen Sachbonus |
| --- | --- | --- |
| Einfach | 2 | Brot, Fackeln, Eichenstämme, Pfeile, Karotten oder Lederstiefel |
| Mittel | 4 | Eisenbarren, Redstone, Lapislazuli, Schild, Amboss oder Eisenwerkzeug mit Effizienz II |
| Knifflig | 6 | Buch mit Reparatur I (Mending), Diamantspitzhacke mit Effizienz III und Haltbarkeit II, Diamantstiefel mit Federfall IV und Haltbarkeit II oder Bogen mit Stärke III und Haltbarkeit II |

Die Anforderung beschreibt die Aufgabe und stuft das Kind nicht ein. Fehler und Hinweise verringern weder Smaragde noch Sachbonus. Bereits erledigte Aufgaben können ohne weitere Auszahlung geübt werden. Die Belohnungen und Lernstände sind persönlich; ein anderes Kind kann dieselbe Aufgabe selbst lösen. **Beim Update werden Sachboni für bereits belohnte Aufgaben nicht nachträglich ausgezahlt.**

Mathematik umfasst unter anderem schriftliche Subtraktion, Multiplikation, Division, Längeneinheiten und Umfang. Deutsch verbindet Textverständnis, Gründe und Folgen, Reihenfolgen, Rechtschreibung und Sprachgebrauch mit den Geschichten der Reisenden. Die Inhalte sind eigene Übungen für die vierte Klasse, keine offiziellen KOMPASS-4-Testaufgaben und keine standardisierte Leistungsbewertung.

## Aufgaben nach KOMPASS 4

Der Katalog enthält 24 zusätzliche, eigenständig entwickelte Aufgaben auf Grundlage der veröffentlichten IBBW-Testhefte 2025/2026: je zwölf für Mathematik und Deutsch. Neu sind unter anderem Stellenwerte, Zahlenmuster, räumliches Denken, Fläche, Zeit, Preisrechnung, Daten, Zufall sowie Textbelege, Wortbedeutung und Sprachuntersuchung.

Die [Recherche mit offiziellen Originalheften und Zuordnung aller neuen Aufgaben](docs/kompass4-recherche.md) dokumentiert die fachliche Grundlage. Texte, Zahlen und Situationen sind neu entwickelt. Die Übungen bleiben ein Lernangebot ohne offizielle KOMPASS-Bewertung.

## Für Eltern: Fortschritt und Denkwege

Die Elternansicht benötigt Minecrafts **Spielleitungsrecht / Berechtigungsstufe 2**. In einer Einzelspielerwelt lassen sich diese Funktionen mit aktivierten Befehlen nutzen. Ein Kind mit denselben Rechten kann die Elternansicht ebenfalls öffnen; es gibt kein separates Elternkennwort.

| Befehl | Wirkung |
| --- | --- |
| `/kompass eltern` | Elternübersicht öffnen |
| `/kompass eltern spieler <Name>` | Lernnotizen zu einem gespeicherten Spieler anzeigen |
| `/kompass eltern export` | Lokalen HTML-Bericht für den eigenen Spieler erstellen |
| `/kompass eltern export <Name>` | Lokalen HTML-Bericht für den gewählten gespeicherten Spieler erstellen |
| `/kompass eltern begegnungen an` | Automatische Begegnungen einschalten |
| `/kompass eltern begegnungen aus` | Automatische Begegnungen ausschalten |
| `/kompass begegnung` | Als Spielleitung eine sichere Begegnung in der Nähe versuchen |

Der Bericht steht im **Weltordner unter `kompass-berichte/<Spieler-UUID>.html`**. In einer Einzelspielerwelt ist das üblicherweise `Spielordner/saves/<Weltname>/kompass-berichte/`. Er lässt sich mit einem Browser öffnen und drucken. Jeder Export ist eine Momentaufnahme; nach weiterem Spielen erneut exportieren. Der HTML-Bericht lädt keine externen Inhalte und übermittelt keine Daten.

Beim Exportknopf im Elterndialog kann Minecraft eine zusätzliche Bestätigung für den geschützten Befehl anzeigen. Den angezeigten Exportbefehl prüfen und bestätigen; die benötigten Spielleitungsrechte gelten weiterhin.

Die Übersicht zeigt gelöste und begonnene Aufgaben, Anforderungen, erste Antworten, Hinweise sowie die letzten Versuche und Schwierigkeiten. Bedienfragen, etwa ein falscher Gegenstand in der Hand, werden getrennt von fachlichen Fehlantworten erfasst. Hinweise zählen nicht als Fehler. Antworten und Zeitangaben dienen dem gemeinsamen Rückblick; die benötigte Zeit wird nicht bewertet.

**Mögliche Misskonzepte sind Gesprächsanlässe, keine Diagnose.** Für ausgewählte falsche Antworten sind mögliche Fehlermuster hinterlegt, etwa Verwechslung von Fläche und Umfang oder Schwierigkeiten beim Entbündeln. Ein Muster wird erst hervorgehoben, wenn passende Antworten bei **mindestens zwei verschiedenen Aufgaben** auftreten. Wiederholte Antworten zur selben Aufgabe liefern keine zusätzliche Bestätigung. Auch dann kann die Antwort durch Lesen, Raten oder Bedienung entstanden sein. Lassen Sie sich den Rechenweg oder die passende Textstelle erklären und prüfen Sie gemeinsam eine weitere Aufgabe.

Frühere Fehlversuche bleiben im Verlauf sichtbar; der Bericht kennzeichnet, wenn eine Aufgabe danach gelöst wurde. Nicht jede falsche Antwort lässt sich einem bekannten Muster zuordnen. Aus dem Fehlen eines Musters folgt keine sichere Aussage über das Beherrschen eines Lernbereichs. Für frühere Dorfprojekte und Übungsfässer wird keine frühere Antwortgeschichte nachträglich rekonstruiert.

## Gespeicherte Welten und optionales Dorf

Fortschritt und Einstellungen werden lokal in **`kompass4mc.json` im Weltordner** gespeichert. Die Namen und das Alter der Lernreisenden werden mit den Minecraft-Figuren gespeichert. Vorhandene Welten und frühere Fortschritte bleiben verwendbar. Vor Updates eine Sicherung des gesamten Weltordners bei geschlossenem Spiel anlegen.

Der bisherige Dorfmodus bleibt über **`/kompass dorf`** beziehungsweise `/kompass abenteuer` optional verfügbar und benötigt Spielleitungsrechte. Mila prüft dort einen Gartenzaun, Jaro einen gebauten Steg und Lio eine echte Apfellieferung. Diese drei einmaligen Gemeinschaftsprojekte haben weiterhin ihren eigenen Fortschritt und eigene Regeln. Im Dorf wird Lios passende Lieferung tatsächlich übergeben; bei den neuen Lernreisenden wird der Stapel nur gezeigt.

Weitere ältere Befehle bleiben erhalten:

| Befehl | Wirkung |
| --- | --- |
| `/kompass` | Hilfe und installierte Version |
| `/kompass suchen` | Richtung und Koordinaten eines Lernreisenden bis 128 Blöcke entfernt; sonst frühere Dorf-/Verstecksuche |
| `/kompass suchen uebungen` | Spur zur optionalen Runde mit Übungsfässern |
| `/kompass uebungsrunde` | Als Spielleitung die frühere Runde mit zwölf Übungsfässern anlegen |
| `/kompass weiter` und `/kompass hinweis` | Eine offene Aufgabe fortsetzen beziehungsweise deren Hinweis öffnen |
| `/kompass versteck <quest-id> <x> <y> <z>` | Als Spielleitung ein vorhandenes Fass oder eine Truhe mit einer älteren Übungsaufgabe verknüpfen |
| `/kompass entfernen <x> <y> <z>` | Als Spielleitung die Verknüpfung lösen; der Block bleibt stehen |

## Entwickeln und prüfen

Pushes und Pull Requests werden mit GitHub Actions gebaut und getestet. Ein Tag wie `v0.3.3`, der zur Version in `gradle.properties` passt, erzeugt ein GitHub Release mit installierbarer JAR, Quellen-JAR, Prüfsummen, Versionsliste und Anleitungen. Die [Release-Anleitung](docs/github-releases.md) beschreibt den Ablauf und die erforderlichen Repository-Einstellungen.

Benötigt werden **Java 25** und beim ersten Abruf der Abhängigkeiten eine Internetverbindung. Der Wrapper verwendet Gradle 9.6.0 und Loom 1.17.21 mit offiziellen Minecraft-Namen.

```sh
./gradlew build --no-daemon
```

Auf diesem Mac steht Java 25 über PhpStorm bereit:

```sh
JAVA_HOME=/Applications/PhpStorm.app/Contents/jbr/Contents/Home \
GRADLE_USER_HOME="$PWD/.tools/gradle-user-home" \
./gradlew build --no-daemon
```

Die installierbare Datei liegt unter `build/libs/kompass4mc-0.3.3.jar`. Die Aufgaben stehen in `src/main/java/de/kompass4mc/learning/LearningCatalog.java`, die Lernnotizen in `src/main/java/de/kompass4mc/progress/` und die Begegnungen in `src/main/java/de/kompass4mc/RoamingNpcs.java`.

Automatisierte Tests prüfen Aufgabenantworten, Fehlermuster und die Elternberichte zusätzlich zu den bisherigen Dorfprojekten, Speicherfällen und Gesprächszuständen. Der automatisierte macOS-Spieltest mit Fabric Client GameTest lädt die Mod-JAR unverändert in Minecraft und prüft NPC-Interaktionen, native Dialoge, alle 48 Aufgaben, Hinweise, Stapelantworten, Belohnungen einschließlich Reparatur, geschützte Elternfunktionen und lokalen Export sowie Lernstand und Inventar nach erneutem Laden. Er verwendet keine Bildschirmsteuerung. Mit der veröffentlichten Version 0.3.2 wurde dabei der Anzeigefehler gefunden. Der Vorabtest der für Release 0.3.3 gebauten JAR besteht vollständig und bestätigt insbesondere den Gesprächsanlass mit Beispiel und späterer Korrektur nach allen 20 Lernbereichen. Auch alle 482 Java-Tests bestehen.

Der echte Simple-Mod-Sync-Wechsel von 0.3.1 auf 0.3.2 einschließlich Neustart wurde auf macOS geprüft. Im Vorabtest von 0.3.3 ist Simple Mod Sync 1.4.1 geladen, die Synchronisation beim Start aber ausgeschaltet, da die neue Version zu diesem Zeitpunkt noch nicht veröffentlicht ist. Dieser Lauf bestätigt daher keinen Updatewechsel auf 0.3.3.

Die optische Lesbarkeit der Spieloberfläche, ein Windows-Spieltest und der alte Dorfmodus sind weiterhin ungeprüft. Die automatisierten Tests ersetzen diese Prüfungen nicht. Die PDFs können mit `create_guides_pdf.py` neu erzeugt werden.

## Quellen

- [Fabric Installer](https://fabricmc.net/use/installer/)
- [Fabric: Mods installieren und Spielordner finden](https://docs.fabricmc.net/players/installing-mods)
- [Fabric für Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html)
- [Offizielles Fabric-Beispielprojekt für 26.3](https://github.com/FabricMC/fabric-example-mod/tree/26.3)
- [Minecraft: natives Dialogformat](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-6)
- [IBBW: Konzeption Kompass 4](https://ibbw-bw.de/,Lde/Startseite/Pruefungen%2B_%2BLernstandserhebungen/Kompass%2B4%2B_%2BKonzeption)

Quelltextlizenz: MIT. Minecraft, Fabric und deren Bibliotheken unterliegen ihren jeweiligen Lizenzen. Eigenständiges Lernprojekt ohne Verbindung zu Mojang, Microsoft, Nintendo oder dem IBBW.
