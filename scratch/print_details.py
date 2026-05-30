with open("/Users/noah/.gemini/antigravity/brain/b16939c5-276a-485d-b390-0ae17d3fdbfb/scratch/extracted_proposal_pretty.md", "r", encoding="utf-8") as f:
    text = f.read()

# Let's search after index 3000 to skip Table of Contents
search_start = 3000

# 1. Implementation Phases
idx = text.find("## 17. Implementation Phases", search_start)
if idx == -1:
    idx = text.find("Implementation Phases", search_start)
if idx != -1:
    print("=== DETAILED IMPLEMENTATION PHASES ===")
    print(text[idx:idx+4000])
else:
    print("Could not find Implementation Phases heading.")

# 2. Spiritual Bloat
idx_bloat = text.find("## 9. Spiritual Bloat", search_start)
if idx_bloat == -1:
    idx_bloat = text.find("Spiritual Bloat", search_start)
if idx_bloat != -1:
    print("\n=== DETAILED SPIRITUAL BLOAT ===")
    print(text[idx_bloat:idx_bloat+2000])

# 3. Lerasium Upgrades
idx_lera = text.find("## 8. Lerasium Upgrades & Alloys", search_start)
if idx_lera == -1:
    idx_lera = text.find("Lerasium Upgrades", search_start)
if idx_lera != -1:
    print("\n=== DETAILED LERASIUM UPGRADES ===")
    print(text[idx_lera:idx_lera+2000])
