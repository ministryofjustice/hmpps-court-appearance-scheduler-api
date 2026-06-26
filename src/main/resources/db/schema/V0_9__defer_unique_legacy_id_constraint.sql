do
$$
    declare
        is_deferrable boolean;
    begin
        -- check if the constraint is deferrable
        select condeferrable
        into is_deferrable
        from pg_constraint con
                 join pg_class cls on con.conrelid = cls.oid
                 join pg_namespace nsp on cls.relnamespace = nsp.oid
        where nsp.nspname = 'public'
          and cls.relname = 'court_appearance'
          and con.conname = 'uq_court_appearance_legacy_id';

        -- if the constraint is not deferrable (or doesn't exist yet), apply the change
        if is_deferrable = false then
            raise notice 'constraint is not deferrable, upgrading ...';

            -- 1. add the new deferrable constraint
            alter table court_appearance
                add constraint uq_court_appearance_legacy_id_deferrable unique (legacy_id) deferrable initially deferred;

            -- 2. drop the old constraint
            alter table court_appearance
                drop constraint if exists uq_court_appearance_legacy_id;

            -- 3. rename the new one to match the original name
            alter table court_appearance
                rename constraint uq_court_appearance_legacy_id_deferrable to uq_court_appearance_legacy_id;

        elsif is_deferrable = true then
            raise notice 'constraint uq_court_appearance_legacy_id is already deferrable, skipping.';
        else
            alter table court_appearance
                add constraint uq_court_appearance_legacy_id unique (legacy_id) deferrable initially deferred;
        end if;
    end
$$;