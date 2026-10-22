# MedStudy UI design guide (for agents)

Read this before touching any UI. It is the source of truth for how MedStudy should look and feel.
Reference image: [docs/design/reference-screens.png](docs/design/reference-screens.png) (three phone screens: home, course detail, study group).

**Status:** partly implemented.
- Done: tokens and shared components in `core/designsystem` (`StudyTokens.kt`, `MedStudyTheme.kt`, `StudyComponents.kt`), the Today screen, the Practice mode grid, and the bottom bar.
- Inherits the new theme but not yet redesigned: Subjects, Progress, Me, Onboarding, and the question screen (its correct-answer colors were updated).
- Missing components from section 4: segmented control, accordion row, hero card, bottom-right tab variant.
New and changed screens follow this document; migrate old screens when you touch them.
- Font: system sans is used for now (Plus Jakarta Sans is not loaded yet).

## 1. Personality

Friendly, colorful, calm. A study app that feels like a well-made planner, not a medical textbook or a dashboard.
- Soft pastel cards, one per subject, each with its own color. Color means "which subject", never decoration.
- Big rounded shapes, generous space, almost no borders or shadows.
- One confident accent (indigo) for actions and progress. Everything else is ink on pastel or white.
- Short copy. Numbers and progress up front ("2 of 10 questions"), explanations below.

## 2. What the reference shows, and how it maps to MedStudy

| Reference element | Use in MedStudy |
|---|---|
| Home: greeting, date, bell | Today header: "Hey, {name}", date, streak chip (no bell, there are no notifications) |
| "Continue learning" large pink card with progress bar | Continue card: today's plan topic or most-due topic, progress = answered today / daily goal |
| Two half-visible subject cards (Biology, English) | Horizontal row of subject cards with question count and accuracy |
| Study group avatar blobs | Not used (no social features). Do not add people or avatars |
| Course detail: hero card with illustration | Subject detail hero: subject gradient, accuracy stat, optional illustration slot |
| Segmented "Lesson / Assignment" | Segmented "Topics / Mistakes" and "Overview / Calendar / Plan" |
| Accordion lessons with check / chevron | Topic rows: check = mastered, chevron = expandable with progress and a Practice button |
| "Today's task" white rows with indigo "Details" pill | Today's plan rows with indigo "Start" pill |
| "Live" card | "Due now" card: chip + topic + recall percent + "Review due topics" |
| "Upcoming discussion" cards with bottom-right tab | Practice mode cards (Daily set, Review due, Mistakes, Saved) |
| Tutor, host, "17 going", live classes, discussion requests | Not in the product. Do not invent these |

## 3. Design tokens

### 3.1 Color

| Token | Hex | Use |
|---|---|---|
| `canvas` | `#F6F6F8` | App background (light gray, not white) |
| `surface` | `#FFFFFF` | Rows, inputs, bottom bar, sheets |
| `ink` | `#14142B` | Headings and primary text |
| `body` | `#3B3F51` | Running text |
| `muted` | `#5F6675` | Secondary text (about 5:1 on canvas; do not go lighter for small text) |
| `hairline` | `#E6E7EC` | Dividers, outlines when a border is unavoidable |
| `accent` | `#5B5BD6` | Primary buttons, active nav, progress fill, checks, links |
| `accent-soft` | `#D9D9F5` | Progress track, selected-row tint |
| `on-accent` | `#FFFFFF` | Text on accent (about 5.3:1) |
| `success` | `#167A61` / tint `#DDF3EA` | Correct answers, mastered |
| `error` | `#B3261E` / tint `#FBE4E2` | Wrong answers, errors |
| `warning` | `#9A6700` / tint `#FFF1CC` | Due / attention chips |

Rules: ink text on every pastel card. Accent is the only saturated color outside subject cards.
Never use black filled buttons.

### 3.2 Subject palette (vertical two-stop gradient, top to bottom)

| Name | Top | Bottom | Notes |
|---|---|---|---|
| pink | `#E9569F` | `#F59AC5` | |
| blue | `#8EC7F7` | `#C3E2FB` | |
| green | `#4FC384` | `#8FE0AE` | |
| yellow | `#F5C93B` | `#F9DE72` | |
| sky | `#A5D3F6` | `#CDE8FB` | Also used for the "Due now" card |
| lavender | `#B9A7F0` | `#D8CCF8` | |
| peach | `#F7A66B` | `#FAC9A0` | |
| teal | `#5FCFC7` | `#9EE4DF` | |

