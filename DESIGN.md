# MedStudy UI design guide (for agents)

Read this before touching any UI. It is the source of truth for how MedStudy should look and feel.
Reference image: [docs/design/reference-screens.png](docs/design/reference-screens.png) (three phone screens: home, course detail, study group).

**Status:** implemented across the app.
- Tokens and components: `core/designsystem` (`StudyTokens.kt`, `MedStudyTheme.kt`, `StudyComponents.kt`).
- Screens built to this guide: Today, Practice, Subjects, Subject detail, Question screen, Progress (Overview / Calendar / Plan), Me, Saved, Onboarding, floating bottom bar.
- Not built: bottom-right notch tab, subject illustrations, dark theme.
- Font: system sans for now (Plus Jakarta Sans is not loaded yet).

## 1. Personality

Friendly, colorful, calm. A study app that feels like a well-made planner, not a medical textbook or a dashboard.
- Soft pastel cards, one per subject, each with its own color. Color means "which subject", never decoration.
- Big rounded shapes, generous space, almost no borders or shadows.
- One neutral action color (deep ink) for buttons, active nav and progress fill, so it sits with every pastel. Everything else is ink on pastel or white.
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
| "Today's task" white rows with indigo "Details" pill | Today's plan rows with an ink "Start" pill |
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
| `accent` | `#14142B` | The action color: primary buttons, active nav pill, progress fill, selected pills, checks |
| `accent-soft` | `#E6E6EE` | Selected-row tint, avatar fill |
| `on-accent` | `#FFFFFF` | Text on accent (about 16:1) |
| `track` | `#EBEBF0` | Progress track on white/gray, segmented control track |
| `success` | `#167A61` / tint `#DDF3EA` | Correct answers, mastered |
| `error` | `#B3261E` / tint `#FBE4E2` | Wrong answers, errors |
| `warning` | `#9A6700` / tint `#FFF1CC` | Due / attention chips |

Rules: ink text on every pastel card. Outside subject cards there are no saturated colors except the semantic ones (success, error, warning).
Progress bars on white or gray: accent fill on a `track` background. On a pastel card: the card's `deep` shade on a 55% white track. Do not use blue or indigo as a global accent.

### 3.2 Subject palette (vertical two-stop gradient, top to bottom)

| Name | Top | Bottom | Notes |
|---|---|---|---|
| pink | `#E9569F` | `#F59AC5` | deep `#A3226B` |
| blue | `#8EC7F7` | `#C3E2FB` | deep `#1F5F99` |
| green | `#4FC384` | `#8FE0AE` | deep `#12744A` |
| yellow | `#F5C93B` | `#F9DE72` | deep `#8A6200` |
| sky | `#A5D3F6` | `#CDE8FB` | deep `#2B6A9C`. Also used for the "Due now" card |
| lavender | `#B9A7F0` | `#D8CCF8` | deep `#6B3FA0` |
| peach | `#F7A66B` | `#FAC9A0` | deep `#A8450B` |
| teal | `#5FCFC7` | `#9EE4DF` | deep `#0F766E` |

**Deep shade.** Each subject also has a `deep` color (the last column): a darker shade of the same hue. Inside a subject-colored card
(Continue card, subject cards, Subject detail header, "Due now" card), the progress bar fill and any primary button use that card's `deep`,
with a white 55% track and white label. White on every deep shade is at least 5:1.
On white or gray surfaces buttons and bars stay ink (`accent`). Implemented as `SubjectGradient.deep` and `PillButton(containerColor = ...)`.

Assign by the subject's index in the catalog sorted by position, then name (`SubjectSummary.colorIndex`, `% 8`). Catalog `position` values are all 0 today, so name order decides.
The same subject has the same color on every screen. Adding a subject can shift later subjects' colors.
Fixed assignments: Practice modes use pink (Daily set), green (Review due), yellow (Mistakes), blue (Subjects), lavender (Saved); the "Due now" card uses sky.
Previous year papers (Practice, between the mode cards and Subjects): one notched card per paper, colour by year, tab = question count, footer "Recall · x of y done".
Notch tab: a 35% white overlay on the card (top tabs).
Ink text on all of these is at least 5:1. Check contrast again if you change a hex.

