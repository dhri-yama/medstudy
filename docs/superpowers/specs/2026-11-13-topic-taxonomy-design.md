# Topic taxonomy and tagging: design

Status: DRAFT, awaiting approval of the approach and the topic list (Appendix A).

## Intent and why

Questions are grouped by subject only in practice. Topics exist, but they come from two unrelated sources and are
inconsistent, so a subject's Topics tab is thin or empty. The user wants topic-wise distinctions inside every
subject so a student can practise and track progress per topic.

Today:
- Question bank (`assets/<subject>.json`, `questions.json`): ~1,600 unique questions, only ~218 tagged. Topics come
  from MedMCQA's raw labels (`topics.json`, 141 topics, referenced by name). Duplicates ("Head & Neck" vs
  "Head and neck"), wrong subjects ("Proteins" under Social & Preventive Medicine), exam sources used as topics
  ("All India exam"), catch-alls ("Miscellaneous").
- Previous-year papers (`assets/pyq/*.json`, `pyq/index.json`): 1,505 questions, ~1,238 tagged with 266 PDF-derived
  topics (`pyq_<subject>_<slug>` ids), many near-duplicates ("Regional Anesthesia" / "Regional anaesthesia").
- The app already renders a Topics tab per subject (`topicSummaries` in `SubjectScreens.kt`, `Category.parentId ==
  subjectId`, `Question.topicIds`). The gap is data quality and coverage, not UI.

## Goals

- One curated, chapter-style topic list per subject (Appendix A), used by both the bank and the papers.
- Every question (bank and papers, ~2,950 unique) has exactly one primary topic from that list.
- Tagging is a rerunnable script in `tools/`, so new questions can be tagged the same way.
- Low-confidence tags are surfaced for review, not silently accepted.

## Non-goals

- No new UI beyond what the existing Topics tab needs to show the new list. A topic hierarchy (chapter > subtopic) is
  out of scope.
- No multi-topic tagging: one primary topic per question.
- No rewriting question text, answers or explanations.
- No changes to subjects themselves (the 20 current subjects stay).

## Anti-goals

- A taxonomy that is still inconsistent or still full of "Miscellaneous" buckets.
- Tags that look plausible but are wrong in bulk, with no way to find them.
- Losing a user's existing progress, bookmarks or plan blocks because topic ids changed.

## Constraints

