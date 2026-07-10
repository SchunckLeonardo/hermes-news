package com.hermesnews.ranking;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "app.ranking")
public record RankingProperties(
		List<String> keywords,
		List<String> officialSources,
		List<String> priorityEntities,
		List<String> launchKeywords) {

	@ConstructorBinding
	public RankingProperties {
		keywords = safeList(keywords);
		officialSources = safeList(officialSources);
		priorityEntities = safeList(priorityEntities);
		launchKeywords = safeList(launchKeywords);
	}

	public RankingProperties(List<String> keywords) {
		this(keywords, List.of(), List.of(), List.of());
	}

	private static List<String> safeList(List<String> values) {
		return values == null ? List.of() : List.copyOf(values);
	}
}
