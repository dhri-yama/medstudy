-- APPLIED to project smofafboqldhgpbazpgs as "delete_account".
-- Learners can delete their own account from the app (a Google Play requirement).
-- Deleting the auth.users row removes everything that hangs off it: profile, exams, attempts, reports, subscription, device slots (all ON DELETE CASCADE).
-- So that deleting and signing up again does not hand out a fresh free trial, the trial's start date is kept against a
-- one-way hash of the email (no email, name or study data is kept) and reused if that email signs up again.

create table public.deleted_accounts (
  email_hash       text primary key,          -- sha256 of the lower-cased email
  trial_started_at timestamptz not null,
  deleted_at       timestamptz not null default now()
);
-- No policies: read and written only by the two functions below.
alter table public.deleted_accounts enable row level security;
revoke all on public.deleted_accounts from anon, authenticated;

create function public.delete_my_account() returns void
language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'not signed in';
  end if;
  insert into public.deleted_accounts (email_hash, trial_started_at)
    select encode(sha256(convert_to(lower(u.email), 'UTF8')), 'hex'), s.trial_started_at
    from auth.users u join public.subscriptions s on s.user_id = u.id
    where u.id = uid and u.email is not null
  on conflict (email_hash) do update
    set trial_started_at = least(public.deleted_accounts.trial_started_at, excluded.trial_started_at), deleted_at = now();
  delete from auth.users where id = uid;
end $$;
revoke execute on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;

-- A returning email continues its old trial clock instead of starting a new one.
create or replace function public.start_trial() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  insert into public.subscriptions (user_id, trial_started_at)
  values (new.id, coalesce(
    (select d.trial_started_at from public.deleted_accounts d
     where d.email_hash = encode(sha256(convert_to(lower(new.email), 'UTF8')), 'hex')),
    now()))
  on conflict do nothing;
  return new;
end $$;
