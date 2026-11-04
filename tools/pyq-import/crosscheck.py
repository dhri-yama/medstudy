"""Cross-checks each imported question against other public copies of the same paper
(paper_downloads/other_sources/**) and writes crosscheck.csv / crosscheck.json.

A question is "unmatched" when no other source of that year has a similar stem; it is "answer conflict" when a
matching source gives a different answer. Years with no usable other source are reported as "not checkable".
"""
import csv, difflib, json, re, subprocess, sys
from pathlib import Path

OTHER = Path("../../paper_downloads/other_sources")
OUT = Path("out")
STOP = set("the a an of in on to is are was were with and or for by from at as that this which following what most true not all these those it its be has have had".split())
LETTERS = "abcd"

def pdf_text(path, layout=True):
    return subprocess.run(["pdftotext", *(["-layout"] if layout else []), str(path), "-"], capture_output=True, text=True).stdout

def toks(s):
    return [w for w in re.sub(r"[^a-z0-9 ]", " ", s.lower()).split() if w not in STOP]

def clean_lines(text):
    out = []
    for ln in text.splitlines():
        ln = ln.strip()
        if not ln or re.fullmatch(r"(docs\.aglasem\.com|AGLASEM\.COM|\d+)", ln, re.I) or "aglasem" in ln.lower() and len(ln) < 40:
            continue
        out.append(ln)
    return out

OPT = re.compile(r"^\(?([a-dA-D])[\).]\s*(.*)")

def parse_mcq(text, qpat, source):
    """Questions with a)-d) options and optional 'Correct answer: X'."""
    qs, cur = [], None
    for ln in clean_lines(text):
        m = re.match(qpat, ln)
        if m and (cur is None or cur["opts"] or len(cur["stem"]) > 0):
            cur = dict(source=source, stem=m.group(1), opts={}, ans=None, last=None); qs.append(cur); continue
        if cur is None:
            continue
        a = re.match(r"^Correct [Aa]nswer\s*[:\-]\s*\(?([A-Da-d])\b", ln)
        if a:
            cur["ans"] = a.group(1).lower(); cur["last"] = "done"; continue
        if cur["last"] == "done":
            continue
        o = OPT.match(ln)
        if o and len(cur["opts"]) < 4 and o.group(1).lower() == LETTERS[len(cur["opts"])]:
            cur["opts"][o.group(1).lower()] = o.group(2); cur["last"] = o.group(1).lower()
        elif cur["last"] in cur["opts"]:
            cur["opts"][cur["last"]] += " " + ln
        elif not cur["opts"]:
            cur["stem"] += " " + ln
    return qs

def parse_qa(text, source):
    """'Q. stem' / 'A. answer' pairs with no options."""
    qs, cur, mode = [], None, None
    for ln in clean_lines(text):
        if re.match(r"^Q\d*[.:]?\s", ln) or re.match(r"^Q\.", ln):
            cur = dict(source=source, stem=re.sub(r"^Q\d*[.:]?\s*", "", ln), opts={}, ans=None, anstext="", last=None); qs.append(cur); mode = "q"
        elif re.match(r"^A\s*[-.:]\s*", ln) and cur is not None:
            cur["anstext"] = re.sub(r"^A\s*[-.:]\s*", "", ln); mode = "a"
        elif cur is not None:
            if mode == "a" and not re.match(r"^\([A-D]\)", ln) and len(ln) < 120:
                cur["anstext"] += " " + ln
            elif mode == "q":
                cur["stem"] += " " + ln
    return qs

def load_sources():
    by_year = {}
    cd = OTHER / "collegedekho"
    if (cd / "neet-pg-2020.pdf").exists():
        by_year.setdefault(2020, []).extend(parse_mcq(pdf_text(cd / "neet-pg-2020.pdf"), r"^\d{1,3}\.\s+(.*)", "collegedekho-2020"))
    for y, f in ((2021, "neet-pg-2021.pdf"), (2022, "neet-pg-2022-question-paper-with-answer-key.pdf"), (2025, "neet-pg-unofficial-answer-key-2025-pdf.pdf")):
        if (cd / f).exists():
            by_year.setdefault(y, []).extend(parse_qa(pdf_text(cd / f, layout=False), f"collegedekho-{y}"))
    for f in sorted((OTHER / "aglasem").glob("NEET_PG_20*_Question_Paper_*.pdf")):
        y = int(f.name[8:12])
        if pdf_text(f):
            by_year.setdefault(y, []).extend(parse_mcq(pdf_text(f), r"^Q\s?\d{1,3}[.:]\s*(.*)", f"aglasem-{f.stem.split('Paper_')[-1]}"))
    return by_year

