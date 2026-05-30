import sys
from PIL import Image

image_path = "/Users/noah/.gemini/antigravity/brain/b16939c5-276a-485d-b390-0ae17d3fdbfb/media__1779923570974.png"
img = Image.open(image_path).convert("RGBA")
width, height = img.size

# Let's print out pixel colors to see where the text/gaps are.
# Alternatively, let's manually measure the grid:
# The image is 1024 x 682.
# Let's see:
# Row 1 has 4 columns: altar_top, altar_side, altar_bottom, altar_inner_side
# Since the image width is 1024, each of the 4 columns in Row 1 should be roughly 1024 / 4 = 256 pixels wide!
# Row 2 has 5 columns: binding_cuff, cuff_top, cuff_side, cuff_inner, blood_channel
# So each of the 5 columns in Row 2 should be roughly 1024 / 5 = 204.8 pixels wide!
# Row 3 has 5 columns: corner, pillar, rune_block, base_trim, stone
# So each of the 5 columns in Row 3 should be roughly 1024 / 5 = 204.8 pixels wide!
# Let's print the actual color of a grid of points to verify the layout!

for y in range(0, height, height // 10):
    row_str = ""
    for x in range(0, width, width // 15):
        r, g, b, a = img.getpixel((x, y))
        brightness = (r + g + b) // 3
        # Use simple char representation
        row_str += "#" if brightness > 30 else "."
    print(f"y={y:3d}: {row_str}")
