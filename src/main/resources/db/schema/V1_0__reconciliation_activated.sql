create table if not exists reconciliation_history
(
    id           uuid not null,
    type         text not null,
    requested_on date not null,
    version      int  not null,
    constraint pk_reconciliation_history primary key (id),
    constraint uq_reconciliation_history_type_requested unique (type, requested_on)
)
;