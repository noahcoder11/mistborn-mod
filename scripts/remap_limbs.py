from pathlib import Path
from PIL import Image

def remap_limbs():
    textures_dir = Path("src/main/resources/assets/mistborn_metal_arts/textures/gui").parent / "entity"
    img_path = textures_dir / "steel_inquisitor.png"
    ref_path = Path("references/entity/steel_inquisitor.png")
    
    if not img_path.exists():
        print(f"Error: {img_path} not found!")
        return
        
    img = Image.open(img_path).convert("RGBA")
    
    # In the high-quality detailed texture:
    # - (0, 16) is the 16x16 skin-toned arm texture
    # - (40, 16) is the 16x16 dark trouser leg texture
    
    # 1. Crop and save the skin-toned arm texture (calling .copy() to evaluate immediately)
    skin_arm = img.crop((0, 16, 16, 32)).copy()
    
    # 2. Crop and save the dark trouser leg texture (calling .copy() to evaluate immediately)
    dark_trousers = img.crop((40, 16, 56, 32)).copy()
    
    # 3. Paste the dark trouser texture onto BOTH Leg regions
    # - Right Leg: (0, 16)
    # - Left Leg: (16, 48)
    img.paste(dark_trousers, (0, 16))
    img.paste(dark_trousers, (16, 48))
    
    # 4. Paste the skin-toned arm texture onto BOTH Arm regions
    # - Right Arm: (40, 16)
    # - Left Arm: (32, 48)
    img.paste(skin_arm, (40, 16))
    img.paste(skin_arm, (32, 48))
    
    # Save the remapped premium texture
    img.save(img_path)
    img.save(ref_path)
    print(f"Successfully remapped limbs on {img_path} and {ref_path}!")

if __name__ == "__main__":
    remap_limbs()
