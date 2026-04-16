create table if not exists migration_system_audit
(
    id          uuid        not null,
    created_at  timestamp   not null,
    created_by  varchar(64) not null,
    modified_at timestamp,
    modified_by varchar(64),
    constraint pk_migration_system_audit primary key (id)
)
;

drop table court_appearance_movement_audit;
drop table court_appearance_movement;

create table court_appearance_movement
(
    id                  uuid        not null,
    version             int         not null,
    court_appearance_id uuid,
    person_identifier   varchar(10) not null,
    prison_code         varchar(6)  not null,
    court_code          varchar(6)  not null,
    reason_id           uuid        not null,
    direction           varchar(3)  not null,
    occurred_at         timestamp   not null,
    comments            text,
    legacy_id           varchar(32),
    constraint pk_court_appearance_movement primary key (id),
    constraint pk_court_appearance_movement_person foreign key (person_identifier) references person_summary (person_identifier),
    constraint pk_court_appearance_movement_court_appearance foreign key (court_appearance_id) references court_appearance (id),
    constraint fk_court_appearance_movement_reason foreign key (reason_id) references court_appearance_reason (id),
    constraint uq_court_appearance_movement_legacy_id unique (legacy_id),
    constraint ch_court_appearance_movement_direction check (direction in ('IN', 'OUT'))
)
;

create index idx_court_appearance_movement_person_identifier on court_appearance_movement (person_identifier);

create table court_appearance_movement_audit
(
    rev_id              bigint      not null,
    rev_type            smallint    not null,
    id                  uuid        not null,
    version             int         not null,
    court_appearance_id uuid,
    person_identifier   varchar(10) not null,
    prison_code         varchar(6)  not null,
    court_code          varchar(6)  not null,
    reason_id           uuid        not null,
    direction           varchar(3)  not null,
    occurred_at         timestamp   not null,
    comments            text,
    legacy_id           varchar(32),
    constraint pk_court_appearance_movement_audit primary key (id, rev_id),
    constraint fk_court_appearance_movement_audit_revision foreign key (rev_id) references audit_revision (id)
)
;
