# Question Content Spec

## Purpose

Define an authoring/import format for the demo question catalog that can later back Room and a versioned API. This is a content contract, not a commitment to build a backend now.

**Current scope:** use clearly labeled demo questions, explanations, and placeholder/demo images only. Do not import real NEET PG or INI-CET past questions, recalled exam content, or third-party question-bank material. Historical content sourcing and permissions are a separate future decision.

## TODO

- [ ] Agree on the identifiers for exams, subjects, and topics.
- [ ] Finalize the JSON schema and publish a `schemaVersion` policy.
- [ ] Add demo question fixtures that exercise text-only, image-based, long-stem, and answer-review states.
- [ ] Add a parser and validator that reports record ID plus field-level errors.
- [ ] Add validation for media files, checksums, dimensions, and accessibility descriptions.
- [ ] Map the validated content model to Room entities without storing image bytes in question rows.
- [ ] Add unit tests for valid and invalid fixtures, unknown schema versions, missing rationales, and broken media references.
- [ ] Document how demo catalog updates preserve bookmarks and attempts.
- [ ] Before any real historical content is introduced, verify rights for stems, options, answer keys, rationales, and every image.
- [ ] If an API is built, finalize endpoint shapes, authentication/entitlement rules, pagination, error format, and catalog/media versioning separately.

## Canonical JSON Format

Use UTF-8 JSON for the authoring/import bundle. The JSON file contains metadata and asset references, not base64-encoded image data. Keep field names stable and include `schemaVersion` so future importers and API clients can reject or migrate incompatible records safely.

```json
{
  "schemaVersion": "1.0",
  "id": "demo.question.sample-001",
  "examId": "NEET_PG",
  "examYear": null,
  "subjectId": "demo.subject.example",
  "topicIds": ["demo.topic.example"],
  "format": "SINGLE_BEST_ANSWER",
  "languageTag": "en-IN",
  "contentBlocks": [
    {
      "type": "text",
      "text": "DEMO ONLY: Replace this placeholder with an authored sample question."
    },
    {
      "type": "image",
      "assetId": "demo.image.sample-001",
      "altText": "Demo placeholder image used to test image rendering.",
      "caption": "Demo image"
    }
  ],
  "options": [
    {
      "id": "option-a",
      "label": "A",
      "position": 0,
      "contentBlocks": [{ "type": "text", "text": "Demo option A" }]
    },
    {
      "id": "option-b",
      "label": "B",
      "position": 1,
      "contentBlocks": [{ "type": "text", "text": "Demo option B" }]
    },
    {
      "id": "option-c",
      "label": "C",
      "position": 2,
      "contentBlocks": [{ "type": "text", "text": "Demo option C" }]
    },
    {
      "id": "option-d",
      "label": "D",
      "position": 3,
      "contentBlocks": [{ "type": "text", "text": "Demo option D" }]
    }
  ],
  "answerKey": {
    "correctOptionIds": ["option-b"],
    "explanation": "DEMO ONLY: Add the reviewed explanation for this sample.",
    "optionRationales": [
      { "optionId": "option-a", "text": "Demo rationale for why option A is not correct." },
      { "optionId": "option-b", "text": "Demo rationale for why option B is correct." },
      { "optionId": "option-c", "text": "Demo rationale for why option C is not correct." },
      { "optionId": "option-d", "text": "Demo rationale for why option D is not correct." }
    ]
  },
  "references": [],
  "provenance": {
    "contentKind": "DEMO",
    "sourceName": "MedStudy demo catalog",
    "sourceReference": null,
    "rightsStatus": "DEMO_ONLY",
    "attribution": null
  },
  "review": {
    "status": "DEMO_NOT_CLINICALLY_REVIEWED",
    "reviewedAt": null
  },
  "revision": 1
}
```

This is a structural example only. Its placeholder content must never be presented as a genuine past-paper question or relied on as medical advice.

## Field Contract

### Question

- `schemaVersion`: version of the import/API representation.
- `id`: immutable, globally unique question ID. Do not encode mutable question text in the ID.
- `examId`: stable exam key, initially `NEET_PG` or `INI_CET`; demo-only questions may use a dedicated demo exam key if they are not intended to imply exam provenance.
- `examYear`: nullable integer. Use `null` for a demo/original question not associated with a specific exam sitting.
- `subjectId`, `topicIds`: stable references into the category catalog. Support more than one topic when a question genuinely spans topics.
- `format`: initially `SINGLE_BEST_ANSWER`; keep it an enum so formats such as multi-select can be added later without changing the meaning of this one.
- `languageTag`: BCP 47 language tag, such as `en-IN`.
- `contentBlocks`: ordered question stem blocks. Text and image blocks may be interleaved.
- `options`: ordered answer options with stable IDs and visible labels. Use the same content-block structure if an option contains an image.
- `answerKey`: correct option ID set, overall explanation, and a rationale for every option.
- `references`: citations supporting the answer and explanation. Demo items may leave this empty but must remain marked demo and not clinically reviewed.
- `provenance`: origin and rights/attribution metadata. Never label demo content as official or historical.
- `review`: review status and date. For future real medical content, add qualified reviewer identity/role and require review before publication.
- `revision`: positive integer incremented when published content changes. Preserve attempt history against the stable question ID.

