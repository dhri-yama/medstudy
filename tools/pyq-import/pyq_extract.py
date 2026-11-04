#!/usr/bin/env python3
"""Extract questions, answers, topics and question images from previous-year paper PDFs.

Usage:  .venv/bin/python pyq_extract.py NEETPG2022 [NEETPG2023 ...]
Output: out/<paperId>/questions.json, out/<paperId>/images/*.webp, out/<paperId>/report.md

Only the question's own picture is kept: branding banners, logos and tutor video frames are dropped
(see classify_image). Everything dropped is listed in the report so nothing disappears silently.
"""
import hashlib
import io
import json
import re
import sys
from pathlib import Path

import fitz  # PyMuPDF
from PIL import Image

ROOT = Path(__file__).resolve().parent
PDF_DIR = ROOT.parent.parent / "paper_downloads"
OUT = ROOT / "out"

PAPERS = {
    "NEETPG2020": dict(file="Neet-PG-2020-PYQs.pdf", layout="A", year=2020),
    "NEETPG2021": dict(file="NEET-PG-2021-PYQs.pdf", layout="A", year=2021),
    "NEETPG2022": dict(file="NEET-PG-2022-PYQs.pdf", layout="A", year=2022),
    "NEETPG2023": dict(file="Neet-PG-2023-previous-year-question-pdf.pdf", layout="A", year=2023),
    "NEETPG2024": dict(file="NEET-PG-PYQs-2024.pdf", layout="B", year=2024),
    "NEETPG2025": dict(file="NEET-PG-Recall-Questions-2025.pdf", layout="C", year=2025),
}

MAX_IMAGE_WIDTH = 1280

# The app's 20 subjects (assets/subjects.json) and the spellings the papers use for them.
SUBJECT_IDS = {
    "Anaesthesia": "anaesthesia", "Anatomy": "anatomy", "Biochemistry": "biochemistry", "Dental": "dental", "ENT": "ent",
    "Forensic Medicine": "forensic_medicine", "Gynaecology & Obstetrics": "gynaecology_and_obstetrics",
    "Medicine": "medicine", "Microbiology": "microbiology", "Ophthalmology": "ophthalmology",
    "Orthopaedics": "orthopaedics", "Pathology": "pathology", "Pediatrics": "pediatrics",
    "Pharmacology": "pharmacology", "Physiology": "physiology", "Psychiatry": "psychiatry", "Radiology": "radiology",
    "Skin": "skin", "Social & Preventive Medicine": "social_and_preventive_medicine", "Surgery": "surgery",
}
SUBJECT_ALIASES = {
    "psm": "Social & Preventive Medicine", "community medicine": "Social & Preventive Medicine",
    "dermatology": "Skin", "derma": "Skin", "anesthesia": "Anaesthesia", "pedia": "Pediatrics",
    "paediatrics": "Pediatrics", "ophtha": "Ophthalmology", "fmt": "Forensic Medicine", "forensic": "Forensic Medicine",
    "ortho": "Orthopaedics", "orthopedics": "Orthopaedics", "obg": "Gynaecology & Obstetrics",
    "obstetrics and gynaecology": "Gynaecology & Obstetrics", "gynecology": "Gynaecology & Obstetrics",
    "neurology": "Medicine", "ear": "ENT",  # sub-headings printed inside the ENT / Medicine sections
}


def canonical_subject(raw):
    """The app's subject name for a spelling used in the papers, or None when it is not a subject at all."""
    key = re.sub(r"\s+", " ", raw.strip()).lower()
    for name in SUBJECT_IDS:
        if name.lower() == key:
            return name
    return SUBJECT_ALIASES.get(key)


# ---------------------------------------------------------------- reading the PDF into a stream
def read_stream(doc):
    """Flatten the document into reading-order items: text lines and images, page by page."""
    stream = []
    for pno, page in enumerate(doc):
        items = []
        for block in page.get_text("dict")["blocks"]:
            if block["type"] != 0:
                continue
            for line in block["lines"]:
                if abs(line["dir"][1]) > 0.05:  # diagonal text is the watermark
                    continue
                text = "".join(s["text"] for s in line["spans"]).replace("​", "").strip()
                if not text:
                    continue
                bold = any((s["flags"] & 16) or "bold" in s["font"].lower() for s in line["spans"] if s["text"].strip())
                items.append(dict(kind="text", page=pno + 1, y=line["bbox"][1], x=line["bbox"][0], text=text, bold=bold))
        for info in page.get_image_info(xrefs=True):
            if not info.get("xref"):
                continue
            x0, y0, x1, y1 = info["bbox"]
            items.append(dict(kind="image", page=pno + 1, y=y0, x=x0, bbox=(x0, y0, x1, y1),
                              w=info["width"], h=info["height"], xref=info["xref"]))
        items.sort(key=lambda it: (round(it["y"]), it["x"]))
        stream.extend(items)
    return stream


