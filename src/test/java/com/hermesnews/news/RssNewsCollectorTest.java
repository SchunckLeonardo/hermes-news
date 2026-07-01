package com.hermesnews.news;

import static org.assertj.core.api.Assertions.assertThat;
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
}
