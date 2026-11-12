-- The free trial is 3 days (it was 15). The length lives only here, on the server; the app just shows what my_entitlement() returns.
-- Existing accounts are measured the same way: their trial ends 3 days after trial_started_at.
create or replace function public.my_entitlement()
returns table (access text, in_trial boolean, ends_at timestamptz, server_now timestamptz)
language sql stable security invoker set search_path = '' as $$
  with e as (
    select s.trial_started_at + interval '3 days' as trial_ends_at,
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
