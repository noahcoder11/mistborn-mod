#!/usr/bin/env python3
"""Programmatically paint the 12 humanoid mob textures for Mistborn: Metal Arts.

Creates detailed, book-accurate, beautifully shaded vanilla-plus 64x64 skins.
Uses deterministic hash-based noise to simulate rich textile weaves and leather folds.
"""

from pathlib import Path
import sys

# Make sure we can import from scripts/
sys.path.append(str(Path(__file__).parent.resolve()))
from clean_texture_pass import png_write, TEXTURES

# ── Palette & Utility Functions ─────────────────────────────────────────────

EMPTY = (0, 0, 0, 0)

# Standard skin tones
SKIN_PALE = (219, 198, 185, 255)
SKIN_PALE_SHADE = (195, 172, 158, 255)
SKIN_TAN = (185, 150, 128, 255)
SKIN_TAN_SHADE = (155, 122, 102, 255)
SKIN_KOLOSS = (75, 98, 122, 255)
SKIN_KOLOSS_SHADE = (56, 76, 94, 255)

# Noble colors
NOBLE_NAVY = (21, 31, 51, 255)
NOBLE_NAVY_LGT = (35, 50, 78, 255)
NOBLE_GOLD = (210, 169, 56, 255)
NOBLE_GOLD_LGT = (245, 205, 95, 255)
NOBLE_SILVER = (200, 208, 212, 255)
NOBLE_SILVER_LGT = (235, 240, 245, 255)
NOBLE_CRIMSON = (130, 36, 36, 255)
NOBLE_CRIMSON_LGT = (175, 55, 55, 255)
NOBLE_CREAM = (230, 222, 210, 255)
NOBLE_CREAM_SHD = (200, 192, 178, 255)

# Common clothes/leather
CLOTH_BLUE = (74, 107, 130, 255)
CLOTH_BLUE_SHD = (50, 75, 95, 255)
CLOTH_GREEN = (53, 77, 59, 255)
CLOTH_GREEN_SHD = (35, 52, 40, 255)
CLOTH_PURPLE = (37, 35, 48, 255)
CLOTH_PURPLE_SHD = (25, 23, 33, 255)
CLOTH_SMOKE = (85, 88, 95, 255)
CLOTH_SMOKE_SHD = (60, 62, 68, 255)
CLOTH_SOOT = (27, 28, 30, 255)
CLOTH_SOOT_LGT = (45, 47, 50, 255)

LEATHER_DARK = (52, 38, 28, 255)
LEATHER_MID = (78, 58, 43, 255)
LEATHER_LGT = (110, 82, 60, 255)

METAL_STEEL = (115, 120, 125, 255)
METAL_STEEL_SHD = (75, 78, 82, 255)
METAL_STEEL_LGT = (175, 180, 185, 255)
METAL_COPPER = (176, 91, 56, 255)
METAL_COPPER_LGT = (220, 125, 85, 255)

# Kandra skeletal
KANDRA_BONE = (222, 215, 206, 255)
KANDRA_BONE_SHD = (190, 182, 172, 255)
KANDRA_MUSCLE = (161, 40, 40, 255)
KANDRA_MUSCLE_SHD = (115, 25, 25, 255)

def get_noise(x, y, seed=0):
    """Deterministic hash-based noise for consistent binary bytes."""
    h = (x * 127 + y * 311 + seed * 73) % 100
    return h

def get_shaded_color(color, x, y, noise_amt, seed=0):
    """Applies shading shifts based on deterministic coordinate hashes."""
    if color[3] == 0:
        return color
    n = get_noise(x, y, seed)
    shift = int((n / 50.0 - 1.0) * noise_amt)
    r = max(0, min(255, color[0] + shift))
    g = max(0, min(255, color[1] + shift))
    b = max(0, min(255, color[2] + shift))
    return (r, g, b, color[3])

def draw_cube(p, u, v, w, h, d, colors, noise_amt=5, seed=0):
    """Fills the 6 faces of a standard 3D humanoid part with textures."""
    if isinstance(colors, tuple) and len(colors) == 6:
        c_top, c_bot, c_r, c_f, c_l, c_b = colors
    else:
        c_top = c_bot = c_r = c_f = c_l = c_b = colors

    def fill_rect(rx, ry, rw, rh, col):
        for dy in range(rh):
            for dx in range(rw):
                x = rx + dx
                y = ry + dy
                if 0 <= x < 64 and 0 <= y < 64:
                    p[y][x] = get_shaded_color(col, x, y, noise_amt, seed)

    # Top
    fill_rect(u + d, v, w, d, c_top)
    # Bottom
    fill_rect(u + d + w, v, w, d, c_bot)
    # Right (outer)
    fill_rect(u, v + d, d, h, c_r)
    # Front
    fill_rect(u + d, v + d, w, h, c_f)
    # Left (inner)
    fill_rect(u + d + w, v + d, d, h, c_l)
    # Back
    fill_rect(u + 2 * d + w, v + d, w, h, c_b)

def px(p, x, y, color):
    """Draw a single absolute pixel safely."""
    if 0 <= x < 64 and 0 <= y < 64:
        p[y][x] = color

def fill_rect_abs(p, rx, ry, rw, rh, color, noise_amt=0, seed=0):
    """Draw a rectangular region with absolute coordinates."""
    for dy in range(rh):
        for dx in range(rw):
            x = rx + dx
            y = ry + dy
            if 0 <= x < 64 and 0 <= y < 64:
                p[y][x] = get_shaded_color(color, x, y, noise_amt, seed)

# ── Character Painters ───────────────────────────────────────────────────────

