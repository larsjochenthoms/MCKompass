#!/usr/bin/env python3
"""Create large-print A4 subtraction worksheets and a separate answer key."""

from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas


ROOT = Path(__file__).resolve().parent
OUT = ROOT / "output" / "pdf"
FONT_DIR = Path(
    "/Users/lars.thoms/.cache/codex-runtimes/codex-primary-runtime/"
    "dependencies/node/node_modules/pdfjs-dist/standard_fonts"
)
TASKS = [
    (867, 243),
    (975, 462),
    (753, 218),
    (842, 376),
    (903, 457),
    (700, 286),
    (1748, 526),
    (2863, 748),
    (4325, 1876),
    (5024, 2687),
    (7010, 3856),
    (9000, 4678),
]
EXPECTED = [624, 513, 535, 466, 446, 414, 1222, 2115, 2449, 2337, 3154, 4322]

INK = colors.HexColor("#171717")
MUTED = colors.HexColor("#555555")
GRID = colors.HexColor("#B9B9B9")
WIDTH, HEIGHT = A4
X0 = 15 * mm
Y0 = 17 * mm
CELL = 20 * mm
COLS, ROWS = 9, 11
TOP = Y0 + ROWS * CELL


def label(c, x, y, text, size=11, bold=False, color=INK):
    c.setFillColor(color)
    c.setFont("WorksheetBold" if bold else "Worksheet", size)
    c.drawString(x, y, text)


def grid(c):
    c.setStrokeColor(GRID)
    c.setLineWidth(0.55)
    for col in range(COLS + 1):
        x = X0 + col * CELL
        c.line(x, Y0, x, TOP)
    for row in range(ROWS + 1):
        y = Y0 + row * CELL
        c.line(X0, y, X0 + COLS * CELL, y)


def cell_text(c, col, row_from_top, text):
    # Center the visible digit height, rather than the complete font em box.
    size = 56
    font = pdfmetrics.getFont("Worksheet")
    cap_height = font.face.capHeight / 1000 * size
    bottom = TOP - (row_from_top + 1) * CELL
    baseline = bottom + (CELL - cap_height) / 2
    c.setFont("Worksheet", size)
    c.setFillColor(INK)
    c.drawCentredString(X0 + (col + 0.5) * CELL, baseline, text)


def operand(c, value, right_col, row):
    digits = str(value)
    for i, digit in enumerate(digits):
        cell_text(c, right_col - len(digits) + 1 + i, row, digit)


def worksheet_page(c, number, minuend, subtrahend):
    c.setPageSize(A4)
    label(c, X0, 280 * mm, "Schriftlich subtrahieren", size=20, bold=True)
    c.setFont("Worksheet", 11)
    c.setFillColor(MUTED)
    c.drawRightString(195 * mm, 280 * mm, f"Blatt {number:02d} / {len(TASKS):02d}")
    label(c, X0, 268 * mm, "Rechne schriftlich. Schreibe jede Ziffer in ein Kästchen.", size=12)
    label(c, X0, 252 * mm, "Name:", size=11)
    label(c, 137 * mm, 252 * mm, "Datum:", size=11)
    c.setStrokeColor(MUTED)
    c.setLineWidth(0.5)
    c.line(29 * mm, 251 * mm, 122 * mm, 251 * mm)
    c.line(153 * mm, 251 * mm, 195 * mm, 251 * mm)

    grid(c)
    # Four occupied columns for 3-digit problems; five for 4-digit problems.
    # The right edge stays fixed to keep the units column in the same place.
    right_col = 6
    longest = len(str(minuend))
    sign_col = right_col - longest
    operand(c, minuend, right_col, 1)
    operand(c, subtrahend, right_col, 2)
    # Draw a true mathematical minus directly, independent of font encoding.
    c.setStrokeColor(INK)
    c.setLineWidth(2.5)
    minus_x = X0 + (sign_col + 0.5) * CELL
    minus_y = TOP - 2.5 * CELL
    c.line(minus_x - 5 * mm, minus_y, minus_x + 5 * mm, minus_y)

    # A complete blank row leaves room for either subtraction convention.
    label(c, X0 + 2.5 * mm, TOP - 3.6 * CELL, "Überträge", 9, color=MUTED)
    c.setStrokeColor(INK)
    c.setLineWidth(1.8)
    rule_y = TOP - 4 * CELL
    c.line(X0 + sign_col * CELL, rule_y, X0 + (right_col + 1) * CELL, rule_y)
    label(c, X0 + 2.5 * mm, TOP - 4.6 * CELL, "Ergebnis", 9, color=MUTED)

    label(c, X0, 9 * mm, "Eigene Übungsaufgabe zur Vorbereitung auf Kompass 4", 8, color=MUTED)
    c.showPage()


