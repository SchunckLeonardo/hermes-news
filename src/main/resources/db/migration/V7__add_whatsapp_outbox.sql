create table whatsapp_outbox (
    id uuid primary key,
    recipient varchar(120) not null,
    message text not null,
    status varchar(20) not null,
    attempts integer not null,
    max_attempts integer not null,
    next_attempt_at timestamp with time zone,
    last_error varchar(500),
    created_at timestamp with time zone not null,
    sent_at timestamp with time zone
);

create index idx_whatsapp_outbox_retry on whatsapp_outbox (status, next_attempt_at, created_at);
