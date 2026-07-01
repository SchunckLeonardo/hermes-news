package com.hermesnews.news;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Component
public class RssNewsCollector implements NewsCollector {

	private static final Logger log = LoggerFactory.getLogger(RssNewsCollector.class);
	private static final int MAX_DISCOVERED_FEEDS = 3;

	private final WebClient webClient;
	private final RssFeedParser parser;
	private final RssProperties properties;
	private final NewsSourceService newsSourceService;
	private final RssFeedDiscovery discovery;
	private final Function<String, String> fetcher;

	@Autowired
	public RssNewsCollector(
			WebClient.Builder builder,
			RssFeedParser parser,
			RssProperties properties,
			RssFeedDiscovery discovery,
			NewsSourceService newsSourceService) {
		this.webClient = builder.clone()
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(properties.maxResponseBytes()))
						.build())
				.build();
		this.parser = parser;
		this.properties = properties;
		this.newsSourceService = newsSourceService;
		this.discovery = discovery;
		this.fetcher = this::fetch;
	}

	RssNewsCollector(
			RssFeedParser parser,
			RssProperties properties,
			NewsSourceService newsSourceService,
			RssFeedDiscovery discovery,
			Function<String, String> fetcher) {
		this.webClient = null;
		this.parser = parser;
		this.properties = properties;
		this.newsSourceService = newsSourceService;
		this.discovery = discovery;
		this.fetcher = fetcher;
	}

	@Override
	public List<CollectedArticle> collect() {
		var feeds = feedsToCollect();
		var articles = new ArrayList<CollectedArticle>();
		for (String feed : feeds) {
			var attempt = collectFrom(feed);
			if (attempt.success()) {
				newsSourceService.recordCollectionSuccess(feed, Instant.now());
				articles.addAll(attempt.articles());
			}
			else {
				log.warn("Skipping RSS feed {}: {}", feed, attempt.message());
				newsSourceService.recordCollectionFailure(feed, attempt.message(), Instant.now());
			}
		}
		return articles;
	}

	public NewsSourceTestResponse testSource(String url) {
		var normalizedUrl = NewsSourceService.normalizePublicHttpUrl(url);
		var attempt = collectFrom(normalizedUrl);
		if (attempt.success()) {
			newsSourceService.recordCollectionSuccess(normalizedUrl, Instant.now());
			return new NewsSourceTestResponse(
					normalizedUrl,
					true,
					attempt.articles().size(),
					attempt.resolvedFeedUrl(),
					"Source returned " + attempt.articles().size() + " article(s).");
		}
		newsSourceService.recordCollectionFailure(normalizedUrl, attempt.message(), Instant.now());
		return new NewsSourceTestResponse(normalizedUrl, false, 0, "", attempt.message());
	}

	List<String> feedsToCollect() {
		var feeds = new LinkedHashSet<String>();
		var configuredFeeds = properties.feeds() == null ? List.<String>of() : properties.feeds();
		for (String feed : configuredFeeds) {
			addFeed(feeds, feed);
		}
		for (String feed : newsSourceService.enabledRssUrls()) {
			addFeed(feeds, feed);
		}
		return List.copyOf(feeds);
	}

	private RssCollectionAttempt collectFrom(String feed) {
		try {
			var body = fetcher.apply(feed);
			if (body == null || body.isBlank()) {
				return RssCollectionAttempt.failure("Empty RSS response");
			}
			var directArticles = parse(feed, body);
			if (!directArticles.isEmpty()) {
				return RssCollectionAttempt.success(feed, directArticles);
			}
			return collectFromDiscoveredFeeds(feed, body);
		}
		catch (WebClientException | IllegalArgumentException exception) {
			return RssCollectionAttempt.failure(exception.getMessage());
		}
		catch (DataBufferLimitException exception) {
			return RssCollectionAttempt.failure("response exceeded RSS max-response-size (" + properties.maxResponseSize() + ")");
		}
	}

	private RssCollectionAttempt collectFromDiscoveredFeeds(String pageUrl, String html) {
		var articles = new ArrayList<CollectedArticle>();
		var discoveredFeeds = discovery.discover(pageUrl, html);
		var resolvedFeedUrl = "";
		var lastError = "no RSS/Atom items or discoverable feed found";
		for (int index = 0; index < discoveredFeeds.size() && index < MAX_DISCOVERED_FEEDS; index++) {
			var discoveredFeed = discoveredFeeds.get(index);
			try {
				var xml = fetcher.apply(discoveredFeed);
				if (xml != null && !xml.isBlank()) {
					var discoveredArticles = parser.parse(discoveredFeed, xml);
					if (!discoveredArticles.isEmpty()) {
						if (resolvedFeedUrl.isBlank()) {
							resolvedFeedUrl = discoveredFeed;
						}
						articles.addAll(discoveredArticles);
					}
					else {
						lastError = "discovered feed returned no RSS/Atom items: " + discoveredFeed;
					}
				}
			}
			catch (RssParsingException | WebClientException | IllegalArgumentException exception) {
				lastError = exception.getMessage();
				log.warn("Skipping discovered RSS feed {} from {}: {}", discoveredFeed, pageUrl, exception.getMessage());
			}
			catch (DataBufferLimitException exception) {
				lastError = "response exceeded RSS max-response-size (" + properties.maxResponseSize() + ")";
				log.warn("Skipping discovered RSS feed {} from {}: response exceeded RSS max-response-size ({})",
						discoveredFeed,
						pageUrl,
						properties.maxResponseSize());
			}
		}
		if (!articles.isEmpty()) {
			return RssCollectionAttempt.success(resolvedFeedUrl, articles);
		}
		return RssCollectionAttempt.failure(lastError);
	}

	private List<CollectedArticle> parse(String sourceName, String body) {
		try {
			return parser.parse(sourceName, body);
		}
		catch (RssParsingException exception) {
			return List.of();
		}
	}

	String fetch(String url) {
		return webClient.get()
				.uri(url)
				.retrieve()
				.bodyToMono(String.class)
				.block(Duration.ofSeconds(15));
	}

	private static void addFeed(LinkedHashSet<String> feeds, String feed) {
		if (feed == null || feed.isBlank()) {
			return;
		}
		try {
			feeds.add(NewsSourceService.normalizePublicHttpUrl(feed));
		}
		catch (IllegalArgumentException exception) {
			log.warn("Skipping invalid RSS source {}: {}", feed, exception.getMessage());
		}
	}

	private record RssCollectionAttempt(List<CollectedArticle> articles, String resolvedFeedUrl, String message) {

		static RssCollectionAttempt success(String resolvedFeedUrl, List<CollectedArticle> articles) {
			return new RssCollectionAttempt(List.copyOf(articles), resolvedFeedUrl, "");
		}

		static RssCollectionAttempt failure(String message) {
			return new RssCollectionAttempt(List.of(), "", message == null || message.isBlank() ? "RSS collection failed" : message);
		}

		boolean success() {
			return !articles.isEmpty();
		}
	}
}
