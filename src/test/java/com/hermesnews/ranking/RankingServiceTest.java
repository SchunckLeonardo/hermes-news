package com.hermesnews.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hermesnews.news.CollectedArticle;
import com.hermesnews.preferences.PersonalPreference;
import com.hermesnews.preferences.PreferenceService;
import com.hermesnews.preferences.PreferenceUpdateRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RankingServiceTest {

	@Test
	void ranksArticlesByKeywordMatches() {
		var service = new RankingService(new RankingProperties(List.of("java", "ai", "cloud")));
		var relevant = new CollectedArticle(
				"rss",
				"1",
				"Java and AI backend on cloud",
				"https://example.com/java-ai",
				"Spring services for AI workloads",
				Instant.parse("2026-06-29T10:00:00Z"));
		var irrelevant = new CollectedArticle(
				"rss",
				"2",
				"Office productivity tips",
				"https://example.com/office",
				"General productivity notes",
				Instant.parse("2026-06-29T09:00:00Z"));

		assertThat(service.score(relevant)).isGreaterThan(service.score(irrelevant));
		assertThat(service.rank(List.of(irrelevant, relevant)).getFirst().article()).isEqualTo(relevant);
	}

	@Test
	void usesPersonalPreferencesToBoostThemesSourcesAndReduceExcludedThemes() {
		var preferences = PersonalPreference.defaults();
		preferences.apply(new PreferenceUpdateRequest(
				List.of("java"),
				List.of("frontend"),
				List.of("infoq"),
				null,
				null,
				null));
		var preferenceService = Mockito.mock(PreferenceService.class);
		when(preferenceService.current()).thenReturn(preferences);
		var service = new RankingService(new RankingProperties(List.of()), preferenceService);
		var preferred = new CollectedArticle(
				"InfoQ",
				"1",
				"Java virtual threads for backend teams",
				"https://example.com/java",
				"Deep dive",
				Instant.parse("2026-06-29T10:00:00Z"));
		var excluded = new CollectedArticle(
				"Other",
				"2",
				"Frontend framework update",
				"https://example.com/frontend",
				"UI notes",
				Instant.parse("2026-06-29T10:00:00Z"));

		assertThat(service.score(preferred)).isGreaterThan(service.score(excluded));
	}

	@Test
	void prioritizesRecentOfficialLaunchNewsAboutPriorityEntities() {
		var clock = Clock.fixed(Instant.parse("2026-06-26T12:00:00Z"), ZoneOffset.UTC);
		var service = new RankingService(
				new RankingProperties(
						List.of("ai", "java", "backend", "cloud"),
						List.of("openai.com"),
						List.of("openai", "gpt", "sol", "terra", "luna"),
						List.of("announces", "launch", "launches", "release", "preview")),
				null,
				clock);
		var openAiLaunch = new CollectedArticle(
				"https://openai.com/news/rss.xml",
				"openai-sol-terra-luna",
				"OpenAI announces Sol, Terra and Luna for tomorrow",
				"https://openai.com/index/previewing-gpt-5-6-sol/",
				"Official preview of Sol, Terra and Luna, a launch that matters for AI developers.",
				Instant.parse("2026-06-26T10:00:00Z"));
		var genericKeywordMatch = new CollectedArticle(
				"community-blog",
				"generic-ai-java-cloud",
				"Java AI backend cloud patterns",
				"https://example.com/generic-ai-java-cloud",
				"General notes about Java, AI, backend, cloud and Redis teams.",
				Instant.parse("2026-06-26T09:00:00Z"));

		assertThat(service.rank(List.of(genericKeywordMatch, openAiLaunch)).getFirst().article())
				.isEqualTo(openAiLaunch);
		assertThat(service.score(openAiLaunch)).isGreaterThan(service.score(genericKeywordMatch));
	}

	@Test
	void explainsRankingSignalsIncludingPersistedFeedback() {
		var clock = Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC);
		RankingFeedbackProvider feedbackProvider = article -> new FeedbackAdjustment(
				5,
				"Feedback positivo em noticias semelhantes");
		var service = new RankingService(
				new RankingProperties(
						List.of("ai"),
						List.of("openai.com"),
						List.of("openai"),
						List.of("launches")),
				null,
				clock,
				feedbackProvider);
		var article = new CollectedArticle(
				"OpenAI",
				"launch-1",
				"OpenAI launches a new AI model",
				"https://openai.com/news/model",
				"Official launch for developers",
				Instant.parse("2026-07-13T10:00:00Z"));

		var ranked = service.rank(List.of(article)).getFirst();

		assertThat(ranked.score()).isPositive();
		assertThat(ranked.explanation())
				.contains("Fonte oficial")
				.contains("Publicada nas ultimas 24 horas")
				.contains("Feedback positivo em noticias semelhantes");
		assertThat(ranked.reasons()).extracting(RankingReason::points).contains(12, 8, 5);
	}
}