Assign by the subject's index in the catalog sorted by position, then name (`SubjectSummary.colorIndex`, `% 8`). Catalog `position` values are all 0 today, so name order decides.
The same subject has the same color on every screen. Adding a subject can shift later subjects' colors.
Fixed assignments: Practice modes use pink (Daily set), green (Review due), yellow (Mistakes), blue (Subjects), lavender (Saved); the "Due now" card uses sky.
Notch tab: a 35% white overlay on the card (top tabs).
Ink text on all of these is at least 5:1. Check contrast again if you change a hex.

### 3.3 Typography

Font: **Plus Jakarta Sans** (open license; load with `androidx.compose.ui:ui-text-google-fonts`, fall back to the system sans). Weights 400, 500, 600 only.

| Role | Size / line | Weight | Example |
|---|---|---|---|
| Screen title | 24 / 30 sp | 600 | "Practice" |
| Card title | 22 / 28 sp | 500 | "Chemistry" |
| Section title | 18 / 24 sp | 600 | "Continue learning" |
| Body | 14 / 20 sp | 400 | Explanations, descriptions |
| Row title | 15 / 20 sp | 600 | "Physics Assignment" |
| Caption | 12 / 16 sp | 400 | "15 classes · 3 left" |
| Button label | 13 / 16 sp | 600 | "Details" |
| Nav label | 11 / 14 sp | 500 | "Home" |

Sentence case everywhere. Respect system font scaling; never fix text height in dp.

### 3.4 Spacing, shape, elevation, motion
- 4dp grid. Screen side padding 20dp. Card padding 16dp. Gaps between cards 12dp, between sections 24dp.
- Radii: cards 24dp, hero 28dp, rows 18dp, inputs 16dp, buttons / chips / segmented control: full pill, icon buttons: circle (44-48dp).
- Elevation: essentially flat. White rows get a soft shadow (y 2dp, blur 8dp, 4% ink). Colored cards have none.
- Touch targets 48dp minimum.
- Motion: 150-250ms ease-out for expand/collapse and progress fill. Honor reduced motion. No decorative animation.
- Icons: Material Symbols outlined, 20-24dp, ink or muted. Active nav icon is accent.

## 4. Components

**Notched card (signature shape).** A 24dp rounded card whose top-right corner is a lighter block (35% white) with 16dp inverted-corner fillets
where it meets the top and right edges. It is not a hole: the card stays one solid shape. The tab holds a small label (a count, "Plan", "Due").
Tab is 38% of the card width and 44dp tall. Implemented as `NotchedCard` with a private path `Shape`. Keep titles out of the tab area:
on wide cards limit the title to about 60% width; on narrow cards add a 28dp spacer above the title. Not built yet: bottom-right action tab.

**Continue card.** Notched card, subject gradient. Title 22sp, subtitle caption, tab = "Due" or "Plan". Bottom: progress bar (accent fill on a white 40% track, 8dp tall, full radius) and caption "2 of 10 questions".

**Subject card.** Half-width notched card: name, "n questions", accuracy line, thin progress bar. Used in a horizontal row on Today and a 2-column grid in Subjects.

**Task row.** White surface, 18dp radius, 12dp padding. Leading 40dp circular outlined icon, title + caption, trailing accent pill button ("Start") with 32dp height.

**Pill button.** Primary = accent fill, white label. Secondary = surface fill, ink label, hairline outline. Height 40dp (small 32dp). Never black.

**Segmented control.** Light track (`#EBEBF0`), selected segment is a white pill with an optional leading icon, height 44dp.

**Accordion row.** White, 18dp radius. Title + caption ("5 questions · 60% correct"). Trailing: filled accent check (mastered) or outlined chevron circle. Expanded: progress bar, "n left" caption, small Practice button.

**Chip.** Pill, 24dp tall, caption text. "Due" = warning tint, "Streak" = white with flame icon, "Live"-style state is not used.

**Bottom navigation.** White bar, 4 items: Today, Practice, Progress, Me. Icon above label. Active = accent icon + label, inactive = muted. No indicator pill. Hidden during a practice session.

**Hero card.** 28dp, subject gradient, circular white back button top-left, title 28sp, one stat (trophy icon + "62% correct"), right side reserved for an optional illustration (empty by default).