def paint_coinshot_bandit():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 101

    # Base Skin Biped cubes
    draw_cube(p, 0, 0, 8, 8, 8, SKIN_TAN, 4, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, CLOTH_BLUE, 6, seed) # Body
    draw_cube(p, 40, 16, 4, 12, 4, SKIN_TAN, 4, seed) # Right Arm
    draw_cube(p, 32, 48, 4, 12, 4, SKIN_TAN, 4, seed) # Left Arm
    draw_cube(p, 0, 16, 4, 12, 4, CLOTH_BLUE_SHD, 5, seed) # Right Leg
    draw_cube(p, 16, 48, 4, 12, 4, CLOTH_BLUE_SHD, 5, seed) # Left Leg

    # ── Head Features ──
    # Dark brown hair front peek
    fill_rect_abs(p, 9, 8, 6, 2, LEATHER_DARK)
    # Goggles: Brass/leather strap around head, blue glowing steel lenses
    fill_rect_abs(p, 0, 10, 32, 1, LEATHER_DARK) # strap
    px(p, 10, 10, NOBLE_GOLD) # left lens border
    px(p, 11, 10, CLOTH_BLUE_SHD)
    px(p, 13, 10, NOBLE_GOLD) # right lens border
    px(p, 14, 10, CLOTH_BLUE_SHD)

    # ── Torso Features ──
    # Dark brown leather vest overlay on front and back
    fill_rect_abs(p, 20, 20, 8, 10, LEATHER_MID, 6, seed) # Front vest
    fill_rect_abs(p, 32, 20, 8, 10, LEATHER_MID, 6, seed) # Back vest
    # Vest open slit at chest showing blue shirt
    fill_rect_abs(p, 23, 20, 2, 5, CLOTH_BLUE)
    # Iron belt with brass buckle
    fill_rect_abs(p, 20, 29, 8, 1, LEATHER_DARK)
    px(p, 23, 29, NOBLE_GOLD)
    px(p, 24, 29, NOBLE_GOLD)
    # Coin pouch details on side belt
    px(p, 18, 25, LEATHER_LGT)
    px(p, 29, 25, LEATHER_LGT)

    # ── Arms & Legs ──
    # Blue sleeves on arms (upper 6 pixels)
    fill_rect_abs(p, 40, 20, 16, 6, CLOTH_BLUE, 5, seed)
    fill_rect_abs(p, 32, 52, 16, 6, CLOTH_BLUE, 5, seed)
    # Leather wrist wraps/fingerless gloves (lower 3 pixels)
    fill_rect_abs(p, 40, 29, 16, 3, LEATHER_MID, 4, seed)
    fill_rect_abs(p, 32, 61, 16, 3, LEATHER_MID, 4, seed)
    # High leather bandit boots (lower 5 pixels of legs)
    fill_rect_abs(p, 0, 27, 16, 5, LEATHER_DARK, 5, seed)
    fill_rect_abs(p, 16, 59, 16, 5, LEATHER_DARK, 5, seed)

    # ── Hood (0, 32) 9x9x9 ──
    # Draws the dark brown hood overlay
    draw_cube(p, 0, 32, 9, 9, 9, LEATHER_MID, 5, seed)
    # Open front showing face
    fill_rect_abs(p, 9, 41, 7, 7, EMPTY)
    # Open bottom
    fill_rect_abs(p, 18, 32, 9, 9, EMPTY)

    return p

def paint_lurcher_guard():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 102

    # Base cubes
    draw_cube(p, 0, 0, 8, 8, 8, SKIN_PALE, 3, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, METAL_STEEL_SHD, 6, seed) # Body (Chainmail base)
    draw_cube(p, 40, 16, 4, 12, 4, METAL_STEEL_SHD, 5, seed) # Right Arm (Chainmail)
    draw_cube(p, 32, 48, 4, 12, 4, METAL_STEEL_SHD, 5, seed) # Left Arm (Chainmail)
    draw_cube(p, 0, 16, 4, 12, 4, METAL_STEEL_SHD, 5, seed) # Right Leg (Chainmail)
    draw_cube(p, 16, 48, 4, 12, 4, METAL_STEEL_SHD, 5, seed) # Left Leg (Chainmail)

    # ── Helmet on Head ──
    # Iron helmet top/sides
    fill_rect_abs(p, 8, 0, 8, 8, METAL_STEEL) # top
    fill_rect_abs(p, 0, 8, 8, 8, METAL_STEEL) # right
    fill_rect_abs(p, 16, 8, 8, 8, METAL_STEEL) # left
    fill_rect_abs(p, 24, 8, 8, 8, METAL_STEEL) # back
    # Helmet nose guard on front face
    fill_rect_abs(p, 8, 8, 8, 2, METAL_STEEL)
    px(p, 11, 10, METAL_STEEL)
    px(p, 12, 10, METAL_STEEL)
    # Guard eyes
    px(p, 10, 10, CLOTH_BLUE)
    px(p, 13, 10, CLOTH_BLUE)

    # ── Torso Features ──
    # Garrison blue-grey tabard / breastplate over body
    # Center blue tabard front and back
    fill_rect_abs(p, 22, 20, 4, 12, CLOTH_BLUE_SHD, 4, seed)
    fill_rect_abs(p, 34, 20, 4, 12, CLOTH_BLUE_SHD, 4, seed)
    # Shiny steel shoulder guards and chest plate outlines
    fill_rect_abs(p, 20, 20, 2, 4, METAL_STEEL, 4, seed)
    fill_rect_abs(p, 26, 20, 2, 4, METAL_STEEL, 4, seed)
    fill_rect_abs(p, 32, 20, 2, 4, METAL_STEEL, 4, seed)
    fill_rect_abs(p, 38, 20, 2, 4, METAL_STEEL, 4, seed)
    # Heavy dark iron belt
    fill_rect_abs(p, 20, 28, 8, 1, LEATHER_DARK)
    px(p, 23, 28, METAL_STEEL_LGT)
    px(p, 24, 28, METAL_STEEL_LGT)

    # ── Limbs ──
    # Heavy steel plate gauntlets on arms (lower 4 pixels)
    fill_rect_abs(p, 40, 28, 16, 4, METAL_STEEL, 4, seed)
    fill_rect_abs(p, 32, 60, 16, 4, METAL_STEEL, 4, seed)
    # Steel highlights on wrist
    fill_rect_abs(p, 40, 28, 4, 1, METAL_STEEL_LGT)
    fill_rect_abs(p, 32, 60, 4, 1, METAL_STEEL_LGT)
    # Iron heavy boots on legs (lower 4 pixels)
    fill_rect_abs(p, 0, 28, 16, 4, LEATHER_DARK, 4, seed)
    fill_rect_abs(p, 16, 60, 16, 4, LEATHER_DARK, 4, seed)

    return p

