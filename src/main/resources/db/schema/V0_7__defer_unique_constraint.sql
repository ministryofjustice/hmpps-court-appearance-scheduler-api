alter table court_appearance
    drop constraint uq_court_appearance_external_reference
;

alter table court_appearance
    add constraint uq_court_appearance_external_reference unique (external_reference) deferrable initially deferred
;