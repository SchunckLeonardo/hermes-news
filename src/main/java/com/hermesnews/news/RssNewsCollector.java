package com.hermesnews.news;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Component
public class RssNewsCollector implements NewsCollector {

	private static final Logger log = LoggerFactory.getLogger(RssNewsCollector.class);

	private final WebClient webClient;
	private final RssFeedParser parser;
	private final RssProperties properties;

	public RssNewsCollector(WebClient.Builder builder, RssFeedParser parser, RssProperties properties) {
		this.webClient = builder.build();
		this.parser = parser;
		this.properties = properties;
	}

	@Override
	public List<CollectedArticle> collect() {
		var feeds = properties.feeds() == null ? List.<String>of() : properties.feeds();
		var articles = new ArrayList<CollectedArticle>();
		for (String feed : feeds) {
			if (feed == null || feed.isBlank()) {
				continue;
			}
			try {
				var xml = webClient.get()
						.uri(feed.trim())
						.retrieve()
						.bodyToMono(String.class)
						.block(Duration.ofSeconds(15));
				if (xml != null && !xml.isBlank()) {
					articles.addAll(parser.parse(feed.trim(), xml));
				}
			}
			catch (RssParsingException | WebClientException exception) {
				log.warn("Skipping RSS feed {}: {}", feed, exception.getMessage());
			}
		}
		return articles;
	}
}
