import json

log_path = "/Users/noah/.gemini/antigravity/brain/b16939c5-276a-485d-b390-0ae17d3fdbfb/.system_generated/logs/transcript.jsonl"
out_path = "/Users/noah/.gemini/antigravity/brain/b16939c5-276a-485d-b390-0ae17d3fdbfb/scratch/extracted_proposal.md"

found_content = None
with open(log_path, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            step = json.loads(line)
            tool_calls = step.get("tool_calls", [])
            for tc in tool_calls:
                args = tc.get("args", {})
                if isinstance(args, str):
                    try:
                        args = json.loads(args)
                    except:
                        pass
                if isinstance(args, dict):
                    content = args.get("CodeContent", "")
                    if "Mistborn Metal Arts — Expanded Systems Design Proposal" in content:
                        found_content = content
                        break
            if found_content:
                break
        except Exception as e:
            continue

if found_content:
    with open(out_path, 'w', encoding='utf-8') as out:
        out.write(found_content)
    print("Successfully found and extracted full original multi-phase proposal!")
else:
    print("Could not find the original proposal in logs.")
