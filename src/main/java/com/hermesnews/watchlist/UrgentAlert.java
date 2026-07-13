package com.hermesnews.watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "urgent_alerts")
public class UrgentAlert {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "watchlist_entry_id", nullable = false)
	private WatchlistEntry watchlistEntry;

	@Column(name = "article_url", nullable = false, length = 1000, unique = true)
	private String articleUrl;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(name = "alerted_at", nullable = false)
	private Instant alertedAt;

	protected UrgentAlert() {
	}

	public UrgentAlert(WatchlistEntry watchlistEntry, String articleUrl, String title, Instant alertedAt) {
		this.watchlistEntry = watchlistEntry;
		this.articleUrl = articleUrl;
		this.title = title;
		this.alertedAt = alertedAt;
	}

	public UUID getId() {
		return id;
	}

	public String getArticleUrl() {
		return articleUrl;
	}
}
