#!/usr/bin/env python3
"""Hand-paint the 64x64 steel inquisitor entity texture.

Based on the concept art reference showing:
- Pale skin head with dark circular eye spikes driven through
- Bare upper torso with blood trails and metal spike studs
- Dark robes/pants on lower body
- Dark tattered cape
- Skin-toned arms with blood stains

Uses the standard Minecraft 64x64 skin UV layout.
"""

from pathlib import Path
from clean_texture_pass import png_write, TEXTURES

# ── Palette ──────────────────────────────────────────────────────────────
# Skin tones
SKIN_LIGHT = (195, 162, 148, 255)
SKIN_MID   = (175, 142, 128, 255)
SKIN_DARK  = (155, 122, 108, 255)
SKIN_SHADE = (138, 108, 95, 255)

# Blood / wounds
BLOOD_DARK  = (95, 28, 22, 255)
BLOOD_MID   = (120, 38, 30, 255)
BLOOD_LIGHT = (140, 55, 42, 255)
WOUND       = (110, 60, 52, 255)

# Metal spikes
METAL_DARK    = (48, 45, 42, 255)
METAL_MID     = (75, 72, 68, 255)
METAL_LIGHT   = (100, 96, 90, 255)
METAL_BRIGHT  = (125, 120, 112, 255)

# Dark robe / cape
ROBE_DARK   = (28, 25, 24, 255)
ROBE_MID    = (42, 38, 36, 255)
ROBE_LIGHT  = (55, 50, 46, 255)
ROBE_SHADE  = (35, 32, 30, 255)

# Belt
BELT_DARK  = (52, 38, 28, 255)
BELT_LIGHT = (72, 55, 40, 255)

# Boots
BOOT_DARK  = (45, 35, 28, 255)
BOOT_MID   = (62, 48, 38, 255)

EMPTY = (0, 0, 0, 0)

