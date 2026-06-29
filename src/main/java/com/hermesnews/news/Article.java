package com.hermesnews.news;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "articles")
public class Article {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 120)
	private String sourceName;

	@Column(length = 200)
	private String externalId;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(nullable = false, length = 1000, unique = true)
	private String url;

	@Column(columnDefinition = "text")
	private String summary;

	private Instant publishedAt;

	@Column(nullable = false)
	private Instant collectedAt;

	@Column(nullable = false)
	private int score;

	protected Article() {
	}

	private Article(
			String sourceName,
			String externalId,
			String title,
			String url,
			String summary,
			Instant publishedAt,
			Instant collectedAt,
			int score) {
		this.sourceName = sourceName;
		this.externalId = externalId;
		this.title = title;
		this.url = url;
		this.summary = summary;
		this.publishedAt = publishedAt;
		this.collectedAt = collectedAt;
		this.score = score;
	}

	public static Article from(CollectedArticle article, int score) {
		return new Article(
				article.sourceName(),
				article.externalId(),
				article.title(),
				article.url(),
				article.summary(),
				article.publishedAt(),
				Instant.now(),
				score);
	}

	public UUID getId() {
		return id;
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getExternalId() {
		return externalId;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public String getSummary() {
		return summary;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}

	public Instant getCollectedAt() {
		return collectedAt;
	}

	public int getScore() {
		return score;
	}
}
