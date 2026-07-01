alter table news_sources
    add column last_success_at timestamp with time zone;

alter table news_sources
    add column last_error_at timestamp with time zone;

alter table news_sources
    add column last_error_message varchar(1000);

alter table news_sources
    add column consecutive_failures integer not null default 0;
