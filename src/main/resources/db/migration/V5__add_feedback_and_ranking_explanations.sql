alter table digest_items add column ranking_explanation text;
alter table digest_items add column event_key varchar(300);

create table article_feedback (
    id uuid primary key,
    article_id uuid not null references articles (id) on delete cascade,
    feedback_type varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_article_feedback_article unique (article_id)
);

create index idx_article_feedback_updated_at on article_feedback (updated_at desc);
