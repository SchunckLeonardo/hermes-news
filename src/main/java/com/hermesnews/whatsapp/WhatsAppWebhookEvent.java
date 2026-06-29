package com.hermesnews.whatsapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_webhook_events")
public class WhatsAppWebhookEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 120)
	private String eventType;

	@Column(length = 120)
	private String instanceName;

	@Column(nullable = false, columnDefinition = "text")
	private String payloadJson;

	@Column(nullable = false)
	private Instant receivedAt = Instant.now();

	protected WhatsAppWebhookEvent() {
	}

	public WhatsAppWebhookEvent(String eventType, String instanceName, String payloadJson) {
		this.eventType = eventType;
		this.instanceName = instanceName;
		this.payloadJson = payloadJson;
	}

	public UUID getId() {
		return id;
	}

	public String getEventType() {
		return eventType;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public String getPayloadJson() {
		return payloadJson;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}
}
