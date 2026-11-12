-- APPLIED to project smofafboqldhgpbazpgs as "backup_and_sync".
-- Bookmark backup, cheaper attempt sync, study-plan settings on the account, and timed mock tests.

-- Bookmarks, one row per saved question. A removed bookmark keeps its row with removed_at set, so the removal
-- reaches the learner's other phone instead of the bookmark coming back from it. The later of saved_at / removed_at wins.
create table public.bookmarks (
  user_id     uuid not null default auth.uid() references auth.users(id) on delete cascade,
  question_id text not null,
  saved_at    timestamptz not null,
  removed_at  timestamptz,
  primary key (user_id, question_id)
);
alter table public.bookmarks enable row level security;
revoke all on public.bookmarks from anon;
create policy "own bookmarks read"   on public.bookmarks for select using (user_id = (select auth.uid()));
create policy "own bookmarks insert" on public.bookmarks for insert with check (user_id = (select auth.uid()));
create policy "own bookmarks update" on public.bookmarks for update
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

-- When the server received each attempt. A phone asks only for attempts received since its last sync,
-- which also catches answers another phone made offline and uploaded late (completed_at would miss those).
alter table public.attempts add column synced_at timestamptz not null default now();
create index attempts_user_synced_idx on public.attempts (user_id, synced_at);

-- Answers given in a timed mock test.
alter table public.attempts drop constraint attempts_context_check;
alter table public.attempts add constraint attempts_context_check
  check (context in ('TOPIC_PRACTICE', 'BOOKMARK_REVIEW', 'PROGRESS_REVIEW', 'MOCK_TEST'));

-- Study-plan settings: minutes a day on the profile, and one row per weekday the learner studies (1 = Monday ... 7 = Sunday).
alter table public.profiles add column daily_minute_goal integer not null default 30 check (daily_minute_goal between 5 and 240);
create table public.profile_study_days (
  user_id uuid not null references public.profiles(user_id) on delete cascade,
  weekday smallint not null check (weekday between 1 and 7),
  primary key (user_id, weekday)
);
alter table public.profile_study_days enable row level security;
revoke all on public.profile_study_days from anon;
create policy "own study days read"   on public.profile_study_days for select using (user_id = (select auth.uid()));
create policy "own study days insert" on public.profile_study_days for insert with check (user_id = (select auth.uid()));
create policy "own study days delete" on public.profile_study_days for delete using (user_id = (select auth.uid()));

-- Saves the plan settings in one step (the weekday list is replaced as a whole).
create function public.set_study_schedule(minutes integer, weekdays smallint[]) returns void
language plpgsql security invoker set search_path = '' as $$
begin
  update public.profiles set daily_minute_goal = minutes where user_id = auth.uid();
  delete from public.profile_study_days where user_id = auth.uid();
  insert into public.profile_study_days (user_id, weekday) select auth.uid(), unnest(weekdays);
end $$;
revoke execute on function public.set_study_schedule(integer, smallint[]) from public, anon;
grant execute on function public.set_study_schedule(integer, smallint[]) to authenticated;
