"""Proposes topic_map.csv: each (subject, PDF topic) -> an existing MedMCQA topic (merge) or a new topic."""
import csv, collections, difflib, json, re, sys
from pathlib import Path

ASSETS = Path("../../app/src/main/assets")
PAPERS = ["NEETPG2020", "NEETPG2021", "NEETPG2022", "NEETPG2023", "NEETPG2024"]


def norm(s):
    s = s.lower().replace("&", " and ")
    s = re.sub(r"[^a-z0-9 ]", " ", s)
    s = re.sub(r"\b(the|of|and|in)\b", " ", s)
    return re.sub(r"\s+", " ", s).strip()


existing = collections.defaultdict(list)  # subject name -> [topic names]
for t in json.loads((ASSETS / "topics.json").read_text()):
    for sub in t["subjects"]:
        existing[sub].append(t["name"])

pairs = collections.Counter()
for p in PAPERS:
    for q in json.loads(Path(f"out/{p}/questions.json").read_text()):
        pairs[(q["subject"], re.sub(r"\s+", " ", q["topic"]).strip() or "(none)")] += 1

rows = []
for (subject, topic), n in sorted(pairs.items()):
    names = existing.get(subject, [])
    by_norm = {norm(x): x for x in names}
    action, target, score = "new", topic, 0.0
    if norm(topic) in by_norm:
        action, target, score = "merge", by_norm[norm(topic)], 1.0
    elif names:
        best = max(names, key=lambda x: difflib.SequenceMatcher(None, norm(topic), norm(x)).ratio())
        score = difflib.SequenceMatcher(None, norm(topic), norm(best)).ratio()
        if score >= 0.82:
            action, target = "merge", best
        elif score >= 0.6:
            action, target = "new", topic  # near miss: reviewer decides
            rows.append((subject, topic, n, "new", topic, f"{score:.2f}", f"similar to: {best}"))
            continue
    rows.append((subject, topic, n, action, target, f"{score:.2f}", ""))

with open("topic_map.csv", "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["subject", "pdf_topic", "questions", "action", "target_topic", "match_score", "note"])
    w.writerows(rows)
c = collections.Counter(r[3] for r in rows)
print(len(rows), dict(c), "near-miss:", sum(1 for r in rows if r[6]))
