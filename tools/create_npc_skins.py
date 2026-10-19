#!/usr/bin/env python3
"""Draw the original Kompass NPC skins; no downloaded or vanilla textures.

The atlas uses Minecraft's 64 x 64 wide-arm player UV layout. Generation uses
only the standard library. Optional --preview uses Pillow for a contact sheet
and a small orthographic inspection render, never for the shipping textures.
"""

from __future__ import annotations

import argparse
import binascii
import json
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art/skins"
DESTINATION = ROOT / "src/main/resources/assets/kompass4mc/textures/entity"
TRANSPARENT = (0, 0, 0, 0)

# Each tuple is (u, v, width, height). Right/left name the character's sides.
UV = {
    "head": {"top": (8, 0, 8, 8), "bottom": (16, 0, 8, 8),
             "right": (0, 8, 8, 8), "front": (8, 8, 8, 8),
             "left": (16, 8, 8, 8), "back": (24, 8, 8, 8)},
    "body": {"top": (20, 16, 8, 4), "bottom": (28, 16, 8, 4),
             "right": (16, 20, 4, 12), "front": (20, 20, 8, 12),
             "left": (28, 20, 4, 12), "back": (32, 20, 8, 12)},
    "right_arm": {"top": (44, 16, 4, 4), "bottom": (48, 16, 4, 4),
                  "right": (40, 20, 4, 12), "front": (44, 20, 4, 12),
                  "left": (48, 20, 4, 12), "back": (52, 20, 4, 12)},
    "left_arm": {"top": (36, 48, 4, 4), "bottom": (40, 48, 4, 4),
                 "right": (32, 52, 4, 12), "front": (36, 52, 4, 12),
                 "left": (40, 52, 4, 12), "back": (44, 52, 4, 12)},
    "right_leg": {"top": (4, 16, 4, 4), "bottom": (8, 16, 4, 4),
                  "right": (0, 20, 4, 12), "front": (4, 20, 4, 12),
                  "left": (8, 20, 4, 12), "back": (12, 20, 4, 12)},
    "left_leg": {"top": (20, 48, 4, 4), "bottom": (24, 48, 4, 4),
                 "right": (16, 52, 4, 12), "front": (20, 52, 4, 12),
                 "left": (24, 52, 4, 12), "back": (28, 52, 4, 12)},
    "hat": {"top": (40, 0, 8, 8), "bottom": (48, 0, 8, 8),
            "right": (32, 8, 8, 8), "front": (40, 8, 8, 8),
            "left": (48, 8, 8, 8), "back": (56, 8, 8, 8)},
}
BASE_PARTS = ("head", "body", "right_arm", "left_arm", "right_leg", "left_leg")


class Skin:
    def __init__(self, specification):
        self.specification = specification
        self.palette = {name: tuple(bytes.fromhex(value)) + (255,)
                        for name, value in specification["palette"].items()}
        self.pixels = [[TRANSPARENT for _ in range(64)] for _ in range(64)]

    def point(self, part, face, x, y, color):
        u, v, width, height = UV[part][face]
        if not (0 <= x < width and 0 <= y < height):
            raise ValueError(f"Pixel outside {part}/{face}: {x}, {y}")
        self.pixels[v + y][u + x] = self.palette[color]

    def rectangle(self, part, face, x, y, width, height, color):
        for py in range(y, y + height):
            for px in range(x, x + width):
                self.point(part, face, px, py, color)

    def fill(self, part, face, color):
        _, _, width, height = UV[part][face]
        self.rectangle(part, face, 0, 0, width, height, color)

    def validate(self):
        for part in BASE_PARTS:
            for face, (u, v, width, height) in UV[part].items():
                if any(self.pixels[y][x][3] != 255
                       for y in range(v, v + height) for x in range(u, u + width)):
                    raise ValueError(f"Transparent pixel in base {part}/{face}")
        if any(pixel[3] not in (0, 255) for row in self.pixels for pixel in row):
            raise ValueError("Unexpected partial transparency")


