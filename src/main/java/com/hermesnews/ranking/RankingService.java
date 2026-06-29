package com.hermesnews.ranking;

import com.hermesnews.news.CollectedArticle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

	private final List<String> keywords;

	public RankingService(RankingProperties properties) {
		this.keywords = properties.keywords() == null
				? List.of()
				: properties.keywords().stream()
						.filter(keyword -> keyword != null && !keyword.isBlank())
						.map(keyword -> keyword.toLowerCase(Locale.ROOT))
						.toList();
	}

	public int score(CollectedArticle article) {
		var title = normalize(article.title());
		var summary = normalize(article.summary());
		var score = 0;
		for (String keyword : keywords) {
			if (title.contains(keyword)) {
				score += 3;
			}
			if (summary.contains(keyword)) {
				score += 1;
			}
		}
		return score;
	}

	public List<RankedArticle> rank(List<CollectedArticle> articles) {
		return articles.stream()
				.map(article -> new RankedArticle(article, score(article)))
				.sorted(Comparator.comparingInt(RankedArticle::score).reversed()
						.thenComparing(ranked -> ranked.article().publishedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
