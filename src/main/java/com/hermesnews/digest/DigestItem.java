package com.hermesnews.digest;

import com.hermesnews.news.Article;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "digest_items")
public class DigestItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "digest_id", nullable = false)
	private Digest digest;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "article_id", nullable = false)
	private Article article;

	@Column(nullable = false)
	private int rankScore;

	@Column(nullable = false)
	private int rankOrder;

	protected DigestItem() {
	}

	public DigestItem(Digest digest, Article article, int rankScore, int rankOrder) {
		this.digest = digest;
		this.article = article;
		this.rankScore = rankScore;
		this.rankOrder = rankOrder;
	}

	public UUID getId() {
		return id;
	}

	public Digest getDigest() {
		return digest;
	}

	public Article getArticle() {
		return article;
	}

	public int getRankScore() {
		return rankScore;
	}

	public int getRankOrder() {
		return rankOrder;
	}
}
