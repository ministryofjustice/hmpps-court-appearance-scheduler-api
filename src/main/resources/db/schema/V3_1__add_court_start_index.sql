create index if not exists idx_court_appearance_start on court_appearance (start);
create index if not exists idx_person_summary_prison_code on person_summary (prison_code, person_identifier);