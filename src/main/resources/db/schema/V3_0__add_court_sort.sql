create table if not exists court_code
(
    code varchar(6) not null,
    sort bytea      not null,
    constraint pk_court_code primary key (code)
)
;

with codes as (select distinct court_code from court_appearance order by 1)
insert
into court_code (code, sort)
select court_code, convert_to(lower(regexp_replace(court_code, '[^a-zA-Z0-9]', '', 'g')), 'LATIN1')
from codes
;

alter table court_appearance
    add constraint fk_court_appearance_court_code foreign key (court_code) references court_code (code)
;