### Content Blocks and Media

Text block:

```json
{ "type": "text", "text": "A paragraph or question-stem segment." }
```

Image block:

```json
{
  "type": "image",
  "assetId": "demo.image.sample-001",
  "altText": "Short meaningful description for screen readers.",
  "caption": "Optional visible caption"
}
```

Store media metadata separately from question JSON or in a catalog-level `assets` manifest:

```json
{
  "id": "demo.image.sample-001",
  "path": "media/demo/sample-001.webp",
  "mimeType": "image/webp",
  "sha256": "REPLACE_WITH_COMPUTED_CHECKSUM",
  "widthPx": 1200,
  "heightPx": 800,
  "contentKind": "DEMO",
  "rightsStatus": "DEMO_ONLY"
}
```

Use relative packaged paths in the offline bundle and stable asset IDs in question records. A future API may return a URL plus checksum instead of a local path. Verify that image references resolve, files decode, aspect ratio is preserved, and meaningful images have accessibility descriptions. Do not put image bytes in JSON or Room question rows.

### Answer and Rationale Rules

- Correctness is defined only by `answerKey.correctOptionIds`; do not duplicate it as an `isCorrect` property on each option.
- For `SINGLE_BEST_ANSWER`, require exactly one correct option ID. Future multi-select formats may allow multiple IDs.
- Require exactly one `optionRationales` entry per option ID, with no duplicate or unknown option IDs.
- Store rationale text separately per option. The app displays the overall explanation and then the rationale for each option after answer submission or Show Answer.
- A future API should not return `answerKey` before the learner submits or requests Show Answer. The offline demo bundle may contain answer keys locally; do not treat client-side answer keys as confidential or sufficient for enforcing a future paywall.

## Suggested Room Model

Normalize the imported representation so relations can be queried and migrated independently:

- `ExamEntity`: `id`, display name, active flag.
- `CategoryEntity`: `id`, `parentId`, display name, sort order.
- `QuestionEntity`: stable ID, exam/year, subject, format, language, revision, provenance/review status.
- `QuestionContentBlockEntity`: question ID, position, type, text or asset ID, alt text, caption.
- `QuestionOptionEntity`: option ID, question ID, label, position.
- `OptionContentBlockEntity`: option ID, position, type and text/media reference.
- `QuestionAnswerKeyEntity`: question ID, option ID. This relation supports one or multiple correct options.
- `QuestionExplanationEntity`: question ID and overall explanation.
- `OptionRationaleEntity`: question ID, option ID, rationale text.
- `MediaAssetEntity`: stable ID, packaged path/cache key, MIME type, dimensions, checksum, provenance/rights status.
- `QuestionReferenceEntity`: question ID, citation, URL/identifier, accessed or reviewed date.
- `QuestionAttemptEntity`: attempt ID, question ID, selected option IDs, result (`CORRECT`, `INCORRECT`, `UNANSWERED`), completion instant, local study date/time zone, and practice context.
- `BookmarkEntity`: question ID and saved timestamp.

Room rows hold metadata and references, not binary media. Catalog refreshes must not delete attempts or bookmarks. Use database migrations when the persisted schema changes; `schemaVersion` applies to the content/API contract and is distinct from Room's database version.

## Future API Sketch

These routes are suggestions for later design, not MVP requirements:

- `GET /v1/catalog?sinceVersion={version}`: catalog version, schema version, category/exam metadata, and changed question/media IDs.
- `GET /v1/questions?examId={id}&year={year}&subjectId={id}&topicId={id}&cursor={cursor}`: paginated question content without answer keys when in assessment mode.
- `GET /v1/questions/{questionId}`: one question, options, and media metadata.
- `POST /v1/questions/{questionId}/answer`: submit selected option IDs and receive correctness plus explanations, subject to the product's assessment rules.
- `POST /v1/attempts/sync`: future authenticated sync of offline attempt events using stable attempt IDs and idempotency keys.

Use JSON over HTTPS, stable IDs, explicit schema/catalog versions, cursor pagination, and structured field-level errors. Media should be served separately with MIME type and checksum metadata. Keep API DTOs separate from Room entities and map both to the same domain model, so a backend does not leak into Compose screens. Finalize authentication, entitlements, caching, retry, and conflict rules only when a backend is planned.

## Publication and Safety Checklist

- Demo records are visibly identified as demo in the development experience and excluded from claims that the catalog contains actual past papers.
- Every question's answer key references existing options; every option has exactly one rationale.
- Every image asset exists, decodes, has a checksum, and has alt text when meaningful.
- Real medical content, when added later, has source citations and qualified review recorded.
- Real historical questions and images are not published until redistribution rights are verified for the intended app, territory, duration, offline caching, and paid/free use.
