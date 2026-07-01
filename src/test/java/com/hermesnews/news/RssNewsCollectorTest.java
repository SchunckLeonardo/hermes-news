package com.hermesnews.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.buffer.DataBufferLimitException;
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

	@Test
	void fetchesRssBodiesLargerThanDefaultWebClientBuffer() throws Exception {
		var sourceService = Mockito.mock(NewsSourceService.class);
		var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		var largeDescription = "large-rss-body".repeat(25_000);
		var rss = """
				<rss version="2.0">
				  <channel>
				    <item>
				      <guid>large-1</guid>
				      <title>Large feed item</title>
				      <link>https://example.com/large-feed-item</link>
				      <description>%s</description>
				    </item>
				  </channel>
				</rss>
				""".formatted(largeDescription);
		server.createContext("/feed", exchange -> {
			var bytes = rss.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/rss+xml");
			exchange.sendResponseHeaders(200, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();
		try {
			var collector = new RssNewsCollector(
					WebClient.builder(),
					new RssFeedParser(),
					new RssProperties(List.of()),
					new RssFeedDiscovery(),
					sourceService);

			var body = collector.fetch("http://127.0.0.1:" + server.getAddress().getPort() + "/feed");

			assertThat(body).contains("Large feed item");
			assertThat(body.getBytes(StandardCharsets.UTF_8)).hasSizeGreaterThan(262_144);
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void skipsFeedWhenResponseExceedsConfiguredBufferLimit() {
		var sourceService = Mockito.mock(NewsSourceService.class);
		when(sourceService.enabledRssUrls()).thenReturn(List.of("https://large.example/feed"));
		var collector = new RssNewsCollector(
				new RssFeedParser(),
				new RssProperties(List.of()),
				sourceService,
				new RssFeedDiscovery(),
				url -> {
					throw new DataBufferLimitException("Exceeded limit on max bytes to buffer");
				});

		var articles = collector.collect();

		assertThat(articles).isEmpty();
	}

	@Test
	void recordsSourceHealthSuccessWhenStoredFeedReturnsArticles() {
		var sourceService = Mockito.mock(NewsSourceService.class);
		when(sourceService.enabledRssUrls()).thenReturn(List.of("https://source.example/feed"));
		var rss = """
				<rss version="2.0">
				  <channel>
				    <item>
				      <guid>source-1</guid>
				      <title>Healthy source</title>
				      <link>https://source.example/article</link>
				    </item>
				  </channel>
				</rss>
				""";
		var collector = new RssNewsCollector(
				new RssFeedParser(),
				new RssProperties(List.of()),
				sourceService,
				new RssFeedDiscovery(),
				url -> rss);

		var articles = collector.collect();

		assertThat(articles).hasSize(1);
		verify(sourceService).recordCollectionSuccess(Mockito.eq("https://source.example/feed"), Mockito.any());
	}

	@Test
	void recordsSourceHealthFailureWhenStoredFeedCannotBeCollected() {
		var sourceService = Mockito.mock(NewsSourceService.class);
		when(sourceService.enabledRssUrls()).thenReturn(List.of("https://broken.example/feed"));
		var collector = new RssNewsCollector(
				new RssFeedParser(),
				new RssProperties(List.of()),
				sourceService,
				new RssFeedDiscovery(),
				url -> "<html><body>no feed here</body></html>");

		var articles = collector.collect();

		assertThat(articles).isEmpty();
		verify(sourceService).recordCollectionFailure(
				Mockito.eq("https://broken.example/feed"),
				contains("no RSS/Atom"),
				Mockito.any());
	}

	@Test
	void testsSingleSourceWithoutCollectingAllConfiguredFeeds() {
		var sourceService = Mockito.mock(NewsSourceService.class);
		var rss = """
				<rss version="2.0">
				  <channel>
				    <item>
				      <guid>test-1</guid>
				      <title>Manual test article</title>
				      <link>https://test.example/article</link>
				    </item>
				  </channel>
				</rss>
				""";
		var collector = new RssNewsCollector(
				new RssFeedParser(),
				new RssProperties(List.of("https://configured.example/feed")),
				sourceService,
				new RssFeedDiscovery(),
				url -> rss);

		var result = collector.testSource("https://test.example/feed");

		assertThat(result.success()).isTrue();
		assertThat(result.articleCount()).isEqualTo(1);
		assertThat(result.url()).isEqualTo("https://test.example/feed");
		verify(sourceService).recordCollectionSuccess(Mockito.eq("https://test.example/feed"), Mockito.any());
	}
}
