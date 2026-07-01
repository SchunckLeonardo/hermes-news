package com.hermesnews.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
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
				new RssFeedDiscovery(),
				sourceService);

		assertThat(collector.feedsToCollect())
				.containsExactly("https://static.example/feed", "https://db.example/feed");
	}

	@Test
	void discoversFeedUrlWhenConfiguredSourceIsAnHtmlPage() {
		var sourceService = Mockito.mock(NewsSourceService.class);
		when(sourceService.enabledRssUrls()).thenReturn(List.of("https://akitaonrails.com/en/"));
		var html = """
				<html>
				  <head>
				    <link rel="alternate" type="application/rss+xml" title="RSS" href="/en/index.xml">
				  </head>
				</html>
				""";
		var rss = """
				<rss version="2.0">
				  <channel>
				    <item>
				      <guid>akita-1</guid>
				      <title>Ruby, AI and backend</title>
				      <link>https://akitaonrails.com/en/posts/ruby-ai</link>
				      <description>Technology post</description>
				    </item>
				  </channel>
				</rss>
				""";
		var responses = Map.of(
				"https://akitaonrails.com/en/", html,
				"https://akitaonrails.com/en/index.xml", rss);
		var collector = new RssNewsCollector(
				new RssFeedParser(),
				new RssProperties(List.of()),
				sourceService,
				new RssFeedDiscovery(),
				responses::get);

		var articles = collector.collect();

		assertThat(articles).hasSize(1);
		assertThat(articles.getFirst().sourceName()).isEqualTo("https://akitaonrails.com/en/index.xml");
		assertThat(articles.getFirst().title()).isEqualTo("Ruby, AI and backend");
	}
}
