-- module

-- member
CREATE
OR REPLACE FUNCTION hamming_distance(a BIGINT, b BIGINT)
RETURNS INT AS $$
BEGIN
RETURN bit_count(a # b); -- XOR 再计数
END;
$$
LANGUAGE plpgsql IMMUTABLE;
SELECT *
FROM embedding_cache
WHERE hamming_distance(simhash, :targetSimHash) <= 3;


-- auto-generated definition
create table embedding_cache
(
    id         bigint primary key,
    sim_hash   bigint not null,
    text       text   not null,
    updated_at timestamp default now()
);

alter table embedding_cache
    owner to root;

create index idx_sim_hash
    on embedding_cache (sim_hash);

