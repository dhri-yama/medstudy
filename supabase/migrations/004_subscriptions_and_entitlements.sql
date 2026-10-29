-- APPLIED to project smofafboqldhgpbazpgs as "add_subscriptions_and_entitlements".
-- 15-day trial that starts at sign-up (server clock), then a paid period written only by the Edge Functions.
-- 3NF: trial end and "is paid" are derived from trial_started_at / current_period_end, never stored.
-- Verified: trigger creates the row; day 1 = full/in_trial/15 days; day 16 = free; paid = full/30 days; users cannot update their own row.

create table public.plans (
  id text primary key,
  name text not null,
  billing_interval text not null check (billing_interval in ('month', 'year')),
  price_inr_paise integer check (price_inr_paise > 0)  -- informational; the app shows Google Play's localized price
);
insert into public.plans (id, name, billing_interval) values ('monthly', 'Monthly', 'month');

create table public.subscriptions (
  user_id            uuid primary key references auth.users(id) on delete cascade,
  trial_started_at   timestamptz not null default now(),
  plan_id            text references public.plans(id),
  current_period_end timestamptz,
  provider           text check (provider in ('google_play')),
  provider_ref       text unique,
  updated_at         timestamptz not null default now(),
  check ((plan_id is null) = (current_period_end is null))
);

create trigger subscriptions_set_updated_at before update on public.subscriptions
  for each row execute function public.set_updated_at();

alter table public.plans enable row level security;
alter table public.subscriptions enable row level security;

create policy "plans are readable" on public.plans for select using (true);
create policy "own subscription read" on public.subscriptions for select using (user_id = (select auth.uid()));

create function public.start_trial() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  insert into public.subscriptions (user_id) values (new.id) on conflict do nothing;
  return new;
end $$;
revoke execute on function public.start_trial() from public, anon, authenticated;

create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.start_trial();

insert into public.subscriptions (user_id) select id from auth.users on conflict do nothing;

create function public.my_entitlement()
returns table (access text, in_trial boolean, ends_at timestamptz, server_now timestamptz)
language sql stable security invoker set search_path = '' as $$
  with e as (
    select s.trial_started_at + interval '15 days' as trial_ends_at,
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
revoke execute on function public.my_entitlement() from public, anon;
grant execute on function public.my_entitlement() to authenticated;
