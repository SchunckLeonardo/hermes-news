package com.hermesnews.ranking;

import com.hermesnews.news.CollectedArticle;
import com.hermesnews.preferences.PreferenceService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

	private final List<String> keywords;
	private final PreferenceService preferenceService;

	public RankingService(RankingProperties properties) {
		this(properties, null);
	}

	@Autowired
	public RankingService(RankingProperties properties, PreferenceService preferenceService) {
		this.keywords = properties.keywords() == null
				? List.of()
				: properties.keywords().stream()
						.filter(keyword -> keyword != null && !keyword.isBlank())
						.map(keyword -> keyword.toLowerCase(Locale.ROOT))
						.toList();
		this.preferenceService = preferenceService;
	}

	public int score(CollectedArticle article) {
		var title = normalize(article.title());
		var summary = normalize(article.summary());
		var source = normalize(article.sourceName());
		var preferences = preferenceService == null ? null : preferenceService.current();
		var preferredThemes = preferences == null ? List.<String>of() : preferences.themes();
		var excludedThemes = preferences == null ? Set.<String>of() : Set.copyOf(preferences.excludedThemes());
		var preferredSources = preferences == null ? List.<String>of() : preferences.sources();
		var score = 0;
		for (String keyword : keywords) {
			if (title.contains(keyword)) {
				score += 3;
			}
			if (summary.contains(keyword)) {
				score += 1;
			}
		}
		for (String theme : preferredThemes) {
			if (title.contains(theme)) {
				score += 5;
			}
			if (summary.contains(theme)) {
				score += 2;
			}
		}
		for (String excluded : excludedThemes) {
			if (title.contains(excluded)) {
				score -= 5;
			}
			if (summary.contains(excluded)) {
				score -= 2;
			}
		}
		for (String preferredSource : preferredSources) {
			if (source.contains(preferredSource)) {
				score += 4;
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
