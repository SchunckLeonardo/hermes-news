package com.hermesnews.news;

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
@Table(name = "news_sources")
public class NewsSource {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 120)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private NewsSourceType type;

	@Column(nullable = false, length = 1000, unique = true)
	private String url;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	@Column
	private Instant lastSuccessAt;

	@Column
	private Instant lastErrorAt;

	@Column(length = 1000)
	private String lastErrorMessage;

	@Column(nullable = false)
	private int consecutiveFailures;

	protected NewsSource() {
	}

	public NewsSource(String name, NewsSourceType type, String url) {
		this.name = name;
		this.type = type;
		this.url = url;
	}

	public void enable() {
		this.enabled = true;
	}

	public void disable() {
		this.enabled = false;
	}

	public void rename(String label) {
		this.name = normalizeLabel(label);
	}

	public void recordCollectionSuccess(Instant occurredAt) {
		this.lastSuccessAt = occurredAt == null ? Instant.now() : occurredAt;
		this.lastErrorMessage = null;
		this.consecutiveFailures = 0;
	}

	public void recordCollectionFailure(String message, Instant occurredAt) {
		this.lastErrorAt = occurredAt == null ? Instant.now() : occurredAt;
		this.lastErrorMessage = truncate(message);
		this.consecutiveFailures++;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public NewsSourceType getType() {
		return type;
	}

	public String getUrl() {
		return url;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastSuccessAt() {
		return lastSuccessAt;
	}

	public Instant getLastErrorAt() {
		return lastErrorAt;
	}

	public String getLastErrorMessage() {
		return lastErrorMessage;
	}

	public int getConsecutiveFailures() {
		return consecutiveFailures;
	}

	private static String truncate(String value) {
		if (value == null || value.isBlank()) {
			return "Unknown source collection failure";
		}
		var normalized = value.strip();
		return normalized.length() > 1000 ? normalized.substring(0, 1000) : normalized;
	}

	private static String normalizeLabel(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Source label must not be blank");
		}
		var normalized = value.strip();
		if (normalized.length() > 120) {
			throw new IllegalArgumentException("Source label must have at most 120 characters");
		}
		return normalized;
	}
}
