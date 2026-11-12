-- APPLIED to project smofafboqldhgpbazpgs as "device_limit".
-- An account can be signed in on 2 devices at a time. A third sign-in is blocked until one of the two logs out,
-- or the learner uses "Sign out my other devices" (allowed once every 7 days, so a shared account can't keep swapping).
-- A "device" is one Supabase Auth session (auth.sessions: one row per sign-in). The app calls claim_device_slot()
-- right after every sign-in and on launch; a session holds a slot only once it has claimed one.
-- Both numbers (2 devices, 7 days) live only in claim_device_slot().

-- One row per session that holds a slot. Logging out deletes the auth session, which frees the slot (cascade).
create table public.device_slots (
  session_id uuid primary key references auth.sessions(id) on delete cascade,
  user_id    uuid not null references auth.users(id) on delete cascade,
  claimed_at timestamptz not null default now()
);
create index device_slots_user_idx on public.device_slots (user_id);

-- When each account last used "Sign out my other devices".
create table public.device_resets (
  user_id       uuid primary key references auth.users(id) on delete cascade,
  last_reset_at timestamptz not null
);

-- No policies: both tables are read and written only by claim_device_slot().
alter table public.device_slots  enable row level security;
alter table public.device_resets enable row level security;
revoke all on public.device_slots, public.device_resets from anon, authenticated;

-- allowed = this session holds a slot. When false, reset_available_at says when "Sign out my other devices"
-- may be used: null means now, otherwise the time the 7-day wait ends.
create function public.claim_device_slot(sign_out_others boolean default false)
returns table (allowed boolean, reset_available_at timestamptz)
language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  sid uuid := (auth.jwt() ->> 'session_id')::uuid;
  next_reset timestamptz;
begin
  if uid is null or sid is null then
    raise exception 'not signed in';
  end if;
  -- One claim at a time per account, so two devices signing in together cannot both take the last slot.
  perform pg_advisory_xact_lock(hashtextextended(uid::text, 0));

  if exists (select 1 from public.device_slots s where s.session_id = sid) then
    return query select true, null::timestamptz;
    return;
  end if;

  if (select count(*) from public.device_slots s where s.user_id = uid) >= 2 then
    select r.last_reset_at + interval '7 days' into next_reset from public.device_resets r where r.user_id = uid;
    if next_reset <= now() then
      next_reset := null;
    end if;
    if not sign_out_others or next_reset is not null then
      return query select false, next_reset;
      return;
    end if;
    -- Ends every other session of this account; their slots go with them.
    delete from auth.sessions a where a.user_id = uid and a.id <> sid;
    insert into public.device_resets (user_id, last_reset_at) values (uid, now())
      on conflict (user_id) do update set last_reset_at = excluded.last_reset_at;
  end if;

  insert into public.device_slots (session_id, user_id) values (sid, uid);
  return query select true, null::timestamptz;
end $$;
revoke execute on function public.claim_device_slot(boolean) from public, anon;
grant execute on function public.claim_device_slot(boolean) to authenticated;
