package com.hermesnews.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.hermesnews.news.CollectedArticle;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

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
}