**Option row (question screen).** White, 16dp radius, radio at left. Selected = accent outline + `accent-soft` tint. After checking: correct = success tint with a "Correct" label, wrong selection = error tint with "Your answer".

## 5. Screens

**Today.** Header (initial-letter circle in the subject-agnostic `accent-soft`, "Hey, {name}", date, streak chip) > "Continue learning" card >
subject cards row > "Today's plan" task rows > "Due now" card (sky gradient, warning chip, recall percent, "Review due topics" button).
Data: `answeredToday`, goal, streak, today's plan block, due areas.

**Practice.** Title, then a 2-column grid of notched cards: Daily set (pink), Review due (green), Mistakes (yellow), Browse by subject (blue), Saved (sky).
Each shows a count in the tab chip. A card with nothing to do is dimmed and not clickable.

**Subjects.** Pill search field, then a 2-column grid of subject cards. Tap opens Subject detail.

**Subject detail.** Hero card > segmented "Topics / Mistakes" > accordion list of topics. Practice buttons start a session.

**Progress.** Title, segmented "Overview / Calendar / Plan". Calendar sits on a white 24dp card; days with activity get an accent dot and the count.
Plan uses task rows. Focus areas use subject-colored chips. Schedule preferences collapse into one white row.

**Question screen.** No bottom bar. Top: circular back button, a thin accent progress bar ("3 of 10"), bookmark icon button. Question text 18sp.
Option rows. Sticky bottom pill button "Check answer" (accent), then "Next" after the review. Explanation in a white card under the options.

**Me.** Large initial-letter avatar, name, exam chip. White rows for Profile, Backup and sync, Question overlay. Sign-in sits in a white card.

**Onboarding.** Pastel hero, one question per step, pill option chips, one accent button. Keep to three steps.

## 6. Do and do not

Do:
- Use the subject palette consistently per subject.
- Keep one primary action per screen, as an accent pill.
- Put progress (numbers and bars) before prose.
- Use white rows on the gray canvas to separate content, not borders.

Do not:
- Add black buttons, heavy shadows, outlined cards nested in cards, or more than the two-stop pastel gradient.
- Put white text on a pastel card.
- Invent tutors, groups, live classes, 3D art, photos, or emoji. Features need a product decision first.
- Hard-code colors or sizes in screens. Read tokens from the theme.

## 7. Compose implementation notes

- Tokens live in `core/designsystem/MedStudyTheme.kt`. Extend it with a `StudyColors` holder exposed through a `CompositionLocal`
  (`subjectGradients`, `accent`, `accentSoft`, `success`/`error`/`warning` tints, `canvas`). Keep `MaterialTheme` color roles in sync
  (`primary = accent`, `background = canvas`, `surface = white`) so Material components inherit the right defaults.
- Add reusable components to `core/designsystem`: `NotchedCard` (custom `Shape`), `StudyProgressBar`, `PillButton`, `TaskRow`,
  `SegmentedTabs`, `AccordionRow`, `StudyChip`, `StudyBottomBar`. Feature modules use these and never re-style Material primitives inline.
- Gradients: `Brush.verticalGradient(top, bottom)` from the subject palette. No other gradients.
- Fonts: Google Fonts provider with a system fallback; no bundled font files for now.
- Screens to migrate, in priority order: `TodayScreen.kt`, `PracticeHomeScreen.kt`, `QuestionPracticeScreen.kt`, `CategoriesScreen.kt`,
  `ProgressScreen.kt`, `MeScreen.kt`, `Onboarding.kt`, the nav bar in `StudyApp.kt`.
- Verify visually on the emulator at 411dp wide and at 1.3x font scale. Check contrast on every new text-on-color pair (4.5:1 for body text).

## 8. Accessibility checklist
- 4.5:1 contrast for text under 18sp, 3:1 for large text and icons.
- Color is never the only signal: pair status colors with a word or icon (Correct, Due, Mastered).
- Content descriptions on icon-only buttons (back, bookmark, streak).
- 48dp touch targets; focus order follows reading order; text scales with system settings.
- Progress bars expose a text value ("2 of 10 questions") to screen readers.

## 9. Open questions
- Dark theme is not designed yet. Do not add one without a palette review.
- Whether subjects get illustrations (the reference uses a 3D virus on the Biology hero). Until decided, the hero's right side stays empty.
- Real photos or avatars for the user. Until decided, use an initial-letter circle.
