create table if not exists hmpps_domain_event
(
    id         uuid    not null,
    version    int     not null,
    entity_id  uuid    not null,
    event_type text    not null,
    event      jsonb   not null,
    published  boolean not null,
    constraint pk_hmpps_domain_event primary key (id)
)
;

create index idx_hmpps_domain_event_unpublished on hmpps_domain_event (id) where (published = false);

create table person_summary
(
    person_identifier varchar(10) not null,
    version           int         not null,
    first_name        varchar(64) not null,
    last_name         varchar(64) not null,
    prison_code       varchar(6),
    cell_location     varchar(64),
    constraint pk_person_summary primary key (person_identifier)
)
;

create index idx_person_summary_name on person_summary (lower(last_name::text), lower(first_name::text));

create table court_appearance_reason
(
    id              uuid         not null default uuidv7(),
    code            varchar(16)  not null,
    description     varchar(255) not null,
    sequence_number int          not null,
    active          boolean      not null,
    external        boolean      not null,
    constraint pk_court_appearance_reason primary key (id),
    constraint uq_court_appearance_reason_code unique (code),
    constraint uq_court_appearance_reason_sequence_number unique (sequence_number)
)
;

create table court_appearance_status
(
    id              uuid         not null default uuidv7(),
    code            varchar(16)  not null,
    description     varchar(255) not null,
    sequence_number int          not null,
    constraint pk_court_appearance_status primary key (id),
    constraint uq_court_appearance_status_code unique (code),
    constraint uq_court_appearance_status_sequence_number unique (sequence_number)
)
;

create table court_appearance
(
    id                uuid        not null,
    version           int         not null,
    person_identifier varchar(10) not null,
    prison_code       varchar(6)  not null,
    court_code        varchar(6)  not null,
    reason_id         uuid        not null,
    external          boolean     not null,
    status_id         uuid        not null,
    start             timestamp   not null,
    "end"             timestamp,
    comments          text,
    legacy_id         bigint,
    constraint pk_court_appearance primary key (id),
    constraint pk_court_appearance_person foreign key (person_identifier) references person_summary (person_identifier),
    constraint fk_court_appearance_reason foreign key (reason_id) references court_appearance_reason (id),
    constraint fk_court_appearance_status foreign key (status_id) references court_appearance_status (id),
    constraint uq_court_appearance_legacy_id unique (legacy_id)
)
;

create index idx_court_appearance_person_status_start on court_appearance (person_identifier, status_id, start);
create index idx_court_appearance_prison_start_status_reason on court_appearance (prison_code, start, status_id, reason_id);
create index idx_court_appearance_prison_start_external on court_appearance (prison_code, start) where external = true;

create table court_appearance_movement
(
    id                  uuid        not null,
    version             int         not null,
    court_appearance_id uuid        not null,
    person_identifier   varchar(10) not null,
    prison_code         varchar(6)  not null,
    court_code          varchar(6)  not null,
    direction           varchar(3)  not null,
    occurred_at         timestamp   not null,
    comments            text,
    legacy_id           varchar(32),
    constraint pk_court_appearance_movement primary key (id),
    constraint pk_court_appearance_movement_court_appearance foreign key (court_appearance_id) references court_appearance (id),
    constraint pk_court_appearance_movement_person foreign key (person_identifier) references person_summary (person_identifier),
    constraint uq_court_appearance_movement_legacy_id unique (legacy_id),
    constraint ch_court_appearance_movement_direction check (direction in ('IN', 'OUT'))
)
;

create index idx_court_appearance_movement_person_identifier on court_appearance_movement (person_identifier);

create table audit_revision
(
    id                bigserial   not null,
    timestamp         timestamp   not null,
    source            varchar(6)  not null,
    affected_entities text[]      not null,
    username          varchar(64) not null,
    caseload_id       varchar(10),
    reason            text,
    constraint pk_audit_revision primary key (id),
    constraint ch_audit_revision_source check (source in ('DPS', 'NOMIS'))
)
;

create table if not exists hmpps_domain_event_audit
(
    rev_id     bigint   not null,
    rev_type   smallint not null,
    id         uuid     not null,
    version    int      not null,
    entity_id  uuid     not null,
    event_type text     not null,
    event      jsonb    not null,
    published  boolean  not null,
    constraint pk_hmpps_domain_event_audit primary key (id, rev_id)
)
;

create index idx_hmpps_domain_event_audit_event_type_entity_id on hmpps_domain_event_audit (event_type, entity_id);

create table court_appearance_audit
(
    rev_id            bigint      not null,
    rev_type          smallint    not null,
    id                uuid        not null,
    version           int         not null,
    person_identifier varchar(10) not null,
    prison_code       varchar(6)  not null,
    court_code        varchar(6)  not null,
    reason_id         uuid        not null,
    external          boolean     not null,
    status_id         uuid        not null,
    start             timestamp   not null,
    "end"             timestamp,
    comments          text,
    legacy_id         bigint,
    constraint pk_court_appearance_audit primary key (id, rev_id),
    constraint fk_court_appearance_audit_revision foreign key (rev_id) references audit_revision (id)
)
;

create table court_appearance_movement_audit
(
    rev_id              bigint      not null,
    rev_type            smallint    not null,
    id                  uuid        not null,
    version             int         not null,
    court_appearance_id uuid        not null,
    person_identifier   varchar(10) not null,
    prison_code         varchar(6)  not null,
    court_code          varchar(6)  not null,
    direction           varchar(3)  not null,
    occurred_at         timestamp   not null,
    comments            text,
    legacy_id           varchar(32),
    constraint pk_court_appearance_movement_audit primary key (id, rev_id),
    constraint fk_court_appearance_movement_audit_revision foreign key (rev_id) references audit_revision (id)
)
;