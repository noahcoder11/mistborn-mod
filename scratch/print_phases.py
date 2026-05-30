with open("/Users/noah/.gemini/antigravity/brain/b16939c5-276a-485d-b390-0ae17d3fdbfb/scratch/extracted_proposal.md", "r", encoding="utf-8") as f:
    text = f.read()

# Let's find "## 17. Implementation Phases"
idx = text.find("## 17. Implementation Phases")
if idx != -1:
    print("=== IMPLEMENTATION PHASES ===")
    print(text[idx:idx+4000])

# Let's also find "## 9. Spiritual Bloat"
idx_bloat = text.find("## 9. Spiritual Bloat")
if idx_bloat != -1:
    print("\n=== SPIRITUAL BLOAT ===")
    print(text[idx_bloat:idx_bloat+2000])

# Let's also find "## 8. Lerasium Upgrades"
idx_lera = text.find("## 8. Lerasium Upgrades")
if idx_lera != -1:
    print("\n=== LERASIUM UPGRADES ===")
    print(text[idx_lera:idx_lera+2000])
