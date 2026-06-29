package com.hermesnews.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HackerNewsCollectorTest {

	@Mock
	private HackerNewsClient client;

	@Test
	void collectsValidTopStoriesWithoutLiveNetworkAccess() {
		when(client.topStories()).thenReturn(List.of(101L, 102L));
		when(client.item(101L)).thenReturn(Optional.of(new HackerNewsItem(
				101L,
				"Postgres tips for backend teams",
				"https://example.com/postgres",
				1782739200L,
				false,
				false)));
		when(client.item(102L)).thenReturn(Optional.of(new HackerNewsItem(
				102L,
				"Deleted item",
				null,
				1782739200L,
				false,
				true)));
		var collector = new HackerNewsCollector(client, new HackerNewsProperties("https://hn.example", 10));

		var articles = collector.collect();

		assertThat(articles).hasSize(1);
		assertThat(articles.getFirst().sourceName()).isEqualTo("hacker-news");
		assertThat(articles.getFirst().externalId()).isEqualTo("101");
	}
}
