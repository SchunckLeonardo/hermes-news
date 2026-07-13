package com.hermesnews.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hermesnews.news.CollectedArticle;
import com.hermesnews.preferences.PersonalPreference;
import com.hermesnews.preferences.PreferenceService;
import com.hermesnews.ranking.RankedArticle;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OllamaAiSummaryServiceTest {

	@Test
	void asksOllamaToSummarizeRankedArticles() {
		var client = new CapturingAiChatClient("Resumo qwen3");
		var preferenceService = preferenceService();
		var service = new OllamaAiSummaryService(client, preferenceService, new AiProperties("ollama", Duration.ofSeconds(5)));
		var article = article("Spring AI with Ollama", "https://example.com/spring-ai");

		var summary = service.summarize(List.of(new RankedArticle(article, 9)));

		assertThat(summary).isEqualTo("Resumo qwen3");
		assertThat(client.systemPrompt).contains("Hermes News");
		assertThat(client.systemPrompt)
					.contains("*Hermes News*")
					.contains("Por que importa:")
					.contains("mostre apenas secoes com noticias")
					.contains("preserve exatamente a ordem e a numeracao")
					.contains("Nao mostre score tecnico");
		assertThat(client.userPrompt)
				.contains("Preferencias do usuario")
				.contains("ai, java, backend, cloud")
				.contains("Spring AI with Ollama")
				.contains("https://example.com/spring-ai")
				.contains("score: 9");
	}

	@Test
	void fallsBackToLocalDigestWhenOllamaFails() {
		var service = new OllamaAiSummaryService((systemPrompt, userPrompt) -> {
			throw new IllegalStateException("ollama unavailable");
		}, preferenceService(), new AiProperties("ollama", Duration.ofSeconds(5)));

		var summary = service.summarize(List.of(new RankedArticle(article("Java backend agents", "https://example.com/java"), 7)));

		assertThat(summary)
				.contains("*Hermes News*")
				.contains("Digest de tecnologia")
				.contains("Java backend agents")
				.contains("https://example.com/java");
	}

	@Test
	void fallsBackToLocalDigestWhenOllamaIsSlow() {
		var service = new OllamaAiSummaryService((systemPrompt, userPrompt) -> {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
			return "late response";
		}, preferenceService(), new AiProperties("ollama", Duration.ofMillis(10)));

		var summary = service.summarize(List.of(new RankedArticle(article("Cloud backend", "https://example.com/cloud"), 7)));

		assertThat(summary)
				.contains("*Hermes News*")
				.contains("Digest de tecnologia")
				.contains("Cloud backend");
	}

	private static PreferenceService preferenceService() {
		var service = Mockito.mock(PreferenceService.class);
		when(service.current()).thenReturn(PersonalPreference.defaults());
		return service;
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