### 3.3 Typography

Page and section headings: **Newsreader** (SIL Open Font License, bundled as `core/designsystem/src/main/res/font/newsreader.ttf`, licence in `THIRD_PARTY_LICENSES/`), semi-bold, in the deep indigo `heading` token (`#2B2063`) for page titles and section headings only (the `PageTitleStyle` and `SectionTitleStyle` text styles). The Practice and Plan page headers (greeting and two-line title) use **Poppins** (OFL, `THIRD_PARTY_LICENSES/Poppins-OFL.txt`) via `TabTitleStyle` (30 / 38 sp, 600, ink) and `TabGreetingStyle`. Cards, buttons, body text and every Material text style stay on the system sans. Weights 400, 500, 600 only.

| Role | Size / line | Weight | Example |
|---|---|---|---|
| Screen title | 27 / 33 sp | 600 (Newsreader, `PageTitleStyle`) | "Practice" |
| Card title | 22 / 28 sp | 500 (system sans) | "Daily set" |
| Section title | 21 / 27 sp | 600 (Newsreader, `SectionTitleStyle`) | "Today's plan" |
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
- Motion: feedback on something the user did, never decoration, and never something that has to finish before the next tap. Tokens live in `StudyMotion`: 120ms for press and option select, 200ms for the answer reveal and fades, 250ms ease-out for progress fill, number changes and screen enter. Pressables shrink to 0.97 while held. The only bounce is a single pop on the bookmark toggle and on the streak flame when the streak grows (larger, with a haptic, on day 7, 30, 100 and 365). No entrance staggers, shakes, confetti or looping animation. With the system "Remove animations" setting on, movement is skipped and only colour changes remain.
- Icons: Material Symbols outlined, 20-24dp, ink or muted. Active nav icon is accent.

## 4. Components

**Notched card (signature shape).** A 24dp rounded card whose top-right corner is a lighter block (35% white) with 16dp inverted-corner fillets
where it meets the top and right edges. It is not a hole: the card stays one solid shape. The tab holds a small label (a count, "Plan", "Due").
Tab is 38% of the card width and 44dp tall. Implemented as `NotchedCard` with a private path `Shape`. Keep titles out of the tab area:
on wide cards limit the title to about 60% width; on narrow cards add a 28dp spacer above the title. Not built yet: bottom-right action tab.