def paint_pewter_thug():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 103

    # Base cubes - bare skin focused!
    draw_cube(p, 0, 0, 8, 8, 8, SKIN_TAN, 4, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, SKIN_TAN, 4, seed) # Body (mostly bare)
    draw_cube(p, 40, 16, 4, 12, 4, SKIN_TAN, 4, seed) # Right Arm
    draw_cube(p, 32, 48, 4, 12, 4, SKIN_TAN, 4, seed) # Left Arm
    draw_cube(p, 0, 16, 4, 12, 4, CLOTH_SOOT, 5, seed) # Right Leg (rugged dark pants)
    draw_cube(p, 16, 48, 4, 12, 4, CLOTH_SOOT, 5, seed) # Left Leg

    # ── Head Features ──
    # Angry eyes (reddish eyebrows)
    px(p, 9, 9, LEATHER_DARK)
    px(p, 10, 9, LEATHER_DARK)
    px(p, 13, 9, LEATHER_DARK)
    px(p, 14, 9, LEATHER_DARK)
    # Scar on cheek
    px(p, 9, 11, NOBLE_CRIMSON)
    px(p, 10, 12, NOBLE_CRIMSON_LGT)
    # Angry mouth
    px(p, 11, 13, SKIN_TAN_SHADE)
    px(p, 12, 13, SKIN_TAN_SHADE)
    # Messy dark hair
    fill_rect_abs(p, 8, 0, 8, 3, CLOTH_SOOT)
    fill_rect_abs(p, 0, 8, 8, 5, CLOTH_SOOT)
    fill_rect_abs(p, 16, 8, 8, 5, CLOTH_SOOT)
    fill_rect_abs(p, 24, 8, 8, 6, CLOTH_SOOT)

    # ── Torso Features ──
    # Tattered sleeveless dark grey vest
    # Front vest has large open chest area exposing skin
    fill_rect_abs(p, 20, 20, 8, 10, CLOTH_SOOT_LGT, 5, seed)
    fill_rect_abs(p, 32, 20, 8, 10, CLOTH_SOOT_LGT, 5, seed)
    # Open bare chest cut-out
    fill_rect_abs(p, 22, 20, 4, 6, SKIN_TAN, 4, seed)
    px(p, 23, 26, SKIN_TAN_SHADE)
    px(p, 24, 26, SKIN_TAN_SHADE)
    # Rope belt
    fill_rect_abs(p, 20, 29, 8, 1, LEATHER_MID)

    # ── Limbs ──
    # Rugged leather arm bands/wraps (middle of arms)
    fill_rect_abs(p, 40, 24, 16, 3, LEATHER_MID, 4, seed)
    fill_rect_abs(p, 32, 56, 16, 3, LEATHER_MID, 4, seed)
    # Heavy thug iron-toe leather boots (lower 4 pixels of legs)
    fill_rect_abs(p, 0, 28, 16, 4, LEATHER_DARK, 4, seed)
    fill_rect_abs(p, 16, 60, 16, 4, LEATHER_DARK, 4, seed)
    # Steel studs on boots (toe highlights)
    px(p, 5, 31, METAL_STEEL_LGT)
    px(p, 21, 63, METAL_STEEL_LGT)

    return p

def paint_tineye_scout():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 104

    # Base cubes
    draw_cube(p, 0, 0, 8, 8, 8, SKIN_PALE, 3, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, CLOTH_GREEN, 6, seed) # Body
    draw_cube(p, 40, 16, 4, 12, 4, CLOTH_GREEN, 5, seed) # Right Arm
    draw_cube(p, 32, 48, 4, 12, 4, CLOTH_GREEN, 5, seed) # Left Arm
    draw_cube(p, 0, 16, 4, 12, 4, CLOTH_GREEN_SHD, 5, seed) # Right Leg
    draw_cube(p, 16, 48, 4, 12, 4, CLOTH_GREEN_SHD, 5, seed) # Left Leg

    # ── Head Features ──
    # Large tin-grey auditory ear-muffs / headband on sides
    fill_rect_abs(p, 0, 9, 2, 4, METAL_STEEL) # Right side earmuff
    fill_rect_abs(p, 16, 9, 2, 4, METAL_STEEL) # Left side earmuff
    fill_rect_abs(p, 8, 7, 8, 1, METAL_STEEL_LGT) # Connecting band over forehead
    # Green camo face mask covering chin and nose
    fill_rect_abs(p, 8, 12, 8, 4, CLOTH_GREEN)
    # Focused silver scout eyes
    px(p, 10, 10, METAL_STEEL_LGT)
    px(p, 13, 10, METAL_STEEL_LGT)

    # ── Torso Features ──
    # Leather diagonal utility strap (chest harness)
    px(p, 20, 20, LEATHER_MID)
    px(p, 21, 21, LEATHER_MID)
    px(p, 22, 22, LEATHER_MID)
    px(p, 23, 23, LEATHER_MID)
    px(p, 24, 24, LEATHER_MID)
    px(p, 25, 25, LEATHER_MID)
    px(p, 26, 26, LEATHER_MID)
    px(p, 27, 27, LEATHER_MID)
    # Back strap
    px(p, 32, 27, LEATHER_MID)
    px(p, 33, 26, LEATHER_MID)
    px(p, 34, 25, LEATHER_MID)
    px(p, 35, 24, LEATHER_MID)
    px(p, 36, 23, LEATHER_MID)
    px(p, 37, 22, LEATHER_MID)
    px(p, 38, 21, LEATHER_MID)
    px(p, 39, 20, LEATHER_MID)
    # Leather belt
    fill_rect_abs(p, 20, 28, 8, 1, LEATHER_DARK)

    # ── Limbs ──
    # Leather wrist bracers (lower 3 pixels of arms)
    fill_rect_abs(p, 40, 29, 16, 3, LEATHER_MID, 4, seed)
    fill_rect_abs(p, 32, 61, 16, 3, LEATHER_MID, 4, seed)
    # High scout boots (lower 6 pixels of legs)
    fill_rect_abs(p, 0, 26, 16, 6, LEATHER_MID, 5, seed)
    fill_rect_abs(p, 16, 58, 16, 6, LEATHER_MID, 5, seed)

    # ── Hood overlay ──
    draw_cube(p, 0, 32, 9, 9, 9, CLOTH_GREEN, 5, seed)
    fill_rect_abs(p, 9, 41, 7, 7, EMPTY)
    fill_rect_abs(p, 18, 32, 9, 9, EMPTY)

    return p

