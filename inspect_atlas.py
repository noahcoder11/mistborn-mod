import sys
from PIL import Image

image_path = "/Users/noah/.gemini/antigravity/brain/b16939c5-276a-485d-b390-0ae17d3fdbfb/media__1779923570974.png"
try:
    img = Image.open(image_path)
    print(f"Format: {img.format}, Size: {img.width}x{img.height}, Mode: {img.mode}")
except Exception as e:
    print(f"Error: {e}")
