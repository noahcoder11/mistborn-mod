"""Downsample the HQ steel inquisitor reference to a clean 64x64 entity texture.

The HQ reference has a non-standard UV layout where the right arm and right leg
regions are swapped compared to standard Minecraft skin format. This script
downsamples and then remaps the body parts to the correct UV positions.
"""

from pathlib import Path
from clean_texture_pass import read_png, png_write, TEXTURES


BG_THRESHOLD = 25


def is_bg(px):
    r, g, b, a = px
    return a < 10 or (r + g + b) < BG_THRESHOLD


def area_downsample(pixels, src_w, src_h, dst_w, dst_h):
    """Downsample by averaging each destination cell's source region."""
    out = [[(0, 0, 0, 0)] * dst_w for _ in range(dst_h)]
    for dy in range(dst_h):
        sy0 = int(dy * src_h / dst_h)
        sy1 = int((dy + 1) * src_h / dst_h)
        for dx in range(dst_w):
            sx0 = int(dx * src_w / dst_w)
            sx1 = int((dx + 1) * src_w / dst_w)
            r_sum, g_sum, b_sum, a_sum, count = 0, 0, 0, 0, 0
            for sy in range(sy0, sy1):
                for sx in range(sx0, sx1):
                    px = pixels[sy][sx]
                    if not is_bg(px):
                        r_sum += px[0]; g_sum += px[1]; b_sum += px[2]; a_sum += px[3]
                        count += 1
            if count == 0:
                out[dy][dx] = (0, 0, 0, 0)
            else:
                out[dy][dx] = (r_sum // count, g_sum // count, b_sum // count, a_sum // count)
    return out


def copy_region(src, dst, sx, sy, dx, dy, w, h):
    """Copy a rectangular region from src to dst."""
    for y in range(h):
        for x in range(w):
            if sy + y < 64 and sx + x < 64 and dy + y < 64 and dx + x < 64:
                dst[dy + y][dx + x] = src[sy + y][sx + x]


def swap_regions(pixels, x1, y1, x2, y2, w, h):
    """Swap two rectangular regions in the pixel array."""
    # Save region 1
    temp = [[(0, 0, 0, 0)] * w for _ in range(h)]
    copy_region(pixels, temp, x1, y1, 0, 0, w, h)
    # Copy region 2 -> region 1
    copy_region(pixels, pixels, x2, y2, x1, y1, w, h)
    # Copy saved region 1 -> region 2
    for y in range(h):
        for x in range(w):
            if y2 + y < 64 and x2 + x < 64:
                pixels[y2 + y][x2 + x] = temp[y][x]


def main():
    src_path = Path("../references/ChatGPT Image May 16, 2026 at 05_22_41 PM.png")
    if not src_path.exists():
        src_path = Path("references/ChatGPT Image May 16, 2026 at 05_22_41 PM.png")
    if not src_path.exists():
        print("Error: Source texture not found!")
        return

    w, h, pixels = read_png(src_path)
    print(f"Source: {w}x{h}")

    out = area_downsample(pixels, w, h, 64, 64)

    # ── Fix UV layout mismatches ──
    # The HQ reference has right arm and right leg content swapped.
    # Standard Minecraft skin UV:
    #   Right Leg: (0, 16) 16x16 region
    #   Right Arm: (40, 16) 16x16 region
    # In the HQ reference:
    #   (0, 16) has ARM content (skin tones)
    #   (40, 16) has LEG content (dark robe)
    # Swap them:
    print("Swapping Right Arm <-> Right Leg UV regions...")
    swap_regions(out, 0, 16, 40, 16, 16, 16)

    # Save
    ref_path = Path("../references/entity/steel_inquisitor.png")
    if not ref_path.parent.exists():
        ref_path = Path("references/entity/steel_inquisitor.png")
    dest_path = TEXTURES / "entity/steel_inquisitor.png"

    png_write(ref_path, 64, 64, out)
    print(f"Saved reference: {ref_path}")
    png_write(dest_path, 64, 64, out)
    print(f"Saved active texture: {dest_path}")


if __name__ == "__main__":
    main()
