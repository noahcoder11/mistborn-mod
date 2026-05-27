from pathlib import Path
from clean_texture_pass import read_png

def main():
    path = Path("src/main/resources/assets/mistborn_metal_arts/textures/entity/steel_inquisitor.png")
    w, h, pixels = read_png(path)
    
    # Metal palette from scripts/paint_inquisitor.py
    metal_colors = [
        (48, 45, 42),
        (75, 72, 68),
        (100, 96, 90),
        (125, 120, 112)
    ]
    
    found = []
    for y in range(h):
        for x in range(w):
            px = pixels[y][x][:3]
            if px in metal_colors:
                found.append((x, y))
    
    if not found:
        print("No exact metal colors found. Checking for similar colors...")
        for y in range(h):
            for x in range(w):
                px = pixels[y][x][:3]
                for mc in metal_colors:
                    if all(abs(px[i] - mc[i]) < 5 for i in range(3)):
                        found.append((x, y))
                        break
    
    print(f"Found {len(found)} metal-like pixels.")
    # Show some clusters
    if found:
        # Group by 8x8 regions
        clusters = {}
        for x, y in found:
            cx, cy = x // 8, y // 8
            clusters[(cx, cy)] = clusters.get((cx, cy), 0) + 1
        for (cx, cy), count in sorted(clusters.items()):
            print(f"Region ({cx*8},{cy*8}) to ({cx*8+7},{cy*8+7}): {count} pixels")

if __name__ == "__main__":
    main()
