from pathlib import Path
from clean_texture_pass import read_png

def main():
    path = Path("src/main/resources/assets/mistborn_metal_arts/textures/entity/steel_inquisitor.png")
    w, h, pixels = read_png(path)
    print(f"Dimensions: {w}x{h}")
    
    # Check common regions
    regions = {
        "Head": (0, 0, 32, 16),
        "Hat": (32, 0, 32, 16),
        "Right Leg": (0, 16, 16, 16),
        "Body": (16, 16, 24, 16),
        "Right Arm": (40, 16, 16, 16),
        "Right Pants": (0, 32, 16, 16),
        "Jacket": (16, 32, 24, 16),
        "Right Sleeve": (40, 32, 16, 16),
        "Left Leg": (16, 48, 16, 16),
        "Left Arm": (32, 48, 16, 16),
        "Left Pants": (0, 48, 16, 16),
        "Left Sleeve": (48, 48, 16, 16)
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
