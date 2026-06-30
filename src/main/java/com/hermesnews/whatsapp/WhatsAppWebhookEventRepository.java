package com.hermesnews.whatsapp;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsAppWebhookEventRepository extends JpaRepository<WhatsAppWebhookEvent, UUID> {

	Optional<WhatsAppWebhookEvent> findByInstanceNameAndMessageId(String instanceName, String messageId);
}