def write_worksheets(path):
    c = canvas.Canvas(str(path), pagesize=A4, pageCompression=1)
    c.setTitle("Schriftlich subtrahieren - 12 große A4-Arbeitsblätter")
    c.setAuthor("Eigene Übungsaufgaben")
    for number, (a, b) in enumerate(TASKS, 1):
        worksheet_page(c, number, a, b)
    c.save()


def write_answers(path):
    c = canvas.Canvas(str(path), pagesize=A4, pageCompression=1)
    c.setTitle("Lösungen und Druckhinweise - Schriftlich subtrahieren")
    c.setAuthor("Eigene Übungsaufgaben")
    label(c, X0, 280 * mm, "Lösungen", size=22, bold=True)
    label(c, X0, 269 * mm, "Schriftlich subtrahieren | 12 Arbeitsblätter", size=12, color=MUTED)

    table_top = 253 * mm
    row_h = 9 * mm
    c.setFillColor(colors.HexColor("#EEEEEE"))
    c.rect(X0, table_top - row_h, 180 * mm, row_h, stroke=0, fill=1)
    label(c, 20 * mm, table_top - 6 * mm, "Blatt", 11, True)
    label(c, 55 * mm, table_top - 6 * mm, "Aufgabe", 11, True)
    label(c, 144 * mm, table_top - 6 * mm, "Ergebnis", 11, True)
    for i, ((a, b), answer) in enumerate(zip(TASKS, EXPECTED), 1):
        y = table_top - i * row_h
        label(c, 20 * mm, y - 6 * mm, f"{i:02d}", 12)
        label(c, 55 * mm, y - 6 * mm, f"{a} − {b}", 12)
        label(c, 144 * mm, y - 6 * mm, str(answer), 12, True)
        c.setStrokeColor(colors.HexColor("#D5D5D5"))
        c.setLineWidth(0.4)
        c.line(X0, y - row_h, 195 * mm, y - row_h)

    label(c, X0, 122 * mm, "So drucken", 13, True)
    label(c, X0, 113 * mm, "DIN A4, Hochformat, einseitig, tatsächliche Größe / 100 %.", 11)
    label(c, X0, 105 * mm, "Bei 100 % sind die Kästchen genau 2 × 2 cm groß.", 11)
    label(c, X0, 97 * mm, "Das Aufgaben-PDF enthält eine Rechnung pro Seite und keine Lösungen.", 11)

    label(c, X0, 81 * mm, "Zur Auswahl der Aufgaben", 13, True)
    notes = [
        "Die Reihe beginnt mit dreistelligen Zahlen und führt zu vierstelligen Zahlen.",
        "Überträge, unterschiedlich lange Zahlen und Nullen steigern den Anspruch.",
        "Freie Kästchen lassen Platz für Notizen und eine eigene Probe.",
        "Die selbst erstellten Blätter üben gezielt die schriftliche Subtraktion.",
        "Sie bilden weder das vollständige Kompass-4-Format noch Originalaufgaben ab.",
    ]
    for i, text in enumerate(notes):
        label(c, X0, (72 - 7 * i) * mm, text, 10)

    label(c, X0, 32 * mm, "Fachliche Orientierung", 10, True)
    links = [
        ("IBBW: Kompass 4 - veröffentlichte Mathematikaufgaben 2025/26",
         "https://ibbw-bw.de/site/pbs-bw-rebrush2024/get/documents_E2067908024/KULTUS.Dachmandant/KULTUS/Dienststellen/ibbw/Kompetenzmessung/dokumente/Kompass%204/Kompass%204_Mathematik_2025_2026.pdf"),
        ("Bildungsplan Baden-Württemberg: Rechenoperationen, Klassen 3/4",
         "https://www.bildungsplaene-bw.de/,Lde/BP2016BW_ALLG_GS_M.V2_IK_3-4_01_02"),
    ]
    for i, (text, url) in enumerate(links):
        y = (25 - 6 * i) * mm
        label(c, X0, y, text, 8.5, color=MUTED)
        c.linkURL(url, (X0, y - 2, X0 + pdfmetrics.stringWidth(text, "Worksheet", 8.5), y + 8), relative=0)
    c.showPage()
    c.save()


def main():
    assert [a - b for a, b in TASKS] == EXPECTED
    assert len(TASKS) == 12 and all(a > b > 0 for a, b in TASKS)
    OUT.mkdir(parents=True, exist_ok=True)
    pdfmetrics.registerFont(TTFont("Worksheet", str(FONT_DIR / "LiberationSans-Regular.ttf")))
    pdfmetrics.registerFont(TTFont("WorksheetBold", str(FONT_DIR / "LiberationSans-Bold.ttf")))
    worksheet_path = OUT / "Kompass4_Schriftliche_Subtraktion_A4.pdf"
    solution_path = OUT / "Kompass4_Subtraktion_Loesungen.pdf"
    write_worksheets(worksheet_path)
    write_answers(solution_path)
    print(worksheet_path)
    print(solution_path)


if __name__ == "__main__":
    main()