def paint_rioter():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 105

    # Base cubes
    draw_cube(p, 0, 0, 8, 8, 8, SKIN_PALE, 3, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, CLOTH_SOOT, 5, seed) # Body
    draw_cube(p, 40, 16, 4, 12, 4, NOBLE_CRIMSON, 5, seed) # Right Arm
    draw_cube(p, 32, 48, 4, 12, 4, NOBLE_CRIMSON, 5, seed) # Left Arm
    draw_cube(p, 0, 16, 4, 12, 4, CLOTH_SOOT, 5, seed) # Right Leg
    draw_cube(p, 16, 48, 4, 12, 4, CLOTH_SOOT, 5, seed) # Left Leg

    # ── Head Features ──
    # Classy dark hair
    fill_rect_abs(p, 8, 0, 8, 3, CLOTH_SOOT)
    fill_rect_abs(p, 0, 8, 8, 4, CLOTH_SOOT)
    fill_rect_abs(p, 16, 8, 8, 4, CLOTH_SOOT)
    fill_rect_abs(p, 24, 8, 8, 6, CLOTH_SOOT)
    # Trimmed dark beard on face front
    fill_rect_abs(p, 8, 14, 8, 2, CLOTH_SOOT)
    px(p, 8, 13, CLOTH_SOOT)
    px(p, 15, 13, CLOTH_SOOT)
    # Monocle over right eye (gold frame, red lens)
    px(p, 13, 10, NOBLE_GOLD)
    px(p, 14, 10, NOBLE_CRIMSON_LGT)

    # ── Torso Features ──
    # Elegant split coat design: Crimson longcoat over soot vest
    # Coat borders on the sides of the chest
    fill_rect_abs(p, 20, 20, 2, 10, NOBLE_CRIMSON, 5, seed)
    fill_rect_abs(p, 26, 20, 2, 10, NOBLE_CRIMSON, 5, seed)
    # Back is fully crimson longcoat
    fill_rect_abs(p, 32, 20, 8, 12, NOBLE_CRIMSON, 5, seed)
    # Gold waistcoat buttons down the center vest
    px(p, 23, 22, NOBLE_GOLD)
    px(p, 24, 24, NOBLE_GOLD)
    px(p, 23, 26, NOBLE_GOLD)
    # White neck ruff/cravat under neck
    fill_rect_abs(p, 23, 20, 2, 1, NOBLE_CREAM)
    # Fancy gold sash belt
    fill_rect_abs(p, 20, 29, 8, 1, NOBLE_GOLD, 4, seed)

    # ── Limbs ──
    # Golden trims on sleeves (cuffs)
    fill_rect_abs(p, 40, 27, 16, 1, NOBLE_GOLD)
    fill_rect_abs(p, 32, 59, 16, 1, NOBLE_GOLD)
    # White noble gloves (lower 2 pixels)
    fill_rect_abs(p, 40, 28, 16, 4, NOBLE_CREAM, 2, seed)
    fill_rect_abs(p, 32, 60, 16, 4, NOBLE_CREAM, 2, seed)
    # Reined fancy boots (lower 3 pixels of legs)
    fill_rect_abs(p, 0, 29, 16, 3, CLOTH_SOOT_LGT, 3, seed)
    fill_rect_abs(p, 16, 61, 16, 3, CLOTH_SOOT_LGT, 3, seed)

    return p

def paint_soother():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 106

    # Base cubes
    draw_cube(p, 0, 0, 8, 8, 8, SKIN_PALE, 3, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, NOBLE_CREAM, 4, seed) # Body (Cream Robes)
    draw_cube(p, 40, 16, 4, 12, 4, NOBLE_CREAM, 4, seed) # Right Arm
    draw_cube(p, 32, 48, 4, 12, 4, NOBLE_CREAM, 4, seed) # Left Arm
    draw_cube(p, 0, 16, 4, 12, 4, NOBLE_CREAM_SHD, 4, seed) # Right Leg
    draw_cube(p, 16, 48, 4, 12, 4, NOBLE_CREAM_SHD, 4, seed) # Left Leg

    # ── Head Features ──
    # Clean combed light blonde hair
    fill_rect_abs(p, 8, 0, 8, 3, NOBLE_GOLD_LGT)
    fill_rect_abs(p, 0, 8, 8, 4, NOBLE_GOLD_LGT)
    fill_rect_abs(p, 16, 8, 8, 4, NOBLE_GOLD_LGT)
    fill_rect_abs(p, 24, 8, 8, 6, NOBLE_GOLD_LGT)
    # Gold headband
    fill_rect_abs(p, 8, 7, 8, 1, NOBLE_GOLD)
    # Peaceful calm blue eyes
    px(p, 10, 10, CLOTH_BLUE)
    px(p, 13, 10, CLOTH_BLUE)

    # ── Torso Features ──
    # Serene sky-blue scarf wrapped around neck and front
    fill_rect_abs(p, 20, 20, 8, 2, CLOTH_BLUE, 4, seed)
    fill_rect_abs(p, 32, 20, 8, 2, CLOTH_BLUE, 4, seed)
    # Scarf tail trailing down the front of the body
    fill_rect_abs(p, 21, 22, 2, 7, CLOTH_BLUE, 4, seed)
    # Elegant gold trims down the robes
    fill_rect_abs(p, 24, 22, 1, 8, NOBLE_GOLD)
    fill_rect_abs(p, 23, 29, 2, 1, NOBLE_GOLD)

    # ── Limbs ──
    # Gold bangles on wrists (lower 2 pixels of arms)
    fill_rect_abs(p, 40, 28, 16, 2, NOBLE_GOLD, 3, seed)
    fill_rect_abs(p, 32, 60, 16, 2, NOBLE_GOLD, 3, seed)
    # Robe bottom shade extensions (lower 3 pixels of legs are sandals/skin)
    fill_rect_abs(p, 0, 29, 16, 2, SKIN_PALE, 3, seed)
    fill_rect_abs(p, 16, 61, 16, 2, SKIN_PALE, 3, seed)
    # Sandal straps
    fill_rect_abs(p, 0, 31, 16, 1, LEATHER_MID)
    fill_rect_abs(p, 16, 63, 16, 1, LEATHER_MID)

    return p

def paint_seeker():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 107

    # Base cubes
    draw_cube(p, 0, 0, 8, 8, 8, SKIN_TAN, 3, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, CLOTH_PURPLE, 5, seed) # Body (Dark purple robes)
    draw_cube(p, 40, 16, 4, 12, 4, CLOTH_PURPLE, 5, seed) # Right Arm
    draw_cube(p, 32, 48, 4, 12, 4, CLOTH_PURPLE, 5, seed) # Left Arm
    draw_cube(p, 0, 16, 4, 12, 4, CLOTH_PURPLE_SHD, 5, seed) # Right Leg
    draw_cube(p, 16, 48, 4, 12, 4, CLOTH_PURPLE_SHD, 5, seed) # Left Leg

    # ── Head Features ──
    # Copper sensory cowl/mask over eyes
    fill_rect_abs(p, 8, 9, 8, 3, METAL_COPPER, 4, seed)
    # Copper glowing lenses
    px(p, 10, 10, METAL_COPPER_LGT)
    px(p, 13, 10, METAL_COPPER_LGT)
    # Sleek dark hair
    fill_rect_abs(p, 8, 0, 8, 3, CLOTH_SOOT)
    fill_rect_abs(p, 0, 8, 8, 4, CLOTH_SOOT)
    fill_rect_abs(p, 16, 8, 8, 4, CLOTH_SOOT)
    fill_rect_abs(p, 24, 8, 8, 6, CLOTH_SOOT)

    # ── Torso Features ──
    # Copper-threaded vertical embroidery on front and back
    fill_rect_abs(p, 23, 20, 2, 9, METAL_COPPER, 4, seed)
    fill_rect_abs(p, 35, 20, 2, 9, METAL_COPPER, 4, seed)
    # Highlight dots along threads
    px(p, 23, 22, METAL_COPPER_LGT)
    px(p, 24, 25, METAL_COPPER_LGT)
    px(p, 23, 28, METAL_COPPER_LGT)
    # Bronze metallic belt
    fill_rect_abs(p, 20, 29, 8, 1, LEATHER_DARK)
    fill_rect_abs(p, 23, 29, 2, 1, METAL_COPPER_LGT)

    # ── Limbs ──
    # Copper threads on cuffs of robes
    fill_rect_abs(p, 40, 27, 16, 1, METAL_COPPER)
    fill_rect_abs(p, 32, 59, 16, 1, METAL_COPPER)
    # Dark hands showing (lower 4 pixels)
    fill_rect_abs(p, 40, 28, 16, 4, SKIN_TAN, 3, seed)
    fill_rect_abs(p, 32, 60, 16, 4, SKIN_TAN, 3, seed)
    # Bronze/copper boots (lower 3 pixels)
    fill_rect_abs(p, 0, 29, 16, 3, METAL_COPPER, 4, seed)
    fill_rect_abs(p, 16, 61, 16, 3, METAL_COPPER, 4, seed)

    return p

