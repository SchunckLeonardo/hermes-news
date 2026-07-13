package com.hermesnews.whatsapp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsAppOutboxRepository extends JpaRepository<WhatsAppOutboxMessage, UUID> {

	List<WhatsAppOutboxMessage> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
			WhatsAppOutboxStatus status,
			Instant nextAttemptAt);
}
