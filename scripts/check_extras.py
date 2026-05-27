from pathlib import Path
from clean_texture_pass import read_png

def main():
    path = Path("src/main/resources/assets/mistborn_metal_arts/textures/entity/steel_inquisitor.png")
    w, h, pixels = read_png(path)
    
    # Check "extra" regions
    regions = {
        "Extra 1": (56, 0, 8, 16),
        "Extra 2": (56, 16, 8, 16),
        "Extra 3": (56, 32, 8, 16),
        "Extra 4": (56, 48, 8, 16)
    }
    
    for name, (x0, y0, rw, rh) in regions.items():
        opaque = 0
        for y in range(y0, y0 + rh):
            for x in range(x0, x0 + rw):
                if y < h and x < w and pixels[y][x][3] > 0:
                    opaque += 1
        print(f"{name} ({x0},{y0}): {opaque} opaque pixels")

if __name__ == "__main__":
    main()