# ---------------------------------------------------------------- image classification
PAGE_CENTER = 306.0


def classify_image(it, recall_layout=False):
    """Return (keep, reason). Conservative: anything doubtful is kept and shown in the contact sheets.

    The 2025 recall PDF (recall_layout=True) follows every question with the tutor's explanation: video frames,
    cropped slides and notes. Those are always pasted as separate, page-centred pictures, while a question's own
    picture is left-aligned like the text. So there the rule is position, not shape (a left-aligned 16:9 graph is a
    real question image; a centred 1173x482 slide is not). In the other papers every picture is the question's own."""
    w, h = it["w"], it["h"]
    x0, y0, x1, y1 = it["bbox"]
    aspect = w / h if h else 0
    if w <= 120 and h <= 120:
        return False, "logo (tiny)"
    if aspect > 3.0 and y0 < 90:
        return False, "header banner"
    if min(w, h) < 40:
        return False, "too small"
    cx, disp_w = (x0 + x1) / 2, x1 - x0
    if recall_layout and disp_w >= 240 and w >= 600 and (abs(cx - 306) <= 6 or abs(cx - 324) <= 6):
        return False, "page-centred tutor/explanation image"
    return True, ""


def image_to_webp(doc, xref):
    pix = fitz.Pixmap(doc, xref)
    smask = doc.xref_get_key(xref, "SMask")
    if pix.colorspace is not None and pix.colorspace.n > 3:  # CMYK etc.
        pix = fitz.Pixmap(fitz.csRGB, pix)
    if smask[0] == "xref":
        if pix.alpha:
            pix = fitz.Pixmap(pix, 0)  # drop the alpha the loader already added; the SMask below is the real one
        mask = fitz.Pixmap(doc, int(smask[1].split()[0]))
        if (mask.width, mask.height) != (pix.width, pix.height):
            mask = fitz.Pixmap(mask, pix.width, pix.height, None)  # scale the mask to the image
        pix = fitz.Pixmap(pix, mask)
    mode = "RGBA" if pix.alpha else ("L" if pix.n == 1 else "RGB")
    img = Image.frombytes(mode, (pix.width, pix.height), pix.samples)
    if mode == "RGBA":
        bg = Image.new("RGB", img.size, "white")
        bg.paste(img, mask=img.split()[3])
        img = bg
    elif mode == "L":
        img = img.convert("RGB")
    if img.width > MAX_IMAGE_WIDTH:
        img = img.resize((MAX_IMAGE_WIDTH, round(img.height * MAX_IMAGE_WIDTH / img.width)), Image.LANCZOS)
    return img


# ---------------------------------------------------------------- layout A (2020-2023)
LIST_START = re.compile(r"^(\d+[.)]\s|[a-dA-D][.)]\s|[•\-–]\s|\(\w\)\s)")


def join_lines(lines):
    out = ""
    for line in lines:
        if not out:
            out = line
        elif LIST_START.match(line):
            out += "\n" + line
        else:
            out += " " + line
    return out.strip()


