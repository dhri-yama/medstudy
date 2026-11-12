# Changelog

Versions follow `MAJOR.MINOR.PATCH` (see `version.properties`). Add notes under **Unreleased** as you work;
`tools/bump-version.sh minor` turns them into a release, commits, and tags `vX.Y.Z`.

## Unreleased

- Previous-year papers: NEET PG 2020-2025 (recall), JIPMER 2018-19 and DNB 2018, with exam tags, images, topics and a Papers tab in Practice.
- Only MedMCQA questions from 2018 onward are kept.
- Calmer Practice layout (Daily set, quick actions, Papers / Subjects tabs).
- Debug-only `DEV_SKIP_AUTH` flag to skip sign-in while testing.
- Version shown in Me; this changelog and the bump script.
- An account can be signed in on 2 devices at a time; a third sign-in is blocked, with "Sign out my other devices" (once every 7 days).
- Log out ends the session on the server; a phone whose session was ended elsewhere returns to sign-in.
- Forgot password (emailed code), Delete account in Me, and Privacy policy / Terms links (addresses in `gradle.properties`).
- Free trial is 3 days.
- Small animations: button press, streak, answer reveal, screen and tab transitions; all respect "Remove animations".

## 0.1.0

- First internal build: onboarding, Supabase sign-in and sync, practice and quiz screens, progress and study plan, question overlay, 15-day trial with paywall and free tier.
