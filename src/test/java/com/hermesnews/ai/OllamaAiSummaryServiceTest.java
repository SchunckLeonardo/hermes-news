package com.hermesnews.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.hermesnews.news.CollectedArticle;
import com.hermesnews.ranking.RankedArticle;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OllamaAiSummaryServiceTest {

	@Test
	void asksOllamaToSummarizeRankedArticles() {
		var client = new CapturingAiChatClient("Resumo qwen3");
		var service = new OllamaAiSummaryService(client);
		var article = article("Spring AI with Ollama", "https://example.com/spring-ai");

		var summary = service.summarize(List.of(new RankedArticle(article, 9)));

		assertThat(summary).isEqualTo("Resumo qwen3");
		assertThat(client.systemPrompt).contains("Hermes News");
		assertThat(client.userPrompt)
				.contains("Spring AI with Ollama")
				.contains("https://example.com/spring-ai")
				.contains("score: 9");
	}

	@Test
	void fallsBackToLocalDigestWhenOllamaFails() {
		var service = new OllamaAiSummaryService((systemPrompt, userPrompt) -> {
			throw new IllegalStateException("ollama unavailable");
		});

		var summary = service.summarize(List.of(new RankedArticle(article("Java backend agents", "https://example.com/java"), 7)));

		assertThat(summary)
				.contains("Daily technology digest")
				.contains("Java backend agents")
				.contains("https://example.com/java");
	}

	private static CollectedArticle article(String title, String url) {
		return new CollectedArticle(
				"rss",
				title.toLowerCase().replace(" ", "-"),
				title,
				url,
				"Technical summary",
				Instant.parse("2026-06-30T08:00:00Z"));
	}

	private static class CapturingAiChatClient implements AiChatClient {

		private final String response;
		private String systemPrompt;
		private String userPrompt;

		private CapturingAiChatClient(String response) {
			this.response = response;
		}

		@Override
		public String complete(String systemPrompt, String userPrompt) {
			this.systemPrompt = systemPrompt;
			this.userPrompt = userPrompt;
			return response;
		}
	}
}
