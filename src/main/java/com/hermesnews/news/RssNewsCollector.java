package com.hermesnews.news;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
		this.webClient = builder.build();
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
			articles.addAll(collectFrom(feed));
		}
		return articles;
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

	private List<CollectedArticle> collectFrom(String feed) {
		try {
			var body = fetcher.apply(feed);
			if (body == null || body.isBlank()) {
				return List.of();
			}
			var directArticles = parse(feed, body);
			if (!directArticles.isEmpty()) {
				return directArticles;
			}
			return collectFromDiscoveredFeeds(feed, body);
		}
		catch (WebClientException | IllegalArgumentException exception) {
			log.warn("Skipping RSS feed {}: {}", feed, exception.getMessage());
			return List.of();
		}
	}

	private List<CollectedArticle> collectFromDiscoveredFeeds(String pageUrl, String html) {
		var articles = new ArrayList<CollectedArticle>();
		var discoveredFeeds = discovery.discover(pageUrl, html);
		for (int index = 0; index < discoveredFeeds.size() && index < MAX_DISCOVERED_FEEDS; index++) {
			var discoveredFeed = discoveredFeeds.get(index);
			try {
				var xml = fetcher.apply(discoveredFeed);
				if (xml != null && !xml.isBlank()) {
					articles.addAll(parser.parse(discoveredFeed, xml));
				}
			}
			catch (RssParsingException | WebClientException | IllegalArgumentException exception) {
				log.warn("Skipping discovered RSS feed {} from {}: {}", discoveredFeed, pageUrl, exception.getMessage());
			}
		}
		if (articles.isEmpty()) {
			log.warn("Skipping RSS feed {}: no RSS/Atom items or discoverable feed found", pageUrl);
		}
		return articles;
	}

	private List<CollectedArticle> parse(String sourceName, String body) {
		try {
			return parser.parse(sourceName, body);
		}
		catch (RssParsingException exception) {
			return List.of();
		}
	}

	private String fetch(String url) {
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
}