def parse_layout_a(stream):
    questions, q, state, opt = [], None, None, None
    for it in stream:
        if it["kind"] == "image":
            if q is not None and state in ("stem", "option"):
                # In these papers pictures always belong to the stem; a page break can put one after the first
                # option's text, so we note where it was seen but never treat it as an option image.
                q["_images"].append(dict(it, phase="stem", seenAfterOption=(state == "option")))
            continue
        t = it["text"]
        m = re.match(r"Ques No:\s*(\d+)", t)
        if m:
            q = dict(sourceNumber=int(m.group(1)), subject="", topic="", subTopic="", _stem=[], _opts={}, answer=None,
                     _images=[], page=it["page"])
            questions.append(q)
            state, opt = "meta", None
            rest = t[m.end():].strip()
            t = rest
            if not t:
                continue
        if q is None:
            continue
        if re.match(r"^Subject:", t):
            q["subject"] = t.split(":", 1)[1].strip()
            state = "meta"
        elif re.match(r"^Topic:", t):
            q["topic"] = t.split(":", 1)[1].strip()
        elif re.match(r"^Sub-?Topic:", t):
            q["subTopic"] = t.split(":", 1)[1].strip()
            state = "stem"
        elif re.match(r"^O([1-4]):", t):
            opt = int(t[1])
            q["_opts"].setdefault(opt, [])
            rest = t.split(":", 1)[1].strip()
            if rest:
                q["_opts"][opt].append(rest)
            state = "option"
        elif re.match(r"^Ans:", t):
            m2 = re.search(r"\d", t)
            q["answer"] = int(m2.group()) if m2 else None
            state = "done"
        elif state == "stem":
            q["_stem"].append(t)
        elif state == "option":
            q["_opts"][opt].append(t)
    for q in questions:
        q["stem"] = join_lines(q.pop("_stem"))
        opts = q.pop("_opts")
        q["options"] = [join_lines(opts.get(i, [])) for i in (1, 2, 3, 4)]
    return questions


# ---------------------------------------------------------------- answer matching (layout B)
def norm(t):
    return re.sub(r"[^a-z0-9]+", " ", t.lower()).strip()


def match_answer(answer_text, options, notes):
    """Index (1-4) of the option the answer text refers to; fuzzy matches are noted, failures return None."""
    target = norm(answer_text)
    normed = [norm(o) for o in options]
    for i, o in enumerate(normed):
        if o and o == target:
            return i + 1
    import difflib
    scored = sorted(((difflib.SequenceMatcher(None, target, o).ratio(), i + 1) for i, o in enumerate(normed)), reverse=True)
    if scored and scored[0][0] >= 0.85 and (len(scored) < 2 or scored[0][0] - scored[1][0] >= 0.05):
        notes.append(f"answer matched approximately ({scored[0][0]:.2f}): {answer_text!r} -> option {scored[0][1]}")
        return scored[0][1]
    return None


# ---------------------------------------------------------------- layout B (2024)
def parse_layout_b(stream):
    questions, q, state = [], None, None
    subject = topic = ""
    for it in stream:
        if it["kind"] == "image":
            if q is not None and state in ("stem", "option"):
                q["_images"].append(dict(it, phase="stem", seenAfterOption=(state == "option")))
            continue
        t = it["text"]
        m = re.match(r"Q\.\s*(\d+)\.?\s*(.*)", t)
        if m:
            q = dict(sourceNumber=int(m.group(1)), subject=subject, topic=topic, subTopic="", _stem=[m.group(2)] if m.group(2) else [],
                     _opts={}, answer=None, _answer_text=None, _images=[], page=it["page"], notes=[])
            questions.append(q)
            state = "stem"
            continue
        if q is None or state is None:
            # before the first question: subject headings and topics
            pass
        if re.match(r"^Topic:", t):
            topic = t.split(":", 1)[1].strip()
            state = None if state == "answer" else state
            if state is None:
                continue
        elif re.match(r"^Correct Answer:", t):
            if q is not None:
                q["_answer_text"] = t.split(":", 1)[1].strip()
                q["_answer_pos"] = (it["page"], it["y"])
                state = "answer"
            continue
        elif q is not None and state == "answer":
            if re.match(r"^[A-Za-z][A-Za-z &/\-]{1,40}$", t) and not re.match(r"^\d\.", t):
                subject = t  # a standalone line after an answer is the next subject heading
            elif (q["_answer_text"] is not None and not re.match(r"^Topic:", t)
                  and q["_answer_pos"][0] == it["page"] and 0 < it["y"] - q["_answer_pos"][1] <= 18):
                q["_answer_text"] += " " + t  # wrapped answer line (close under the answer, same page)
                q["_answer_pos"] = (it["page"], it["y"])
            continue
        elif re.match(r"^[A-Za-z][A-Za-z &/\-]{1,40}$", t) and state in (None, "answer") and not re.match(r"^\d\.", t):
            subject = t
            continue
        if q is None:
            continue
        mo = re.match(r"^([1-4])\.\s*(.*)", t)
        if mo and (state == "stem" or state == "option") and (int(mo.group(1)) - 1 == len(q["_opts"])):
            q["_opts"][int(mo.group(1))] = [mo.group(2)] if mo.group(2) else []
            state = "option"
        elif state == "stem":
            q["_stem"].append(t)
        elif state == "option":
            q["_opts"][max(q["_opts"])].append(t)
        # subject/topic changes apply to the next question
    for q in questions:
        q["stem"] = join_lines(q.pop("_stem"))
        opts = q.pop("_opts")
        q["options"] = [join_lines(opts.get(i, [])) for i in (1, 2, 3, 4)]
        text = q.pop("_answer_text")
        q.pop("_answer_pos", None)
        if text:
            text = re.sub(r"\s*\((?:incorrect|correct)[^)]*\)\s*$", "", text, flags=re.I)  # "(Incorrect Statement)" notes
        q["answer"] = match_answer(text, q["options"], q["notes"]) if text else None
        if text:
            q["answerText"] = text
    return questions


