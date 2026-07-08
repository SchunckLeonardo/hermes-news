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
	private static final int MAX_TITLE_LENGTH = 140;

	@Override
	public String summarize(List<RankedArticle> articles) {
		if (articles.isEmpty()) {
			return """
					*Hermes News*
					Digest de tecnologia

					Nenhuma noticia nova relevante foi coletada hoje.
					""".trim();
		}
		var grouped = groupedByTheme(articles);
		var total = grouped.values().stream().mapToInt(List::size).sum();
		var builder = new StringBuilder("*Hermes News*\nDigest de tecnologia\n\n")
				.append(total == 1 ? "1 noticia nova selecionada." : total + " noticias novas selecionadas.")
				.append("\n\n");
		var itemNumber = 1;
		for (var entry : grouped.entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}
			builder.append("*").append(entry.getKey()).append("*\n");
			for (RankedArticle ranked : entry.getValue()) {
				var article = ranked.article();
				builder.append(itemNumber++)
						.append(". *")
						.append(cleanTitle(article.title()))
						.append("*\n")
						.append("Por que importa: ")
						.append(reason(article.summary()))
						.append("\n")
						.append("Fonte: ")
						.append(sourceName(article.sourceName()))
						.append("\n")
						.append("Link: ")
						.append(cleanLine(article.url(), Integer.MAX_VALUE))
						.append("\n");
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
		var text = (safe(ranked.article().title()) + " " + safe(ranked.article().summary())).toLowerCase(Locale.ROOT);
		if (containsTerm(text, "ai") || containsTerm(text, "llm") || text.contains("inteligencia artificial")) {
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

	private static boolean containsTerm(String text, String term) {
		return text.matches(".*\\b" + term + "\\b.*");
	}

	private static String cleanTitle(String value) {
		var cleaned = cleanLine(value, MAX_TITLE_LENGTH);
		if (cleaned.isBlank()) {
			return "Noticia sem titulo";
		}
		return cleaned;
	}

	private static String reason(String value) {
		var cleaned = cleanLine(value, MAX_SUMMARY_LENGTH);
		if (cleaned.isBlank()) {
			return "Resumo indisponivel; abra o link para ver os detalhes.";
		}
		return cleaned;
	}

	private static String sourceName(String value) {
		var cleaned = cleanLine(value, 80);
		if (cleaned.isBlank()) {
			return "Fonte desconhecida";
		}
		return cleaned;
	}

	private static String cleanLine(String value, int maxLength) {
		if (value == null) {
			return "";
		}
		var cleaned = value
				.replaceAll("<[^>]+>", " ")
				.replaceAll("\\s+", " ")
				.trim();
		if (cleaned.length() <= maxLength) {
			return cleaned;
		}
		return cleaned.substring(0, maxLength).trim() + "...";
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}
}
