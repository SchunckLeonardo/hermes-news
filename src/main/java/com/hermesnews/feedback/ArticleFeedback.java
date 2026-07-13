package com.hermesnews.feedback;

import com.hermesnews.news.Article;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "article_feedback")
public class ArticleFeedback {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "article_id", nullable = false)
	private Article article;

	@Enumerated(EnumType.STRING)
	@Column(name = "feedback_type", nullable = false, length = 20)
	private FeedbackType type;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ArticleFeedback() {
	}

	public ArticleFeedback(Article article, FeedbackType type) {
		this.article = article;
		this.type = type;
		var now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	public void changeTo(FeedbackType value) {
		this.type = value;
		this.updatedAt = Instant.now();
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

	public Article getArticle() {
		return article;
	}

	public FeedbackType getType() {
		return type;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