def create_texture():
    # 64x64 canvas, transparent by default
    p = [[EMPTY] * 64 for _ in range(64)]

    def fill(x0, y0, w, h, color):
        for y in range(y0, min(y0 + h, 64)):
            for x in range(x0, min(x0 + w, 64)):
                p[y][x] = color

    def px(x, y, color):
        if 0 <= x < 64 and 0 <= y < 64:
            p[y][x] = color

    # ════════════════════════════════════════════════════════════════════
    # HEAD (UV origin 0,0 — 32x16 region)
    # ════════════════════════════════════════════════════════════════════

    # Head Top (8,0) 8x8 — bald scalp
    fill(8, 0, 8, 8, SKIN_MID)
    # Slight variation
    for x in range(9, 15):
        px(x, 1, SKIN_LIGHT)
    for x in range(10, 14):
        px(x, 2, SKIN_LIGHT)
    px(10, 6, SKIN_DARK)
    px(13, 5, SKIN_DARK)

    # Head Bottom (16,0) 8x8 — chin/jaw underside
    fill(16, 0, 8, 8, SKIN_DARK)
    fill(17, 1, 6, 6, SKIN_SHADE)
    px(20, 3, BLOOD_DARK)  # blood drip from mouth

    # Head Right side (0,8) 8x8
    fill(0, 8, 8, 8, SKIN_MID)
    fill(1, 9, 6, 6, SKIN_MID)
    # Spike entry on right side of head
    px(3, 11, METAL_MID)
    px(4, 11, METAL_DARK)
    px(3, 12, METAL_DARK)
    # Blood around spike
    px(2, 11, BLOOD_MID)
    px(5, 11, BLOOD_MID)
    px(3, 10, BLOOD_DARK)
    px(4, 12, BLOOD_DARK)
    # Shade bottom
    for x in range(0, 8):
        px(x, 15, SKIN_SHADE)

    # Head Front — FACE (8,8) 8x8
    fill(8, 8, 8, 8, SKIN_MID)
    # Lighter forehead
    fill(9, 8, 6, 2, SKIN_LIGHT)
    # Left eye spike (dark circle with metal ring)
    px(9, 10, METAL_MID)
    px(10, 10, METAL_DARK)
    px(9, 11, METAL_DARK)
    px(10, 11, METAL_DARK)
    # Right eye spike
    px(13, 10, METAL_DARK)
    px(14, 10, METAL_MID)
    px(13, 11, METAL_DARK)
    px(14, 11, METAL_DARK)
    # Blood from eyes
    px(9, 12, BLOOD_MID)
    px(10, 12, BLOOD_DARK)
    px(13, 12, BLOOD_DARK)
    px(14, 12, BLOOD_MID)
    px(10, 13, BLOOD_DARK)
    px(13, 13, BLOOD_DARK)
    # Nose bridge
    px(11, 11, SKIN_DARK)
    px(12, 11, SKIN_DARK)
    # Mouth
    px(11, 14, SKIN_SHADE)
    px(12, 14, SKIN_SHADE)
    # Jaw line
    fill(8, 15, 8, 1, SKIN_SHADE)
    # Crown / hairline detail  
    px(9, 8, SKIN_DARK)
    px(14, 8, SKIN_DARK)

    # Head Left side (16,8) 8x8
    fill(16, 8, 8, 8, SKIN_MID)
    # Spike entry on left side
    px(19, 11, METAL_DARK)
    px(20, 11, METAL_MID)
    px(19, 12, METAL_DARK)
    # Blood
    px(18, 11, BLOOD_MID)
    px(21, 11, BLOOD_MID)
    px(19, 10, BLOOD_DARK)
    px(20, 12, BLOOD_DARK)
    for x in range(16, 24):
        px(x, 15, SKIN_SHADE)

    # Head Back (24,8) 8x8
    fill(24, 8, 8, 8, SKIN_MID)
    fill(25, 9, 6, 6, SKIN_MID)
    # Spike exit points on back of head
    px(26, 11, METAL_DARK)
    px(27, 11, METAL_MID)
    px(28, 11, METAL_DARK)
    px(29, 11, METAL_MID)
    # Blood around exits
    px(26, 10, BLOOD_DARK)
    px(29, 10, BLOOD_DARK)
    px(26, 12, BLOOD_MID)
    px(29, 12, BLOOD_MID)
    for x in range(24, 32):
        px(x, 15, SKIN_SHADE)

    # ════════════════════════════════════════════════════════════════════
    # BODY (UV origin 16,16 — 24x16 region)
    # ════════════════════════════════════════════════════════════════════

    # Body Top (20,16) 8x4
    fill(20, 16, 8, 4, SKIN_MID)
    px(22, 17, BLOOD_DARK)   # spike entry point top
    px(25, 17, BLOOD_DARK)

    # Body Bottom (28,16) 8x4
    fill(28, 16, 8, 4, ROBE_MID)
    fill(29, 17, 6, 2, ROBE_DARK)

    # Body Right side (16,20) 4x12
    fill(16, 20, 4, 5, SKIN_MID)       # upper body - skin
    fill(16, 25, 4, 1, BELT_DARK)      # belt line
    fill(16, 26, 4, 6, ROBE_MID)       # lower body - robe
    px(17, 21, BLOOD_MID)
    px(18, 22, METAL_DARK)              # side spike stud
    px(17, 23, BLOOD_DARK)
    fill(16, 30, 4, 2, ROBE_DARK)

    # Body Front (20,20) 8x12
    fill(20, 20, 8, 5, SKIN_MID)       # bare chest
    fill(20, 25, 8, 1, BELT_DARK)      # belt
    fill(20, 26, 8, 6, ROBE_MID)       # dark robe lower
    # Chest details — blood and spike studs
    # Center chest spike wounds
    px(23, 21, METAL_DARK)
    px(24, 21, METAL_MID)
    px(23, 22, BLOOD_DARK)
    px(24, 22, BLOOD_DARK)
    # Blood trails down from chest
    px(23, 23, BLOOD_MID)
    px(24, 23, BLOOD_MID)
    px(23, 24, BLOOD_DARK)
    px(24, 24, BLOOD_DARK)
    # Side spike studs
    px(21, 22, METAL_DARK)
    px(26, 22, METAL_DARK)
    px(21, 23, BLOOD_DARK)
    px(26, 23, BLOOD_DARK)
    # Skin highlights
    px(22, 20, SKIN_LIGHT)
    px(25, 20, SKIN_LIGHT)
    # Belt details
    px(22, 25, BELT_LIGHT)
    px(25, 25, BELT_LIGHT)
    # Dark robe lower body
    fill(20, 28, 8, 4, ROBE_DARK)
    px(22, 27, ROBE_LIGHT)
    px(25, 27, ROBE_LIGHT)

    # Body Left side (28,20) 4x12
    fill(28, 20, 4, 5, SKIN_MID)
    fill(28, 25, 4, 1, BELT_DARK)
    fill(28, 26, 4, 6, ROBE_MID)
    px(29, 21, BLOOD_MID)
    px(30, 22, METAL_DARK)
    px(29, 23, BLOOD_DARK)
    fill(28, 30, 4, 2, ROBE_DARK)

    # Body Back (32,20) 8x12
    fill(32, 20, 8, 5, SKIN_MID)       # upper back - skin
    fill(32, 25, 8, 1, BELT_DARK)
    fill(32, 26, 8, 6, ROBE_MID)
    # Back spike entry points
    px(35, 21, METAL_DARK)
    px(36, 21, METAL_MID)
    px(35, 22, BLOOD_DARK)
    px(36, 22, BLOOD_DARK)
    px(33, 23, METAL_DARK)
    px(38, 23, METAL_DARK)
    px(33, 24, BLOOD_DARK)
    px(38, 24, BLOOD_DARK)
    # Skin variation
    px(34, 20, SKIN_LIGHT)
    px(37, 20, SKIN_LIGHT)
    fill(32, 28, 8, 4, ROBE_DARK)

    # ════════════════════════════════════════════════════════════════════
    # RIGHT LEG (UV origin 0,16 — 16x16 region)
    # ════════════════════════════════════════════════════════════════════

    # Right Leg Top (4,16) 4x4
    fill(4, 16, 4, 4, ROBE_MID)
    # Right Leg Bottom (8,16) 4x4
    fill(8, 16, 4, 4, ROBE_DARK)

    # Right Leg Right side (0,20) 4x12
    fill(0, 20, 4, 12, ROBE_MID)
    fill(0, 28, 4, 4, ROBE_DARK)
    px(1, 22, ROBE_LIGHT)
    px(2, 26, ROBE_SHADE)
    # Boot
    fill(0, 30, 4, 2, BOOT_DARK)

    # Right Leg Front (4,20) 4x12
    fill(4, 20, 4, 12, ROBE_MID)
    fill(4, 28, 4, 4, ROBE_DARK)
    px(5, 21, ROBE_LIGHT)
    px(6, 24, ROBE_SHADE)
    # Belt buckle area
    px(5, 20, BELT_LIGHT)
    # Boot
    fill(4, 30, 4, 2, BOOT_DARK)
    px(5, 30, BOOT_MID)

    # Right Leg Left side (8,20) 4x12
    fill(8, 20, 4, 12, ROBE_MID)
    fill(8, 28, 4, 4, ROBE_DARK)
    fill(8, 30, 4, 2, BOOT_DARK)

    # Right Leg Back (12,20) 4x12
    fill(12, 20, 4, 12, ROBE_MID)
    fill(12, 28, 4, 4, ROBE_DARK)
    fill(12, 30, 4, 2, BOOT_DARK)
    px(13, 22, ROBE_LIGHT)

    # ════════════════════════════════════════════════════════════════════
    # RIGHT ARM (UV origin 40,16 — 16x16 region)
    # ════════════════════════════════════════════════════════════════════

    # Right Arm Top (44,16) 4x4
    fill(44, 16, 4, 4, SKIN_MID)
    # Right Arm Bottom (48,16) 4x4
    fill(48, 16, 4, 4, SKIN_SHADE)

    # Right Arm Outer (40,20) 4x12
    fill(40, 20, 4, 6, SKIN_MID)       # upper arm - skin
    fill(40, 26, 4, 6, ROBE_MID)       # lower arm - wrapped cloth
    px(41, 21, BLOOD_MID)
    px(42, 22, METAL_DARK)              # spike stud
    px(41, 23, BLOOD_DARK)
    px(42, 24, SKIN_DARK)
    fill(40, 30, 4, 2, ROBE_DARK)

    # Right Arm Front (44,20) 4x12
    fill(44, 20, 4, 6, SKIN_MID)
    fill(44, 26, 4, 6, ROBE_MID)
    px(45, 22, BLOOD_MID)
    px(46, 23, BLOOD_DARK)
    px(45, 24, SKIN_DARK)
    fill(44, 30, 4, 2, ROBE_DARK)

    # Right Arm Inner (48,20) 4x12
    fill(48, 20, 4, 6, SKIN_MID)
    fill(48, 26, 4, 6, ROBE_MID)
    fill(48, 30, 4, 2, ROBE_DARK)

    # Right Arm Back (52,20) 4x12
    fill(52, 20, 4, 6, SKIN_MID)
    fill(52, 26, 4, 6, ROBE_MID)
    px(53, 21, BLOOD_DARK)
    px(54, 23, METAL_DARK)
    fill(52, 30, 4, 2, ROBE_DARK)

    # ════════════════════════════════════════════════════════════════════
    # LEFT LEG (UV origin 16,48 — 16x16 region)
    # ════════════════════════════════════════════════════════════════════

    # Left Leg Top (20,48) 4x4
    fill(20, 48, 4, 4, ROBE_MID)
    # Left Leg Bottom (24,48) 4x4
    fill(24, 48, 4, 4, ROBE_DARK)

    # Left Leg Right side (16,52) 4x12
    fill(16, 52, 4, 12, ROBE_MID)
    fill(16, 60, 4, 4, ROBE_DARK)
    fill(16, 62, 4, 2, BOOT_DARK)
    px(17, 54, ROBE_LIGHT)

    # Left Leg Front (20,52) 4x12
    fill(20, 52, 4, 12, ROBE_MID)
    fill(20, 60, 4, 4, ROBE_DARK)
    fill(20, 62, 4, 2, BOOT_DARK)
    px(21, 52, BELT_LIGHT)
    px(22, 55, ROBE_LIGHT)
    px(21, 62, BOOT_MID)

    # Left Leg Left side (24,52) 4x12
    fill(24, 52, 4, 12, ROBE_MID)
    fill(24, 60, 4, 4, ROBE_DARK)
    fill(24, 62, 4, 2, BOOT_DARK)

    # Left Leg Back (28,52) 4x12
    fill(28, 52, 4, 12, ROBE_MID)
    fill(28, 60, 4, 4, ROBE_DARK)
    fill(28, 62, 4, 2, BOOT_DARK)

    # ════════════════════════════════════════════════════════════════════
    # LEFT ARM (UV origin 32,48 — 16x16 region)
    # ════════════════════════════════════════════════════════════════════

    # Left Arm Top (36,48) 4x4
    fill(36, 48, 4, 4, SKIN_MID)
    # Left Arm Bottom (40,48) 4x4
    fill(40, 48, 4, 4, SKIN_SHADE)

    # Left Arm Outer (32,52) 4x12
    fill(32, 52, 4, 6, SKIN_MID)
    fill(32, 58, 4, 6, ROBE_MID)
    px(33, 53, BLOOD_MID)
    px(34, 54, METAL_DARK)
    px(33, 55, BLOOD_DARK)
    fill(32, 62, 4, 2, ROBE_DARK)

    # Left Arm Front (36,52) 4x12
    fill(36, 52, 4, 6, SKIN_MID)
    fill(36, 58, 4, 6, ROBE_MID)
    px(37, 54, BLOOD_MID)
    px(38, 55, BLOOD_DARK)
    fill(36, 62, 4, 2, ROBE_DARK)

    # Left Arm Inner (40,52) 4x12
    fill(40, 52, 4, 6, SKIN_MID)
    fill(40, 58, 4, 6, ROBE_MID)
    fill(40, 62, 4, 2, ROBE_DARK)

    # Left Arm Back (44,52) 4x12
    fill(44, 52, 4, 6, SKIN_MID)
    fill(44, 58, 4, 6, ROBE_MID)
    px(45, 53, BLOOD_DARK)
    px(46, 55, METAL_DARK)
    fill(44, 62, 4, 2, ROBE_DARK)

    # ════════════════════════════════════════════════════════════════════
    # SPIKE TEXTURE (40,0) 8x8 — used by model spike cubes
    # ════════════════════════════════════════════════════════════════════
    fill(40, 0, 8, 8, METAL_MID)
    # Metal texture variation
    fill(41, 1, 6, 6, METAL_MID)
    px(42, 2, METAL_BRIGHT)
    px(45, 2, METAL_BRIGHT)
    px(43, 4, METAL_LIGHT)
    px(44, 3, METAL_DARK)
    px(46, 5, METAL_DARK)
    px(41, 6, METAL_DARK)
    px(44, 6, METAL_LIGHT)
    # Outline
    for x in range(40, 48):
        px(x, 0, METAL_DARK)
        px(x, 7, METAL_DARK)
    for y in range(0, 8):
        px(40, y, METAL_DARK)
        px(47, y, METAL_DARK)

    # ════════════════════════════════════════════════════════════════════
    # CAPE TEXTURE (0,34) — used by model cape cube (10×16×1, UV 22×17)
    # ════════════════════════════════════════════════════════════════════
    # Cape front face occupies (1,35) to (10,50) — 10x16
    # Cape back face occupies (12,35) to (21,50) — 10x16
    # Top face at (1,34) 10x1, sides at (0,35) 1x16 and (11,35) 1x16
    fill(0, 34, 22, 17, ROBE_DARK)
    # Front face — tattered dark fabric with ragged edges
    for y in range(35, 51):
        for x in range(1, 11):
            if (x + y) % 3 == 0:
                px(x, y, ROBE_MID)
            elif (x + y) % 5 == 0:
                px(x, y, ROBE_SHADE)
    # Ragged bottom edge
    px(2, 50, EMPTY)
    px(5, 50, EMPTY)
    px(8, 50, EMPTY)
    px(3, 49, ROBE_SHADE)
    px(7, 49, ROBE_SHADE)
    # Back face — similar pattern
    for y in range(35, 51):
        for x in range(12, 22):
            if (x + y) % 3 == 1:
                px(x, y, ROBE_MID)
            elif (x + y) % 5 == 2:
                px(x, y, ROBE_SHADE)
    # Blood stains on cape
    px(4, 40, BLOOD_DARK)
    px(5, 41, BLOOD_DARK)
    px(7, 43, BLOOD_MID)

    # ════════════════════════════════════════════════════════════════════
    # HAT OVERLAY (32,0) 8x8 — second head layer, unused, make empty
    # ════════════════════════════════════════════════════════════════════
    # Leave empty (transparent) — already EMPTY by default

    return p


def main():
    pixels = create_texture()

    ref_path = Path("../references/entity/steel_inquisitor.png")
    if not ref_path.parent.exists():
        ref_path = Path("references/entity/steel_inquisitor.png")

    dest_path = TEXTURES / "entity/steel_inquisitor.png"

    png_write(ref_path, 64, 64, pixels)
    print(f"Saved reference: {ref_path}")

    png_write(dest_path, 64, 64, pixels)
    print(f"Saved active texture: {dest_path}")


if __name__ == "__main__":
    main()
