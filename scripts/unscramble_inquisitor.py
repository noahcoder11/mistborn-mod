"""Remap the steel inquisitor reference texture to standard 64x64 skin layout."""

from pathlib import Path
from clean_texture_pass import read_png, png_write, TEXTURES


def main():
    src_path = Path("references/entity/steel_inquisitor.png")
    if not src_path.exists():
        print("Source texture missing!")
        return

    w, h, pix = read_png(src_path)
    # Target 64x64 standard skin
    out = [[(0, 0, 0, 0) for _ in range(64)] for _ in range(64)]

    # 1. Remap Head (Source is at V=8..23, Target is at V=0..15)
    for y in range(16):
        for x in range(64):
            if 8 + y < h:
                out[y][x] = pix[8 + y][x]

    # 2. Remap Body/Arms/Legs (Source is at V=24..39, Target is at V=16..31)
    for y in range(16, 32):
        for x in range(64):
            if 8 + y < h:
                out[y][x] = pix[8 + y][x]

    # 3. Remap Lower Body/Arms/Legs (Source is at V=40..55, Target is at V=32..47)
    for y in range(32, 48):
        for x in range(64):
            if 8 + y < h:
                out[y][x] = pix[8 + y][x]

    # Save Entity Texture to both assets and references
    dest_path = TEXTURES / "entity/steel_inquisitor.png"
    ref_path = Path("references/entity/steel_inquisitor.png")
    png_write(dest_path, 64, 64, out)
    png_write(ref_path, 64, 64, out)
    print(f"Saved remapped entity texture to {dest_path} and {ref_path}")

    # Extract Axe (Source U=0-15, V=48-63)
    axe_out = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            if 48 + y < h and x < w:
                axe_out[y][x] = pix[48 + y][x]

    axe_dest = TEXTURES / "item/inquisitor_axe.png"
    axe_ref = Path("references/item/inquisitor_axe.png")
    png_write(axe_dest, 16, 16, axe_out)
    png_write(axe_ref, 16, 16, axe_out)
    print(f"Saved axe texture to {axe_dest} and {axe_ref}")


if __name__ == "__main__":
    main()