def paint_smoker():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 108

    # Base cubes
    draw_cube(p, 0, 0, 8, 8, 8, SKIN_PALE, 3, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, CLOTH_SOOT, 5, seed) # Body
    draw_cube(p, 40, 16, 4, 12, 4, CLOTH_SMOKE_SHD, 5, seed) # Right Arm
    draw_cube(p, 32, 48, 4, 12, 4, CLOTH_SMOKE_SHD, 5, seed) # Left Arm
    draw_cube(p, 0, 16, 4, 12, 4, CLOTH_SOOT, 5, seed) # Right Leg
    draw_cube(p, 16, 48, 4, 12, 4, CLOTH_SOOT, 5, seed) # Left Leg

    # ── Head Features ──
    # Linen beige mouth wrap on chin/face
    fill_rect_abs(p, 8, 12, 8, 4, NOBLE_CREAM_SHD, 3, seed)
    # Messy smoke-grey hair
    fill_rect_abs(p, 8, 0, 8, 4, CLOTH_SMOKE)
    fill_rect_abs(p, 0, 8, 8, 5, CLOTH_SMOKE)
    fill_rect_abs(p, 16, 8, 8, 5, CLOTH_SMOKE)
    fill_rect_abs(p, 24, 8, 8, 6, CLOTH_SMOKE)
    # Hollow soot-shaded eyes
    px(p, 10, 10, CLOTH_SOOT_LGT)
    px(p, 13, 10, CLOTH_SOOT_LGT)

    # ── Torso Features ──
    # Tattered smoke-grey shroud draped over shoulders and body
    fill_rect_abs(p, 20, 20, 8, 9, CLOTH_SMOKE, 5, seed)
    fill_rect_abs(p, 32, 20, 8, 9, CLOTH_SMOKE, 5, seed)
    # Ripped cloth cuts showing dark soot clothing underneath
    px(p, 21, 23, CLOTH_SOOT)
    px(p, 25, 25, CLOTH_SOOT)
    px(p, 23, 27, CLOTH_SOOT)
    # Dark waist wrap belt
    fill_rect_abs(p, 20, 29, 8, 1, CLOTH_SOOT)

    # ── Limbs ──
    # Ash-smeared bare hands (lower 3 pixels of arms)
    fill_rect_abs(p, 40, 29, 16, 3, SKIN_PALE_SHADE, 4, seed)
    fill_rect_abs(p, 32, 61, 16, 3, SKIN_PALE_SHADE, 4, seed)
    # Soot smudges on hands
    px(p, 41, 30, CLOTH_SOOT)
    px(p, 34, 62, CLOTH_SOOT)
    # Smokey grey boots (lower 4 pixels)
    fill_rect_abs(p, 0, 28, 16, 4, CLOTH_SMOKE_SHD, 4, seed)
    fill_rect_abs(p, 16, 60, 16, 4, CLOTH_SMOKE_SHD, 4, seed)

    return p

def paint_atium_seer():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 109

    # Base cubes
    draw_cube(p, 0, 0, 8, 8, 8, SKIN_PALE, 2, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, NOBLE_NAVY, 5, seed) # Body
    draw_cube(p, 40, 16, 4, 12, 4, NOBLE_NAVY, 5, seed) # Right Arm
    draw_cube(p, 32, 48, 4, 12, 4, NOBLE_NAVY, 5, seed) # Left Arm
    draw_cube(p, 0, 16, 4, 12, 4, CLOTH_SOOT, 4, seed) # Right Leg
    draw_cube(p, 16, 48, 4, 12, 4, CLOTH_SOOT, 4, seed) # Left Leg

    # ── Head Features ──
    # Combed dark noble hair
    fill_rect_abs(p, 8, 0, 8, 3, CLOTH_SOOT)
    fill_rect_abs(p, 0, 8, 8, 4, CLOTH_SOOT)
    fill_rect_abs(p, 16, 8, 8, 4, CLOTH_SOOT)
    fill_rect_abs(p, 24, 8, 8, 6, CLOTH_SOOT)
    # sapphire-blue monocle on left eye (silver border)
    px(p, 10, 10, NOBLE_SILVER)
    px(p, 9, 10, CLOTH_BLUE)

    # ── Torso Features ──
    # Pristine white dress shirt chest insert
    fill_rect_abs(p, 23, 20, 2, 6, NOBLE_CREAM, 1, seed)
    # Sapphire tie
    px(p, 23, 22, CLOTH_BLUE)
    px(p, 24, 23, CLOTH_BLUE_SHD)
    # Tailcoat silver lining buttons
    fill_rect_abs(p, 21, 20, 1, 8, NOBLE_SILVER)
    fill_rect_abs(p, 26, 20, 1, 8, NOBLE_SILVER)
    fill_rect_abs(p, 33, 20, 1, 10, NOBLE_SILVER)
    fill_rect_abs(p, 38, 20, 1, 10, NOBLE_SILVER)
    # Elegant belt
    fill_rect_abs(p, 20, 28, 8, 1, CLOTH_SOOT)

    return p

