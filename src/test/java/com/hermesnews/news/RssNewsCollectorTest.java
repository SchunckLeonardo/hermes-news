package com.hermesnews.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

class RssNewsCollectorTest {

	@Test
	void combinesConfiguredFeedsWithEnabledStoredSourcesWithoutDuplicates() {
		var sourceService = Mockito.mock(NewsSourceService.class);
		when(sourceService.enabledRssUrls()).thenReturn(List.of("https://db.example/feed", "https://static.example/feed"));
		var collector = new RssNewsCollector(
				WebClient.builder(),
				new RssFeedParser(),
				new RssProperties(List.of("https://static.example/feed")),
				sourceService);

		assertThat(collector.feedsToCollect())
				.containsExactly("https://static.example/feed", "https://db.example/feed");
	}
}
