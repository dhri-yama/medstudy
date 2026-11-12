-- APPLIED to project smofafboqldhgpbazpgs as "crash_reports_and_free_limit".
-- Crash reports sent by the app, and a report of accounts answering past the free plan's daily allowance.

-- The app saves a crash on the phone and uploads it the next time it syncs. Read them in the dashboard;
-- decode a trace from a release build with that version's mapping.txt.
create table public.crash_reports (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid default auth.uid() references auth.users(id) on delete set null,
  app_version  text not null,
  version_code integer not null,
  android_sdk  integer not null,
  device       text not null,
  crashed_at   timestamptz not null,
  stack_trace  text not null check (length(stack_trace) <= 20000),
  created_at   timestamptz not null default now()
);
create index crash_reports_created_idx on public.crash_reports (created_at);
-- Insert only: nobody reads crash reports through the API.
alter table public.crash_reports enable row level security;
revoke all on public.crash_reports from anon;
create policy "own crash insert" on public.crash_reports for insert with check (user_id = (select auth.uid()));

-- The trial length now lives in one place, used by my_entitlement() and by the report below.
create function public.trial_length() returns interval
language sql immutable set search_path = '' as $$ select interval '3 days' $$;

create or replace function public.my_entitlement()
returns table (access text, in_trial boolean, ends_at timestamptz, server_now timestamptz)
language sql stable security invoker set search_path = '' as $$
  with e as (
    select s.trial_started_at + public.trial_length() as trial_ends_at,
           s.current_period_end,
           (s.current_period_end is not null and now() < s.current_period_end) as paid
    from public.subscriptions s
  )
  select case when paid or now() < trial_ends_at then 'full' else 'free' end,
         (not paid and now() < trial_ends_at),
         greatest(trial_ends_at, coalesce(current_period_end, trial_ends_at)),
         now()
  from e;
$$;

-- The free plan allows 10 questions a day, and the phone enforces that. This is the server's check on it:
-- days on which an account with no trial and no paid period synced more than 20 answers (twice the allowance,
-- so a learner in another time zone is never listed by mistake). It reports; it does not block anything.
-- Days are counted in India time. For the owner only (dashboard / service role).
create view public.free_limit_overruns with (security_invoker = true) as
  select a.user_id,
         (a.completed_at at time zone 'Asia/Kolkata')::date as study_date,
         count(*) as answered
  from public.attempts a
  join public.subscriptions s on s.user_id = a.user_id
  where a.completed_at >= s.trial_started_at + public.trial_length()
    and (s.current_period_end is null or a.completed_at >= s.current_period_end)
  group by 1, 2
  having count(*) > 20;
revoke all on public.free_limit_overruns from anon, authenticated;
