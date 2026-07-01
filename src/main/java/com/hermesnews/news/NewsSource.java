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
}
