#!/bin/bash
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE openfga;
EOSQL
create table text_embedding_cache
(
    id         bigint       not null
        primary key,
    sim_hash   bigint       not null,
    text       text         not null,
    embedding  DOUBLE PRECISION[] not null,
    updated_at timestamp default now()
);

alter table text_embedding_cache
    owner to root;

create index idx_sim_hash
    on text_embedding_cache (sim_hash);

