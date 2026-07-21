do
$$
    declare
        is_deferrable boolean;
    begin
        select condeferrable
        into is_deferrable
        from pg_constraint con
                 join pg_class cls on con.conrelid = cls.oid
                 join pg_namespace nsp on cls.relnamespace = nsp.oid
        where nsp.nspname = 'public'
          and cls.relname = 'court_appearance_movement'
          and con.conname = 'uq_court_appearance_movement_legacy_id';

        -- if the constraint is not deferrable (or doesn't exist yet), apply the change
        if is_deferrable = true then
            raise notice 'constraint uq_court_appearance_movement_legacy_id is already deferrable, skipping.';
        elsif is_deferrable = false then
            raise notice 'constraint is not deferrable, upgrading ...';

            alter table court_appearance_movement
                add constraint uq_court_appearance_movement_legacy_id_deferrable unique (legacy_id) deferrable initially deferred;

            alter table court_appearance_movement
                drop constraint if exists uq_court_appearance_movement_legacy_id;

            alter table court_appearance_movement
                rename constraint uq_court_appearance_movement_legacy_id_deferrable to uq_court_appearance_movement_legacy_id;
        else
            alter table court_appearance_movement
                add constraint uq_court_appearance_movement_legacy_id unique (legacy_id) deferrable initially deferred;
        end if;
    end
$$;