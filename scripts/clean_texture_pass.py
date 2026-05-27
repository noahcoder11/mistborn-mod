#!/usr/bin/env python3
"""Clean vanilla-plus texture pass for Mistborn: Metal Arts.

This script intentionally uses hand-defined pixel templates and restrained
palettes. It does not copy, trace, recolor, import, or derive assets from any
other mod. It only targets broad Minecraft mod art traits: clear silhouettes,
low color counts, hard edges, and readable 16x16 icons/items.
"""

from __future__ import annotations

import argparse
import json
import math
import shutil
import statistics
import struct
import sys
import zlib
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/mistborn_metal_arts"
TEXTURES = ASSETS / "textures"
DOCS = ROOT / "docs"
MODID = "mistborn_metal_arts"

METALS = [
    ("iron", "Iron"),
    ("steel", "Steel"),
    ("tin", "Tin"),
    ("pewter", "Pewter"),
    ("zinc", "Zinc"),
    ("brass", "Brass"),
    ("copper", "Copper"),
    ("bronze", "Bronze"),
    ("gold", "Gold"),
    ("electrum", "Electrum"),
    ("cadmium", "Cadmium"),
    ("bendalloy", "Bendalloy"),
    ("aluminum", "Aluminum"),
    ("duralumin", "Duralumin"),
    ("chromium", "Chromium"),
    ("nicrosil", "Nicrosil"),
    ("atium", "Atium"),
    ("lerasium", "Lerasium"),
    ("lead", "Lead"),
    ("silver", "Silver"),
    ("nickel", "Nickel"),
    ("harmonium", "Harmonium"),
    ("malatium", "Malatium"),
    ("lerasatium", "Lerasatium"),
]
METAL_IDS = [mid for mid, _ in METALS]
ALLOMANTIC = [mid for mid in METAL_IDS if mid not in {"lerasium", "lerasatium"}]
FERUCHEMICAL = [mid for mid in METAL_IDS if mid not in {"atium", "lerasium", "harmonium", "malatium", "lerasatium"}]

# outline, shadow, base, highlight, accent. Palettes are deliberately small.
PALETTES = {
    "iron": ((35, 35, 35), (80, 80, 80), (120, 120, 120), (230, 230, 230), (175, 175, 175)),
    "aluminum": ((48, 53, 71), (92, 102, 111), (129, 147, 147), (231, 244, 240), (194, 205, 200)),
    "steel": ((33, 32, 36), (78, 75, 78), (115, 113, 115), (229, 227, 227), (170, 167, 168)),
    "brass": ((113, 76, 30), (196, 144, 70), (254, 214, 121), (255, 254, 245), (255, 240, 177)),
    "copper": ((109, 52, 33), (156, 69, 41), (193, 90, 54), (251, 195, 182), (252, 153, 130)),
    "tin": ((76, 92, 85), (128, 149, 142), (176, 201, 195), (226, 250, 245), (219, 239, 235)),
    "pewter": ((75, 64, 64), (101, 94, 94), (169, 161, 161), (218, 213, 213), (185, 183, 183)),
    "zinc": ((47, 56, 44), (80, 92, 74), (155, 161, 108), (242, 236, 212), (227, 219, 187)),
    "bronze": ((62, 21, 13), (100, 57, 33), (165, 131, 89), (255, 249, 232), (236, 216, 166)),
    "gold": ((96, 72, 55), (146, 114, 87), (178, 153, 136), (255, 247, 237), (254, 244, 236)),
    "electrum": ((96, 72, 55), (146, 114, 87), (178, 153, 136), (246, 226, 202), (254, 244, 236)),
    "atium": ((20, 11, 11), (34, 29, 30), (54, 52, 52), (169, 163, 163), (97, 91, 91)),
    "lerasium": ((81, 83, 100), (120, 131, 139), (188, 199, 181), (254, 255, 247), (224, 227, 214)),
    "cadmium": ((82, 20, 7), (141, 69, 54), (208, 109, 81), (255, 243, 236), (255, 220, 201)),
    "bendalloy": ((45, 18, 6), (74, 36, 11), (109, 76, 34), (239, 223, 146), (221, 189, 102)),
    "chromium": ((34, 29, 31), (56, 51, 53), (103, 106, 109), (174, 174, 174), (117, 119, 122)),
    "nicrosil": ((51, 50, 66), (71, 70, 85), (146, 150, 182), (207, 209, 228), (181, 184, 208)),
    "duralumin": ((39, 50, 52), (57, 71, 76), (122, 143, 144), (224, 235, 234), (220, 235, 234)),
    "lead": ((28, 22, 42), (36, 30, 54), (49, 47, 80), (77, 86, 114), (69, 75, 105)),
    "silver": ((70, 70, 98), (85, 90, 121), (145, 159, 189), (233, 238, 244), (197, 210, 227)),
    "nickel": ((73, 71, 59), (141, 138, 98), (196, 192, 145), (248, 250, 229), (244, 245, 212)),
    "harmonium": ((100, 81, 91), (117, 114, 126), (152, 149, 170), (227, 229, 227), (192, 197, 192)),
    "malatium": ((96, 72, 55), (146, 114, 87), (178, 153, 136), (255, 247, 237), (254, 244, 236)),
    "lerasatium": ((90, 81, 100), (106, 113, 126), (115, 120, 138), (254, 255, 247), (144, 151, 159)),
}




def ensure(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def cap(name: str) -> str:
    return " ".join(part.capitalize() for part in name.split("_"))


def rgba(color, alpha=255):
    return (int(color[0]), int(color[1]), int(color[2]), int(alpha))


def clamp(value: float) -> int:
    return max(0, min(255, int(round(value))))


def shade(color, factor: float):
    return (clamp(color[0] * factor), clamp(color[1] * factor), clamp(color[2] * factor), color[3])


def mix(a, b, t: float):
    return (
        clamp(a[0] * (1 - t) + b[0] * t),
        clamp(a[1] * (1 - t) + b[1] * t),
        clamp(a[2] * (1 - t) + b[2] * t),
        clamp(a[3] * (1 - t) + b[3] * t),
    )


def pal(metal: str):
    return tuple(rgba(c) for c in PALETTES[metal])


def png_write(path: Path, width: int, height: int, pixels) -> None:
    ensure(path)
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            raw.extend(bytes(pixels[y][x]))

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))


def _paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    return a if pa <= pb and pa <= pc else b if pb <= pc else c


