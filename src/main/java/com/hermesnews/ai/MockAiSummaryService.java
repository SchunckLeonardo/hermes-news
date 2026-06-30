package com.hermesnews.ai;

import com.hermesnews.ranking.RankedArticle;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockAiSummaryService implements AiSummaryService {

	private static final int MAX_ITEMS = 10;
	private static final int MAX_SUMMARY_LENGTH = 240;

	@Override
	public String summarize(List<RankedArticle> articles) {
		if (articles.isEmpty()) {
			return "Daily technology digest\n\nNo relevant technology news collected today.";
		}
		var builder = new StringBuilder("Daily technology digest\n\n");
		var digestArticles = articles.stream().limit(MAX_ITEMS).toList();
		for (int index = 0; index < digestArticles.size(); index++) {
			var ranked = digestArticles.get(index);
			builder.append(index + 1)
					.append(". ")
					.append(ranked.article().title())
					.append(" (score ")
					.append(ranked.score())
					.append(")\n")
					.append(ranked.article().url())
					.append("\n");
			if (ranked.article().summary() != null && !ranked.article().summary().isBlank()) {
				builder.append(cleanSummary(ranked.article().summary())).append("\n");
			}
			builder.append("\n");
		}
		return builder.toString().trim();
	}

	private static String cleanSummary(String value) {
		var cleaned = value
				.replaceAll("<[^>]+>", " ")
				.replaceAll("\\s+", " ")
				.trim();
		if (cleaned.length() <= MAX_SUMMARY_LENGTH) {
			return cleaned;
		}
		return cleaned.substring(0, MAX_SUMMARY_LENGTH).trim() + "...";
	}
}