def paint_mistborn_assassin():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 110

    # Specific colors from reference sheet:
    vin_skin = (223, 195, 178, 255)       # #dfc3b2 -> light beige skin
    vin_skin_shd = (197, 163, 144, 255)   # #c5a390 -> shaded skin
    vin_hair = (28, 29, 34, 255)          # #1c1d22 -> dark charcoal/black
    vin_eyes = (78, 56, 45, 255)          # #4e382d -> dark brown
    
    # Cloak / Robe / Hood fabric colors:
    cloak_charcoal = (33, 34, 38, 255)    # #212226 -> main dark charcoal
    cloak_highlight = (53, 55, 61, 255)   # #35373d -> high charcoal
    cloak_shade = (21, 22, 25, 255)       # #151619 -> dark shade
    
    # Under-vest tunic colors (quilted/banded vest):
    vest_grey = (47, 48, 53, 255)         # #2f3035 -> medium quilted grey
    vest_highlight = (62, 63, 69, 255)    # #3e3f45 -> light quilted grey
    vest_dark = (32, 33, 36, 255)         # #202124 -> dark quilted grey
    
    # Pauldrons & Greaves:
    pauldron_base = (43, 45, 49, 255)     # #2b2d31
    pauldron_shade = (28, 29, 32, 255)    # #1c1d20
    
    # Belt / pouches:
    belt_leather = (88, 62, 43, 255)      # #583e2b
    belt_buckle = (159, 163, 169, 255)    # #9fa3a9
    gem_blue = (28, 143, 166, 255)        # #1c8fa6 -> turquoise pouch base
    gem_blue_lgt = (94, 195, 214, 255)    # #5ec3d6 -> pouch highlight
    
    # Boot colors:
    boot_base = (37, 39, 42, 255)         # #25272a
    boot_highlight = (53, 55, 60, 255)    # #35373c
    boot_strap = (79, 82, 88, 255)        # #4f5258

    # Base cubes - Skin & basic clothing layer
    draw_cube(p, 0, 0, 8, 8, 8, vin_skin, 2, seed) # Head base
    draw_cube(p, 16, 16, 8, 12, 4, vest_grey, 4, seed) # Body under-vest
    draw_cube(p, 40, 16, 4, 12, 4, cloak_shade, 3, seed) # Right Arm base
    draw_cube(p, 32, 48, 4, 12, 4, cloak_shade, 3, seed) # Left Arm base
    draw_cube(p, 0, 16, 4, 12, 4, cloak_shade, 4, seed) # Right Leg base
    draw_cube(p, 16, 48, 4, 12, 4, cloak_shade, 4, seed) # Left Leg base

    # ── 1. Head Face, Hair & Ponytail Details ──
    # Combed dark hair on front/top/sides/back of head
    # Hair locks on front face (Vin's locks)
    fill_rect_abs(p, 8, 8, 8, 2, vin_hair, 2, seed)
    fill_rect_abs(p, 8, 10, 1, 2, vin_hair) # left temple lock
    fill_rect_abs(p, 15, 10, 1, 2, vin_hair) # right temple lock
    
    # Vin's eyes: Dark brown eyes with a single white pixel at outer corners
    px(p, 10, 10, vin_eyes)
    px(p, 9, 10, (255, 255, 255, 255)) # eye white
    px(p, 13, 10, vin_eyes)
    px(p, 14, 10, (255, 255, 255, 255)) # eye white
    
    # Mouth / blush details
    px(p, 11, 13, vin_skin_shd)
    px(p, 12, 13, vin_skin_shd)

    # Ponytail Bun on back of the head (tied with brown leather tie)
    # Head back is at x = 24 to 31, y = 8 to 15
    fill_rect_abs(p, 25, 9, 6, 6, vin_hair, 2, seed)
    fill_rect_abs(p, 27, 10, 2, 1, belt_leather) # brown ponytail tie
    fill_rect_abs(p, 27, 11, 2, 4, vin_hair, 1, seed) # ponytail hanging down

    # ── 2. Torso (Body) Details ──
    # Exposed collar neck skin (V-neck)
    px(p, 23, 20, vin_skin)
    px(p, 24, 20, vin_skin)
    px(p, 23, 21, vin_skin)
    px(p, 24, 21, vin_skin)
    # Silver lacing cross on neck collar
    px(p, 23, 20, belt_buckle)
    px(p, 24, 21, belt_buckle)

    # Coat collar scarf around V-neck sides
    fill_rect_abs(p, 20, 20, 3, 2, cloak_charcoal, 3, seed)
    fill_rect_abs(p, 25, 20, 3, 2, cloak_charcoal, 3, seed)

    # Quilted under-vest front with dark coat side folds
    # Center columns are quilted vest, sides are dark coat flaps
    for y in range(22, 28):
        # Left coat flap
        fill_rect_abs(p, 20, y, 2, 1, cloak_charcoal, 3, seed)
        # Right coat flap
        fill_rect_abs(p, 26, y, 2, 1, cloak_charcoal, 3, seed)
        # Center vest
        col = vest_highlight if y % 3 == 0 else (vest_grey if y % 3 == 1 else vest_dark)
        fill_rect_abs(p, 22, y, 4, 1, col, 2, seed)

    # Vertical sheathed dagger hanging from belt on her left front side (x = 25, y = 25 to 27)
    px(p, 25, 25, belt_leather) # dagger hilt wrap
    px(p, 25, 26, belt_buckle)  # steel crossguard
    fill_rect_abs(p, 25, 27, 1, 1, CLOTH_SOOT) # sheath blade

    # Leather Belt with Silver Buckle
    fill_rect_abs(p, 20, 28, 8, 1, belt_leather)
    px(p, 23, 28, belt_buckle)
    px(p, 24, 28, belt_buckle)

    # Turquoise Gemstone/Pouch on player's left hip (outer side)
    px(p, 28, 28, gem_blue)
    px(p, 29, 28, gem_blue_lgt)
    px(p, 28, 29, gem_blue_lgt)
    px(p, 29, 29, gem_blue)

    # Torso Back Medallion (circle plate design)
    fill_rect_abs(p, 34, 22, 4, 4, cloak_charcoal, 2, seed)
    px(p, 35, 23, pauldron_shade)
    px(p, 36, 23, pauldron_shade)
    px(p, 34, 24, pauldron_shade)
    px(p, 37, 24, pauldron_shade)
    px(p, 35, 25, pauldron_shade)
    px(p, 36, 25, pauldron_shade)
    # Back straps
    px(p, 32, 23, belt_leather)
    px(p, 33, 23, belt_leather)
    px(p, 38, 23, belt_leather)
    px(p, 39, 23, belt_leather)
    fill_rect_abs(p, 32, 28, 8, 1, belt_leather)

    # ── 3. Arms Details (Pauldrons, Sleeves & Turquoise Bangle) ──
    # Pauldrons (Shoulders) - upper 4 pixels of both arms
    fill_rect_abs(p, 40, 20, 16, 4, pauldron_base, 3, seed)
    for x in [40, 44, 48, 52]:
        px(p, x, 20, pauldron_shade)
        px(p, x + 3, 20, pauldron_shade)
    fill_rect_abs(p, 32, 52, 16, 4, pauldron_base, 3, seed)
    for x in [32, 36, 40, 44]:
        px(p, x, 52, pauldron_shade)
        px(p, x + 3, 52, pauldron_shade)

    # Wrapped Sleeves - horizontal stripes of dark brown & charcoal
    for dy in range(6):
        stripe_color = vin_hair if dy % 2 == 0 else cloak_charcoal
        fill_rect_abs(p, 40, 24 + dy, 16, 1, stripe_color, 2, seed)
        fill_rect_abs(p, 32, 56 + dy, 16, 1, stripe_color, 2, seed)

    # Turquoise Bangle wrapped around her Left Wrist (y = 60 of Left Arm coordinates)
    # Left Arm starts at 32, 48 (16x16 region), so wrist is at y = 60 of outer face
    fill_rect_abs(p, 32, 60, 16, 1, gem_blue)
    px(p, 33, 60, gem_blue_lgt)
    px(p, 37, 60, gem_blue_lgt)

    # Exposed skin hands - lower 2 pixels of arms
    fill_rect_abs(p, 40, 30, 16, 2, vin_skin, 2, seed)
    fill_rect_abs(p, 32, 62, 16, 2, vin_skin, 2, seed)

    # ── 4. Legs Details (Greaves & Knee Guards) ──
    # Boots base
    fill_rect_abs(p, 0, 25, 16, 7, boot_base, 4, seed)
    fill_rect_abs(p, 16, 57, 16, 7, boot_base, 4, seed)
    
    # Knee guards (greaves) at y = 24 on base legs (y = 23 to 24 of Right Leg)
    # Right leg knee guard (front x = 4 to 7, y = 23 to 24)
    fill_rect_abs(p, 4, 23, 4, 2, pauldron_base)
    px(p, 5, 23, pauldron_shade)
    px(p, 6, 23, pauldron_shade)
    # Left leg knee guard (front x = 20 to 23, y = 55 to 56)
    fill_rect_abs(p, 20, 55, 4, 2, pauldron_base)
    px(p, 21, 55, pauldron_shade)
    px(p, 22, 55, pauldron_shade)

    # Boot highlights
    for y in [26, 30]:
        fill_rect_abs(p, 0, y, 16, 1, boot_highlight, 2, seed)
    for y in [58, 62]:
        fill_rect_abs(p, 16, y, 16, 1, boot_highlight, 2, seed)

    # Boot straps with silver buckles
    for y in [27, 29]:
        fill_rect_abs(p, 0, y, 16, 1, boot_strap)
        px(p, 5, y, belt_buckle)
    for y in [59, 61]:
        fill_rect_abs(p, 16, y, 16, 1, boot_strap)
        px(p, 21, y, belt_buckle)

    # ── 5. Hood Overlay ──
    draw_cube(p, 0, 32, 9, 9, 9, cloak_charcoal, 4, seed)
    fill_rect_abs(p, 9, 41, 7, 7, EMPTY)
    fill_rect_abs(p, 18, 32, 9, 9, EMPTY)
    # Leather clasp at neck front
    px(p, 11, 47, belt_leather)
    px(p, 12, 47, belt_leather)
    px(p, 11, 48, belt_buckle)

    # ── 6. Tassels (32, 0) region for Mistcloak ──
    for x in range(32, 64):
        h_noise = get_noise(x, 12, seed)
        tassel_len = 8 + (h_noise % 9)  # strand lengths
        gap_noise = get_noise(x, 57, seed)
        is_gap = (gap_noise % 5 == 0)   # 20% transparent gaps
        
        for y in range(0, 16):
            if is_gap or y >= tassel_len:
                p[y][x] = EMPTY
            else:
                shade_val = get_noise(x, y, seed + 10)
                if shade_val % 4 == 0:
                    tassel_color = cloak_highlight
                elif shade_val % 4 == 1:
                    tassel_color = cloak_charcoal
                else:
                    tassel_color = cloak_shade
                p[y][x] = get_shaded_color(tassel_color, x, y, 3, seed)

    return p

