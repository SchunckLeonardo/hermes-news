package com.hermesnews.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hermesnews.news.Article;
import com.hermesnews.news.ArticleRepository;
import com.hermesnews.news.CollectedArticle;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NewsHistoryServiceTest {

	@Mock
	private ArticleRepository articleRepository;

	@Test
	void searchesPersistedArticlesWithinTheRequestedTimeWindow() {
		var now = Instant.parse("2026-07-13T12:00:00Z");
		var article = Article.from(new CollectedArticle(
				"OpenAI",
				"1",
				"OpenAI launches a model",
				"https://openai.com/model",
				"Launch details",
				now.minus(Duration.ofDays(1))), 30);
		when(articleRepository.searchHistory(
				"openai",
				now.minus(Duration.ofDays(7)),
				PageRequest.of(0, 5))).thenReturn(List.of(article));
		var service = new NewsHistoryService(articleRepository, Clock.fixed(now, ZoneOffset.UTC));

		var result = service.search(" OpenAI ", Duration.ofDays(7), 5);

		assertThat(result.query()).isEqualTo("OpenAI");
		assertThat(result.articles()).containsExactly(article);
		verify(articleRepository).searchHistory(
				"openai",
				now.minus(Duration.ofDays(7)),
				PageRequest.of(0, 5));
	}
}
