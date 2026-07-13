package com.hermesnews.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.hermesnews.news.CollectedArticle;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticEventClustererTest {

	private final SemanticEventClusterer clusterer = new SemanticEventClusterer();

	@Test
	void groupsDifferentHeadlinesAboutTheSameEventAndKeepsTheHighestRankedRepresentative() {
		var official = ranked(
				"OpenAI launches Sol, Terra and Luna models",
				"https://openai.com/sol-terra-luna",
				60);
		var secondary = ranked(
				"Sol, Terra and Luna unveiled by OpenAI",
				"https://example.com/openai-new-models",
				35);

		var clustered = clusterer.cluster(List.of(official, secondary));

		assertThat(clustered).hasSize(1);
		assertThat(clustered.getFirst().article()).isEqualTo(official.article());
		assertThat(clustered.getFirst().eventKey()).contains("openai").contains("sol").contains("terra").contains("luna");
	}

	@Test
	void keepsDifferentEventsAboutTheSameCompanySeparate() {
		var launch = ranked(
				"OpenAI launches Sol model",
				"https://openai.com/sol",
				60);
		var hiring = ranked(
				"OpenAI hires a new chief financial officer",
				"https://example.com/openai-cfo",
				40);

		assertThat(clusterer.cluster(List.of(launch, hiring))).hasSize(2);
	}

	private static RankedArticle ranked(String title, String url, int score) {
		return new RankedArticle(new CollectedArticle(
				"source",
				url,
				title,
				url,
				title,
				Instant.parse("2026-07-13T10:00:00Z")), score);
	}
}