def paint_koloss():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 111

    # Base cubes - giant sagging blue-grey skin folds
    draw_cube(p, 0, 0, 8, 8, 8, SKIN_KOLOSS, 8, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, SKIN_KOLOSS, 8, seed) # Body
    draw_cube(p, 40, 16, 4, 12, 4, SKIN_KOLOSS, 8, seed) # Right Arm
    draw_cube(p, 32, 48, 4, 12, 4, SKIN_KOLOSS, 8, seed) # Left Arm
    draw_cube(p, 0, 16, 4, 12, 4, SKIN_KOLOSS_SHADE, 8, seed) # Right Leg
    draw_cube(p, 16, 48, 4, 12, 4, SKIN_KOLOSS_SHADE, 8, seed) # Left Leg

    # ── Head Features ──
    # Saggy loose folds under eyes
    fill_rect_abs(p, 8, 11, 8, 2, SKIN_KOLOSS_SHADE, 6, seed)
    # Glow red angry eyes
    px(p, 10, 10, NOBLE_CRIMSON_LGT)
    px(p, 13, 10, NOBLE_CRIMSON_LGT)
    # Stitches on face
    px(p, 9, 13, CLOTH_SOOT)
    px(p, 12, 14, CLOTH_SOOT)

    # ── Torso Features ──
    # Saggy skin overlapping sheets and dark stitches
    fill_rect_abs(p, 20, 23, 8, 1, SKIN_KOLOSS_SHADE) # Skin overlap seam
    fill_rect_abs(p, 32, 23, 8, 1, SKIN_KOLOSS_SHADE)
    # Stitch marks on torso
    px(p, 22, 21, CLOTH_SOOT)
    px(p, 23, 21, CLOTH_SOOT)
    px(p, 25, 25, CLOTH_SOOT)
    px(p, 26, 25, CLOTH_SOOT)
    px(p, 34, 22, CLOTH_SOOT)
    px(p, 37, 26, CLOTH_SOOT)
    # Rough leather loincloth at bottom (lower 4 pixels)
    fill_rect_abs(p, 20, 28, 8, 4, LEATHER_DARK, 6, seed)
    fill_rect_abs(p, 32, 28, 8, 4, LEATHER_DARK, 6, seed)

    # ── Limbs ──
    # Muscle stitches on arms
    px(p, 41, 20, CLOTH_SOOT)
    px(p, 42, 23, CLOTH_SOOT)
    px(p, 34, 54, CLOTH_SOOT)
    px(p, 35, 57, CLOTH_SOOT)
    # Leather wraps on feet ankles (lower 3 pixels of legs)
    fill_rect_abs(p, 0, 29, 16, 3, LEATHER_MID, 5, seed)
    fill_rect_abs(p, 16, 61, 16, 3, LEATHER_MID, 5, seed)

    return p