def jacc(a, b):
    a, b = set(toks(a)), set(toks(b))
    return len(a & b) / len(a | b) if a | b else 0.0

def stem_score(ext, q):
    a = toks(ext["stem"]); b = toks(q["stem"])
    if not a or not b:
        return 0.0
    full_b = set(toks(q["stem"] + " " + " ".join(q["options"])))
    contain = sum(1 for w in a if w in full_b) / len(a)
    if len(a) < 14 or not ext["opts"]:  # short recall fragments: containment
        return contain if len(a) >= 3 else 0.0
    ratio = difflib.SequenceMatcher(None, " ".join(a), " ".join(b)).ratio()
    return max(ratio, 0.9 * contain if contain > 0.85 else 0)

def option_score(ext, q):
    if len(ext["opts"]) < 4:
        return 0.0
    return sum(max(jacc(o, m) for m in q["options"]) for o in ext["opts"].values()) / 4

def score(ext, q):
    """Similarity of an external entry to one of our questions (0..1). The same question is often reworded between
    copies, so identical options count as a match even when the stems differ."""
    st, op = stem_score(ext, q), option_score(ext, q)
    if op >= 0.6 and st >= 0.25:
        return max(st, 0.5 * st + 0.5 * op, 0.62)
    return st

def ext_answer_text(e):
    if e["opts"] and e.get("ans") in e["opts"]:
        return e["opts"][e["ans"]]
    return e.get("anstext") or ""

def best_option(q, text):
    t = set(toks(text))
    if not t:
        return None, 0
    sc = [len(t & set(toks(o))) / len(t | set(toks(o)) or {1}) for o in q["options"]]
    i = max(range(4), key=lambda k: sc[k])
    return i + 1, sc[i]

def run():
    by_year = load_sources()
    rows, flags = [], {}
    for pdir in sorted(OUT.glob("NEETPG20*")):
        paper = pdir.name; year = int(paper[-4:])
        qs = [q for q in json.loads((pdir / "questions.json").read_text()) if not q["exclude"]]
        srcs = by_year.get(year, [])
        names = sorted({s["source"].split("-")[0] for s in srcs})
        print(f"{paper}: {len(qs)} questions, {len(srcs)} external entries from {names or 'no usable source'}")
        for q in qs:
            best, bs = None, 0.0
            for e in srcs:
                s = score(e, q)
                if s > bs: best, bs = e, s
            status = "not checkable" if not srcs else ("matched" if bs >= 0.62 else "unmatched")
            conflict = ""
            if status == "matched":
                at = ext_answer_text(best)
                if at:
                    opt, osc = best_option(q, at)
                    if osc >= 0.34 and opt != q["answer"]:
                        conflict = f"{best['source']} answer looks like option {opt}, ours is {q['answer']}"
            rows.append([paper, q["sourceNumber"], status, f"{bs:.2f}", best["source"] if best and status == "matched" else "", conflict, q["stem"][:90].replace("\n", " ")])
            flags[q["id"]] = dict(status=status, conflict=bool(conflict))
    with open("crosscheck.csv", "w", newline="") as f:
        w = csv.writer(f); w.writerow(["paper", "number", "status", "score", "matched_source", "answer_conflict", "stem"]); w.writerows(rows)
    Path("crosscheck.json").write_text(json.dumps(flags, indent=0))
    for paper in sorted({r[0] for r in rows}):
        r = [x for x in rows if x[0] == paper]
        c = {s: sum(1 for x in r if x[2] == s) for s in ("matched", "unmatched", "not checkable")}
        print(paper, c, "conflicts:", sum(1 for x in r if x[5]))

if __name__ == "__main__":
    run()
