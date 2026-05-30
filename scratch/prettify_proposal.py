import json
import codecs

with open("/Users/noah/.gemini/antigravity/brain/b16939c5-276a-485d-b390-0ae17d3fdbfb/scratch/extracted_proposal.md", "r", encoding="utf-8") as f:
    raw_content = f.read()

# Clean up JSON string wrapping if present
if raw_content.startswith('"') and raw_content.endswith('"'):
    # Let's decode it as a JSON string
    try:
        raw_content = json.loads(raw_content)
    except Exception as e:
        raw_content = raw_content[1:-1].replace('\\"', '"').replace('\\\\', '\\')

# Replace literal \n and \t
pretty_content = raw_content.replace("\\n", "\n").replace("\\t", "\t")

# Write to pretty file
pretty_path = "/Users/noah/.gemini/antigravity/brain/b16939c5-276a-485d-b390-0ae17d3fdbfb/scratch/extracted_proposal_pretty.md"
with open(pretty_path, "w", encoding="utf-8") as out:
    out.write(pretty_content)

print("Prettified markdown written to scratch/extracted_proposal_pretty.md")

# Let's search and print sections
# 1. Implementation Phases
idx = pretty_content.find("## 17. Implementation Phases")
if idx == -1:
    idx = pretty_content.find("Implementation Phases")
if idx != -1:
    print("\n=== ROADMAP PHASES ===")
    print(pretty_content[idx:idx+3500])

# 2. Spiritual Bloat
idx_bloat = pretty_content.find("## 9. Spiritual Bloat")
if idx_bloat == -1:
    idx_bloat = pretty_content.find("Spiritual Bloat")
if idx_bloat != -1:
    print("\n=== SPIRITUAL BLOAT ===")
    print(pretty_content[idx_bloat:idx_bloat+1500])

# 3. Lerasium Upgrades
idx_lera = pretty_content.find("## 8. Lerasium Upgrades")
if idx_lera == -1:
    idx_lera = pretty_content.find("Lerasium Upgrades")
if idx_lera != -1:
    print("\n=== LERASIUM UPGRADES ===")
    print(pretty_content[idx_lera:idx_lera+1500])
