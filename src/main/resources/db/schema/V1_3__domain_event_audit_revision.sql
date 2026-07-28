do
$$
    declare
        constraint_exists boolean;
    begin
        select exists (select 1
                       from information_schema.table_constraints
                       where table_schema = 'public'
                         and table_name = 'hmpps_domain_event_audit'
                         and constraint_name = 'fk_domain_event_audit_revision')
        into constraint_exists;

        if constraint_exists = true then
            raise notice 'constraint already exists, skipping ...';
        else
            alter table hmpps_domain_event_audit
                add constraint fk_domain_event_audit_revision foreign key (rev_id) references audit_revision (id)
            ;
        end if;
    end
$$;

