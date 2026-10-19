# GitHub Releases und Simple Mod Sync

Quellcode: [larsjochenthoms/MCKompass](https://github.com/larsjochenthoms/MCKompass)
Downloads: [GitHub Releases](https://github.com/larsjochenthoms/MCKompass/releases)

Minecraft Java **26.3**, Fabric Loader **ab 0.19.5**, Java **25**. Die Update-Einrichtung gilt für einen Minecraft-Client mit Fabric und einen bestimmten Spielordner. Auf einem anderen PC oder in einem weiteren Spielordner muss sie einmal erneut eingerichtet werden.

## Einmalig für Eltern einrichten

1. Minecraft vollständig schließen. Im Launcher bei der genutzten Fabric-Installation den tatsächlichen **Spielordner** nachsehen. Bei einem eigenen Spielordner sind die Standardpfade nicht maßgeblich.
2. Den gesamten Weltordner sichern. Im Einzelspieler liegt er gewöhnlich unter `Spielordner/saves/Weltname/`.
3. Die passende Einrichtungshilfe aus dem neuesten Release herunterladen. Sie richtet Simple Mod Sync und die feste Updateadresse im gewählten Spielordner ein. Bestehende Welten und Lernberichte werden dabei nicht übertragen.
4. Die Fabric-Installation starten. Simple Mod Sync prüft die Versionsliste und lädt die dort freigegebenen Mod-Dateien. Falls Dateien aktualisiert wurden, Minecraft vollständig schließen und erneut starten, bevor die Welt geöffnet wird.
5. Im Spiel mit `/kompass` die installierte Version prüfen. Anschließend dieselbe Welt öffnen und ein Gespräch mit einem Lernreisenden ausprobieren.

### Windows

Benötigt wird **PowerShell 5.1 oder neuer**. `install-simple-mod-sync.ps1` aus dem neuesten Release herunterladen. Im Downloadordner eine PowerShell öffnen und den folgenden Befehl verwenden; den Beispielpfad durch den tatsächlichen Spielordner ersetzen:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\install-simple-mod-sync.ps1 -GameDirectory 'C:\Minecraft\Kompass'
```

`ExecutionPolicy Bypass` gilt hier für diesen PowerShell-Aufruf. Die Datei ist ausführbarer Code; deshalb die Einrichtungshilfe aus dem eigenen Repository herunterladen und vor dem Start prüfen.

### Mac

Benötigt wird **macOS 12 oder neuer**. `install-simple-mod-sync.command` aus dem neuesten Release herunterladen. Per Doppelklick starten und den gewünschten Fabric-Spielordner eingeben. Alternativ im Terminal:

```sh
bash install-simple-mod-sync.command '/Users/DEINNAME/Library/Application Support/minecraft'
```

Der Pfad ist ein Beispiel. Wenn die Datei im Downloadordner liegt, zuerst dorthin wechseln oder ihren vollständigen Pfad angeben. Falls der Doppelklick wegen fehlender Ausführungsrechte nicht startet, funktioniert der Aufruf mit `bash` ohne Änderung der Dateirechte.

### Was die Einrichtungshilfen ändern

Beide Hilfen laden die aktuelle Kompass-Mod, die passende Fabric API und eine für GitHub geeignete Simple-Mod-Sync-Ausgabe herunter. Sie prüfen die Downloads mit SHA-512, installieren die JARs unter `mods/` und schreiben `config/simplemodsync.json` im gewählten Spielordner. Dort wird die feste Versionsliste eingetragen und die Prüfung beim Spielstart aktiviert.

Bisherige JARs dieser drei verwalteten Mods und eine vorhandene Sync-Konfiguration werden zuvor unter `.kompass-backups/<Zeit>/` im Spielordner gesichert. Welten, Lernberichte und andere Mods werden nicht verschoben. Den gesamten Weltordner trotzdem vor Updates gesondert sichern.

Die Hilfen können bei geschlossenem Minecraft später erneut ausgeführt werden, etwa nach einem unterbrochenen Download oder wenn ein Update beim Spielstart nicht abgeschlossen wurde. Die Prüfsummenprüfung gehört zur Einrichtungshilfe; sie ist keine Zusage über eine entsprechende Prüfung durch Simple Mod Sync bei späteren Starts.

Die feste Updateadresse ist:

```text
https://github.com/larsjochenthoms/MCKompass/releases/latest/download/simple-mod-sync.json
```

Sie zeigt auf die Versionsliste des neuesten stabilen Releases. Die Liste enthält konkrete Downloadadressen der Kompass-Mod und ihrer Fabric API. Simple Mod Sync muss aus einer Ausgabe stammen, die GitHub als Downloadquelle erlaubt; die CurseForge-Ausgabe von Simple Mod Sync schränkt Downloadquellen ein. Die Einrichtungshilfe verwendet eine für GitHub geeignete Ausgabe.

## Was bei späteren Updates passiert

Neue freigegebene Versionen erscheinen als GitHub Release. Simple Mod Sync prüft die feste Adresse beim Start des Fabric-Spiels. Für die Nutzung einer heruntergeladenen Änderung ist ein vollständiger Minecraft-Neustart erforderlich. Vor einer neuen Version den Weltordner sichern; die Aktualisierung der JAR ersetzt keine Sicherung.

Die Synchronisation ist an diesen Spielordner gebunden. Andere Launcher-Profile mit einem anderen Spielordner erhalten die Einrichtung nicht automatisch. Die Kompass-Mod bleibt auch manuell installierbar: Minecraft schließen, alte Kompass-JAR aus `mods` nehmen und genau eine neue normale JAR einlegen. Eine Quellen-JAR mit `-sources` gehört nicht in `mods`.

Bei einem Downloadfehler die Internetverbindung und die Release-Seite prüfen. Keine JAR aus einem ungeprüften Spiegelserver einsetzen. Bei Versionskonflikten Minecraft schließen und prüfen, ob genau eine Kompass-JAR, die passende Fabric API und Simple Mod Sync für Minecraft 26.3 vorhanden sind. Die Konfiguration erst ändern, wenn der gewählte Spielordner eindeutig feststeht.

## Quellcode und Releases pflegen

Die Repository-Dateien enthalten Quellcode, Build-Konfiguration, Tests und Anleitungen. Lokale Laufzeitdateien wie `run/`, `.tools/`, Gradle-Caches, Welten, `kompass4mc.json`, Elternberichte, Schlüssel und Zugangsdaten gehören nicht ins Repository.

GitHub Actions baut und testet Änderungen bei Pushes und Pull Requests mit Java 25 und dem Gradle Wrapper. Zum Veröffentlichen einer neuen stabilen Version:

1. Änderungen umsetzen und die Tests ausführen: `./gradlew build --no-daemon`.
2. `mod_version` in `gradle.properties` erhöhen und Versionsangaben in README und Anleitungen angleichen. Die PDF-Anleitungen neu erzeugen und prüfen.
3. Die Änderungen committen und auf GitHub pushen.
4. Einen passenden Versions-Tag setzen und pushen, beispielsweise:

   ```sh
   git tag v0.3.2
   git push origin v0.3.2
   ```

5. Den Release-Lauf unter **Actions** prüfen. Tag und `mod_version` müssen übereinstimmen. Erst ein erfolgreich gebautes und getestetes Release ist die Grundlage für die Versionsliste.
6. Die veröffentlichten Dateien kontrollieren und das Update zunächst in einer gesicherten Testwelt ausprobieren. Dabei auch den zweiten Spielstart nach dem Download prüfen.

Zum Release gehören die installierbare Kompass-JAR, eine Quellen-JAR, `SHA256SUMS`, `simple-mod-sync.json`, die beiden PDF-Anleitungen und die Einrichtungshilfen. Die Quellen-JAR ist für Entwickler, die normale JAR für Spieler. Die Versionsliste verweist auf die unverwechselbare Version im betreffenden Release und auf die passende Fabric API. Der feste `latest`-Link liefert den freigegebenen Stand.

Für das Veröffentlichen verwendet der Workflow das automatisch bereitgestellte `GITHUB_TOKEN` des Repositorys. Ein persönlicher GitHub-Schlüssel wird dafür nicht im Quellcode gespeichert. Das Repository muss GitHub Actions ausführen dürfen und Releases mit dem Workflow-Token zulassen. Wenn die Organisations- oder Repository-Regeln dies beschränken, muss der Eigentümer die entsprechenden Einstellungen freigeben.

Ein falscher oder fehlgeschlagener Tag darf nicht durch Überschreiben bereits verteilter JARs repariert werden. Fehler korrigieren, eine neue Version anlegen und über ein neues Release ausliefern. Dadurch bleiben Version, Prüfsumme und Downloaddatei nachvollziehbar.

## Prüfung vor dem ersten gemeinsamen Spielen

Der automatische Build ersetzt den Spieltest nicht. Einmal praktisch prüfen:

- Einrichtung in einem frischen Fabric-Spielordner und Installation der Dateien durch Simple Mod Sync.
- Vollständiger Neustart und Versionsanzeige mit `/kompass`.
- Laden einer gesicherten Welt, NPC-Gespräch, Aufgabe, Belohnung und lokaler Elternbericht.
- Wechsel auf eine spätere freigegebene Version ohne doppelte Kompass-JAR und ohne Verlust des Lernstands.

Ein vollständiger interaktiver Test mit Simple Mod Sync steht zum Einrichtungsstand noch aus.

## Offizielle Dokumentation

- [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases)
- [GitHub Actions](https://docs.github.com/en/actions)
- [Simple Mod Sync - Anleitung](https://github.com/oxydien/simple-mod-sync/blob/main/DOCS.md)
- [Fabric - Mods installieren](https://docs.fabricmc.net/players/installing-mods)
