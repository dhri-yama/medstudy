"""Turns out/<paper>/questions.json + topic_map.csv + topics_2025.json into the app's assets under app/src/main/assets/pyq/."""
import csv, json, re, shutil
from pathlib import Path

ASSETS = Path("../../app/src/main/assets")
DEST = ASSETS / "pyq"
PAPERS = {
    "NEETPG2020": ("NEET PG 2020", 2020, "RECALL"), "NEETPG2021": ("NEET PG 2021", 2021, "RECALL"),
    "NEETPG2022": ("NEET PG 2022", 2022, "RECALL"), "NEETPG2023": ("NEET PG 2023", 2023, "RECALL"),
    "NEETPG2024": ("NEET PG 2024", 2024, "RECALL"), "NEETPG2025": ("NEET PG 2025", 2025, "RECALL"),
}
FIXES = {"Forensic Identificatio": "Forensic Identification"}


def clean_id(s):  # mirrors MedMcqaRepository.cleanId
    for a, b in ((" ", "_"), ("&", "and"), ("/", "_"), ("-", "_"), (".", ""), (",", ""), ("'", ""), ('"', "")):
        s = s.lower().replace(a, b)
    return s


subjects = {s["name"]: s["id"] for s in json.loads((ASSETS / "subjects.json").read_text())}
existing = {}  # (subject name, lower topic name) -> existing topic id
for i, t in enumerate(json.loads((ASSETS / "topics.json").read_text())):
    for sub in t["subjects"]:
        existing.setdefault((sub, t["name"].lower()), f"topic_{i}_{clean_id(t['name'])}")

tmap = {(r["subject"], r["pdf_topic"]): FIXES.get(r["target_topic"], r["target_topic"]) for r in csv.DictReader(open("topic_map.csv"))}
crosscheck = json.loads(Path("crosscheck.json").read_text()) if Path("crosscheck.json").exists() else {}
tags25 = json.loads(Path("topics_2025.json").read_text())

new_topics = {}  # id -> {id, subjectId, name}


def topic_id(subject, name):
    name = FIXES.get(name, name)
    if not name or name == "(none)":
        return None
    hit = existing.get((subject, name.lower()))
    if hit:
        return hit
    tid = f"pyq_{subjects[subject]}_{clean_id(name)}"
    new_topics[tid] = dict(id=tid, subjectId=subjects[subject], name=name)
    return tid


if DEST.exists():
    shutil.rmtree(DEST)
(DEST / "images").mkdir(parents=True)
index = []
for pid, (title, year, kind) in PAPERS.items():
    src = Path("out") / pid
    out = []
    for q in json.loads((src / "questions.json").read_text()):
        if q["exclude"]:
            continue
        subject = q["subject"]
        if pid == "NEETPG2025":
            n = str(q["sourceNumber"])
            subject = tags25["subjectOverride"].get(n, subject)
            topic = tags25["topics"][n]
        else:
            topic = tmap.get((subject, re.sub(r"\s+", " ", q["topic"]).strip() or "(none)"), q["topic"])
        tid = topic_id(subject, topic)
        imgs = []
        for im in q["images"]:
            dst = DEST / "images" / pid / Path(im["file"]).name
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy(src / im["file"], dst)
            imgs.append(dict(id=f"pyq_{q['id']}_{im['n']}", path=f"pyq/images/{pid}/{dst.name}",
                             width=im["width"], height=im["height"], sha256=im["sha256"]))
        out.append(dict(id=q["id"], number=q["sourceNumber"], subjectId=subjects[subject], topicId=tid,
                        stem=q["stem"], options=q["options"], answer=q["answer"], images=imgs,
                        sourceFile=q["source"]["file"], sourcePage=q["source"]["page"],
                        crossCheck=crosscheck.get(q["id"], {}).get("status", "not checkable"),
                        answerConflict=crosscheck.get(q["id"], {}).get("conflict", False)))
    (DEST / f"{pid}.json").write_text(json.dumps(out, ensure_ascii=False, indent=0))
    index.append(dict(id=pid, examId="NEET_PG", year=year, title=title, kind=kind, count=len(out),
                      sourceName=json.loads((src / "questions.json").read_text())[0]["source"]["file"]))
(DEST / "index.json").write_text(json.dumps(dict(papers=index, topics=sorted(new_topics.values(), key=lambda t: t["id"])), ensure_ascii=False, indent=1))
print({p["id"]: p["count"] for p in index}, "new topics:", len(new_topics))
