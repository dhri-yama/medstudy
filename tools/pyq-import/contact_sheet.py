#!/usr/bin/env python3
"""Build labelled contact sheets of the extracted images for visual review.
Usage: contact_sheet.py NEETPG2022 [per_sheet=12]   ->  out/<paper>/sheets/sheet_01.jpg ...
Also: contact_sheet.py NEETPG2022 dropped        ->  sheet of the images the filter dropped (to confirm they were not questions)."""
import json, sys
from pathlib import Path
from PIL import Image, ImageDraw
import fitz
sys.path.insert(0, str(Path(__file__).parent))
import pyq_extract as px

def sheet(items, path, cols=4, cell=300):
    rows = (len(items) + cols - 1) // cols
    canvas = Image.new("RGB", (cols * cell, rows * (cell + 22)), "white")
    d = ImageDraw.Draw(canvas)
    for i, (label, img) in enumerate(items):
        img = img.copy(); img.thumbnail((cell - 8, cell - 8))
        x, y = (i % cols) * cell, (i // cols) * (cell + 22)
        canvas.paste(img, (x + 4, y + 22 + (cell - img.height) // 2))
        d.text((x + 4, y + 4), label, fill="black")
    canvas.save(path, quality=80)

def main():
    paper, arg = sys.argv[1], (sys.argv[2] if len(sys.argv) > 2 else "12")
    out = px.OUT / paper
    (out / "sheets").mkdir(exist_ok=True)
    if arg == "dropped":
        doc = fitz.open(px.PDF_DIR / px.PAPERS[paper]["file"])
        items = []
        for it in px.read_stream(doc):
            if it["kind"] == "image":
                keep, reason = px.classify_image(it, px.PAPERS[paper]["layout"] == "C")
                if not keep and not reason.startswith(("logo", "header")):
                    items.append((f"p{it['page']} {it['w']}x{it['h']} x{it['bbox'][0]:.0f}", px.image_to_webp(doc, it["xref"])))
        for n in range(0, len(items), 20):
            sheet(items[n:n+20], out / "sheets" / f"dropped_{n//20+1:02d}.jpg", cols=5, cell=240)
        print(len(items), "dropped images ->", out / "sheets")
        return
    per = int(arg)
    qs = json.loads((out / "questions.json").read_text())
    items = []
    for q in qs:
        for im in q["images"]:
            items.append((f"{q['id'].split('-')[1]}#{im['n']} {im['width']}x{im['height']} {q['stem'][:30]!r}", Image.open(out / im["file"]).convert("RGB")))
    for n in range(0, len(items), per):
        sheet(items[n:n+per], out / "sheets" / f"sheet_{n//per+1:02d}.jpg")
    print(len(items), "images ->", len(range(0, len(items), per)), "sheets in", out / "sheets")
main()