def draw_head(skin):
    style = skin.specification["style"]
    for face in UV["head"]:
        skin.fill("head", face, "skin" if face != "top" else "hair")
    for face in ("right", "left", "back"):
        skin.rectangle("head", face, 0, 0, 8, 3, "hair")
        if face == "back":
            skin.rectangle("head", face, 0, 3, 8, 3 if style == "traveller" else 2, "hair")
        else:
            skin.rectangle("head", face, 0, 3, 2, 2, "hair")
            skin.rectangle("head", face, 6, 3, 2, 2, "hair_shadow")
            skin.point("head", face, 4, 4, "skin_shadow")
            skin.point("head", face, 4, 5, "skin_light")
        skin.rectangle("head", face, 0, 0, 1, 3, "hair_shadow")
        skin.rectangle("head", face, 2, 1, 3, 1, "hair_light")
    skin.rectangle("head", "top", 1, 1, 5, 2, "hair_light")
    skin.rectangle("head", "top", 6, 2, 2, 6, "hair_shadow")
    skin.fill("head", "bottom", "skin_shadow")
    skin.rectangle("head", "front", 0, 0, 8, 2, "hair")
    # Three different, deliberately asymmetrical fringes.
    fringes = {"traveller": ((0, 2), (1, 2), (2, 2), (6, 2), (7, 2), (0, 3)),
               "builder": ((0, 2), (2, 2), (5, 2), (7, 2)),
               "reader": ((0, 2), (1, 2), (2, 2), (3, 2), (4, 2), (0, 3))}
    for x, y in fringes[style]:
        skin.point("head", "front", x, y, "hair")
    skin.rectangle("head", "front", 2, 0, 4, 1, "hair_light")
    skin.point("head", "front", 0, 1, "hair_shadow")
    for x in (1, 6):
        skin.point("head", "front", x, 4, "white")
    for x in (2, 5):
        skin.point("head", "front", x, 4, "eyes")
    skin.point("head", "front", 3, 5, "skin_shadow")
    skin.point("head", "front", 4, 5, "skin_light")
    for x in (3, 4):
        skin.point("head", "front", x, 6, "mouth")
    skin.point("head", "front", 1, 6, "skin_light")
    skin.point("head", "front", 6, 6, "skin_light")
    # Sparse second-layer hair highlights: face and eyes remain unobscured.
    skin.rectangle("hat", "top", 1, 1, 5, 2, "hair_light")
    for x, y in ((1, 0), (2, 0), (5, 1), (6, 1)):
        skin.point("hat", "front", x, y, "hair_light")
    if style == "traveller":
        for face in ("right", "left", "back"):
            skin.rectangle("hat", face, 1, 4, 2, 2, "hair")
            skin.point("hat", face, 2, 5, "hair_light")


