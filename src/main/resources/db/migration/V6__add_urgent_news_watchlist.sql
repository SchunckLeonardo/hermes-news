create table watchlist_entries (
    id uuid primary key,
    term varchar(160) not null unique,
    enabled boolean not null default true,
    cooldown_minutes bigint not null,
    last_alerted_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table urgent_alerts (
    id uuid primary key,
    watchlist_entry_id uuid not null references watchlist_entries (id) on delete cascade,
    article_url varchar(1000) not null unique,
    title varchar(500) not null,
    alerted_at timestamp with time zone not null
);

create index idx_watchlist_entries_enabled on watchlist_entries (enabled, term);
create index idx_urgent_alerts_alerted_at on urgent_alerts (alerted_at desc);