# ---------------------------------------------------------------- layout C (2025 recall)
def parse_layout_c(stream):
    questions, q, state, opt = [], None, None, None
    subject = ""
    for it in stream:
        if it["kind"] == "image":
            if q is not None and state in ("stem", "option"):
                q["_images"].append(dict(it, phase="stem", seenAfterOption=(state == "option")))
            continue
        t, bold = it["text"], it["bold"]
        m = re.match(r"^Q\.\s*(.*)", t)
        if m:
            q = dict(sourceNumber=len(questions) + 1, subject=subject, topic="", subTopic="", _stem=[m.group(1)],
                     _opts={}, answer=None, _images=[], page=it["page"], notes=[])
            questions.append(q)
            state, opt = "stem", None
            continue
        # Options are lettered A-D, a-d or numbered 1-4; each must be the next one in order.
        mo = re.match(r"^([A-Da-d1-4])[.)]\s*(.*)", t)
        marker = ("ABCD".find(mo.group(1).upper()) + 1 or int(mo.group(1))) if mo else 0
        if mo and q is not None and state in ("stem", "option") and marker == len(q["_opts"]) + 1:
            opt = marker
            q["_opts"][opt] = [mo.group(2)]
            if bold:
                q.setdefault("_bold", []).append(opt)
            state = "option"
            continue
        if bold and not mo and (q is None or state == "option" or state is None) and canonical_subject(t):
            subject = t  # a bold line that is exactly a subject name is a section heading
            state = None
            continue
        if q is None:
            continue
        if state == "stem":
            q["_stem"].append(t)
        elif state == "option":
            q["_opts"][opt].append(t)
            if bold and opt not in q.get("_bold", []):
                q.setdefault("_bold", []).append(opt)
    for q in questions:
        q["stem"] = join_lines(q.pop("_stem"))
        opts = q.pop("_opts")
        q["options"] = [join_lines(opts.get(i, [])) for i in (1, 2, 3, 4)]
        bold = sorted(set(q.pop("_bold", [])))
        if len(bold) == 1:
            q["answer"] = bold[0]
        elif bold:
            q["notes"].append(f"several bold options {bold}")
    return questions


PARSERS = {"A": parse_layout_a, "B": parse_layout_b, "C": parse_layout_c}


# ---------------------------------------------------------------- image handling and output
def load_overrides(paper_id):
    """overrides/<paper>.json: {"drop": ["<question id>#<n>", ...]} where n is the 1-based image order in that question.
    Used for explanation slides that are not distinguishable by layout; each was checked by eye in the contact sheets."""
    f = ROOT / "overrides" / f"{paper_id}.json"
    return json.loads(f.read_text()) if f.exists() else {"drop": []}


