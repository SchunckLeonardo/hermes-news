package com.hermesnews.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.hermesnews.news.CollectedArticle;
import com.hermesnews.ranking.RankedArticle;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class MockAiSummaryServiceTest {

	@Test
	void createsReadableDigestWithoutCallingExternalAi() {
		var service = new MockAiSummaryService();
		var article = new CollectedArticle(
				"hacker-news",
				"123",
				"Java agents for backend teams",
				"https://example.com/java-agents",
				"Agent workflow details",
				Instant.parse("2026-06-29T08:00:00Z"));

		var digest = service.summarize(List.of(new RankedArticle(article, 8)));

		assertThat(digest)
				.contains("Hermes News - Digest de tecnologia")
				.contains("Java")
				.contains("Java agents for backend teams")
				.contains("Fonte: hacker-news")
				.contains("https://example.com/java-agents");
	}

	@Test
	void groupsArticlesByTechnologyThemeAndDeduplicatesLinks() {
		var java = new CollectedArticle(
				"rss",
				"java",
				"Java backend performance",
				"https://example.com/java",
				"Spring backend details",
				Instant.parse("2026-06-29T08:00:00Z"));
		var ai = new CollectedArticle(
				"rss",
				"ai",
				"AI infrastructure",
				"https://example.com/ai",
				"LLM cloud summary",
				Instant.parse("2026-06-29T08:00:00Z"));

		var digest = new MockAiSummaryService().summarize(List.of(
				new RankedArticle(java, 9),
				new RankedArticle(ai, 8),
				new RankedArticle(java, 7)));

		assertThat(digest).contains("IA", "Java", "Backend", "Cloud");
		assertThat(digest.indexOf("https://example.com/java"))
				.isEqualTo(digest.lastIndexOf("https://example.com/java"));
	}

	@Test
	void stripsHtmlAndLimitsDigestSize() {
		var articles = IntStream.rangeClosed(1, 12)
				.mapToObj(index -> new RankedArticle(new CollectedArticle(
						"rss",
						"article-" + index,
						"Article " + index,
						"https://example.com/article-" + index,
						"<p>Summary <strong>" + index + "</strong></p>",
						Instant.parse("2026-06-29T08:00:00Z")), index))
				.toList();

		var digest = new MockAiSummaryService().summarize(articles);

		assertThat(digest).contains("Summary 1");
		assertThat(digest).doesNotContain("<p>");
		assertThat(digest).doesNotContain("Article 11");
	}
}
