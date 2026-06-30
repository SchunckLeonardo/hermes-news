create table personal_preferences (
    id uuid primary key,
    themes text not null,
    excluded_themes text not null,
    sources text not null,
    news_limit integer not null,
    digest_time time not null,
    language varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
