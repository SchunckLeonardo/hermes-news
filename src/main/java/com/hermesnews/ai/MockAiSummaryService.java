package com.hermesnews.ai;

import com.hermesnews.ranking.RankedArticle;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
			return "Hermes News - Digest de tecnologia\n\nNenhuma noticia relevante foi coletada hoje.";
		}
		var grouped = groupedByTheme(articles);
		var builder = new StringBuilder("Hermes News - Digest de tecnologia\n\n");
		for (var entry : grouped.entrySet()) {
			builder.append(entry.getKey()).append("\n");
			if (entry.getValue().isEmpty()) {
				builder.append("- Nenhum destaque.\n\n");
				continue;
			}
			for (RankedArticle ranked : entry.getValue()) {
				var article = ranked.article();
				builder.append("- ")
						.append(article.title())
						.append(" (score ")
						.append(ranked.score())
						.append(")\n")
						.append("  Fonte: ")
						.append(article.sourceName())
						.append("\n")
						.append("  Link: ")
						.append(article.url())
						.append("\n");
				if (article.summary() != null && !article.summary().isBlank()) {
					builder.append("  Resumo: ").append(cleanSummary(article.summary())).append("\n");
				}
			}
			builder.append("\n");
		}
		return builder.toString().trim();
	}

	private static LinkedHashMap<String, List<RankedArticle>> groupedByTheme(List<RankedArticle> articles) {
		var grouped = new LinkedHashMap<String, List<RankedArticle>>();
		for (String theme : List.of("IA", "Java", "Backend", "Cloud", "Outras")) {
			grouped.put(theme, new java.util.ArrayList<>());
		}
		var seenUrls = new LinkedHashSet<String>();
		for (RankedArticle ranked : articles.stream().limit(MAX_ITEMS).toList()) {
			if (!seenUrls.add(ranked.article().url())) {
				continue;
			}
			grouped.get(themeFor(ranked)).add(ranked);
		}
		return grouped;
	}

	private static String themeFor(RankedArticle ranked) {
		var text = (ranked.article().title() + " " + ranked.article().summary()).toLowerCase(Locale.ROOT);
		if (text.contains("ai") || text.contains("llm") || text.contains("inteligencia artificial")) {
			return "IA";
		}
		if (text.contains("java") || text.contains("spring")) {
			return "Java";
		}
		if (text.contains("backend") || text.contains("api") || text.contains("postgres") || text.contains("redis")) {
			return "Backend";
		}
		if (text.contains("cloud") || text.contains("aws") || text.contains("kubernetes")) {
			return "Cloud";
		}
		return "Outras";
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
