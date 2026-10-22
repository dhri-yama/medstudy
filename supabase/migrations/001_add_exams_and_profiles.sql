-- APPLIED to project smofafboqldhgpbazpgs as "add_exams_and_profiles".
-- exams: an exam's name depends on the exam, not on the profile, so it is its own table (3NF).
-- profiles: one row per user; exam_id references exams.

create table public.exams (
  id   text primary key,
  name text not null unique
);

insert into public.exams (id, name) values
  ('neet_pg', 'NEET PG'),
  ('aiims',   'AIIMS'),
  ('inicet',  'INI-CET');

create table public.profiles (
  user_id             uuid primary key references auth.users(id) on delete cascade,
  display_name        text not null check (length(btrim(display_name)) > 0),
  exam_id             text not null references public.exams(id),
  exam_date           date,
  daily_question_goal integer not null default 10 check (daily_question_goal between 1 and 100),
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now()
);

create function public.set_updated_at() returns trigger
language plpgsql set search_path = '' as $$
begin
  new.updated_at = now();
  return new;
end $$;

create trigger profiles_set_updated_at before update on public.profiles
  for each row execute function public.set_updated_at();

alter table public.exams    enable row level security;
alter table public.profiles enable row level security;

create policy "exams are readable" on public.exams for select using (true);

create policy "own profile read"   on public.profiles for select using (user_id = (select auth.uid()));
create policy "own profile insert" on public.profiles for insert with check (user_id = (select auth.uid()));
create policy "own profile update" on public.profiles for update
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "own profile delete" on public.profiles for delete using (user_id = (select auth.uid()));

