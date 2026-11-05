"""add.py PAPER <<< lines of 'qNNN: explanation' (keys are the id suffix after the paper, e.g. q001); '!q001: note' lines record a doubt about the key."""
import json, sys
from pathlib import Path
paper = sys.argv[1]
f = Path(f"explanations/{paper}.json"); d = json.loads(f.read_text()) if f.exists() else {}
doubts = Path("explanations/doubts.md")
n = 0
for line in sys.stdin.read().splitlines():
    line = line.strip()
    if not line or ":" not in line: continue
    key, text = line.split(":", 1)
    doubt = key.startswith("!"); key = key.lstrip("!").strip(); text = text.strip()
    qid = f"{paper.lower()}-{key}"
    if doubt:
        with doubts.open("a") as h: h.write(f"- {qid}: {text}\n")
    else:
        d[qid] = text; n += 1
f.write_text(json.dumps(d, indent=0, ensure_ascii=False))
print(paper, "added", n, "total", len(d))
