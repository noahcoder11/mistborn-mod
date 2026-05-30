with open("/Users/noah/.gemini/antigravity/brain/b16939c5-276a-485d-b390-0ae17d3fdbfb/scratch/extracted_proposal_pretty.md", "r", encoding="utf-8") as f:
    for line in f:
        trimmed = line.strip()
        if trimmed.startswith("#") or trimmed.startswith("##"):
            print(trimmed)