- Offline app. Topics ship as bundled assets; no network lookup.
- Existing user data may reference old topic ids (`StudyEntities.kt` has a `topicId` on plan blocks) and attempts are
  keyed by question id. A migration or id-mapping step is needed so nothing breaks (see What's left to the builder).
- Question files are bundled assets; edits must keep them valid against `QuestionCatalogValidator`.

## Approaches considered

Topic list:
- A. Clean up the existing labels only: cheap, but still thin and uneven (Psychiatry would have 1 topic).
- **B. Curated standard list per subject, remap everything onto it (chosen).** Only option that gives consistent
  topics everywhere.
- C. Let a model discover topics bottom-up: uneven and unstable between runs.

Tagging:
- **LLM assigns each question to the fixed list, low-confidence flagged for review (chosen).**
- Keyword rules: weak, stems rarely contain the topic word.
- Manual: ~2,950 questions by hand, too slow.

Chosen by the user: B with LLM tagging.

## Design

1. `tools/topic-taxonomy/taxonomy.json`: source of truth. Per subject, a list of `{id, name}` where
   `id = topic_<subjectId>_<slug>`. Generated from Appendix A once approved.
2. Remap pass (script): for every already-tagged question (bank label or `pyq_*` id), propose a new topic by asking
   the model to choose from the fixed list, using the old label as a hint. Old label to new id is stored in a mapping
   file so it is reviewable and reusable.
3. Tagging pass (script): for each untagged question, send stem, options and correct answer plus the subject's topic
   list; model returns `{topicId, confidence}`. Batched per subject. Questions below a confidence threshold, or whose
   returned id is not in the list, go to `needs_review.csv` and are tagged only after review.
4. Write-back: bank questions get the new topic name in `topic_name`/`topic`; paper questions get `topicId`; a new
   `topics.json` (or equivalent) and `pyq/index.json` topic list are regenerated from `taxonomy.json`. Old topic
   entries are removed.
5. Spot check: print a random sample (about 10 per subject) with the chosen topic for the user to eyeball before the
   data is committed. Report per-subject topic counts and flag topics with fewer than about 5 questions.
6. App: load topics from the new list, keep existing ids working through the old-id mapping so plan blocks and any
   stored topic references resolve.

## Decided by the user

- Curated list per subject, then LLM tagging against that fixed list.

## My calls (the user can override)

- One primary topic per question.
- Topic ids use `topic_<subject>_<slug>`.
- Questions currently labelled "Unknown" subject (12) get a subject assigned by the same pass; the subject list does
  not change.
- Aim for 8 to 18 topics per subject; small subjects (Dental, Skin, Anaesthesia) get fewer.
- Review threshold: below 0.7 model confidence goes to `needs_review.csv`.

## What's left to the builder

- Exact old-id to new-id migration for stored plan blocks and any persisted topic references; check Room
  schema/migrations before changing ids.
- Whether to keep a single `topics.json` or fold bank and paper topics into one asset; follow whichever keeps the
  existing loaders (`MedMcqaRepository`, `PyqRepository`) simplest.
- Batching, prompting and retry details of the tagging script, and the model used.
- Updating `QuestionCatalogValidator` tests if they assume the old topic shape.

---

## Appendix A: draft topic list (to approve or edit)

**Anatomy** (14): General Anatomy & Osteology; Upper Limb; Lower Limb; Thorax; Abdomen; Pelvis & Perineum;
Head & Neck; Neuroanatomy; Cranial Nerves; General Embryology; Systemic Embryology; Histology; Genetics (Anatomy);
Back & Spine.

**Physiology** (13): General Physiology & Cell; Nerve & Muscle Physiology; Blood & Body Fluids; Cardiovascular System;
Respiratory System; Renal & Acid-Base; Gastrointestinal System; Endocrinology; Reproductive System;
Central Nervous System; Special Senses; Autonomic Nervous System; Exercise, Temperature & Special Topics.

**Biochemistry** (13): Amino Acids & Proteins; Enzymes; Carbohydrate Chemistry & Metabolism; Lipid Metabolism;
Amino Acid & Protein Metabolism; Nucleotide Metabolism; Vitamins & Minerals; Bioenergetics & Oxidative Phosphorylation;
Molecular Biology (Replication, Transcription, Translation); Genetics & Molecular Techniques; Heme, Liver & Special Tissues;
Nutrition; Cell Biology & Membrane Transport.

**Pathology** (14): General Pathology (Cell Injury, Inflammation, Repair); Hemodynamics; Neoplasia; Immunopathology;
Genetics & Paediatric Pathology; Haematology (RBC); Haematology (WBC & Lymphoma); Haemostasis & Transfusion;
Cardiovascular & Vascular; Respiratory; Gastrointestinal & Hepatobiliary; Renal & Urinary; Endocrine & Breast;
CNS, Bone, Skin & Female Genital.

**Pharmacology** (15): General Pharmacology; Autonomic Nervous System; Autacoids & NSAIDs; Cardiovascular Drugs;
Respiratory Drugs; Gastrointestinal Drugs; CNS Drugs; Endocrine Drugs; Antimicrobials (Antibacterial);
Antimycobacterial, Antifungal, Antiviral & Antiparasitic; Anticancer & Immunomodulators; Drugs & Blood;
Renal & Diuretics; Toxicology & Adverse Drug Effects; Miscellaneous (Vitamins, Chelators, Gout, Bone).

**Microbiology** (12): General Microbiology & Sterilisation; Immunology; Bacteriology (Gram Positive);
Bacteriology (Gram Negative); Other Bacteria (Spirochaetes, Mycobacteria, Rickettsia, Chlamydia); Virology; Mycology;
Parasitology (Protozoa); Parasitology (Helminths); Hospital Infection & Lab Diagnosis; Antimicrobial Resistance;
Clinical & Applied Microbiology.

**Forensic Medicine** (11): Forensic Identification; Thanatology & Postmortem Changes; Forensic Traumatology (Injuries);
Asphyxial Deaths; Firearms & Ballistics; Sexual Offences & Jurisprudence; Medical Jurisprudence & Legal Procedures;
Toxicology (General & Corrosives); Toxicology (Poisons: Plant, Animal, Drugs); Forensic Psychiatry & Miscellaneous;
Biological Materials & Evidence.

**Social & Preventive Medicine** (12): Concepts of Health & Disease; Epidemiology; Biostatistics; Screening;
Communicable Diseases; Non-Communicable Diseases; Nutrition & Health; Environment & Water; Vaccines & Immunisation;
National Health Programmes; Health Planning, Management & Education; Maternal, Child Health & Demography.

**Medicine** (14): Cardiology; Respiratory Medicine; Gastroenterology & Hepatology; Nephrology & Electrolytes;
Endocrinology & Diabetes; Haematology; Rheumatology & Immunology; Neurology; Infectious Diseases;
Oncology; Critical Care & Emergency; Toxicology & Environmental; Genetics & Nutrition; Dermatology & Miscellaneous.

**Surgery** (14): General Surgery & Principles; Trauma & Burns; Oesophagus & Stomach; Hepatobiliary & Pancreas;
Intestines, Appendix & Spleen; Colorectal & Anal; Breast; Thyroid, Parathyroid & Adrenal; Head & Neck;
Hernia & Abdominal Wall; Urology; Vascular Surgery; Cardiothoracic & Neurosurgery; Transplant, Plastic & Oncology.

**Gynaecology & Obstetrics** (13): Anatomy & Physiology of Reproduction; Normal Pregnancy & Antenatal Care;
Labour & Delivery; Abnormal Labour & Obstetric Procedures; Antepartum & Postpartum Haemorrhage;
Medical Disorders in Pregnancy; Hypertensive Disorders & Fetal Medicine; Puerperium & Neonate;
Menstrual Disorders & Endocrinology; Infertility & Contraception; Benign Gynaecological Conditions;
Gynaecological Oncology; Infections, Menopause & Miscellaneous.

**Pediatrics** (12): Growth & Development; Neonatology; Nutrition & Malnutrition; Immunisation; Infectious Diseases;
Respiratory; Cardiology; Gastroenterology & Hepatology; Neurology; Nephrology & Fluid-Electrolytes;
Genetics, Metabolic & Endocrine; Haematology, Oncology & Miscellaneous.

**Ophthalmology** (12): Anatomy & Physiology of Eye; Optics & Refraction; Lids, Lacrimal & Orbit; Conjunctiva;
Cornea & Sclera; Uveal Tract; Lens & Cataract; Glaucoma; Retina & Vitreous; Neuro-Ophthalmology & Visual Pathway;
Squint & Ocular Motility; Ocular Injuries & Community Ophthalmology.

**ENT** (10): Anatomy & Physiology of Ear; Diseases of External & Middle Ear; Inner Ear, Hearing & Vestibular;
Nose & Paranasal Sinuses; Pharynx & Tonsils; Larynx & Airway; Oral Cavity & Salivary Glands; Neck & Head Tumours;
Facial Nerve & Skull Base; Miscellaneous.

**Orthopaedics** (12): Fractures & General Trauma; Upper Limb Injuries; Lower Limb Injuries; Spine & Pelvis;
Bone & Joint Infections; Bone Tumours; Metabolic & Endocrine Bone Disease; Arthritis & Joint Disorders;
Paediatric Orthopaedics; Peripheral Nerve Injuries; Sports Injuries & Regional Conditions; Orthopaedic Management & Miscellaneous.

**Psychiatry** (12): Basics & Psychology; Schizophrenia & Psychotic Disorders; Mood Disorders; Anxiety, OCD & Stress-Related;
Somatoform & Dissociative Disorders; Substance Use Disorders; Personality Disorders; Organic Mental Disorders;
Child Psychiatry; Eating, Sleep & Sexual Disorders; Psychopharmacology & Treatment; Forensic Psychiatry & Miscellaneous.

**Radiology** (10): Basics & Imaging Physics; Radiation Protection & Radiotherapy; Chest Imaging; Gastrointestinal Imaging;
Genitourinary Imaging; Musculoskeletal Imaging; Neuroimaging & Head & Neck; Obstetric & Ultrasound Imaging;
Nuclear Medicine; Contrast, Interventional & Miscellaneous.

**Anaesthesia** (9): Pre-Anaesthetic Evaluation; Anaesthetic Equipment & Monitoring; Inhalational Agents;
Intravenous Agents; Muscle Relaxants & Reversal; Local & Regional Anaesthesia; Airway Management & Oxygen Therapy;
Resuscitation & Critical Care; Special Situations & Pain Medicine.

**Skin** (10): Introduction & Basics; Bacterial Infections; Viral Infections; Fungal Infections; Papulosquamous Disorders;
Vesiculobullous Disorders; Pigmentary Disorders; Hair, Nail & Appendage Disorders; STIs & Leprosy;
Systemic Diseases, Drugs & Miscellaneous.

**Dental** (8): Oral Anatomy & Tooth Development; Dental Caries & Periodontal Disease; Oral Pathology & Cysts;
Oral Tumours; Oral Manifestations of Systemic Disease; Dental Materials & Procedures; Orthodontics & Maxillofacial;
Oral Infections & Miscellaneous.
