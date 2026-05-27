from pathlib import Path
from PIL import Image

def clear_overlays():
    textures_dir = Path("src/main/resources/assets/mistborn_metal_arts/textures/gui").parent / "entity"
    img_path = textures_dir / "steel_inquisitor.png"
    ref_path = Path("references/entity/steel_inquisitor.png")
    
    if not img_path.exists():
        print(f"Error: {img_path} not found!")
        return
        
    img = Image.open(img_path).convert("RGBA")
    pixels = img.load()
    
    # Standard overlay regions to clear completely:
    # 1. Hat (head overlay): x = 32..64, y = 0..16
    for y in range(0, 16):
        for x in range(32, 64):
            pixels[x, y] = (0, 0, 0, 0)
            
    # 2. Right Sleeve (right arm overlay): x = 40..56, y = 32..48
    for y in range(32, 48):
        for x in range(40, 56):
            pixels[x, y] = (0, 0, 0, 0)
            
    # 3. Left Sleeve (left arm overlay): x = 48..64, y = 48..64
    for y in range(48, 64):
        for x in range(48, 64):
            pixels[x, y] = (0, 0, 0, 0)
            
    # 4. Jacket (torso overlay): x = 16..40, y = 32..48
    # Note: The custom tattered cape occupies x = 0..22, y = 34..51.
    # So we clear x = 22..40 completely for y = 32..48.
    # And clear y = 32..33 for x = 16..22.
    for y in range(32, 48):
        for x in range(22, 40):
            pixels[x, y] = (0, 0, 0, 0)
    for y in range(32, 34):
        for x in range(16, 22):
            pixels[x, y] = (0, 0, 0, 0)
            
    # 5. Right Pants (right leg overlay): x = 0..16, y = 32..48
    # Clear y = 32..33 (which does not overlap with the cape)
    for y in range(32, 34):
        for x in range(0, 16):
            pixels[x, y] = (0, 0, 0, 0)
            
    # 6. Left Pants (left leg overlay): x = 0..16, y = 48..64
    # Clear y = 52..64 (which does not overlap with the cape)
    for y in range(52, 64):
        for x in range(0, 16):
            pixels[x, y] = (0, 0, 0, 0)
            
    # Save the cleaned texture
    img.save(img_path)
    img.save(ref_path)
    print(f"Successfully cleared unused overlay regions in {img_path} and {ref_path}!")

if __name__ == "__main__":
    clear_overlays()
