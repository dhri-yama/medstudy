-- APPLIED to project smofafboqldhgpbazpgs as "add_question_reports".
-- Learners flag bad questions (typos, wrong answers). Insert and read own rows only; reports are reviewed by the owner in the dashboard.
create table public.question_reports (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null default auth.uid() references auth.users(id) on delete cascade,
  question_id text not null,
  reason      text not null check (reason in ('Wrong answer', 'Typo or missing text', 'Unclear question', 'Other')),
  created_at  timestamptz not null default now()
);
create index question_reports_user_idx on public.question_reports (user_id);

alter table public.question_reports enable row level security;

create policy "own reports insert" on public.question_reports for insert with check (user_id = (select auth.uid()));
create policy "own reports read"   on public.question_reports for select using (user_id = (select auth.uid()));
