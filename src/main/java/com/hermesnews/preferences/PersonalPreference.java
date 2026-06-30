package com.hermesnews.preferences;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "personal_preferences")
public class PersonalPreference {

	private static final String DEFAULT_THEMES = "ai,java,backend,cloud";
	private static final String DEFAULT_LANGUAGE = "pt-BR";
	private static final int DEFAULT_NEWS_LIMIT = 10;
	private static final LocalTime DEFAULT_DIGEST_TIME = LocalTime.of(8, 0);

	@Id
	private UUID id;

	@Column(nullable = false)
	private String themes;

	@Column(name = "excluded_themes", nullable = false)
	private String excludedThemes;

	@Column(nullable = false)
	private String sources;

	@Column(name = "news_limit", nullable = false)
	private int newsLimit;

	@Column(name = "digest_time", nullable = false)
	private LocalTime digestTime;

	@Column(nullable = false)
	private String language;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PersonalPreference() {
	}

	private PersonalPreference(
			UUID id,
			String themes,
			String excludedThemes,
			String sources,
			int newsLimit,
			LocalTime digestTime,
			String language) {
		this.id = id;
		this.themes = themes;
		this.excludedThemes = excludedThemes;
		this.sources = sources;
		this.newsLimit = newsLimit;
		this.digestTime = digestTime;
		this.language = language;
	}

	public static PersonalPreference defaults() {
		return new PersonalPreference(
				UUID.randomUUID(),
				DEFAULT_THEMES,
				"",
				"",
				DEFAULT_NEWS_LIMIT,
				DEFAULT_DIGEST_TIME,
				DEFAULT_LANGUAGE);
	}

	public List<String> themes() {
		return split(themes);
	}

	public List<String> excludedThemes() {
		return split(excludedThemes);
	}

	public List<String> sources() {
		return split(sources);
	}

	public int newsLimit() {
		return newsLimit;
	}

	public LocalTime digestTime() {
		return digestTime;
	}

	public String language() {
		return language;
	}

	public void apply(PreferenceUpdateRequest request) {
		if (request == null) {
			return;
		}
		var currentThemes = new LinkedHashSet<>(themes());
		currentThemes.addAll(normalize(request.addThemes()));
		this.themes = join(currentThemes);

		var currentExcluded = new LinkedHashSet<>(excludedThemes());
		currentExcluded.addAll(normalize(request.removeThemes()));
		this.excludedThemes = join(currentExcluded);

		var requestedSources = normalize(request.sources());
		if (!requestedSources.isEmpty()) {
			this.sources = join(requestedSources);
		}
		if (request.newsLimit() != null) {
			this.newsLimit = Math.max(1, Math.min(25, request.newsLimit()));
		}
		if (request.digestTime() != null) {
			this.digestTime = request.digestTime();
		}
		if (hasText(request.language())) {
			this.language = request.language().trim();
		}
	}

	@PrePersist
	void prePersist() {
		var now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	private static List<String> split(String value) {
		if (!hasText(value)) {
			return List.of();
		}
		var values = new ArrayList<String>();
		for (String item : value.split(",")) {
			var normalized = normalize(item);
			if (hasText(normalized) && !values.contains(normalized)) {
				values.add(normalized);
			}
		}
		return List.copyOf(values);
	}

	private static List<String> normalize(List<String> values) {
		if (values == null) {
			return List.of();
		}
		var normalized = new ArrayList<String>();
		for (String value : values) {
			var item = normalize(value);
			if (hasText(item) && !normalized.contains(item)) {
				normalized.add(item);
			}
		}
		return normalized;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static String join(Iterable<String> values) {
		var normalized = new ArrayList<String>();
		for (String value : values) {
			var item = normalize(value);
			if (hasText(item) && !normalized.contains(item)) {
				normalized.add(item);
			}
		}
		return String.join(",", normalized);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