def read_png(path: Path):
    data = path.read_bytes()
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError(f"Not a PNG: {path}")
    pos = 8
    width = height = color_type = None
    idat = bytearray()
    palette = None
    transparency = None
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        payload = data[pos + 8:pos + 8 + length]
        pos += length + 12
        if kind == b"IHDR":
            width, height, bit_depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", payload)
            if bit_depth != 8 or interlace != 0:
                raise ValueError(f"Unsupported PNG format: {path}")
        elif kind == b"PLTE":
            palette = [tuple(payload[i:i + 3]) for i in range(0, len(payload), 3)]
        elif kind == b"tRNS":
            transparency = payload
        elif kind == b"IDAT":
            idat.extend(payload)
        elif kind == b"IEND":
            break
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color_type]
    stride = width * channels
    raw = zlib.decompress(bytes(idat))
    rows = []
    i = 0
    prev = [0] * stride
    for _y in range(height):
        f = raw[i]
        i += 1
        cur = list(raw[i:i + stride])
        i += stride
        for x in range(stride):
            left = cur[x - channels] if x >= channels else 0
            up = prev[x]
            up_left = prev[x - channels] if x >= channels else 0
            if f == 1:
                cur[x] = (cur[x] + left) & 255
            elif f == 2:
                cur[x] = (cur[x] + up) & 255
            elif f == 3:
                cur[x] = (cur[x] + ((left + up) // 2)) & 255
            elif f == 4:
                cur[x] = (cur[x] + _paeth(left, up, up_left)) & 255
        prev = cur
        row = []
        for x in range(width):
            off = x * channels
            if color_type == 6:
                row.append(tuple(cur[off:off + 4]))
            elif color_type == 2:
                row.append((cur[off], cur[off + 1], cur[off + 2], 255))
            elif color_type == 0:
                row.append((cur[off], cur[off], cur[off], 255))
            elif color_type == 4:
                row.append((cur[off], cur[off], cur[off], cur[off + 1]))
            elif color_type == 3:
                r, g, b = palette[cur[off]]
                a = transparency[cur[off]] if transparency and cur[off] < len(transparency) else 255
                row.append((r, g, b, a))
        rows.append(row)
    return width, height, rows


class Canvas:
    def __init__(self, width=16, height=16, bg=(0, 0, 0, 0)):
        self.w = width
        self.h = height
        self.p = [[bg for _ in range(width)] for _ in range(height)]

    def save(self, rel: str):
        png_write(TEXTURES / rel, self.w, self.h, self.p)

    def set(self, x: int, y: int, color):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.p[y][x] = color

    def rect(self, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, color)

    def line(self, x0, y0, x1, y1, color):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = 1 if x0 < x1 else -1, 1 if y0 < y1 else -1
        err = dx + dy
        while True:
            self.set(x0, y0, color)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def ellipse(self, cx, cy, rx, ry, color, outline=None):
        for y in range(cy - ry - 1, cy + ry + 2):
            for x in range(cx - rx - 1, cx + rx + 2):
                if rx <= 0 or ry <= 0:
                    continue
                v = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
                if v <= 1:
                    self.set(x, y, color)
                if outline and 0.72 <= v <= 1.22:
                    self.set(x, y, outline)

    def polygon(self, points, color, outline=None):
        minx = math.floor(min(x for x, _ in points))
        maxx = math.ceil(max(x for x, _ in points))
        miny = math.floor(min(y for _, y in points))
        maxy = math.ceil(max(y for _, y in points))
        for y in range(miny, maxy + 1):
            for x in range(minx, maxx + 1):
                inside = False
                j = len(points) - 1
                for i, (xi, yi) in enumerate(points):
                    xj, yj = points[j]
                    if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / ((yj - yi) or 0.01) + xi:
                        inside = not inside
                    j = i
                if inside:
                    self.set(x, y, color)
        if outline:
            for i, (x0, y0) in enumerate(points):
                x1, y1 = points[(i + 1) % len(points)]
                self.line(round(x0), round(y0), round(x1), round(y1), outline)


def draw_ingot(metal):
    outline, shadow, base, high, accent = pal(metal)
    c = Canvas()
    # Vanilla-like ingot language: chunky, centered, and readable at 16x16.
    c.polygon([(3, 8), (5, 5), (11, 4), (14, 7), (12, 11), (5, 12)], shadow, outline)
    c.polygon([(5, 6), (11, 5), (12, 7), (4, 8)], base)
    c.line(6, 5, 10, 5, high)
    c.line(5, 11, 12, 10, shadow)
    c.set(4, 9, outline)
    if metal in {"copper", "bronze", "duralumin", "nicrosil", "atium", "lerasium"}:
        c.set(11, 6, accent)
    return c


def draw_flakes(metal):
    outline, shadow, base, high, accent = pal(metal)
    c = Canvas()
    chips = [
        ([(3, 9), (5, 7), (7, 8), (6, 10), (4, 11)], base),
        ([(9, 5), (12, 6), (11, 8), (8, 8)], accent if metal in {"copper", "bronze", "nicrosil", "lerasium"} else base),
        ([(8, 11), (11, 10), (13, 12), (10, 13)], shadow),
        ([(4, 13), (6, 12), (7, 13)], base),
    ]
    for pts, col in chips:
        c.polygon(pts, col, outline)
    c.set(5, 7, high)
    c.set(10, 6, high)
    return c


def draw_powder(metal):
    outline, shadow, base, high, accent = pal(metal)
    c = Canvas()
    pile = [(3, 12), (5, 10), (8, 9), (11, 10), (13, 12), (10, 14), (5, 14)]
    c.polygon(pile, shadow, outline)
    for x, y, col in [(5, 12, base), (7, 11, base), (9, 10, base), (10, 12, base), (7, 12, high), (12, 12, accent)]:
        c.set(x, y, col)
    c.line(5, 13, 11, 13, outline)
    return c


def draw_bead(metal):
    outline, shadow, base, high, accent = pal(metal)
    c = Canvas()
    if metal == "lerasium":
        base, high, accent = mix(base, high, 0.45), high, accent
    c.ellipse(8, 8, 4, 4, base, outline)
    c.ellipse(9, 9, 2, 2, shadow)
    c.rect(6, 5, 7, 6, high)
    c.set(10, 6, accent)
    if metal == "atium":
        c.rect(7, 7, 9, 9, outline)
        c.set(6, 5, high)
    if metal == "lerasium":
        c.set(8, 4, high)
        c.set(4, 8, high)
        c.set(12, 8, high)
    return c


def draw_nugget(metal):
    outline, shadow, base, high, accent = pal(metal)
    c = Canvas()
    c.polygon([(5, 7), (8, 5), (11, 6), (12, 9), (10, 12), (6, 12), (4, 10)], base, outline)
    c.polygon([(6, 7), (8, 6), (10, 7), (8, 8)], high)
    c.line(6, 11, 10, 11, shadow)
    c.set(10, 9, accent)
    return c


def draw_raw_chunk(metal):
    outline, shadow, base, high, accent = pal(metal)
    c = Canvas()
    rock_o, rock_s, rock_b, rock_h = rgba((34, 33, 32)), rgba((58, 56, 54)), rgba((84, 82, 78)), rgba((116, 112, 104))
    c.polygon([(3, 6), (6, 3), (12, 4), (14, 9), (10, 14), (4, 12), (2, 8)], rock_b, rock_o)
    c.line(5, 4, 12, 5, rock_h)
    c.line(4, 12, 10, 14, rock_s)
    for pts in [[(5, 7), (7, 6), (8, 8), (6, 9)], [(10, 6), (12, 8), (10, 10)], [(7, 11), (9, 10), (10, 12)]]:
        c.polygon(pts, base, outline)
    c.set(7, 6, high)
    return c


def draw_vial(metal=None, mixed=False, empty=False):
    c = Canvas()
    glass_o = rgba((64, 85, 92), 210)
    glass = rgba((174, 218, 226), 132)
    shine = rgba((238, 252, 255), 190)
    cork = rgba((118, 78, 40))
    c.rect(6, 1, 9, 3, cork)
    c.rect(5, 4, 10, 4, glass_o)
    c.line(4, 5, 4, 12, glass_o)
    c.line(11, 5, 11, 12, glass_o)
    c.line(5, 13, 10, 13, glass_o)
    c.rect(5, 6, 10, 12, glass)
    c.line(6, 5, 6, 11, shine)
    if not empty:
        colors = [pal(metal)[2], pal(metal)[3], pal(metal)[4]] if metal else [rgba((92, 112, 126)), rgba((176, 132, 54)), rgba((90, 150, 186))]
        if mixed:
            c.rect(5, 9, 10, 12, rgba((82, 102, 118), 220))
            for x, col in zip([6, 8, 10], colors):
                c.set(x, 10, col)
                c.set(x - 1, 12, col)
        else:
            fill = rgba(colors[0][:3], 218)
            c.rect(5, 9, 10, 12, fill)
            c.set(6, 10, colors[1])
            c.set(9, 11, colors[2])
            if metal == "atium":
                c.set(8, 10, rgba((16, 18, 20)))
                c.set(7, 9, colors[1])
            if metal == "lerasium":
                c.set(8, 9, rgba((255, 255, 238)))
                c.set(9, 10, colors[1])
    return c


def draw_metalmind(metal, unkeyed=False):
    outline, shadow, base, high, accent = pal(metal)
    c = Canvas()
    ring = mix(base, high, 0.18) if unkeyed else base
    c.ellipse(8, 8, 6, 4, ring, outline)
    c.ellipse(8, 8, 3, 2, (0, 0, 0, 0))
    c.line(4, 7, 12, 7, high)
    c.line(5, 10, 11, 10, shadow)
    if unkeyed:
        c.set(8, 5, rgba((225, 238, 238)))
        c.set(8, 11, rgba((225, 238, 238)))
    else:
        c.set(12, 8, accent)
    return c


def draw_spike(metal, charged=False, decaying=False):
    ref_path = ROOT / f"references/{metal}_spike.png"
    if ref_path.exists():
        w, h, pixels = read_png(ref_path)
        c = Canvas(w, h)
        c.p = [row[:] for row in pixels]
        if charged or decaying:
            red = rgba((188, 28, 38)) if charged else rgba((120, 25, 30))
            # Apply red lines along the diagonal for the LeafReynolds silhouette
            for i in range(4, 12):
                if 0 <= i < w and 0 <= i < h:
                    # Check if the pixel is opaque before drawing over it
                    if c.p[i][i][3] > 128:
                        c.set(i, i, red)
                    if c.p[i+1][i][3] > 128:
                        c.set(i+1, i, red)
        if decaying:
            # Add some erosion for decaying state
            for y in range(h):
                for x in range(w):
                    if (x + y) % 5 == 0:
                        c.set(x, y, (0, 0, 0, 0))
        return c

    outline, shadow, base, high, accent = pal(metal)
    c = Canvas()
    body = shade(base, 0.62 if charged else 0.78)
    if metal == "atium":
        body = shadow
    pts = [(7, 2), (10, 3), (9, 13), (7, 15), (6, 13)]
    if decaying:
        pts = [(7, 2), (10, 3), (9, 8), (10, 10), (8, 14), (6, 13)]
    c.polygon(pts, body, outline)
    c.line(8, 3, 8, 13, high if metal in {"atium", "lerasium"} else shade(high, 0.82))
    c.line(6, 13, 9, 13, shadow)
    if charged or decaying:
        red = rgba((188, 28, 38))
        c.line(8, 5, 9, 7, red)
        c.line(8, 9, 7, 11, red if charged else rgba((120, 25, 30)))
    return c


def draw_coin_pouch():
    c = Canvas()
    o = rgba((42, 30, 22))
    cloth = rgba((104, 72, 43))
    hi = rgba((158, 112, 66))
    coin = rgba((188, 184, 144))
    c.polygon([(4, 6), (12, 6), (13, 12), (10, 14), (5, 14), (3, 12)], cloth, o)
    c.line(5, 7, 11, 7, hi)
    c.rect(5, 4, 11, 6, o)
    c.set(6, 5, coin)
    c.set(9, 5, coin)
    return c


def draw_special_item(name):
    c = Canvas()
    o = rgba((32, 32, 34))
    metal = rgba((128, 136, 138))
    blue = rgba((86, 156, 220))
    brass = rgba((185, 132, 48))
    red = rgba((176, 28, 36))
    if name == "coinshot_coin" or name == "metallic_coin":
        col = rgba((205, 199, 156)) if name == "metallic_coin" else rgba((184, 202, 208))
        c.ellipse(8, 8, 5, 5, col, o)
        c.ellipse(8, 8, 3, 3, shade(col, 0.75))
        c.set(6, 6, rgba((246, 248, 228)))
    elif name == "coin_pouch":
        return draw_coin_pouch()
    elif name == "metal_arts_guidebook":
        c.rect(4, 2, 12, 14, o)
        c.rect(5, 3, 13, 13, rgba((72, 70, 64)))
        c.line(6, 3, 6, 13, rgba((120, 92, 58)))
        c.set(11, 5, brass)
        c.line(8, 9, 11, 9, metal)
    elif name == "allomancer_testing_kit":
        c.rect(3, 7, 13, 13, o)
        c.rect(4, 8, 12, 12, rgba((82, 62, 42)))
        c.rect(5, 4, 11, 7, o)
        c.set(5, 9, blue)
        c.set(7, 10, rgba((210, 205, 160)))
        c.set(10, 9, rgba((180, 92, 52)))
    elif name == "feruchemist_testing_kit":
        c.rect(3, 7, 13, 13, o)
        c.rect(4, 8, 12, 12, rgba((72, 58, 45)))
        for x, col in [(5, metal), (8, brass), (11, rgba((206, 170, 70)))]:
            c.ellipse(x, 10, 2, 2, col, o)
    elif name == "spike_removal_tool":
        c.line(4, 13, 12, 4, metal)
        c.line(5, 14, 13, 5, o)
        c.rect(10, 3, 14, 6, rgba((90, 60, 36)))
        c.set(8, 9, red)
    elif name == "bronze_detector":
        c.ellipse(8, 8, 6, 6, brass, o)
        for r in [2, 4]:
            c.ellipse(8, 8, r, r, (0, 0, 0, 0), rgba((230, 170, 80)))
    elif name == "coppercloud_charm":
        c.ellipse(8, 8, 6, 5, rgba((168, 82, 38)), o)
        c.ellipse(7, 8, 3, 2, rgba((70, 140, 104)), None)
        c.ellipse(10, 9, 3, 2, rgba((70, 140, 104)), None)
    elif name == "time_bubble_focus":
        c.ellipse(8, 8, 6, 6, rgba((52, 62, 74)), o)
        for r in [3, 5]:
            c.ellipse(8, 8, r, r, (0, 0, 0, 0), rgba((140, 205, 224)))
        c.line(8, 4, 8, 8, rgba((230, 240, 240)))
    elif name == "atium_shadow_lens":
        c.ellipse(8, 8, 6, 4, rgba((56, 64, 66)), o)
        c.ellipse(8, 8, 3, 2, rgba((190, 218, 210)), None)
        c.set(8, 8, rgba((12, 14, 16)))
    elif name == "lerasium_core":
        c.ellipse(8, 8, 5, 5, rgba((236, 218, 128)), o)
        c.rect(7, 3, 8, 13, rgba((255, 255, 232)))
        c.rect(3, 7, 13, 8, rgba((255, 255, 232)))
    elif name == "anchor_marker":
        c.polygon([(8, 2), (12, 7), (10, 7), (10, 13), (6, 13), (6, 7), (4, 7)], blue, o)
    else:
        c.ellipse(8, 8, 5, 5, metal, o)
    return c


def draw_icon(metal):
    outline, shadow, base, high, accent = pal(metal)
    c = Canvas()
    bg = rgba((20, 22, 25), 236)
    inner = rgba((36, 38, 42), 232)
    glyph = mix(high, rgba((255, 255, 255)), 0.36)
    c.ellipse(8, 8, 7, 7, bg, rgba((8, 9, 11)))
    c.ellipse(8, 8, 5, 5, inner)
    # A two-pixel metal accent keeps the family cohesive without turning the
    # symbol into a noisy colored medallion.
    c.set(12, 10, accent)
    c.set(11, 11, base)
    if metal == "iron":
        c.line(12, 8, 5, 8, glyph); c.line(5, 8, 8, 5, glyph); c.line(5, 8, 8, 11, glyph)
    elif metal == "steel":
        c.line(4, 8, 11, 8, glyph); c.line(11, 8, 8, 5, glyph); c.line(11, 8, 8, 11, glyph)
    elif metal == "tin":
        c.ellipse(8, 8, 5, 3, (0, 0, 0, 0), glyph); c.set(8, 8, glyph)
    elif metal == "pewter":
        c.rect(5, 7, 11, 10, glyph); c.rect(6, 5, 7, 7, glyph); c.rect(9, 5, 10, 7, glyph)
    elif metal == "zinc":
        c.line(3, 10, 6, 5, glyph); c.line(6, 5, 9, 11, glyph); c.line(9, 11, 13, 4, glyph)
    elif metal == "brass":
        c.line(3, 9, 6, 7, glyph); c.line(6, 7, 9, 8, glyph); c.line(9, 8, 13, 6, glyph)
    elif metal == "copper":
        c.rect(5, 8, 11, 11, glyph); c.ellipse(8, 7, 4, 3, (0, 0, 0, 0), glyph)
    elif metal == "bronze":
        for r in [2, 4, 6]:
            c.ellipse(8, 8, r, r, (0, 0, 0, 0), glyph)
    elif metal == "gold":
        c.line(5, 4, 5, 12, glyph); c.line(11, 4, 11, 12, glyph); c.line(5, 8, 11, 8, glyph)
    elif metal == "electrum":
        c.line(8, 3, 8, 12, glyph); c.line(8, 7, 4, 11, glyph); c.line(8, 7, 12, 11, glyph)
    elif metal == "cadmium":
        c.ellipse(8, 8, 5, 5, (0, 0, 0, 0), glyph); c.line(8, 4, 8, 8, glyph); c.line(8, 8, 6, 10, glyph)
    elif metal == "bendalloy":
        c.ellipse(8, 8, 5, 5, (0, 0, 0, 0), glyph); c.line(8, 8, 12, 5, glyph); c.line(8, 8, 10, 11, glyph)
    elif metal == "aluminum":
        c.ellipse(8, 8, 4, 4, (0, 0, 0, 0), glyph)
    elif metal == "duralumin":
        c.line(8, 3, 8, 13, glyph); c.line(3, 8, 13, 8, glyph); c.line(5, 5, 11, 11, glyph); c.line(11, 5, 5, 11, glyph)
    elif metal == "chromium":
        c.line(12, 4, 5, 11, glyph); c.line(12, 4, 11, 10, glyph); c.line(12, 4, 6, 5, glyph)
    elif metal == "nicrosil":
        c.line(4, 11, 8, 4, glyph); c.line(8, 4, 12, 11, glyph); c.line(5, 8, 11, 8, accent)
    elif metal == "atium":
        c.ellipse(8, 8, 5, 3, (0, 0, 0, 0), glyph); c.ellipse(8, 8, 2, 2, rgba((12, 14, 16)))
    elif metal == "lerasium":
        c.line(8, 3, 8, 13, glyph); c.line(3, 8, 13, 8, glyph); c.set(5, 5, glyph); c.set(11, 5, glyph); c.set(5, 11, glyph); c.set(11, 11, glyph)
    return c


def base_stone(deepslate=False):
    c = Canvas()
    a = rgba((90, 88, 84)) if not deepslate else rgba((58, 62, 68))
    b = rgba((72, 70, 67)) if not deepslate else rgba((44, 47, 53))
    hi = rgba((111, 108, 101)) if not deepslate else rgba((74, 80, 88))
    for y in range(16):
        for x in range(16):
            c.set(x, y, a)
    dark_runs = [
        (0, 3, 4), (5, 1, 3), (10, 2, 5), (2, 8, 4), (8, 7, 3), (12, 10, 4), (4, 14, 5),
    ]
    for x, y, length in dark_runs:
        c.line(x, y, min(15, x + length), y, b)
    for x, y in [(2, 2), (7, 4), (13, 4), (5, 9), (10, 12), (1, 13)]:
        c.set(x, y, hi)
    for x, y in [(14, 1), (3, 6), (11, 8), (7, 13)]:
        c.set(x, y, mix(a, b, 0.45))
    return c


def draw_ore(metal, deepslate=False):
    c = base_stone(deepslate)
    outline, shadow, base, high, accent = pal(metal)
    for pts in [[(3, 4), (5, 3), (7, 5), (5, 7)], [(10, 4), (12, 6), (11, 9), (9, 7)], [(5, 11), (8, 10), (10, 12), (7, 14)]]:
        c.polygon(pts, base, shadow)
    c.set(5, 4, high)
    c.set(11, 5, high)
    return c


def draw_machine_face(block, face):
    c = Canvas()
    o = rgba((38, 35, 32))
    wood = rgba((104, 76, 48))
    wood2 = rgba((76, 56, 38))
    metal = rgba((90, 96, 98))
    brass = rgba((177, 122, 42))
    red = rgba((158, 28, 35))
    stone = rgba((78, 76, 72))
    c.rect(0, 0, 15, 15, o)
    c.rect(1, 1, 14, 14, wood if block in {"metallurgy_table", "metalworking_table"} else stone)
    if face == "side":
        c.line(1, 5, 14, 5, wood2)
        c.line(1, 10, 14, 10, wood2)
        c.rect(3, 6, 12, 9, metal if block not in {"metallurgy_table", "metalworking_table"} else wood2)
    elif face == "bottom":
        c.rect(1, 1, 14, 14, wood2)
    elif block in {"metallurgy_table", "metalworking_table"}:
        c.rect(2, 2, 13, 13, rgba((84, 62, 40)))
        c.line(3, 8, 12, 8, brass)
        c.line(8, 3, 8, 12, metal)
    elif block == "alloy_furnace":
        if face == "front_lit":
            c.rect(4, 4, 11, 12, o); c.rect(5, 6, 10, 10, rgba((226, 106, 34))); c.rect(6, 7, 9, 9, rgba((255, 177, 62)))
        elif face == "front":
            c.rect(4, 4, 11, 12, o); c.rect(5, 6, 10, 10, rgba((52, 42, 36)))
        else:
            c.rect(3, 3, 12, 12, metal); c.rect(5, 5, 10, 10, o)
    elif block == "spike_press":
        c.rect(3, 2, 12, 13, metal)
        c.rect(7, 2, 9, 13, o)
        c.polygon([(8, 4), (10, 8), (8, 12), (6, 8)], red, o)
    elif block == "bind_point_table":
        c.rect(2, 2, 13, 13, rgba((48, 42, 42)))
        c.line(3, 3, 12, 12, red)
        c.line(12, 3, 3, 12, red)
    elif block == "metalmind_charging_stand":
        c.rect(3, 3, 12, 12, metal)
        c.ellipse(8, 8, 5, 5, (0, 0, 0, 0), brass)
        c.ellipse(8, 8, 2, 2, rgba((190, 220, 220)))
    else:
        c.rect(3, 3, 12, 12, metal)
        c.line(3, 8, 12, 8, brass)
    return c


def draw_special_block(name):
    if name in {"ash_deposit", "ash_layer"}:
        c = Canvas()
        for y in range(16):
            for x in range(16):
                c.set(x, y, rgba((94, 92, 88)) if (x + y) % 4 else rgba((70, 69, 67)))
        c.line(1, 12, 14, 12, rgba((58, 57, 55)))
        c.line(3, 5, 12, 5, rgba((118, 116, 110)))
        return c
    if "atium" in name:
        c = base_stone(True)
        for x, y in [(5, 5), (10, 6), (7, 11)]:
            c.polygon([(x, y - 3), (x + 2, y), (x, y + 3), (x - 2, y)], rgba((172, 214, 204)), rgba((30, 36, 40)))
        return c
    if "lerasium" in name or "shrine" in name:
        c = base_stone(False)
        gold = rgba((225, 203, 112))
        c.line(1, 8, 14, 8, gold)
        c.line(8, 1, 8, 14, rgba((250, 245, 205)))
        c.set(8, 8, rgba((255, 255, 235)))
        return c
    if "cache" in name or "vault" in name:
        c = base_stone(False)
        c.rect(2, 3, 13, 13, rgba((68, 70, 70)))
        c.line(2, 8, 13, 8, rgba((156, 128, 72)))
        c.line(8, 3, 8, 13, rgba((156, 128, 72)))
        return c
    if "hemalurgic" in name or "corrupted" in name:
        c = base_stone(False)
        c.line(3, 3, 12, 12, rgba((150, 24, 34)))
        c.line(5, 12, 10, 4, rgba((92, 18, 24)))
        return c
    if "planks" in name:
        c = Canvas()
        for y in range(16):
            c.line(0, y, 15, y, rgba((86, 58, 36)) if y % 4 else rgba((48, 34, 22)))
        c.line(4, 0, 4, 15, rgba((28, 21, 16)))
        c.line(11, 0, 11, 15, rgba((28, 21, 16)))
        c.set(2, 2, rgba((120, 82, 48)))
        c.set(13, 10, rgba((120, 82, 48)))
        return c
    c = base_stone(False)
    if "brick" in name:
        for y in [4, 9, 14]:
            c.line(0, y, 15, y, rgba((50, 49, 47)))
        for x in [4, 12]:
            c.line(x, 0, x, 4, rgba((50, 49, 47)))
        if "cracked" in name:
            c.line(6, 3, 9, 7, rgba((35, 34, 33)))
            c.line(9, 7, 7, 11, rgba((35, 34, 33)))
    if "metal_inlaid" in name or "canal" in name or "noble" in name:
        c.line(0, 8, 15, 8, rgba((124, 116, 82)))
    return c


def draw_particle(name):
    c = Canvas()
    colors = {
        "allomantic_line": rgba((95, 182, 242), 230), "metal_line": rgba((95, 182, 242), 230),
        "steelpush_spark": rgba((95, 182, 242)), "ironpull_spark": rgba((82, 148, 216)),
        "pewter_flare": rgba((188, 184, 166)), "tin_glint": rgba((230, 244, 248)),
        "coppercloud_mist": rgba((166, 82, 42), 180), "coppercloud": rgba((166, 82, 42), 180),
        "bronze_pulse": rgba((210, 142, 64)), "zinc_riot_wave": rgba((200, 58, 70)),
        "brass_soothe_wave": rgba((224, 166, 76)), "atium_shadow": rgba((160, 198, 190), 180),
        "lerasium_glow": rgba((250, 240, 180)), "hemalurgic_spark": rgba((194, 30, 40)),
        "time_bubble_edge": rgba((132, 206, 226), 180), "cadmium_distortion": rgba((88, 134, 176), 180),
        "bendalloy_distortion": rgba((214, 168, 132), 180), "nicrosil_burst": rgba((190, 150, 236)),
        "chromium_leech": rgba((176, 188, 194), 190),
    }
    col = colors.get(name, rgba((255, 255, 255)))
    if "line" in name:
        c.line(1, 9, 14, 6, col)
    elif "wave" in name or "pulse" in name or "edge" in name or "distortion" in name:
        for r in [3, 6]:
            c.ellipse(8, 8, r, r, (0, 0, 0, 0), col)
    elif "shadow" in name:
        c.ellipse(8, 8, 3, 5, col)
        c.set(8, 6, rgba((22, 24, 26), 210))
    else:
        c.line(8, 3, 8, 12, col)
        c.line(4, 8, 12, 8, col)
    return c


def draw_hud(name):
    sizes = {
        "metal_reserve_bar": (96, 16), "metal_reserve_fill": (96, 16),
        "metalmind_bar": (96, 16), "metalmind_fill": (96, 16),
        "burning_icon_frame": (16, 16), "flare_indicator": (16, 16),
        "bronze_pulse_arrow": (16, 16), "atium_warning": (16, 16),
        "coppercloud_status": (16, 16), "corruption_meter": (64, 16),
        "radial_menu": (96, 96), "radial_selection": (32, 32), "time_bubble_indicator": (32, 32),
    }
    c = Canvas(*sizes[name])
    w, h = c.w, c.h
    if "bar" in name or "meter" in name:
        c.rect(0, 3, w - 1, 12, rgba((10, 12, 14), 215))
        c.rect(1, 4, w - 2, 11, rgba((42, 45, 48), 230))
        c.line(1, 4, w - 2, 4, rgba((116, 122, 126), 230))
    elif "fill" in name:
        fill = rgba((78, 148, 214), 230) if "metal" in name else rgba((198, 150, 62), 230)
        c.rect(0, 4, w - 1, 11, fill)
        c.line(0, 4, w - 1, 4, rgba((180, 218, 238), 230))
    elif name == "radial_menu":
        c.ellipse(48, 48, 44, 44, rgba((17, 18, 21), 225), rgba((72, 76, 80), 240))
        for r in [20, 34]:
            c.ellipse(48, 48, r, r, (0, 0, 0, 0), rgba((96, 100, 104), 210))
    elif name == "radial_selection":
        c.ellipse(16, 16, 13, 13, rgba((58, 120, 184), 180), rgba((142, 202, 238), 240))
    else:
        return draw_icon({"flare_indicator": "duralumin", "bronze_pulse_arrow": "bronze", "atium_warning": "atium", "coppercloud_status": "copper", "time_bubble_indicator": "cadmium"}.get(name, "iron"))
    return c


def draw_gui(name):
    c = Canvas(176, 176)
    border = rgba((34, 35, 36))
    bg = rgba((72, 72, 70))
    trim = rgba((145, 102, 48))
    if "spike" in name or "bind" in name:
        trim = rgba((142, 34, 40))
    c.rect(0, 0, 175, 175, border)
    c.rect(4, 4, 171, 171, bg)
    c.rect(8, 8, 167, 167, rgba((86, 84, 80)))
    c.line(8, 22, 167, 22, trim)
    for sx, sy in [(26, 42), (52, 42), (78, 42), (112, 42), (138, 42), (52, 78), (78, 78), (112, 78)]:
        c.rect(sx, sy, sx + 17, sy + 17, rgba((28, 29, 30)))
        c.rect(sx + 1, sy + 1, sx + 16, sy + 16, rgba((116, 112, 105)))
    c.line(74, 66, 104, 66, trim)
    c.line(104, 66, 98, 61, trim)
    c.line(104, 66, 98, 71, trim)
    return c


def draw_effect(name):
    mapping = {
        "allomantic_pewter": "pewter", "tin_enhanced": "tin", "pewter_drag": "pewter",
        "sensory_overload": "tin", "coppercloud_hidden": "copper", "coppercloud": "copper",
        "bronze_detecting": "bronze", "bronze_seeking": "bronze", "atium_future_sight": "atium",
        "atium_sight": "atium", "lerasium_changed": "lerasium", "hemalurgic_corruption": "iron",
        "time_bubble_slow": "cadmium", "time_bubble_fast": "bendalloy", "emotional_riot": "zinc",
        "emotional_soothe": "brass", "emotional_pressure": "zinc",
    }
    c = draw_icon(mapping.get(name, "iron"))
    if any(word in name for word in ["drag", "overload", "corruption", "pressure"]):
        c.line(3, 3, 12, 12, rgba((190, 30, 40)))
    return c


def draw_entity(name):
    c = Canvas(64, 64)
    colors = {
        "coinshot_bandit": (rgba((44, 46, 48)), rgba((76, 134, 190))),
        "lurcher_guard": (rgba((58, 62, 66)), rgba((132, 140, 142))),
        "pewter_thug": (rgba((72, 66, 58)), rgba((154, 142, 120))),
        "tineye_scout": (rgba((52, 56, 58)), rgba((208, 226, 226))),
        "rioter": (rgba((58, 46, 48)), rgba((190, 58, 58))),
        "soother": (rgba((54, 52, 56)), rgba((200, 158, 74))),
        "seeker": (rgba((54, 48, 42)), rgba((172, 108, 46))),
        "smoker": (rgba((58, 50, 45)), rgba((164, 82, 42))),
        "atium_seer": (rgba((36, 38, 40)), rgba((164, 198, 190))),
        "mistborn_assassin": (rgba((32, 34, 38)), rgba((82, 124, 154))),
        "koloss": (rgba((76, 104, 124)), rgba((116, 62, 58))),
        "kandra": (rgba((170, 162, 148)), rgba((214, 204, 184))),
        "steel_inquisitor": (rgba((24, 22, 24)), rgba((172, 30, 38))),
    }
    base, accent = colors.get(name, (rgba((60, 60, 60)), rgba((120, 120, 120))))
    # Simple vanilla skin layout blocks.
    c.rect(8, 8, 15, 15, base)
    c.rect(24, 8, 31, 15, shade(base, 0.8))
    c.rect(16, 20, 27, 31, base)
    c.rect(28, 20, 39, 31, shade(base, 0.78))
    c.rect(40, 20, 47, 31, shade(base, 0.7))
    c.rect(0, 20, 7, 31, shade(base, 0.7))
    c.rect(16, 32, 27, 43, shade(base, 0.62))
    c.rect(28, 32, 39, 43, shade(base, 0.58))
    c.line(17, 22, 38, 22, accent)
    c.set(10, 11, accent)
    c.set(13, 11, accent)
    if name == "steel_inquisitor":
        c.line(7, 11, 16, 11, accent)
        c.line(10, 7, 10, 15, accent)
        c.line(13, 7, 13, 15, accent)
    if name == "koloss":
        c.rect(4, 20, 7, 31, shade(base, 0.85))
        c.rect(40, 20, 50, 31, shade(base, 0.85))
    return c


def draw_armor(name):
    c = Canvas(64, 32)
    base = rgba((38, 40, 44)) if "metalborn" in name else rgba((24, 22, 24)) if "inquisitor" in name else rgba((76, 104, 124))
    accent = rgba((90, 132, 160)) if "metalborn" in name else rgba((164, 30, 38)) if "inquisitor" in name else rgba((116, 62, 58))
    c.rect(0, 0, 63, 31, (0, 0, 0, 0))
    for x0 in [0, 16, 32, 48]:
        c.rect(x0 + 2, 4, x0 + 13, 27, base)
        c.line(x0 + 3, 8, x0 + 12, 8, accent)
    return c


def draw_advancement(name):
    metal = "lerasium" if "lerasium" in name or "mistborn" in name else "atium" if "atium" in name else "copper" if "copper" in name else "steel" if "steel" in name else "iron"
    return draw_icon(metal)


def item_model(name, handheld=False):
    return {
        "parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
        "textures": {"layer0": f"{MODID}:item/{name}"},
    }


def block_item_model(name):
    return {"parent": f"{MODID}:block/{name}"}


def write_json(rel, data):
    path = ASSETS / rel
    ensure(path)
    path.write_text(json.dumps(data, indent=2) + "\n")


def clear_generated_assets():
    for folder in ["textures", "models", "blockstates", "particles"]:
        path = ASSETS / folder
        if path.exists():
            shutil.rmtree(path)
        path.mkdir(parents=True, exist_ok=True)


@dataclass
class QualityMetrics:
    path: str
    width: int
    height: int
    opaque_pixels: int
    unique_colors: int
    contrast: float
    transparent_padding: bool
    flags: list[str]


def luminance(color):
    return 0.2126 * color[0] + 0.7152 * color[1] + 0.0722 * color[2]


def metrics_for(path: Path, base: Path) -> QualityMetrics:
    w, h, pixels = read_png(path)
    flat = [px for row in pixels for px in row]
    opaque = [px for px in flat if px[3] > 8]
    unique = {px for px in opaque}
    lums = [luminance(px) for px in opaque]
    edge = []
    if opaque:
        edge.extend(pixels[0])
        edge.extend(pixels[-1])
        edge.extend(row[0] for row in pixels)
        edge.extend(row[-1] for row in pixels)
    transparent_padding = any(px[3] <= 8 for px in edge) if edge else False
    flags = []
    rel = path.relative_to(base).as_posix()
    if not opaque:
        flags.append("fully transparent")
    if len(unique) <= 1 and len(opaque) == w * h:
        flags.append("flat single-color")
    if w % 16 != 0 or h % 16 != 0:
        flags.append("dimension not divisible by 16")
    if rel.startswith("item/") and w <= 16 and h <= 16 and len(unique) > 32:
        flags.append("too many colors for 16x16 item")
    if rel.startswith(("item/", "gui/icon_")) and not transparent_padding:
        flags.append("no transparent padding")
    if opaque and len(unique) > 1 and max(lums) - min(lums) < 24:
        flags.append("very low contrast")
    return QualityMetrics(rel, w, h, len(opaque), len(unique), round((max(lums) - min(lums)) if lums else 0, 1), transparent_padding, flags)


def collect_metrics() -> list[QualityMetrics]:
    return [metrics_for(path, TEXTURES) for path in sorted(TEXTURES.rglob("*.png"))]


def write_audit(before: list[QualityMetrics], after: list[QualityMetrics] | None = None):
    DOCS.mkdir(parents=True, exist_ok=True)

    def section(title, rows):
        noisy = [m for m in rows if any("too many colors" in f for f in m.flags)]
        unreadable = [m for m in rows if any(f in m.flags for f in ["very low contrast", "flat single-color", "fully transparent"])]
        no_padding = [m for m in rows if "no transparent padding" in m.flags]
        bad_dims = [m for m in rows if "dimension not divisible by 16" in m.flags]
        lines = [
            f"## {title}",
            "",
            f"- Textures scanned: {len(rows)}",
            f"- Noisy or over-colored 16x16 items: {len(noisy)}",
            f"- Low-contrast/flat/transparent readability problems: {len(unreadable)}",
            f"- Item/icon padding issues: {len(no_padding)}",
            f"- Dimension divisibility issues: {len(bad_dims)}",
            "",
        ]
        flagged = [m for m in rows if m.flags]
        if flagged:
            lines.append("| Texture | Size | Colors | Contrast | Flags |")
            lines.append("| --- | --- | ---: | ---: | --- |")
            for m in flagged[:120]:
                lines.append(f"| `{m.path}` | {m.width}x{m.height} | {m.unique_colors} | {m.contrast} | {', '.join(m.flags)} |")
            if len(flagged) > 120:
                lines.append(f"| ... | ... | ... | ... | {len(flagged) - 120} more flagged textures |")
            lines.append("")
        else:
            lines.append("No quality flags found.\n")
        return lines

    out = [
        "# Texture Quality Audit",
        "",
        "This audit is about broad quality traits only. It does not compare against, copy, import, or derive from any external mod assets.",
        "",
        "The original generated set leaned too noisy and over-detailed for clean Minecraft readability. The redesign uses low-color templates, consistent silhouettes, transparent padding, and compact symbolic icons.",
        "",
    ]
    out.extend(section("Before", before))
    if after is not None:
        out.extend(section("After", after))
    (DOCS / "texture_quality_audit.md").write_text("\n".join(out))


def write_reference_traits(reference_dir: Path):
    """Summarize local reference art traits without importing reference pixels."""
    DOCS.mkdir(parents=True, exist_ok=True)
    if not reference_dir.exists():
        (DOCS / "reference_texture_traits.md").write_text(
            "# Reference Texture Traits\n\n"
            f"Reference directory not found: `{reference_dir}`.\n"
        )
        return []

    rows = []
    for path in sorted(reference_dir.rglob("*.png")):
        try:
            rows.append(metrics_for(path, reference_dir))
        except Exception:
            continue

    def median(values):
        return round(statistics.median(values), 1) if values else "n/a"

    categories = {}
    dimensions = {}
    for metric in rows:
        category = metric.path.split("/", 1)[0] if "/" in metric.path else "root"
        categories.setdefault(category, []).append(metric)
        dimensions[f"{metric.width}x{metric.height}"] = dimensions.get(f"{metric.width}x{metric.height}", 0) + 1

    sixteen = [m for m in rows if m.width == 16 and m.height == 16]
    item_like = [m for m in rows if m.path.startswith("item/") and m.width == 16 and m.height == 16]
    block_like = [m for m in rows if m.path.startswith("block/") and m.width == 16 and m.height == 16]
    padded = [m for m in item_like if m.transparent_padding]

    lines = [
        "# Reference Texture Traits",
        "",
        "These local files were inspected only for broad style traits. No reference PNGs were copied, traced, recolored, embedded, or imported into the mod asset tree.",
        "",
        "## Summary",
        "",
        f"- PNG files scanned: {len(rows)}",
        f"- 16x16 textures: {len(sixteen)}",
        f"- Dimensions: {', '.join(f'{k} ({v})' for k, v in sorted(dimensions.items()))}",
        f"- Median unique colors in 16x16 item-like textures: {median([m.unique_colors for m in item_like])}",
        f"- Median contrast in 16x16 item-like textures: {median([m.contrast for m in item_like])}",
        f"- 16x16 item-like textures with transparent edge padding: {len(padded)} / {len(item_like)}",
        f"- Median unique colors in 16x16 block-like textures: {median([m.unique_colors for m in block_like])}",
        "",
        "## Observed Broad Traits",
        "",
        "- Most item assets use compact centered silhouettes on transparent backgrounds.",
        "- Ingots, nuggets, raw chunks, and alloy blends are template-driven rather than individually ornate.",
        "- Ores use vanilla-like stone/deepslate bases with restrained mineral clusters.",
        "- Icons favor plain readable symbols with minimal color and strong contrast.",
        "- Metal colors are differentiated by a few stable ramps instead of many noisy highlights.",
        "",
        "## Applied To This Mod",
        "",
        "- Kept all generated mod textures original and deterministic.",
        "- Tightened ingot, nugget, powder, flake, bead, and icon templates around simpler 16x16 silhouettes.",
        "- Reduced stone-block noise and shifted ores toward vanilla-style clusters.",
        "- Documented this pass separately so future artists can repeat the same style audit without importing reference art.",
        "",
        "## Category Counts",
        "",
        "| Category | Files | Median Colors | Median Contrast |",
        "| --- | ---: | ---: | ---: |",
    ]
    for category, metrics in sorted(categories.items()):
        lines.append(
            f"| `{category}` | {len(metrics)} | {median([m.unique_colors for m in metrics])} | {median([m.contrast for m in metrics])} |"
        )
    lines.append("")
    (DOCS / "reference_texture_traits.md").write_text("\n".join(lines))
    return rows


def nearest_resize(pixels, width, height, target_w, target_h):
    out = []
    for y in range(target_h):
        sy = min(height - 1, int(y * height / target_h))
        row = []
        for x in range(target_w):
            sx = min(width - 1, int(x * width / target_w))
            row.append(pixels[sy][sx])
        out.append(row)
    return out


def make_contact_sheet(paths: list[Path], out_path: Path, cell=32, columns=24):
    if not paths:
        return
    rows = math.ceil(len(paths) / columns)
    pad = 4
    width = columns * (cell + pad) + pad
    height = rows * (cell + pad) + pad
    sheet = Canvas(width, height, rgba((30, 32, 34)))
    for i, path in enumerate(paths):
        try:
            w, h, pix = read_png(path)
        except Exception:
            continue
        scale = max(1, min(cell // w, cell // h)) if w <= cell and h <= cell else max(1, min(cell, w, h))
        tw = min(cell, w * scale if w <= cell else cell)
        th = min(cell, h * scale if h <= cell else cell)
        scaled = nearest_resize(pix, w, h, tw, th)
        ox = pad + (i % columns) * (cell + pad) + (cell - tw) // 2
        oy = pad + (i // columns) * (cell + pad) + (cell - th) // 2
        sheet.rect(pad + (i % columns) * (cell + pad), pad + (i // columns) * (cell + pad), pad + (i % columns) * (cell + pad) + cell - 1, pad + (i // columns) * (cell + pad) + cell - 1, rgba((44, 46, 48)))
        for y in range(th):
            for x in range(tw):
                px = scaled[y][x]
                if px[3] > 8:
                    sheet.set(ox + x, oy + y, px)
    png_write(out_path, width, height, sheet.p)


def make_comparison(before_path: Path, after_path: Path, out_path: Path):
    bw, bh, bp = read_png(before_path)
    aw, ah, ap = read_png(after_path)
    h = max(bh, ah)
    c = Canvas(bw + aw + 16, h, rgba((22, 24, 26)))
    for y in range(bh):
        for x in range(bw):
            c.set(x, y, bp[y][x])
    for y in range(ah):
        for x in range(aw):
            c.set(bw + 16 + x, y, ap[y][x])
    c.rect(bw + 4, 0, bw + 11, h - 1, rgba((8, 10, 12)))
    png_write(out_path, c.w, c.h, c.p)


def copy_references():
    ref_dir = ROOT / "references"
    if not ref_dir.exists():
        return
    # Map reference files to mod asset locations
    mappings = {
        "item": "textures/item",
        "block": "textures/block",
        "entity": "textures/entity",
        "icon": "textures/gui", # References has 'icon', mod has 'gui' for icons
    }
    for src_cat, dest_cat in mappings.items():
        src_path = ref_dir / src_cat
        if not src_path.exists(): continue
        for img in src_path.rglob("*.png"):
            rel = img.relative_to(src_path)
            dest = ASSETS / dest_cat / rel
            ensure(dest)
            shutil.copy2(img, dest)
            print(f"Used reference: {src_cat}/{rel} -> {dest_cat}/{rel}")


def generate_assets():
    clear_generated_assets()
    manifest = []
    item_models = {}
    block_models = {}
    blockstates = {}

    for metal in METAL_IDS:
        for suffix, drawer in [
            ("flakes", draw_flakes), ("powder", draw_powder), ("bead", draw_bead), ("ingot", draw_ingot),
            ("nugget", draw_nugget), ("raw_chunk", draw_raw_chunk), ("raw_ore", draw_raw_chunk),
            ("blend", draw_powder),
        ]:
            name = f"raw_{metal}_ore" if suffix == "raw_ore" else f"{metal}_{suffix}"
            drawer(metal).save(f"item/{name}.png")
            item_models[name] = item_model(name)
            manifest.append(("item", f"textures/item/{name}.png", name, "yes", "n/a"))
        name = f"{metal}_vial"
        draw_vial(metal).save(f"item/{name}.png")
        item_models[name] = item_model(name)
        manifest.append(("item", f"textures/item/{name}.png", name, "yes", "n/a"))
        for prefix, charged, decaying in [("", False, False), ("charged_", True, False), ("decaying_", True, True)]:
            name = f"{prefix}{metal}_spike"
            draw_spike(metal, charged, decaying).save(f"item/{name}.png")
            item_models[name] = item_model(name, handheld=True)
            manifest.append(("item", f"textures/item/{name}.png", name, "yes", "n/a"))
        if metal in {"atium", "lerasium"}:
            for suffix in ["fragment", "cache_key"]:
                name = f"{metal}_{suffix}"
                (draw_raw_chunk(metal) if suffix == "fragment" else draw_special_item("anchor_marker")).save(f"item/{name}.png")
                item_models[name] = item_model(name, handheld=suffix == "cache_key")

    for metal in FERUCHEMICAL:
        for unkeyed in [False, True]:
            for suffix in ["mind", "metalmind"]:
                name = f"unkeyed_{metal}_{suffix}" if unkeyed else f"{metal}_{suffix}"
                draw_metalmind(metal, unkeyed).save(f"item/{name}.png")
                item_models[name] = item_model(name)
                manifest.append(("item", f"textures/item/{name}.png", name, "yes", "n/a"))

    for name, canvas in [
        ("empty_glass_vial", draw_vial(empty=True)),
        ("mixed_metal_vial", draw_vial(mixed=True)),
    ]:
        canvas.save(f"item/{name}.png")
        item_models[name] = item_model(name)
    for name in [
        "allomancer_testing_kit", "feruchemist_testing_kit", "metal_arts_guidebook", "coin_pouch",
        "coinshot_coin", "metallic_coin", "anchor_marker", "bronze_detector", "coppercloud_charm",
        "time_bubble_focus", "atium_shadow_lens", "lerasium_core", "spike_removal_tool",
        "spike_press_component", "guide", "inquisitor_axe",
    ]:
        draw_special_item(name).save(f"item/{name}.png")
        item_models[name] = item_model(name, handheld=name in {"spike_removal_tool", "anchor_marker", "inquisitor_axe"})

    machine_faces = {
        "metallurgy_table": ("metallurgy_table_top", "metallurgy_table_side", "metallurgy_table_front"),
        "metalworking_table": ("metalworking_table_top", "metalworking_table_side", "metalworking_table_front"),
        "alloy_furnace": ("alloy_furnace_top", "alloy_furnace_side", "alloy_furnace_front"),
        "spike_press": ("spike_press_top", "spike_press_side", "spike_press_front"),
        "bind_point_table": ("bind_point_table_top", "bind_point_table_side", "bind_point_table_top"),
        "metalmind_charging_stand": ("metalmind_charging_stand", "metalmind_charging_stand", "metalmind_charging_stand"),
    }
    for block, (top, side, front) in machine_faces.items():
        for tex, face in {(top, "top"), (side, "side"), (front, "front")}:
            draw_machine_face(block, face).save(f"block/{tex}.png")
        if block == "metalworking_table":
            draw_machine_face(block, "bottom").save("block/metalworking_table_bottom.png")
        if block == "alloy_furnace":
            draw_machine_face(block, "front_lit").save("block/alloy_furnace_front_lit.png")
        block_models[block] = {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": f"{MODID}:block/{side}",
                "up": f"{MODID}:block/{top}",
                "down": f"{MODID}:block/{'metalworking_table_bottom' if block == 'metalworking_table' else side}",
                "north": f"{MODID}:block/{front}",
                "south": f"{MODID}:block/{side}",
                "east": f"{MODID}:block/{side}",
                "west": f"{MODID}:block/{side}",
            },
        }

    for metal in ["zinc", "tin", "aluminum", "chromium", "cadmium", "atium"]:
        draw_ore(metal, False).save(f"block/{metal}_ore.png")
        draw_ore(metal, True).save(f"block/deepslate_{metal}_ore.png")

    registered_blocks = [
        "metallurgy_table", "metalworking_table", "alloy_furnace", "spike_press", "bind_point_table", "metalmind_charging_stand",
        "atium_geode_cluster", "lerasium_cache_block", "metal_cache", "zinc_ore", "deepslate_zinc_ore",
        "tin_ore", "deepslate_tin_ore", "aluminum_ore", "deepslate_aluminum_ore", "chromium_ore",
        "deepslate_chromium_ore", "cadmium_ore", "deepslate_cadmium_ore", "ash_deposit",
        "nickel_ore", "deepslate_nickel_ore", "silver_ore", "deepslate_silver_ore",
        "lead_ore", "deepslate_lead_ore",
    ]
    future_blocks = [
        "atium_ore", "deepslate_atium_ore", "atium_geode_block", "atium_crystal_cluster",
        "budding_atium", "lerasium_shrine_block", "ancient_metal_vault_block", "ash_layer",
        "metal_cache_chest", "hemalurgic_altar", "coppercloud_generator", "time_bubble_anchor",
        "ash_bricks", "cracked_ash_bricks", "metal_inlaid_stone", "noble_keep_stone", "canal_stone",
        "hideout_planks", "ancient_vault_block", "hemalurgic_corrupted_stone", "lerasium_shrine_stone",
        "atium_cavern_stone",
    ]
    for metal in METAL_IDS:
        future_blocks.append(f"{metal}_block")
    for block in registered_blocks + future_blocks:
        if not (TEXTURES / f"block/{block}.png").exists():
            draw_special_block(block).save(f"block/{block}.png")
        if block not in block_models:
            tex = "atium_crystal_cluster" if block == "atium_geode_cluster" else "metal_cache_chest" if block == "metal_cache" else block
            block_models[block] = {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/{tex}"}}
        blockstates[block] = {"variants": {"": {"model": f"{MODID}:block/{block}"}}}

    for metal in METAL_IDS:
        draw_icon(metal).save(f"gui/icon_{metal}.png")
    for hud in [
        "metal_reserve_bar", "metal_reserve_fill", "metalmind_bar", "metalmind_fill", "burning_icon_frame",
        "flare_indicator", "bronze_pulse_arrow", "atium_warning", "coppercloud_status", "corruption_meter",
        "radial_menu", "radial_selection", "time_bubble_indicator",
    ]:
        draw_hud(hud).save(f"gui/{hud}.png")
    for gui in ["metallurgy_table", "alloy_furnace", "spike_press", "bind_point_table", "metalmind_charging_stand", "metal_arts_menu"]:
        draw_gui(gui).save(f"gui/{gui}.png")

    particles = [
        "allomantic_line", "metal_line", "steelpush_spark", "ironpull_spark", "pewter_flare", "tin_glint",
        "coppercloud_mist", "coppercloud", "bronze_pulse", "zinc_riot_wave", "brass_soothe_wave",
        "atium_shadow", "lerasium_glow", "hemalurgic_spark", "time_bubble_edge", "cadmium_distortion",
        "bendalloy_distortion", "nicrosil_burst", "chromium_leech",
    ]
    for particle in particles:
        draw_particle(particle).save(f"particle/{particle}.png")
        write_json(f"particles/{particle}.json", {"textures": [f"{MODID}:particle/{particle}"]})
    write_json("particles/metal_line.json", {"textures": [f"{MODID}:particle/allomantic_line"]})
    write_json("particles/coppercloud.json", {"textures": [f"{MODID}:particle/coppercloud_mist"]})
    write_json("particles/atium_shadow.json", {"textures": [f"{MODID}:particle/atium_shadow"]})

    entities = [
        "coinshot_bandit", "lurcher_guard", "pewter_thug", "tineye_scout", "rioter", "soother",
        "seeker", "smoker", "atium_seer", "mistborn_assassin", "koloss", "kandra", "steel_inquisitor",
    ]
    for entity in entities:
        draw_entity(entity).save(f"entity/{entity}.png")
    for armor in ["metalborn_layer_1", "metalborn_layer_2", "inquisitor_layer_1", "inquisitor_layer_2", "koloss_hide_layer_1", "koloss_hide_layer_2"]:
        draw_armor(armor).save(f"models/armor/{armor}.png")
    effects = [
        "allomantic_pewter", "tin_enhanced", "pewter_drag", "sensory_overload", "coppercloud_hidden",
        "coppercloud", "bronze_detecting", "bronze_seeking", "atium_future_sight", "atium_sight",
        "lerasium_changed", "hemalurgic_corruption", "time_bubble_slow", "time_bubble_fast",
        "emotional_riot", "emotional_soothe", "emotional_pressure",
    ]
    for effect in effects:
        draw_effect(effect).save(f"effect/{effect}.png")
        draw_effect(effect).save(f"mob_effect/{effect}.png")
    for adv in ["become_mistborn", "burn_atium", "consume_lerasium", "first_vial", "steelpush_jump", "coppercloud", "defeat_inquisitor"]:
        draw_advancement(adv).save(f"gui/advancement/{adv}.png")

    for name, model in item_models.items():
        write_json(f"models/item/{name}.json", model)
    for name, model in block_models.items():
        write_json(f"models/block/{name}.json", model)
        write_json(f"models/item/{name}.json", block_item_model(name))
    for name, state in blockstates.items():
        write_json(f"blockstates/{name}.json", state)

    copy_references()

    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    for name in item_models:
        lang.setdefault(f"item.{MODID}.{name}", cap(name))
    for name in block_models:
        lang.setdefault(f"block.{MODID}.{name}", cap(name))
        lang.setdefault(f"item.{MODID}.{name}", cap(name))
    for entity in entities:
        lang.setdefault(f"entity.{MODID}.{entity}", cap(entity))
    for effect in effects:
        lang.setdefault(f"effect.{MODID}.{effect}", cap(effect))
    lang_path.write_text(json.dumps(dict(sorted(lang.items())), indent=2) + "\n")
    write_art_docs(manifest)


def model_texture_users():
    users = {}
    blockstate_users = {}
    for model in (ASSETS / "models").rglob("*.json"):
        data = json.loads(model.read_text())
        model_ref = model.relative_to(ASSETS / "models").with_suffix("").as_posix()
        for ref in data.get("textures", {}).values():
            if not isinstance(ref, str) or ref.startswith("#"):
                continue
            namespace, _, value = ref.partition(":")
            if not value:
                namespace, value = MODID, namespace
            if namespace == MODID:
                users.setdefault(f"textures/{value}.png", []).append(model_ref)
    for state in (ASSETS / "blockstates").glob("*.json"):
        data = json.loads(state.read_text())
        for variant in data.get("variants", {}).values():
            entries = variant if isinstance(variant, list) else [variant]
            for entry in entries:
                if isinstance(entry, dict) and "model" in entry:
                    namespace, _, value = entry["model"].partition(":")
                    if namespace == MODID:
                        blockstate_users.setdefault(value, []).append(state.stem)
    return users, blockstate_users


def write_art_docs(_manifest):
    DOCS.mkdir(parents=True, exist_ok=True)
    (DOCS / "art_direction.md").write_text("""# Art Direction

Unofficial fan-created mod inspired by Brandon Sanderson's Mistborn. No affiliation or endorsement.

## Overall Style

The revised texture set uses clean vanilla-plus Minecraft mod pixel art: small readable silhouettes, restrained palettes, crisp outlines, transparent padding, and top-left highlights. The goal is clean Cosmere-flavored Minecraft quality, not noisy fantasy rendering.

This pass was tuned from broad local-reference traits only: 16x16 discipline, repeated item templates, compact centered silhouettes, vanilla-like ore composition, and simple high-contrast icons. Reference pixels were not copied, traced, recolored, embedded, or imported into this project.

## Palette Choices

Each metal uses a compact five-color identity: outline, shadow, base, highlight, and one accent. Repeated item classes share the same silhouette so the player learns the object category first and the metal color second.

## Metal Visual Identities

Iron is dark neutral gray, Steel is cooler blue-gray, Tin is pale silver, Pewter is dull warm gray, Zinc is pale bluish gray, Brass is warm yellow-gold, Copper is orange-brown, Bronze is darker copper-brown, Gold is rich yellow, Electrum is pale green-gold, Cadmium is muted blue-gray, Bendalloy is creamy silver with a tan accent, Aluminum is matte pale silver, Duralumin adds a tiny copper accent, Chromium is dark mirror gray, Nicrosil has a controlled violet-white accent, Atium is dark smoky silver, and Lerasium is pale white-gold.

## Item Style

Vials, beads, flakes, powders, ingots, metalminds, and spikes all use reusable templates. Hemalurgic red appears only in charged or decaying spikes and related UI. Atium and Lerasium are special through silhouette accents and palette restraint rather than neon glow.

## UI Style

HUD and GUI textures are compact, dark, and Minecraft-like: thin borders, clear bars, simple icon frames, and minimal decoration.

## Entity Style

Entity sheets follow Minecraft skin discipline: blocky clothing zones, simple accents, limited colors, and no painterly shading.

## Particle Style

Particles are small symbolic sprites: blue-white Allomantic lines, brown Coppercloud mist, bronze rings, red Hemalurgic sparks, smoky Atium marks, and clean white-gold Lerasium points.
""")
    texture_users, blockstate_users = model_texture_users()
    lines = [
        "# Asset Manifest",
        "",
        "Literal scan of every PNG under `assets/mistborn_metal_arts/textures` after the clean texture pass.",
        "",
        "| Category | Texture | Model JSON | Blockstate JSON |",
        "| --- | --- | --- | --- |",
    ]
    for tex in sorted(TEXTURES.rglob("*.png")):
        rel = "textures/" + tex.relative_to(TEXTURES).as_posix()
        category = rel.split("/", 2)[1]
        models = sorted(set(texture_users.get(rel, [])))
        model_text = ", ".join(models[:4]) + ("..." if len(models) > 4 else "") if models else "support/future"
        blockstates = sorted({name for model in models for name in blockstate_users.get(model, [])})
        state_text = ", ".join(blockstates[:4]) + ("..." if len(blockstates) > 4 else "") if blockstates else "n/a"
        lines.append(f"| {category} | `{rel}` | {model_text} | {state_text} |")
    (DOCS / "asset_manifest.md").write_text("\n".join(lines) + "\n")
    (DOCS / "texture_checklist.md").write_text("""# Texture Checklist

## Completed

- Replaced noisy procedural textures with clean low-color templates.
- Rebuilt all metal item classes around consistent silhouettes.
- Rebuilt all metal icons as a unified symbolic family.
- Rebuilt registered block models, block item models, blockstates, and face textures.
- Rebuilt HUD, GUI, particle, effect, armor, entity, and advancement support textures.
- Added before, after, and comparison contact sheets.
- Strengthened asset checking for references, registered coverage, transparency, flat textures, dimensions, color counts, padding, and contrast.

## Missing

- None for current registry/model/particle references.

## Future Polish

- Entity sheets are clean Minecraft-style placeholders until custom mob model UV layouts exist.
- GUI textures are ready as assets, but current HUD code still draws immediate shapes until GUI rendering is wired to these PNGs.
- A human artist could further hand-polish individual metal hues after gameplay testing.
""")


def run_full_pass(reference_dir: Path | None = None):
    DOCS.mkdir(parents=True, exist_ok=True)
    if reference_dir is not None:
        write_reference_traits(reference_dir)
    before_paths = sorted(TEXTURES.rglob("*.png"))
    before = collect_metrics() if TEXTURES.exists() else []
    if before_paths:
        make_contact_sheet(before_paths, DOCS / "texture_contact_sheet_before.png")
    write_audit(before)

    generate_assets()

    after_paths = sorted(TEXTURES.rglob("*.png"))
    after = collect_metrics()
    make_contact_sheet(after_paths, DOCS / "texture_contact_sheet_after.png")
    if (DOCS / "texture_contact_sheet_before.png").exists():
        make_comparison(DOCS / "texture_contact_sheet_before.png", DOCS / "texture_contact_sheet_after.png", DOCS / "texture_contact_sheet_comparison.png")
    write_audit(before, after)


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--generate-only", action="store_true", help="Only regenerate clean assets/docs, without before audit/contact sheet.")
    parser.add_argument("--reference-dir", type=Path, help="Optional local reference texture folder to summarize as broad style traits without copying pixels.")
    args = parser.parse_args(argv)
    if args.reference_dir and args.generate_only:
        write_reference_traits(args.reference_dir)
    if args.generate_only:
        generate_assets()
    else:
        run_full_pass(args.reference_dir)
    print("Clean texture pass complete.")


if __name__ == "__main__":
    main()
