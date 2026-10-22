-- Run once in Supabase Dashboard → SQL Editor.
create table if not exists public.attempts (
  id text primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  question_id text not null,
  selected_option_ids jsonb not null,
  result text not null,
  completed_at bigint not null,
  study_date text not null,
  time_zone text not null,
  context text not null
);
create index if not exists attempts_user_idx on public.attempts(user_id);

alter table public.attempts enable row level security;

create policy "own attempts read"   on public.attempts for select using (user_id = auth.uid());
create policy "own attempts insert" on public.attempts for insert with check (user_id = auth.uid());
create policy "own attempts delete" on public.attempts for delete using (user_id = auth.uid());
