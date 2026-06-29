package com.hermesnews.news;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Component
public class HackerNewsApiClient implements HackerNewsClient {

	private static final Logger log = LoggerFactory.getLogger(HackerNewsApiClient.class);
	private static final ParameterizedTypeReference<List<Long>> LONG_LIST = new ParameterizedTypeReference<>() {
	};

	private final WebClient webClient;

	public HackerNewsApiClient(WebClient.Builder builder, HackerNewsProperties properties) {
		this.webClient = builder.baseUrl(properties.baseUrl()).build();
	}

	@Override
	public List<Long> topStories() {
		try {
			var stories = webClient.get()
					.uri("/topstories.json")
					.retrieve()
					.bodyToMono(LONG_LIST)
					.block(Duration.ofSeconds(10));
			return stories == null ? List.of() : stories;
		}
		catch (WebClientException exception) {
			log.warn("Could not fetch Hacker News top stories: {}", exception.getMessage());
			return List.of();
		}
	}

	@Override
	public Optional<HackerNewsItem> item(long id) {
		try {
			return Optional.ofNullable(webClient.get()
					.uri("/item/{id}.json", id)
					.retrieve()
					.bodyToMono(HackerNewsItem.class)
					.block(Duration.ofSeconds(10)));
		}
		catch (WebClientException exception) {
			log.warn("Could not fetch Hacker News item {}: {}", id, exception.getMessage());
			return Optional.empty();
		}
	}
}
