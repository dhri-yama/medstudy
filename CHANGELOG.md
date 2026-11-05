# Changelog

Versions follow `MAJOR.MINOR.PATCH` (see `version.properties`). Add notes under **Unreleased** as you work;
`tools/bump-version.sh minor` turns them into a release, commits, and tags `vX.Y.Z`.

## Unreleased

- Previous-year papers: NEET PG 2020-2025 (recall), JIPMER 2018-19 and DNB 2018, with exam tags, images, topics and a Papers tab in Practice.
- Only MedMCQA questions from 2018 onward are kept.
- Calmer Practice layout (Daily set, quick actions, Papers / Subjects tabs).
- Debug-only `DEV_SKIP_AUTH` flag to skip sign-in while testing.
- Version shown in Me; this changelog and the bump script.

## 0.1.0

- First internal build: onboarding, Supabase sign-in and sync, practice and quiz screens, progress and study plan, question overlay, 15-day trial with paywall and free tier.