def save_images(doc, paper_id, questions, out_dir, report, cfg_layout):
    drop_set = set(load_overrides(paper_id)["drop"])
    img_dir = out_dir / "images"
    img_dir.mkdir(parents=True, exist_ok=True)
    seen = {}
    for q in questions:
        q["images"] = []
        for n, it in enumerate(q.pop("_images")):
            keep, reason = classify_image(it, cfg_layout == "C")
            if keep and f"{q['id']}#{n + 1}" in drop_set:
                keep, reason = False, "dropped by visual review (explanation slide)"
            if not keep:
                report["dropped"].append((q["id"], it["page"], it["w"], it["h"], reason))
                continue
            if it["xref"] in seen and seen[it["xref"]] != q["id"]:
                report["shared"].append((q["id"], seen[it["xref"]], it["xref"]))
            seen[it["xref"]] = q["id"]
            name = f"{q['id']}_{len(q['images']) + 1}.webp"
            img = image_to_webp(doc, it["xref"])
            img.save(img_dir / name, "WEBP", quality=85, method=6)
            data = (img_dir / name).read_bytes()
            q["images"].append(dict(n=n + 1, file=f"images/{name}", width=img.width, height=img.height,
                                    sha256=hashlib.sha256(data).hexdigest(), phase=it["phase"], page=it["page"]))
            report["kept"] += 1


def run(paper_id):
    cfg = PAPERS[paper_id]
    doc = fitz.open(PDF_DIR / cfg["file"])
    out_dir = OUT / paper_id
    out_dir.mkdir(parents=True, exist_ok=True)
    stream = read_stream(doc)
    questions = PARSERS[cfg["layout"]](stream)
    for q in questions:
        q["id"] = f"{paper_id.lower()}-q{q['sourceNumber']:03d}"
        q["examPaperId"] = paper_id
        q["source"] = dict(file=cfg["file"], page=q.pop("page"))
    report = dict(dropped=[], shared=[], kept=0)
    for q in questions:
        q["exclude"] = None
        opts = q["options"]
        if any(not o for o in opts):
            q["exclude"] = "missing option text"
        elif len(set(norm(o) for o in opts)) < 4:
            q["exclude"] = "two options have identical text in the source"
        elif q["answer"] not in (1, 2, 3, 4):
            q["exclude"] = "answer missing or matches no option"
    for q in questions:
        q["subjectRaw"] = q["subject"]
        canon = canonical_subject(q["subject"])
        q["subject"] = canon or ""
        q["subjectId"] = SUBJECT_IDS.get(canon, "")
        if q["subjectRaw"] and not canon:
            q.setdefault("notes", []).append(f"unknown subject {q['subjectRaw']!r}")
    save_images(doc, paper_id, questions, out_dir, report, cfg["layout"])
    (out_dir / "questions.json").write_text(json.dumps(questions, indent=1, ensure_ascii=False))
    write_report(paper_id, cfg, questions, report, out_dir)
    return questions


def write_report(paper_id, cfg, questions, report, out_dir):
    issues = []
    nums = [q["sourceNumber"] for q in questions]
    dup_nums = sorted({n for n in nums if nums.count(n) > 1})
    for q in questions:
        if any(not o for o in q["options"]):
            issues.append(f"{q['id']}: missing option text")
        if q["answer"] not in (1, 2, 3, 4):
            issues.append(f"{q['id']}: answer is {q['answer']!r}")
        if not q["stem"]:
            issues.append(f"{q['id']}: empty stem")
        if not q["subject"]:
            issues.append(f"{q['id']}: no subject")
        for note in q.get("notes", []):
            issues.append(f"{q['id']}: {note}")
    lines = [f"# {paper_id} extraction report", "",
             f"- source: {cfg['file']}",
             f"- questions parsed: {len(questions)} (numbers {min(nums, default=0)}-{max(nums, default=0)})",
             f"- duplicate question numbers: {dup_nums or 'none'}",
             f"- questions with an image: {sum(1 for q in questions if q['images'])}",
             f"- images kept: {report['kept']}, dropped: {len(report['dropped'])}",
             f"- excluded from import (source problems): {sum(1 for q in questions if q['exclude'])}", ""]
    if report["dropped"]:
        counts = {}
        for _, _, _, _, reason in report["dropped"]:
            counts[reason] = counts.get(reason, 0) + 1
        lines += ["## Dropped images by reason", *[f"- {r}: {c}" for r, c in counts.items()], ""]
    excluded = [f"{q['id']}: {q['exclude']}" for q in questions if q["exclude"]]
    lines += [f"## Excluded ({len(excluded)})", *(excluded or ["none"]), ""]
    lines += [f"## Problems ({len(issues)})", *(issues[:200] or ["none"])]
    (out_dir / "report.md").write_text("\n".join(lines) + "\n")
    print("\n".join(lines[:12]))


if __name__ == "__main__":
    for pid in sys.argv[1:] or ["NEETPG2022"]:
        run(pid)