def paint_kandra():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 112

    # Base cubes - simple villager / traveler disguise
    draw_cube(p, 0, 0, 8, 8, 8, SKIN_PALE, 3, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, NOBLE_CREAM_SHD, 5, seed) # Body (Beige Tunic)
    draw_cube(p, 40, 16, 4, 12, 4, NOBLE_CREAM_SHD, 5, seed) # Right Arm
    draw_cube(p, 32, 48, 4, 12, 4, NOBLE_CREAM_SHD, 5, seed) # Left Arm
    draw_cube(p, 0, 16, 4, 12, 4, LEATHER_DARK, 5, seed) # Right Leg (Brown Pants)
    draw_cube(p, 16, 48, 4, 12, 4, LEATHER_DARK, 5, seed) # Left Leg

    # ── Head Features ──
    # Soft brown hair
    fill_rect_abs(p, 8, 0, 8, 3, LEATHER_MID)
    fill_rect_abs(p, 0, 8, 8, 4, LEATHER_MID)
    fill_rect_abs(p, 16, 8, 8, 4, LEATHER_MID)
    fill_rect_abs(p, 24, 8, 8, 5, LEATHER_MID)
    # Friendly green eyes
    px(p, 10, 10, CLOTH_GREEN)
    px(p, 13, 10, CLOTH_GREEN)

    # ── Torso Features ──
    # Tunic V-neck showing skin
    fill_rect_abs(p, 23, 20, 2, 3, SKIN_PALE)
    # Rope belt
    fill_rect_abs(p, 20, 28, 8, 1, LEATHER_LGT)

    # ── Limbs ──
    # sleeves border exposing hands (lower 3 pixels of arms are skin)
    fill_rect_abs(p, 40, 29, 16, 3, SKIN_PALE, 3, seed)
    fill_rect_abs(p, 32, 61, 16, 3, SKIN_PALE, 3, seed)
    # Leather boots (lower 4 pixels of legs)
    fill_rect_abs(p, 0, 28, 16, 4, LEATHER_MID, 4, seed)
    fill_rect_abs(p, 16, 60, 16, 4, LEATHER_MID, 4, seed)

    return p

def paint_kandra_true():
    p = [[EMPTY] * 64 for _ in range(64)]
    seed = 113

    # Base cubes - skeletal exposed bones & muscle
    draw_cube(p, 0, 0, 8, 8, 8, KANDRA_MUSCLE, 6, seed) # Head
    draw_cube(p, 16, 16, 8, 12, 4, KANDRA_MUSCLE_SHD, 6, seed) # Body
    draw_cube(p, 40, 16, 4, 12, 4, KANDRA_MUSCLE_SHD, 6, seed) # Right Arm
    draw_cube(p, 32, 48, 4, 12, 4, KANDRA_MUSCLE_SHD, 6, seed) # Left Arm
    draw_cube(p, 0, 16, 4, 12, 4, KANDRA_MUSCLE_SHD, 6, seed) # Right Leg
    draw_cube(p, 16, 48, 4, 12, 4, KANDRA_MUSCLE_SHD, 6, seed) # Left Leg

    # ── Head (Skeletal skull overlays) ──
    # Bone white skull plate on front face
    fill_rect_abs(p, 9, 8, 6, 5, KANDRA_BONE, 4, seed)
    # Glowing white hollow eyes
    px(p, 10, 10, NOBLE_CREAM)
    px(p, 13, 10, NOBLE_CREAM)
    # Nose nasal cavity hole
    px(p, 11, 11, CLOTH_SOOT)
    px(p, 12, 11, CLOTH_SOOT)
    # Skull teeth detail on mouth line
    fill_rect_abs(p, 10, 13, 4, 1, KANDRA_BONE_SHD)
    px(p, 10, 13, NOBLE_CREAM)
    px(p, 12, 13, NOBLE_CREAM)

    # ── Torso (Exposed ribcage) ──
    # Bone white ribcage overlay lines
    fill_rect_abs(p, 23, 20, 2, 10, KANDRA_BONE, 3, seed) # Sternum
    for y in [22, 24, 26, 28]:
        fill_rect_abs(p, 20, y, 8, 1, KANDRA_BONE, 3, seed) # Rib lines
    # Hemalurgic metal mind spikes in collarbones (steel studs)
    px(p, 21, 20, METAL_STEEL_LGT)
    px(p, 26, 20, METAL_STEEL_LGT)

    # ── Limbs (Bones wrapping muscle) ──
    # Vertical bone white shafts down arms & legs representing exposed radius/ulna/femur
    fill_rect_abs(p, 41, 20, 2, 10, KANDRA_BONE, 3, seed)
    fill_rect_abs(p, 45, 20, 2, 10, KANDRA_BONE, 3, seed)
    fill_rect_abs(p, 33, 52, 2, 10, KANDRA_BONE, 3, seed)
    fill_rect_abs(p, 37, 52, 2, 10, KANDRA_BONE, 3, seed)

    fill_rect_abs(p, 5, 20, 2, 10, KANDRA_BONE, 3, seed)
    fill_rect_abs(p, 9, 20, 2, 10, KANDRA_BONE, 3, seed)
    fill_rect_abs(p, 21, 52, 2, 10, KANDRA_BONE, 3, seed)
    fill_rect_abs(p, 25, 52, 2, 10, KANDRA_BONE, 3, seed)

    return p

# ── Main Generator ──────────────────────────────────────────────────────────

def main():
    dest_dir = TEXTURES / "entity"
    dest_dir.mkdir(parents=True, exist_ok=True)

    mobs = {
        "coinshot_bandit": paint_coinshot_bandit(),
        "lurcher_guard": paint_lurcher_guard(),
        "pewter_thug": paint_pewter_thug(),
        "tineye_scout": paint_tineye_scout(),
        "rioter": paint_rioter(),
        "soother": paint_soother(),
        "seeker": paint_seeker(),
        "smoker": paint_smoker(),
        "atium_seer": paint_atium_seer(),
        "mistborn_assassin": paint_mistborn_assassin(),
        "koloss": paint_koloss(),
        "kandra": paint_kandra(),
        "kandra_true": paint_kandra_true()
    }

    for name, pixels in mobs.items():
        dest_path = dest_dir / f"{name}.png"
        png_write(dest_path, 64, 64, pixels)
        print(f"Generated: {dest_path.name} ({len(pixels)}x{len(pixels[0])})")

if __name__ == "__main__":
    main()
