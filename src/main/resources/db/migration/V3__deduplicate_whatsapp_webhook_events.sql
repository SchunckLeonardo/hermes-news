alter table whatsapp_webhook_events
    add column message_id varchar(120);

alter table whatsapp_webhook_events
    add column remote_jid varchar(180);

alter table whatsapp_webhook_events
    add column from_me boolean;

create unique index ux_whatsapp_webhook_events_instance_message
    on whatsapp_webhook_events (instance_name, message_id);
