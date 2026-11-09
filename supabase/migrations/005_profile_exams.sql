-- A learner can prepare for several exams. profiles.exam_id stays as the primary exam (older app versions still read it);
-- every chosen exam, including the primary one, is a row here (3NF: one fact per row, no arrays).
create table public.profile_exams (
  user_id uuid not null references public.profiles(user_id) on delete cascade,
  exam_id text not null references public.exams(id),
  primary key (user_id, exam_id)
);
create index profile_exams_exam_idx on public.profile_exams (exam_id);

alter table public.profile_exams enable row level security;
revoke all on public.profile_exams from anon;

create policy "own exams read"   on public.profile_exams for select using (user_id = (select auth.uid()));
create policy "own exams insert" on public.profile_exams for insert with check (user_id = (select auth.uid()));
create policy "own exams delete" on public.profile_exams for delete using (user_id = (select auth.uid()));
