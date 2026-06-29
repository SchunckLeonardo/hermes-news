package com.hermesnews.digest;

import com.hermesnews.whatsapp.WhatsAppSendStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "digests")
public class Digest {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private DigestStatus status = DigestStatus.CREATED;

	@Column(nullable = false)
	private Instant generatedAt = Instant.now();

	private Instant sentAt;

	@Column(nullable = false, columnDefinition = "text")
	private String message;

	@Column(nullable = false, length = 40)
	private String channel = "WHATSAPP";

	protected Digest() {
	}

	private Digest(String message) {
		this.message = message;
	}

	public static Digest created(String message) {
		return new Digest(message);
	}

	public void applySendStatus(WhatsAppSendStatus sendStatus) {
		this.status = switch (sendStatus) {
			case SENT -> DigestStatus.SENT;
			case SKIPPED -> DigestStatus.SKIPPED;
			case FAILED -> DigestStatus.FAILED;
		};
		if (sendStatus == WhatsAppSendStatus.SENT) {
			this.sentAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public DigestStatus getStatus() {
		return status;
	}

	public Instant getGeneratedAt() {
		return generatedAt;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public String getMessage() {
		return message;
	}

	public String getChannel() {
		return channel;
	}
}
