import sys
import os
from PIL import Image

image_path = "/Users/noah/.gemini/antigravity/brain/b16939c5-276a-485d-b390-0ae17d3fdbfb/media__1779923570974.png"
img = Image.open(image_path).convert("RGBA")
width, height = img.size

# We have 3 rows.
# Row 1 height: 0 to 227
# Row 2 height: 227 to 454
# Row 3 height: 454 to 682

# Let's define the cells in each row:
# Row 1 has 4 cells of width 256
# Row 2 has 5 cells of width 1024/5 = 204.8
# Row 3 has 5 cells of width 1024/5 = 204.8

cells = []

# Row 1: 4 columns
for col in range(4):
    x0 = int(col * 256)
    x1 = int((col + 1) * 256)
    cells.append(("row1_col" + str(col), x0, 0, x1, 227))

# Row 2: 5 columns
for col in range(5):
    x0 = int(col * 204.8)
    x1 = int((col + 1) * 204.8)
    cells.append(("row2_col" + str(col), x0, 227, x1, 454))

# Row 3: 5 columns
for col in range(5):
    x0 = int(col * 204.8)
    x1 = int((col + 1) * 204.8)
    cells.append(("row3_col" + str(col), x0, 454, x1, 682))

# Map cell indices to final texture names:
texture_names = {
    "row1_col0": "altar_top",
    "row1_col1": "altar_side",
    "row1_col2": "altar_bottom",
    "row1_col3": "altar_inner_side",
    
    "row2_col0": "binding_cuff",
    "row2_col1": "cuff_top",
    "row2_col2": "cuff_side",
    "row2_col3": "cuff_inner",
    "row2_col4": "blood_channel",
    
    "row3_col0": "corner",
    "row3_col1": "pillar",
    "row3_col2": "rune_block",
    "row3_col3": "base_trim",
    "row3_col4": "stone"
}

output_dir = "/Users/noah/Documents/Codex/2026-05-14/you-are-an-expert-minecraft-mod/src/main/resources/assets/mistborn_metal_arts/textures/block"
os.makedirs(output_dir, exist_ok=True)

for cell_id, x0, y0, x1, y1 in cells:
    name = texture_names[cell_id]
    cell_img = img.crop((x0, y0, x1, y1))
    
    # Let's find the actual square texture inside this cell.
    # The text is at the top (usually top 30-40 pixels is text).
    # The background is black (R, G, B < 15).
    # Let's find the bounding box of non-black pixels starting from Y = 35.
    cx0, cy0, cx1, cy1 = cell_img.width, cell_img.height, 0, 0
    has_pixels = False
    
    # We scan starting below the text (e.g. Y=35 relative to cell Y0)
    for cy in range(35, cell_img.height):
        for cx in range(cell_img.width):
            r, g, b, a = cell_img.getpixel((cx, cy))
            if r > 15 or g > 15 or b > 15:
                has_pixels = True
                if cx < cx0: cx0 = cx
                if cy < cy0: cy0 = cy
                if cx > cx1: cx1 = cx
                if cy > cy1: cy1 = cy
                
    if has_pixels:
        # Crop the active texture area!
        # Make sure it's a square.
        tw = cx1 - cx0 + 1
        th = cy1 - cy0 + 1
        size = max(tw, th)
        
        # Center the crop to ensure it's a square and captures everything nicely.
        # But actually, we can just crop from cx0, cy0 to cx1, cy1!
        cropped = cell_img.crop((cx0, cy0, cx1 + 1, cy1 + 1))
        
        # Resize to standard 16x16 or 32x32 for high quality pixel art!
        # Let's use 32x32 or 16x16. 32x32 maintains the gorgeous detail of the concept texture atlas!
        resized = cropped.resize((32, 32), Image.Resampling.NEAREST)
        
        out_path = os.path.join(output_dir, name + ".png")
        resized.save(out_path)
        print(f"Saved: {name}.png ({cropped.width}x{cropped.height} -> 32x32)")
    else:
        print(f"Failed to find texture in cell {cell_id} ({name})")
