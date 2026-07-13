package com.hermesnews.whatsapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_outbox")
public class WhatsAppOutboxMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 120)
	private String recipient;

	@Column(nullable = false, columnDefinition = "text")
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WhatsAppOutboxStatus status = WhatsAppOutboxStatus.PENDING;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "max_attempts", nullable = false)
	private int maxAttempts;

	@Column(name = "next_attempt_at")
	private Instant nextAttemptAt;

	@Column(name = "last_error", length = 500)
	private String lastError;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "sent_at")
	private Instant sentAt;

	protected WhatsAppOutboxMessage() {
	}

	public WhatsAppOutboxMessage(String recipient, String message, int maxAttempts, Instant createdAt) {
		this.recipient = recipient == null ? "" : recipient.trim();
		this.message = message == null ? "" : message;
		this.maxAttempts = Math.max(1, maxAttempts);
		this.createdAt = createdAt;
		this.nextAttemptAt = createdAt;
	}

	public void apply(WhatsAppSendResult result, Instant attemptedAt, Duration baseDelay) {
		attempts++;
		var safeResult = result == null ? WhatsAppSendResult.failed("gateway returned no result") : result;
		switch (safeResult.status()) {
			case SENT -> {
				status = WhatsAppOutboxStatus.SENT;
				sentAt = attemptedAt;
				nextAttemptAt = null;
				lastError = null;
			}
			case SKIPPED -> {
				status = WhatsAppOutboxStatus.SKIPPED;
				nextAttemptAt = null;
				lastError = sanitize(safeResult.detail());
			}
			case FAILED -> {
				status = WhatsAppOutboxStatus.FAILED;
				lastError = sanitize(safeResult.detail());
				nextAttemptAt = attempts < maxAttempts
						? attemptedAt.plus(backoff(baseDelay, attempts))
						: null;
			}
		}
	}

	public boolean isRetryable() {
		return status == WhatsAppOutboxStatus.FAILED && attempts < maxAttempts && nextAttemptAt != null;
	}

	private static Duration backoff(Duration baseDelay, int attempt) {
		var safeBase = baseDelay == null || baseDelay.isNegative() || baseDelay.isZero()
				? Duration.ofMinutes(1)
				: baseDelay;
		var multiplier = 1L << Math.min(10, Math.max(0, attempt - 1));
		return safeBase.multipliedBy(multiplier);
	}

	private static String sanitize(String detail) {
		if (detail == null) {
			return null;
		}
		var cleaned = detail.replaceAll("[\\r\\n\\t]+", " ").trim();
		return cleaned.length() <= 500 ? cleaned : cleaned.substring(0, 500);
	}

	public UUID getId() {
		return id;
	}

	public String getRecipient() {
		return recipient;
	}

	public String getMessage() {
		return message;
	}

	public WhatsAppOutboxStatus getStatus() {
		return status;
	}

	public int getAttempts() {
		return attempts;
	}

	public Instant getNextAttemptAt() {
		return nextAttemptAt;
	}

	public String getLastError() {
		return lastError;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getSentAt() {
		return sentAt;
	}
}
