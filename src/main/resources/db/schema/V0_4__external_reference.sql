alter table court_appearance
    add column external_reference text,
    add constraint uq_court_appearance_external_reference unique (external_reference)
;

alter table court_appearance_audit
    add column external_reference text
;