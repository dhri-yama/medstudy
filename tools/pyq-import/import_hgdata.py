"""Adds exam-labelled questions from hgdata (the MedMCQA dump) as extra papers: JIPMER 2018/2019 and DNB 2018.
Writes app/src/main/assets/pyq/<paper>.json and merges the paper rows into index.json (run after finalize.py)."""
import json, re, collections, difflib
from pathlib import Path

ASSETS = Path("../../app/src/main/assets"); DEST = ASSETS / "pyq"
HG = Path("../../hgdata/data/train.json")
WANT = {"JIPMER 2018": ("JIPMER2018", "JIPMER", 2018, "JIPMER 2018"), "JIPMER 2019": ("JIPMER2019", "JIPMER", 2019, "JIPMER 2019"),
        "DNB 2018": ("DNB2018", "DNB", 2018, "DNB 2018")}
subjects = {s["name"]: s["id"] for s in json.loads((ASSETS / "subjects.json").read_text())}
vocab = json.loads(Path("out/vocab.json").read_text())

def clean_id(s):
    for a, b in ((" ", "_"), ("&", "and"), ("/", "_"), ("-", "_"), (".", ""), (",", ""), ("'", ""), ('"', "")):
        s = s.lower().replace(a, b)
    return s

existing = {}
for i, t in enumerate(json.loads((ASSETS / "topics.json").read_text())):
    for sub in t["subjects"]:
        existing.setdefault((sub, t["name"].lower()), f"topic_{i}_{clean_id(t['name'])}")
index = json.loads((DEST / "index.json").read_text())
new_topics = {t["id"]: t for t in index["topics"]}

STOP = set("the a an of in on to is and or for by with from at as its all not true false following most which what".split())
def toks(s): return {w for w in re.sub(r"[^a-z0-9 ]", " ", s.lower()).split() if w not in STOP and len(w) > 2}
GENERIC = {"miscellaneous", "others", "all india exam", "introduction", "general"}

import math
LABEL_RE = re.compile(r"(?i)\b(NEET|AIIMS|FMGE|JIPMER|DNB|PGI|AI|MH|AP|TN|KA|UP|Delhi|CMC|Kerala|DPG|Bihar|UPSC|NIMHANS|COMEDK|SGPGI)\b.*\b(19|20)\d{2}\b")
_model = {}

def _train():
    """Per subject: a tf-idf centroid for every topic with enough labelled MedMCQA questions."""
    docs = collections.defaultdict(list)
    for line in HG.read_text().splitlines():
        if not line.strip(): continue
        x = json.loads(line); t = (x.get("topic_name") or "").strip()
        if not t or t.lower() in GENERIC or LABEL_RE.search(t) or x["subject_name"] not in subjects: continue
        docs[x["subject_name"]].append((t, toks(x["question"] + " " + (x.get("exp") or "")[:600])))
    for sub, items in docs.items():
        df = collections.Counter(w for _, ws in items for w in ws)
        n = len(items); cent = collections.defaultdict(collections.Counter); cnt = collections.Counter()
        for t, ws in items:
            cnt[t] += 1
            for w in ws: cent[t][w] += math.log(n / df[w])
        _model[sub] = ({t: (c, math.sqrt(sum(v * v for v in c.values()))) for t, c in cent.items() if cnt[t] >= 30}, df, n)
    print("topic model:", {k: len(v[0]) for k, v in _model.items()})

def classify(subject, text):
    """Best topic for the subject by cosine similarity to the topic centroids; None when unsure."""
    if not _model: _train()
    if subject not in _model: return None
    cents, df, n = _model[subject]
    q = {w: math.log(n / df[w]) for w in toks(text) if w in df}
    qn = math.sqrt(sum(v * v for v in q.values())) or 1
    scores = sorted(((sum(q[w] * c[w] for w in q) / (qn * cn), t) for t, (c, cn) in cents.items() if cn), reverse=True)
    if len(scores) < 2: return None
    (s1, t1), (s2, _) = scores[0], scores[1]
    return t1 if s1 >= 0.08 and s1 >= 1.25 * s2 else None

def topic_id(subject, name):
    hit = existing.get((subject, name.lower()))
    if hit: return hit
    tid = f"pyq_{subjects[subject]}_{clean_id(name)}"
    new_topics[tid] = dict(id=tid, subjectId=subjects[subject], name=name)
    return tid

rows = collections.defaultdict(list); skipped = collections.Counter()
for line in HG.read_text().splitlines():
    if not line.strip(): continue
    x = json.loads(line)
    label = (x.get("topic_name") or "").strip()
    if label not in WANT: continue
    if x["subject_name"] not in subjects: skipped["no subject"] += 1; continue
    opts = [x["opa"], x["opb"], x["opc"], x["opd"]]
    if x["cop"] not in (1, 2, 3, 4) or not all((o or "").strip() for o in opts) or len(set(opts)) < 4:
        skipped["bad options/answer"] += 1; continue
    rows[label].append(x)

papers = []
for label, (pid, exam, year, title) in WANT.items():
    out = []
    for n, x in enumerate(rows[label], 1):
        sub = x["subject_name"]; exp = (x.get("exp") or "").strip()
        topic = classify(sub, x["question"] + " " + exp)
        out.append(dict(id=f"{pid.lower()}-q{n:03d}", number=n, subjectId=subjects[sub], topicId=topic_id(sub, topic) if topic else None,
                        stem=x["question"].strip(), options=[o.strip() for o in [x["opa"], x["opb"], x["opc"], x["opd"]]], answer=x["cop"], images=[],
                        sourceFile="MedMCQA (hgdata)", sourcePage=0, explanation=exp if len(exp) >= 40 else "",
                        crossCheck="not checkable", answerConflict=False))
    (DEST / f"{pid}.json").write_text(json.dumps(out, ensure_ascii=False, indent=0))
    papers.append(dict(id=pid, examId=exam, year=year, title=title, kind="OFFICIAL", count=len(out), sourceName="MedMCQA", rights="LICENSED"))
    print(pid, len(out), "topic tagged:", sum(1 for q in out if q["topicId"]), "explained:", sum(1 for q in out if q["explanation"]))

index["papers"] = [p for p in index["papers"] if p["id"] not in {q["id"] for q in papers}] + papers
index["topics"] = sorted(new_topics.values(), key=lambda t: t["id"])
(DEST / "index.json").write_text(json.dumps(index, ensure_ascii=False, indent=1))
print("skipped:", dict(skipped))
