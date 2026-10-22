-- APPLIED to project smofafboqldhgpbazpgs as "normalize_attempts_3nf".
-- Replaces the first-version attempts table (empty when written) with a 3NF design:
--  * selected_option_ids jsonb (a list, not atomic) -> attempt_selected_options, one row per option
--  * study_date dropped: derivable from completed_at + time_zone
--  * completed_at becomes timestamptz
--  * question_id has no foreign key: the question bank ships inside the app.
-- The old table is renamed, not dropped; delete attempts_legacy once you no longer need it.

alter table public.attempts rename to attempts_legacy;
alter index public.attempts_pkey rename to attempts_legacy_pkey;
alter index public.attempts_user_idx rename to attempts_legacy_user_idx;
alter table public.attempts_legacy rename constraint attempts_user_id_fkey to attempts_legacy_user_id_fkey;

create table public.attempts (
  id           text primary key,
  user_id      uuid not null default auth.uid() references auth.users(id) on delete cascade,
  question_id  text not null,
  result       text not null check (result in ('CORRECT', 'INCORRECT', 'UNANSWERED')),
  context      text not null check (context in ('TOPIC_PRACTICE', 'BOOKMARK_REVIEW', 'PROGRESS_REVIEW')),
  completed_at timestamptz not null,
  time_zone    text not null
);
create index attempts_user_completed_idx on public.attempts (user_id, completed_at);

create table public.attempt_selected_options (
  attempt_id text not null references public.attempts(id) on delete cascade,
  option_id  text not null,
  primary key (attempt_id, option_id)
);

alter table public.attempts                 enable row level security;
alter table public.attempt_selected_options enable row level security;

create policy "own attempts read"   on public.attempts for select using (user_id = (select auth.uid()));
create policy "own attempts insert" on public.attempts for insert with check (user_id = (select auth.uid()));
create policy "own attempts delete" on public.attempts for delete using (user_id = (select auth.uid()));

create policy "own attempt options read" on public.attempt_selected_options for select
  using (exists (select 1 from public.attempts a where a.id = attempt_id and a.user_id = (select auth.uid())));
create policy "own attempt options insert" on public.attempt_selected_options for insert
  with check (exists (select 1 from public.attempts a where a.id = attempt_id and a.user_id = (select auth.uid())));
create policy "own attempt options delete" on public.attempt_selected_options for delete
  using (exists (select 1 from public.attempts a where a.id = attempt_id and a.user_id = (select auth.uid())));
