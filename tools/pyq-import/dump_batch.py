"""Prints a batch of questions (compact) for writing explanations: dump_batch.py PAPER START COUNT"""
import json, sys
from pathlib import Path
paper, start, count = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
qs = json.loads(Path(f"../../app/src/main/assets/pyq/{paper}.json").read_text())
done = json.loads(Path(f"explanations/{paper}.json").read_text()) if Path(f"explanations/{paper}.json").exists() else {}
todo = [q for q in qs if q["id"] not in done][start:start + count]
print(f"# {paper}: {len(done)} done, {len(qs) - len(done)} left; showing {len(todo)}")
for q in todo:
    img = " [IMG]" if q["images"] else ""
    ans = "ABCD"[q["answer"] - 1]
    opts = " | ".join(f"{'ABCD'[i]}:{o[:60]}" for i, o in enumerate(q["options"]))
    print(f"{q['id'][-8:]}{img} {q['stem'][:230]} || {opts} || KEY {ans}")
