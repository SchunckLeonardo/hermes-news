create table news_sources (
    id uuid primary key,
    name varchar(120) not null,
    type varchar(40) not null,
    url varchar(1000) not null unique,
    enabled boolean not null default true,
    created_at timestamp with time zone not null
);

create table articles (
    id uuid primary key,
    source_name varchar(120) not null,
    external_id varchar(200),
    title varchar(500) not null,
    url varchar(1000) not null unique,
    summary text,
    published_at timestamp with time zone,
    collected_at timestamp with time zone not null,
    score integer not null default 0
);

create index idx_articles_score on articles (score desc, published_at desc);

create table digests (
    id uuid primary key,
    status varchar(40) not null,
    generated_at timestamp with time zone not null,
    sent_at timestamp with time zone,
    message text not null,
    channel varchar(40) not null
);

create table digest_items (
    id uuid primary key,
    digest_id uuid not null references digests (id) on delete cascade,
    article_id uuid not null references articles (id),
    rank_score integer not null,
    rank_order integer not null
);

create table whatsapp_webhook_events (
    id uuid primary key,
    event_type varchar(120) not null,
    instance_name varchar(120),
    payload_json text not null,
    received_at timestamp with time zone not null
);
