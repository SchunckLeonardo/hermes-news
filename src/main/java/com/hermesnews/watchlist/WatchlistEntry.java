package com.hermesnews.watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "watchlist_entries")
public class WatchlistEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 160, unique = true)
	private String term;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "cooldown_minutes", nullable = false)
	private long cooldownMinutes;

	@Column(name = "last_alerted_at")
	private Instant lastAlertedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected WatchlistEntry() {
	}

	public WatchlistEntry(String term, Duration cooldown) {
		this.term = normalizeTerm(term);
		this.cooldownMinutes = Math.max(1, cooldown.toMinutes());
		var now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	public boolean canAlert(Instant now) {
		return enabled && (lastAlertedAt == null || !lastAlertedAt.plus(Duration.ofMinutes(cooldownMinutes)).isAfter(now));
	}

	public void markAlerted(Instant instant) {
		this.lastAlertedAt = instant;
		this.updatedAt = instant;
	}

	public void enable() {
		this.enabled = true;
	}

	public void disable() {
		this.enabled = false;
	}

	@PrePersist
	void prePersist() {
		var now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getTerm() {
		return term;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public long getCooldownMinutes() {
		return cooldownMinutes;
	}

	public Instant getLastAlertedAt() {
		return lastAlertedAt;
	}

	private static String normalizeTerm(String value) {
		var normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
		if (normalized.isBlank() || normalized.length() > 160) {
			throw new IllegalArgumentException("Watchlist term must have between 1 and 160 characters");
		}
		return normalized;
	}
}