**Continue card.** Notched card, subject gradient. Title 22sp, subtitle caption, tab = "Due" or "Plan". Bottom: progress bar (the card's deep shade on a white 55% track, 8dp tall, full radius) and caption "2 of 10 questions".

**Subject card.** Half-width notched card: name, "n questions", accuracy line, thin progress bar. Used in a horizontal row on Today and a 2-column grid in Subjects.

**Task row.** White surface, 18dp radius, 12dp padding. Leading 40dp circular outlined icon, title + caption, trailing accent pill button ("Start") with 32dp height.

**Pill button.** Primary = accent (ink) fill, white label. Secondary = surface fill, ink label, hairline outline. Height 44dp (small 32dp).

**Top tabs (`SegmentedTabs`).** Plain Poppins labels on a hairline, left-aligned, 44dp tall: the selected label is ink and semi-bold with a 3dp ink line under it that slides to the chosen tab; the others are muted.

**Accordion row.** White, 18dp radius. Title + caption ("5 questions · 60% correct"). Trailing: filled accent check (mastered) or outlined chevron circle. Expanded: progress bar, "n left" caption, small Practice button.

**Chip.** Pill, 24dp tall, caption text. "Due" = warning tint, "Streak" = white with flame icon, "Live"-style state is not used.

**Bottom navigation.** A floating near-black pill (accent ink, 24dp side margin, soft shadow) with four icons: Today, Practice, Progress, Me.
The active item is a white 54dp circle with an ink icon; the other icons are white on the black. No text labels (each item's label is its accessibility description).
Hidden during a practice session. Component: `StudyBottomBar`.

**Filter pill.** `FilterPill`: selected = accent fill with white label, otherwise white with a hairline. Used for subjects, weekdays, exam choice.

**Text field.** `StudyTextField`: 16dp radius, white fill, hairline border, ink border when focused.

**Card.** `StudyCard`: white, 24dp radius, 16dp padding, groups settings and form fields on the gray canvas.

**Hero card.** 28dp, subject gradient, circular white back button top-left, title 28sp, one stat (trophy icon + "62% correct"), right side reserved for an optional illustration (empty by default).

**Option row (question screen).** White, 16dp radius, a lettered circle (A-D) at left. Selected = accent outline + `accent-soft` tint with a filled ink letter circle. After checking: correct = success tint with a "Correct" label, wrong selection = error tint with "Your answer".

## 5. Screens

**Today.** Header (initial-letter avatar, "Hey, {name}", date, streak chip) > Continue notched card (no heading) >
day pills (no heading) > the selected day's plan > "Quick practice" > Subjects row > "Due now" card.
- Continue card: at least 236dp tall with translucent decorative circles. Top: title, subject, a white chip with "10 questions · 30 min". Bottom: a 48sp count of questions answered today
  ("3 / 10 questions today") above the deep-shade progress bar. Tab = "Plan", "Due" or "Today". Tapping it opens that topic.
- This week: seven tall stadium-shaped day pills for Monday to Sunday of the current week. Only today is colored: it uses the Continue card's subject gradient.
  Every other day is a white pill with a hairline border. A day you tap becomes solid ink with white text; if that is today it keeps its color and gains a 2dp ink ring (today is selected by default).
  A small dot above the day name marks a day with answers (solid) or a scheduled plan (faded).
- Plan section: "Today's plan" or "Plan for Fri, 23 Oct", driven by the selected day. Plan rows show a Start pill only for today.
- Quick practice: two notched cards, each at least 184dp tall, "Quick 5" (five fresh questions, teal) and "Mistakes" (count in the tab, yellow, dimmed when 0).
  The 22sp title sits near the top and the description at the bottom, with a soft translucent circle in the bottom-right corner.
- Subjects row: cards 268dp wide and at least 192dp tall; the name and question count sit at the top, the progress bar and accuracy at the bottom, with the same corner circle.
  (The Subjects grid screen keeps the compact version of the card.)
- Tapping a subject card opens Subject detail.

**Practice.** One scrolling page, no sub-menu. Header: title and subtitle, plus a white "Saved · n" pill (bookmark icon, live count) that opens Saved.
- Daily set hero: a pink notched card, at least 208dp tall, title and "n fresh questions · about m min" at the top, a deep-pink "Start" pill at the bottom left, a large translucent circle, count in the tab.
- Review due (green) and Mistakes (yellow): two notched cards side by side, at least 160dp tall, title near the top and description at the bottom, count in the tab, small corner circle. A card with nothing to do is dimmed and not tappable.
- Subjects: a "Subjects" heading, a white search field with the hint inside, then every subject as a 2-column grid of subject cards (at least 184dp tall, name and count at the top, progress bar and accuracy at the bottom).
  Tapping a subject opens Subject detail; Back returns to Practice. There is no separate Subjects screen.

**Subject detail.** Hero card (back button, name, accuracy and answered chips, progress bar, "Practice all n") > top tabs "Topics / Mistakes" >
a plain white card per topic: the name (with a green "Mastered" chip when earned), a progress bar in the subject's deep shade on a light track, "n questions · x% correct", and a small deep-shade "Practice" pill on the right.
No chevrons or expand arrows. Mistakes lists missed questions with a "Practice n mistakes" button.

**Arc header (Practice, Progress, Me).** The soft layered arcs from the profile header sit at the very top of these three tabs, behind the status bar: four lavender bands fading from a deeper tone at the top edge to the lightest nearest the content, ending in the canvas color (`ArcHeader` / `ArcBackdrop`, scaled to the header height; 196dp on Me, 150dp on Practice and Progress).
The title sits at the bottom-left over the lightest band, ink on lavender. The header scrolls away with the content. Screens that use it pass no status-bar padding and let the header draw edge to edge (`Modifier.bleed`).

**Progress.** A big two-line Poppins title (no arc), then top tabs "Overview / Calendar / Plan" in the underline style above.
(`SegmentedTabs` is used everywhere top tabs appear, e.g. Subject detail's Topics / Mistakes.)
- Overview: two stat notched cards (answered, accuracy), a "By subject" list (a subject-colored dot, answered and accuracy text, a thin accuracy bar), then "Needs attention":
  topics you've missed (under 70% over 5+ answers) or may be forgetting (recall under 85%), each with a + to add it to the plan. Empty until a topic has 5 answers.
- Calendar: a month header with round arrow buttons and "n days · n questions"; a white 28dp card holding the month as 42dp day circles.
  Circles shade pink with the number of answers that day (1-4 light, 5-9 pink, 10+ deep), today has an ink ring, the selected day is solid ink; a small Fewer-to-More legend sits under the grid.
  Subject filter pills, then a notched card for the selected day (date, "25 questions · 16% correct") and that day's questions as rows with a subject dot and a Correct / Missed / Shown chip.
- Plan: a lavender notched hero ("Next 7 days", blocks, questions and minutes, a Smart or Starter tab, a deep-shade "Regenerate plan" pill);
  each block is a white row with a gradient date tile (weekday and date, in the subject's color), the topic, "10 questions · 30 min" and a small "Edit" chip. There are no arrow buttons: tapping a row opens a dialog with day pills to move the block to another study day, a "Remove" pill and a "Done" pill;
  then "Your schedule": a white card "Study days" with seven equal stadium pills (Mon to Sun; selected = solid ink with white text, others light gray; at least one day must stay on) and a "n study days a week" line;
  two pastel notched tiles side by side, "Questions" (green) and "Minutes" (yellow), each with a 44sp number, a "per day" tab and round white - / + buttons in steps of 5 (dimmed at the limits);
  and a closing line like "That's 70 questions and 3 h 30 min a week."

**Question screen.** No bottom bar. The top is a colored header card in the question's subject gradient (bottom corners 28dp, under the status bar):
a white circular back button (60dp with a 32dp arrow, the same size as the floating bookmark), the subject name, a progress bar in the subject's deep shade on a white track, and "Question n of N · k correct" (the count appears once you've answered something).
There is generous space (about 34dp) between that row and the question. Below, in the same card: a chip with the source tag when the stem ends in one (e.g. "DNB 10; NEET 13", split off by `splitSourceTag`), then the question text at 19sp.
Lettered option rows sit on the gray canvas (selected = the subject's deep shade for border and letter circle; correct = success tint, wrong pick = error tint).
The bookmark is a floating 60dp circular button with a 32dp icon at the bottom right, above the answer buttons, on its own layer so it does not scroll with the content. The circle stays white; the icon is an outline in ink and becomes a solid fill in the subject's deep shade once saved.
Sticky bottom pills: "Show answer" + "Check answer" (deep shade), then "Previous" + "Next", or "See results" on the last question.
After checking: a white explanation card with a Correct / Not quite chip, and a muted "Something wrong with this question? Report it" link that opens a dialog (Wrong answer, Typo or missing text, Unclear question, Other).
Reports are queued on the phone and sent to the `question_reports` table when signed in.

**Results screen** (after "See results"): a notched card in the session's subject color (the first palette color if the session mixes subjects) with a headline (Great work / Nice effort / Keep going, from the percentage), a 64sp percent-correct, and the question count in the tab;
three stat tiles (Correct in success tint, Missed in error tint, Shown in white); then a deep-shade "Retry n mistakes" pill (only when there are mistakes) and a white "Done" pill that returns to Today.

**Me.** Modeled on a profile screen with a layered color header. Content is edge to edge under the status bar.
- Header: four concentric arch bands in the lavender ramp (darkest at the top edge, lightest nearest the content), ending in the canvas color. A white circular settings button sits top-left (opens Edit profile).
  The 102dp avatar (initial letter for now; an avatar maker is planned) sits across the lightest arch with a canvas-colored ring.
- Below: name (30sp), "Joined in {year}" (local only), exam and daily-goal chips.
- Stats: three rows, each a 56dp rounded tile in a subject gradient (pink, blue, green) with a deep-shade icon, a 22sp value and a muted label: questions answered, correct overall, days you studied.
- Study streak: an 84dp peach tile with a flame, a 40sp "n day(s)", and a one-line hint.
- Backup and sync: a white `StudyCard` (sign in, sync, sign out).
- Question overlay: a teal `NotchedCard` (min 280dp, decorative circle, tab "On" or "Off") with a 64sp minutes number between circular minus and plus buttons,
  preset pills (1, 5, 10, 15, 20) and a deep-teal "Turn on overlay" button (white "Turn off overlay" when on). Replaces the old slider.
- Edit profile (from the settings button): back button, then a white card with name, exam pills, daily goal slider and Save changes.

**Question overlay (over other apps).** A bottom sheet built in Compose (`OverlaySheet.kt`, hosted in the service by `OverlayOwner`), not the old plain Android views.
- The rest of the screen dims to 40% black; the sheet slides up from the bottom with 28dp top corners.
- Colored header: the top of the sheet is a block in the question's subject gradient with 28dp bottom corners and a large translucent circle. On it: a white drag handle, a translucent-white subject chip, "Quick question", a translucent-white "Skip" pill (dismisses without recording an attempt), and the question text at 18sp (scrolls if long, max 190dp).
- Below the header, on the canvas color: lettered option rows exactly like the in-app question screen (the selected row uses the subject's deep shade for its border and letter circle), scrolling up to 340dp.
- Buttons: "Show answer" (secondary) and "Check answer" (the subject's deep shade, disabled until an option is chosen).
- After answering: green and red rows with "Correct" or "Your answer", a white card with a Correct / Not quite / Answer shown verdict and the explanation, and a "Done" button in the deep shade that closes the sheet and restarts the timer.
- If saving fails, an inline error appears and the buttons stay usable.

**Plan banner (Today).** Under the greeting: a full-width pill. During the trial it reads "12 days left in your free trial" with "Plans" (white; amber for the last 3 days). On the free plan it reads "Free plan · 10 questions left today" with "Upgrade" (amber). Hidden when subscribed. Tapping opens the paywall.

**Paywall.** Full screen, shown from the banner, from Me, and when a free user taps a locked feature (Review due, Mistakes, overlay, regenerate plan) or has used the daily allowance. Arc header with a close button and "Go unlimited"; one line on the user's state (days left in the trial, or that it has ended and the free plan allows 10 questions a day); a lavender notched card "MedStudy Plus" with the price (Google Play's localized price once loaded, ₹99 until then) and "per month, cancel any time"; four check-marked perks; a deep-purple "Subscribe" pill. The page uses the lavender ramp only: arc header, card, check circles and button; "Restore purchase"; and a muted "Not now" / "Continue with the free plan". Billing errors show in red above the button.

**Subscription card (Me).** Above Account: the plan name ("Free trial", "MedStudy Plus", "Free plan"), one line of detail (days left, "Active until 29 Nov 2026", or the free allowance) and a pill: "See plans" (ink) or "Manage in Google Play" (outlined) when subscribed.

**Free tier rules** (one place: `Entitlement`): 10 new questions a day, no question overlay, no Review due or Mistakes sessions, no plan regeneration. Progress, calendar and Saved stay available. The overlay switches itself off when access drops to the free tier.

**Saved.** Back button + title + count, then task rows; empty state in muted text.

**Onboarding.** The one place color covers the whole screen. Three steps, each a full-screen subject gradient (pink, green, yellow) that cross-fades into the next (600ms),
with three soft translucent circles that glide to new positions per step. A 3-segment step bar at the top uses the step's deep shade on a white track.
Steps slide horizontally (forward from the right, back from the left). Buttons: white "Back" pill + a "Next / Get started" pill in the step's deep shade.
- Step 1, name: large title, white text field (auto-focused, keyboard Next advances), a live avatar + "Hey, {name}" appears as you type.
- Step 2, exam: big white selectable cards (name + one-line description); the chosen card scales up with a bounce, gets a deep-shade border and a check, and gives a haptic tick.
- Step 3, daily goal: a 96sp number that updates live, "about {n x 3} minutes", a slider, preset pills (5, 10, 20, 30, 50) and a one-line summary.
Keep it to these four steps; do not add more without a product reason.
- Step 4, account (required, it is what the server-side trial is attached to): lavender step, "Create your account", subtitle "Your 3-day free trial starts now". White email and password fields (hint inside), a line "No card needed. After 3 days you can subscribe, or keep a limited free plan.", a "Already have an account? Sign in" toggle, and a deep-shade "Start free trial" pill (becomes "Sign in" in sign-in mode). Step 1 also has an "I already have an account" link that jumps here in sign-in mode. An existing user who is not signed in sees only this step, with their profile pre-filled and no Back button.

## 6. Do and do not

Do:
- Use the subject palette consistently per subject.
- Keep one primary action per screen, as an accent pill.
- Put progress (numbers and bars) before prose.
- Use white rows on the gray canvas to separate content, not borders.

Do not:
- Add blue or indigo accents, heavy shadows, outlined cards nested in cards, or more than the two-stop pastel gradient.
- Put white text on a pastel card.
- Invent tutors, groups, live classes, 3D art, photos, or emoji. Features need a product decision first.
- Hard-code colors or sizes in screens. Read tokens from the theme.

## 7. Compose implementation notes

- Tokens live in `core/designsystem/MedStudyTheme.kt`. Extend it with a `StudyColors` holder exposed through a `CompositionLocal`
  (`subjectGradients`, `accent`, `accentSoft`, `success`/`error`/`warning` tints, `canvas`). Keep `MaterialTheme` color roles in sync
  (`primary = accent`, `background = canvas`, `surface = white`) so Material components inherit the right defaults.
- Shared components in `core/designsystem`: `NotchedCard`, `StudyProgressBar`, `PillButton`, `FilterPill`, `TaskRow`, `SegmentedTabs`,
  `HeroCard`, `StudyCard`, `StudyChip`, `StudyAvatar`, `StudyTextField`, `IconCircleButton`, `StudyBottomBar`, `studySliderColors`. Feature modules use these and never re-style Material primitives inline.
- Gradients: `Brush.verticalGradient(top, bottom)` from the subject palette. No other gradients.
- Fonts: Google Fonts provider with a system fallback; no bundled font files for now.
- Screen files: `TodayScreen.kt`, `PracticeHomeScreen.kt`, `SubjectScreens.kt`, `SavedScreen.kt`, `MeScreen.kt`, `AccountScreen.kt`, `Onboarding.kt` (app module);
  `QuestionPracticeScreen.kt` (feature/practice); `ProgressScreen.kt` (feature/progress). The old `feature/categories` module was removed.
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