def draw_body(skin):
    style = skin.specification["style"]
    for face in UV["body"]:
        skin.fill("body", face, "jacket")
    skin.fill("body", "bottom", "jacket_shadow")
    for face in ("right", "left"):
        skin.rectangle("body", face, 0, 0, 1, 12, "jacket_shadow")
        skin.rectangle("body", face, 2, 1, 1, 8, "jacket_light")
    for face in ("front", "back"):
        skin.rectangle("body", face, 0, 0, 1, 12, "jacket_shadow")
        skin.rectangle("body", face, 7, 0, 1, 12, "jacket_shadow")
        skin.rectangle("body", face, 1, 1, 1, 7, "jacket_light")
        skin.rectangle("body", face, 0, 11, 8, 1, "jacket_shadow")
    skin.rectangle("body", "front", 3, 0, 2, 9, "shirt")
    skin.rectangle("body", "front", 2, 0, 4, 1, "skin_shadow")
    if style == "builder":
        # Work vest over an undershirt, reinforced pockets, and a tool belt.
        skin.rectangle("body", "front", 3, 1, 2, 9, "shirt")
        for x in (1, 5):
            skin.rectangle("body", "front", x, 5, 2, 3, "jacket_shadow")
            skin.rectangle("body", "front", x, 5, 2, 1, "jacket_light")
        skin.rectangle("body", "front", 0, 9, 8, 2, "bag_shadow")
        skin.rectangle("body", "front", 3, 9, 2, 2, "metal")
        skin.rectangle("body", "front", 6, 8, 1, 3, "tool")
        skin.rectangle("body", "front", 5, 8, 2, 1, "tool")
        skin.rectangle("body", "back", 2, 3, 4, 3, "accent")
        skin.rectangle("body", "back", 3, 3, 2, 1, "tool")
        skin.rectangle("body", "back", 0, 9, 8, 2, "bag_shadow")
    elif style == "traveller":
        # Coral scarf, compass pin, a pair of coat pockets, and a backpack.
        skin.rectangle("body", "front", 2, 0, 4, 2, "accent")
        skin.rectangle("body", "front", 4, 2, 1, 3, "accent")
        skin.point("body", "front", 1, 3, "metal")
        skin.point("body", "front", 1, 4, "tool")
        for x in (1, 5):
            skin.rectangle("body", "front", x, 7, 2, 3, "jacket_shadow")
            skin.rectangle("body", "front", x, 7, 2, 1, "jacket_light")
        skin.rectangle("body", "back", 1, 2, 6, 8, "bag_shadow")
        skin.rectangle("body", "back", 2, 2, 4, 6, "bag")
        skin.rectangle("body", "back", 2, 3, 4, 1, "shoe_light")
        skin.rectangle("body", "back", 3, 5, 2, 2, "metal")
        skin.rectangle("body", "back", 2, 8, 4, 1, "shoe_light")
    else:
        # Golden scarf and the strap of a red satchel with a visible book.
        skin.rectangle("body", "front", 2, 0, 4, 2, "accent")
        skin.rectangle("body", "front", 2, 2, 1, 3, "accent")
        for y in range(9):
            skin.rectangle("body", "front", 6 - y // 2, y + 2, 1, 1, "bag_shadow")
        skin.rectangle("body", "front", 1, 8, 2, 3, "jacket_shadow")
        skin.rectangle("body", "front", 5, 8, 2, 3, "jacket_shadow")
        skin.rectangle("body", "back", 1, 5, 6, 5, "bag_shadow")
        skin.rectangle("body", "back", 2, 5, 4, 4, "bag")
        skin.rectangle("body", "back", 2, 4, 4, 1, "tool")
        skin.rectangle("body", "back", 3, 6, 2, 1, "metal")
        for y in range(5):
            skin.point("body", "back", 2 + y // 2, y, "bag_shadow")


def draw_limbs(skin):
    style = skin.specification["style"]
    for arm in ("right_arm", "left_arm"):
        for face in UV[arm]:
            skin.fill(arm, face, "shirt" if style == "builder" else "jacket")
            if face in ("front", "back", "right", "left"):
                skin.rectangle(arm, face, 0, 0, 1, 8, "jacket_shadow" if style != "builder" else "skin_shadow")
                skin.rectangle(arm, face, 2, 1, 1, 6, "shirt" if style == "builder" else "jacket_light")
                skin.rectangle(arm, face, 0, 8, 4, 1, "jacket_light")
                skin.rectangle(arm, face, 0, 9, 4, 3, "skin")
                skin.rectangle(arm, face, 0, 9, 1, 3, "skin_shadow")
                skin.point(arm, face, 2, 10, "skin_light")
        skin.fill(arm, "bottom", "skin_shadow")
    # A small sleeve badge gives each silhouette a second accent color.
    skin.rectangle("left_arm", "left", 1, 3, 2, 2, "accent")
    for leg in ("right_leg", "left_leg"):
        for face in UV[leg]:
            skin.fill(leg, face, "trousers")
            if face in ("front", "back", "right", "left"):
                skin.rectangle(leg, face, 0, 0, 1, 8, "trousers_shadow")
                skin.rectangle(leg, face, 2, 1, 1, 6, "trousers_light")
                skin.rectangle(leg, face, 0, 8, 4, 3, "shoe")
                skin.rectangle(leg, face, 0, 11, 4, 1, "sole")
                if face == "front":
                    skin.rectangle(leg, face, 1, 9, 2, 1, "shoe_light")
                    if style == "builder":
                        skin.rectangle(leg, face, 1, 5, 2, 2, "trousers_shadow")
        skin.fill(leg, "bottom", "sole")


def png_chunk(kind, data):
    payload = kind + data
    return struct.pack(">I", len(data)) + payload + struct.pack(">I", binascii.crc32(payload) & 0xffffffff)


def write_png(skin, path):
    data = b"".join(b"\0" + b"".join(bytes(pixel) for pixel in row) for row in skin.pixels)
    path.write_bytes(b"\x89PNG\r\n\x1a\n"
                     + png_chunk(b"IHDR", struct.pack(">IIBBBBB", 64, 64, 8, 6, 0, 0, 0))
                     + png_chunk(b"IDAT", zlib.compress(data, 9)) + png_chunk(b"IEND", b""))


def write_svg(skin, path):
    # Single-pixel horizontal runs retain crisp edges when edited or enlarged.
    lines = ['<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64" shape-rendering="crispEdges">',
             f'<title>{skin.specification["name"]} — original Kompass4MC player skin</title>',
             '<desc>64 by 64 wide-arm UV atlas. Original artwork, MIT license; see LICENSE.</desc>']
    for y, row in enumerate(skin.pixels):
        x = 0
        while x < 64:
            color = row[x]
            end = x + 1
            while end < 64 and row[end] == color:
                end += 1
            if color[3]:
                rgb = "".join(f"{channel:02x}" for channel in color[:3])
                lines.append(f'<rect x="{x}" y="{y}" width="{end - x}" height="1" fill="#{rgb}"/>')
            x = end
    path.write_text("\n".join(lines + ["</svg>", ""]), encoding="utf-8")


def create_previews(skins):
    from PIL import Image, ImageDraw, ImageFont

    destination = ROOT / "output/qa/npc-skins"
    destination.mkdir(parents=True, exist_ok=True)
    contact = Image.new("RGB", (1320, 530), "#f2eee3")
    draw = ImageDraw.Draw(contact)
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 23)
        small = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 15)
    except OSError:
        font = small = ImageFont.load_default()
    draw.text((28, 14), "Eigene NPC-Skins · 64 × 64 · breite Arme · MIT", font=font, fill="#263d3c")
    for index, skin in enumerate(skins):
        x0 = 28 + index * 432
        draw.text((x0, 54), skin.specification["name"], font=font, fill="#263d3c")
        checker = Image.new("RGBA", (64, 64), "#e9e8e3")
        cd = ImageDraw.Draw(checker)
        for y in range(0, 64, 4):
            for x in range(0, 64, 4):
                if (x // 4 + y // 4) % 2:
                    cd.rectangle((x, y, x + 3, y + 3), fill="#d6d7d1")
        texture = Image.new("RGBA", (64, 64))
        texture.putdata([pixel for row in skin.pixels for pixel in row])
        checker.alpha_composite(texture)
        contact.paste(checker.convert("RGB").resize((384, 384), Image.Resampling.NEAREST), (x0, 91))
        draw.text((x0, 487), "Alle Basisflächen opak · Haar-Overlay transparent", font=small, fill="#52605b")
    contact.save(destination / "skin-atlases.png")

    view = Image.new("RGB", (1380, 610), "#f2eee3")
    draw = ImageDraw.Draw(view)
    draw.text((28, 14), "Texturprüfung · orthografische Vorder- und Rückansicht", font=font, fill="#263d3c")
    # (part, x, y, z, width, height, depth), model pixels; the arms are four pixels wide.
    cubes = [("left_leg", 0, 20, -2, 4, 12, 4),
             ("right_leg", -4, 20, -2, 4, 12, 4),
             ("left_arm", 4, 8, -2, 4, 12, 4),
             ("body", -4, 8, -2, 8, 12, 4),
             ("right_arm", -8, 8, -2, 4, 12, 4),
             ("head", -4, 0, -4, 8, 8, 8)]
    scale = 12

    def paint_cube(skin, part, x, y, z, width, height, depth, origin, reverse=False, overlay=False):
        def project(px, py, pz):
            if reverse:
                px, pz = -px, -pz
            return (origin[0] + scale * (px - pz * .48),
                    origin[1] + scale * (py + pz * .23))

        front = "back" if reverse else "front"
        side = "left" if reverse else "right"
        face_points = [
            (side, (x + width if reverse else x, y, z if reverse else z + depth),
             (0, 0, depth if reverse else -depth), (0, height, 0)),
            (front, (x + width if reverse else x, y, z + depth if reverse else z),
             (-width if reverse else width, 0, 0), (0, height, 0)),
            ("top", (x, y, z + depth), (width, 0, 0), (0, 0, -depth)),
        ]
        for face, start, across, down in face_points:
            u, v, tw, th = UV[part][face]
            for py in range(th):
                for px in range(tw):
                    color = skin.pixels[v + py][u + px]
                    if not color[3]:
                        continue
                    points = []
                    for dx, dy in ((0, 0), (1, 0), (1, 1), (0, 1)):
                        tx, ty = (px + dx) / tw, (py + dy) / th
                        points.append(project(*(start[i] + across[i] * tx + down[i] * ty for i in range(3))))
                    shade = 1.0 if face in ("front", "back") else .80 if face != "top" else 1.07
                    fill = tuple(min(255, round(channel * shade)) for channel in color[:3])
                    draw.polygon(points, fill=fill)

    for index, skin in enumerate(skins):
        x0 = 48 + index * 450
        draw.text((x0, 58), skin.specification["name"], font=font, fill="#263d3c")
        for reverse, center in ((False, x0 + 104), (True, x0 + 323)):
            draw.ellipse((center - 78, 490, center + 78, 512), fill="#d3d5c7")
            for cube in cubes:
                paint_cube(skin, *cube, (center, 110), reverse=reverse)
                if cube[0] == "head":
                    paint_cube(skin, "hat", -4.12, -.12, -4.12, 8.24, 8.24, 8.24,
                               (center, 110), reverse=reverse, overlay=True)
            draw.text((center - 43, 533), "Rückseite" if reverse else "Vorderseite", font=small, fill="#52605b")
    draw.text((28, 579), "QA-Vorschau aus den UV-Flächen; endgültige Darstellung und Bewegung erfolgen im Spiel.", font=small, fill="#52605b")
    view.save(destination / "skin-model-preview.png")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview", action="store_true", help="also render QA previews with Pillow")
    args = parser.parse_args()
    DESTINATION.mkdir(parents=True, exist_ok=True)
    skins = []
    for key in ("mila", "jaro", "lio"):
        skin = Skin(json.loads((SOURCE / f"{key}.json").read_text(encoding="utf-8")))
        draw_head(skin)
        draw_body(skin)
        draw_limbs(skin)
        skin.validate()
        write_png(skin, DESTINATION / f"{key}.png")
        write_svg(skin, SOURCE / f"{key}.svg")
        skins.append(skin)
        print(f"{skin.specification['name']}: 64×64 RGBA; all 36 base faces opaque; {DESTINATION / (key + '.png')}")
    if args.preview:
        create_previews(skins)


if __name__ == "__main__":
    main()
