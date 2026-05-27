"""Generate a color-coded UV guide for the 64x64 steel inquisitor skin layout."""

from pathlib import Path
from clean_texture_pass import png_write, TEXTURES


def main():
    # 64x64 standard skin layout
    pix = [[(0, 0, 0, 0) for _ in range(64)] for _ in range(64)]

    def fill(x, y, w, h, color):
        for i in range(h):
            for j in range(w):
                if 0 <= y+i < 64 and 0 <= x+j < 64:
                    pix[y+i][x+j] = color

    # HEAD - Yellow/Orange
    fill(0, 0, 32, 16, (255, 255, 0, 255))     # Head region
    fill(8, 8, 8, 8, (255, 165, 0, 255))        # Face (Front)

    # BODY - Red/Pink
    fill(16, 16, 24, 16, (255, 0, 0, 255))      # Body region
    fill(20, 20, 8, 12, (255, 100, 100, 255))    # Chest (Front)

    # ARMS - Blue/Cyan
    fill(40, 16, 16, 16, (0, 0, 255, 255))       # Right Arm
    fill(32, 48, 16, 16, (0, 255, 255, 255))      # Left Arm

    # LEGS - Green/Lime
    fill(0, 16, 16, 16, (0, 255, 0, 255))        # Right Leg
    fill(16, 48, 16, 16, (100, 255, 100, 255))    # Left Leg

    # SPIKES (Custom Area 1) - Grey/Silver
    fill(40, 0, 8, 8, (180, 180, 180, 255))

    # CAPE (Custom Area 2) - Purple/Magenta
    fill(48, 48, 16, 16, (128, 0, 128, 255))

    dest = TEXTURES / "entity/steel_inquisitor.png"
    png_write(dest, 64, 64, pix)
    print(f"Saved UV color guide to {dest}")


if __name__ == "__main__":
    main()
