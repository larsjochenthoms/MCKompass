# Eigene NPC-Skins

Mila, Jaro und Lio sind eigens für Kompass4MC gezeichnete Pixeltexturen. Sie
enthalten keine kopierten Minecraft-Texturen und stehen unter der MIT-Lizenz
des Projekts (`../../LICENSE`). Alle drei verwenden das breite Spielermodell
mit 64 × 64 Pixeln und vier Pixel breiten Armen.

Die JSON-Dateien enthalten die benannten Farbpaletten und die jeweilige
Kleidungsvariante. Die UV-Flächen, Frisuren und Kleidungsdetails sind im
Generator `../../tools/create_npc_skins.py` beschrieben. Der Generator benötigt
für die Spieltexturen nur Python 3 mit Standardbibliothek:

```sh
python3 tools/create_npc_skins.py
```

Er schreibt die drei PNGs nach
`src/main/resources/assets/kompass4mc/textures/entity/` und die identischen,
verlustfrei editierbaren SVG-Pixelatlanten in diesen Ordner. Alle Basisflächen
sind opak; unbenutzte Bereiche und freie Bereiche der zweiten Hautschicht sind
transparent. Die zweite Kopfschicht ergänzt lediglich die Frisuren.

Mit Pillow kann zusätzlich eine vergrößerte Pixelansicht und eine einfache
räumliche Vorschau zur Kontrolle erzeugt werden:

```sh
python3 tools/create_npc_skins.py --preview
```

Die Vorschau liegt anschließend unter `output/qa/npc-skins/`. Sie ist eine
Prüfansicht der Texturen; das tatsächliche Rendering übernimmt Minecraft.
