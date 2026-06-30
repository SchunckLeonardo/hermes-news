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

	@Column(length = 120)
	private String messageId;

	@Column(length = 180)
	private String remoteJid;

	private Boolean fromMe;

	@Column(nullable = false)
	private Instant receivedAt = Instant.now();

	protected WhatsAppWebhookEvent() {
	}

	public WhatsAppWebhookEvent(String eventType, String instanceName, String payloadJson) {
		this(eventType, instanceName, payloadJson, null, null, null);
	}

	public WhatsAppWebhookEvent(
			String eventType,
			String instanceName,
			String payloadJson,
			String messageId,
			String remoteJid,
			Boolean fromMe) {
		this.eventType = eventType;
		this.instanceName = instanceName;
		this.payloadJson = payloadJson;
		this.messageId = messageId;
		this.remoteJid = remoteJid;
		this.fromMe = fromMe;
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

	public String getMessageId() {
		return messageId;
	}

	public String getRemoteJid() {
		return remoteJid;
	}

	public Boolean getFromMe() {
		return fromMe;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}
}
