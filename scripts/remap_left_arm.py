from pathlib import Path
from PIL import Image

def remap_left_arm():
    textures_dir = Path("src/main/resources/assets/mistborn_metal_arts/textures/gui").parent / "entity"
    img_path = textures_dir / "steel_inquisitor.png"
    ref_path = Path("references/entity/steel_inquisitor.png")
    
    if not img_path.exists():
        print(f"Error: {img_path} not found!")
        return
        
    img = Image.open(img_path).convert("RGBA")
    
    # In the high-quality detailed texture:
    # - (40, 16) is the 16x16 skin-toned arm texture (Right Arm)
    # - (32, 48) is the 16x16 Left Arm region, which currently has dark trousers
    
    # Crop the skin-toned arm texture
    skin_arm = img.crop((40, 16, 56, 32)).copy()
    
    # Paste the skin-toned arm texture onto Left Arm region (32, 48)
    img.paste(skin_arm, (32, 48))
    
    # Save the corrected premium texture
    img.save(img_path)
    img.save(ref_path)
    print(f"Successfully copied skin-toned Right Arm to Left Arm on {img_path} and {ref_path}!")

if __name__ == "__main__":
    remap_left_arm()
