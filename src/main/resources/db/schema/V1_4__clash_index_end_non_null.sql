alter table court_appearance
    alter "end" set not null
;

create index if not exists idx_court_appearance_person_start_end on court_appearance(person_identifier, start, "end")
;